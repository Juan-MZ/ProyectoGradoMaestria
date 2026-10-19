package co.javeriana.msw.arquitecta.asistente;

import co.javeriana.msw.arquitecta.asistente.ClienteModelo.ErrorModelo;
import co.javeriana.msw.arquitecta.interfaz.BarraDesplazamiento;
import co.javeriana.msw.arquitecta.interfaz.Boton;
import co.javeriana.msw.arquitecta.interfaz.Chip;
import co.javeriana.msw.arquitecta.interfaz.Tarjeta;
import co.javeriana.msw.arquitecta.interfaz.Tema;
import co.javeriana.msw.arquitecta.interfaz.TextoAjustado;
import co.javeriana.msw.arquitecta.motor.Resultado;
import co.javeriana.msw.arquitecta.reglas.Violacion;
import co.javeriana.msw.arquitecta.servicio.Asistencia;
import co.javeriana.msw.arquitecta.servicio.ErrorAnalisis;
import co.javeriana.msw.arquitecta.servicio.Perfil;
import co.javeriana.msw.arquitecta.servicio.ResultadoAnalisis;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Ventana del asistente de catálogo: conversa con un modelo de lenguaje para
 * redactar el catálogo de reglas de un proyecto que no lo tiene.
 *
 * <p>La persona configura el modelo, ve exactamente qué se enviará, pide una
 * propuesta, la discute, la corrige a mano y la previsualiza contra su propio
 * código antes de adoptarla. Nada se aplica sin que ella lo guarde.</p>
 */
public final class VentanaAsistente extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final int ANCHO_LATERAL = 340;

    private final Path proyecto;
    private final Consumer<Path> usar;
    private Perfil perfil;
    private Conversacion conversacion;
    private ResultadoAnalisis vistaPrevia;
    private boolean vistaPreviaVigente;
    private final List<String> transcripcion = new ArrayList<>();

    private final JComboBox<Proveedor> proveedor = new JComboBox<>(Proveedor.values());
    private final JTextField url = new JTextField();
    private final JComboBox<String> modelo = new JComboBox<>();
    private final JPasswordField clave = new JPasswordField();
    private final JCheckBox recordarClave = new JCheckBox("Recordar la clave en este equipo");
    private final TextoAjustado estadoConexion = texto("", Tema.TEXTO_SUAVE);
    private final TextoAjustado privacidad = texto("", Tema.TEXTO_SUAVE);

    private final JEditorPane conversacionVista = new JEditorPane("text/html", "");
    private final JTextArea entrada = new JTextArea(3, 20);
    private final Boton proponer = new Boton("Proponer catálogo", Boton.Estilo.PRIMARIO);
    private final Boton enviar = new Boton("Enviar", Boton.Estilo.SECUNDARIO);

    private final Chip estadoCatalogo = new Chip(12f);
    private final TextoAjustado detalleCatalogo = new TextoAjustado("", Tema.texto(12f), Tema.TEXTO_SUAVE, 480);
    private final JTextArea editor = new JTextArea();
    private final Chip estadoVista = new Chip(12f);
    private final JLabel crVista = new JLabel(" ");
    private final JTextArea hallazgosVista = new JTextArea(5, 20);
    private final Boton previsualizar = new Boton("Previsualizar", Boton.Estilo.SECUNDARIO);
    private final Boton guardar = new Boton("Guardar y usar…", Boton.Estilo.PRIMARIO);

    private final JLabel mensaje = new JLabel(" ");
    private final JProgressBar progreso = new JProgressBar();
    private boolean ocupado;
    private boolean cargandoConfiguracion;

    private VentanaAsistente(Frame padre, Path proyecto, Consumer<Path> usar) {
        super(padre, "Asistente de catálogo · Arquitecta", false);
        this.proyecto = proyecto;
        this.usar = usar;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 680));
        setSize(1260, 820);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.FONDO);
        raiz.add(construirEncabezado(), BorderLayout.NORTH);
        raiz.add(construirLateral(), BorderLayout.WEST);
        raiz.add(construirCentro(), BorderLayout.CENTER);
        raiz.add(construirEstado(), BorderLayout.SOUTH);
        setContentPane(raiz);

        cargarConfiguracion(ConfiguracionModelo.cargar());
        mostrarCatalogo(null);
        actualizarControles();
        agregarNota("Pida una propuesta: el asistente leerá la estructura del proyecto y sugerirá capas,"
                + " dependencias permitidas, umbral y exenciones, justificando cada decisión. Antes puede"
                + " contarle cómo está organizado su sistema en el cuadro de abajo.");
        cargarPerfil();
    }

    /**
     * Abre el asistente para un proyecto.
     *
     * @param usar recibe la ruta del catálogo cuando la persona lo guarda.
     */
    public static void abrir(Frame padre, Path proyecto, Path catalogoActual, Consumer<Path> usar) {
        VentanaAsistente ventana = new VentanaAsistente(padre, proyecto, usar);
        if (catalogoActual != null && Files.isRegularFile(catalogoActual)) {
            try {
                ventana.editor.setText(Files.readString(catalogoActual, StandardCharsets.ISO_8859_1));
                ventana.editor.setCaretPosition(0);
                ventana.agregarNota("Se cargó el catálogo actual (" + catalogoActual.getFileName()
                        + "): puede pedir al asistente que lo revise o lo complete.");
            } catch (IOException e) {
                // Sin el catálogo actual se empieza de cero.
            }
        }
        ventana.setLocationRelativeTo(padre);
        ventana.setVisible(true);
    }

    // =========================================================== construcción

    private JComponent construirEncabezado() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Tema.SUPERFICIE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE), new EmptyBorder(14, 20, 12, 20)));
        JLabel titulo = new JLabel("Asistente de catálogo");
        titulo.setFont(Tema.negrita(18f));
        titulo.setForeground(Tema.TEXTO);
        Path nombre = proyecto.getFileName();
        JLabel subtitulo = new JLabel("Ayuda a declarar la arquitectura prevista de «"
                + (nombre == null ? proyecto : nombre) + "». Las tres Fitness Functions son las de la herramienta:"
                + " el asistente solo propone sus parámetros, y usted decide qué se adopta.");
        subtitulo.setFont(Tema.texto(12.5f));
        subtitulo.setForeground(Tema.TEXTO_SUAVE);
        p.add(titulo);
        p.add(Box.createVerticalStrut(3));
        p.add(subtitulo);
        return p;
    }

    private JComponent construirLateral() {
        JPanel lateral = new JPanel();
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setBackground(Tema.LATERAL);
        lateral.setBorder(new EmptyBorder(16, 16, 16, 16));

        Tarjeta modeloTarjeta = tarjeta();
        modeloTarjeta.add(subtitulo("MODELO DE LENGUAJE"));
        modeloTarjeta.add(Box.createVerticalStrut(8));
        modeloTarjeta.add(campo("Proveedor", proveedor));
        modeloTarjeta.add(campo("Dirección del servicio", url));
        modelo.setEditable(true);
        if (modelo.getEditor().getEditorComponent() instanceof JTextField editorModelo) {
            editorModelo.setForeground(Tema.TEXTO);
            editorModelo.setBackground(Tema.SUPERFICIE);
            editorModelo.setFont(Tema.texto(13f));
            editorModelo.setBorder(new EmptyBorder(3, 6, 3, 6));
        }
        proveedor.setBackground(Tema.SUPERFICIE);
        Boton probar = new Boton("Probar conexión", Boton.Estilo.DISCRETO);
        probar.addActionListener(e -> probarConexion());
        modeloTarjeta.add(campo("Modelo", modelo));
        modeloTarjeta.add(campo("Clave de API", clave));
        recordarClave.setOpaque(false);
        recordarClave.setFont(Tema.texto(12f));
        recordarClave.setForeground(Tema.TEXTO_SUAVE);
        recordarClave.setAlignmentX(Component.LEFT_ALIGNMENT);
        recordarClave.setToolTipText("Se guarda en " + ConfiguracionModelo.ARCHIVO
                + ", en su carpeta de usuario, nunca en la del proyecto");
        modeloTarjeta.add(recordarClave);
        modeloTarjeta.add(Box.createVerticalStrut(6));
        modeloTarjeta.add(fila(probar));
        modeloTarjeta.add(Box.createVerticalStrut(4));
        modeloTarjeta.add(estadoConexion);
        lateral.add(modeloTarjeta);
        lateral.add(Box.createVerticalStrut(12));

        Tarjeta privacidadTarjeta = tarjeta();
        privacidadTarjeta.add(subtitulo("QUÉ SALE DE ESTE EQUIPO"));
        privacidadTarjeta.add(Box.createVerticalStrut(8));
        privacidadTarjeta.add(privacidad);
        privacidadTarjeta.add(Box.createVerticalStrut(8));
        Boton ver = new Boton("Ver el perfil que se envía", Boton.Estilo.DISCRETO);
        ver.addActionListener(e -> verPerfil());
        privacidadTarjeta.add(fila(ver));
        lateral.add(privacidadTarjeta);
        lateral.add(Box.createVerticalGlue());

        proveedor.addActionListener(e -> {
            if (!cargandoConfiguracion) {
                cambiarProveedor((Proveedor) proveedor.getSelectedItem());
            }
        });

        JScrollPane desplazable = new JScrollPane(lateral, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        desplazable.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Tema.BORDE));
        BarraDesplazamiento.aplicar(desplazable, Tema.LATERAL);
        desplazable.setPreferredSize(new Dimension(ANCHO_LATERAL, 400));
        return desplazable;
    }

    private JComponent construirCentro() {
        // Conversación
        JPanel izquierda = new JPanel(new BorderLayout(0, 8));
        izquierda.setOpaque(false);
        izquierda.setBorder(new EmptyBorder(14, 14, 14, 7));
        izquierda.add(subtitulo("CONVERSACIÓN"), BorderLayout.NORTH);
        conversacionVista.setEditable(false);
        conversacionVista.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        conversacionVista.setFont(Tema.texto(13f));
        conversacionVista.setBackground(Tema.SUPERFICIE);
        conversacionVista.setBorder(new EmptyBorder(10, 12, 10, 12));
        JScrollPane desplazableConversacion = new JScrollPane(conversacionVista);
        desplazableConversacion.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        BarraDesplazamiento.aplicar(desplazableConversacion, Tema.SUPERFICIE);
        izquierda.add(desplazableConversacion, BorderLayout.CENTER);

        JPanel redactar = new JPanel(new BorderLayout(0, 6));
        redactar.setOpaque(false);
        entrada.setLineWrap(true);
        entrada.setWrapStyleWord(true);
        entrada.setFont(Tema.texto(13f));
        entrada.setForeground(Tema.TEXTO);
        entrada.setBackground(Tema.SUPERFICIE);
        entrada.setCaretColor(Tema.TEXTO);
        entrada.setBorder(new EmptyBorder(6, 8, 6, 8));
        entrada.setToolTipText("Cuéntele cómo está organizado su sistema, responda sus preguntas o pídale cambios");
        JScrollPane desplazableEntrada = new JScrollPane(entrada);
        desplazableEntrada.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        BarraDesplazamiento.aplicar(desplazableEntrada, Tema.SUPERFICIE);
        redactar.add(desplazableEntrada, BorderLayout.CENTER);
        proponer.addActionListener(e -> proponer());
        enviar.addActionListener(e -> enviarMensaje());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(enviar);
        botones.add(proponer);
        redactar.add(botones, BorderLayout.SOUTH);
        izquierda.add(redactar, BorderLayout.SOUTH);

        // Catálogo propuesto
        JPanel derecha = new JPanel(new BorderLayout(0, 8));
        derecha.setOpaque(false);
        derecha.setBorder(new EmptyBorder(14, 7, 14, 14));
        JPanel cabeceraCatalogo = new JPanel();
        cabeceraCatalogo.setOpaque(false);
        cabeceraCatalogo.setLayout(new BoxLayout(cabeceraCatalogo, BoxLayout.Y_AXIS));
        JPanel tituloCatalogo = new JPanel(new BorderLayout());
        tituloCatalogo.setOpaque(false);
        tituloCatalogo.add(subtitulo("CATÁLOGO PROPUESTO · EDITABLE"), BorderLayout.WEST);
        tituloCatalogo.add(estadoCatalogo, BorderLayout.EAST);
        tituloCatalogo.setAlignmentX(Component.LEFT_ALIGNMENT);
        cabeceraCatalogo.add(tituloCatalogo);
        cabeceraCatalogo.add(Box.createVerticalStrut(4));
        detalleCatalogo.setAlignmentX(Component.LEFT_ALIGNMENT);
        cabeceraCatalogo.add(detalleCatalogo);
        derecha.add(cabeceraCatalogo, BorderLayout.NORTH);

        editor.setFont(Tema.codigo(12.5f));
        editor.setForeground(Tema.TEXTO);
        editor.setBackground(Tema.SUPERFICIE);
        editor.setCaretColor(Tema.TEXTO);
        editor.setBorder(new EmptyBorder(8, 10, 8, 10));
        editor.setTabSize(4);
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                alEditar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                alEditar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                alEditar();
            }
        });
        JScrollPane desplazableEditor = new JScrollPane(editor);
        desplazableEditor.setBorder(BorderFactory.createLineBorder(Tema.BORDE));
        BarraDesplazamiento.aplicar(desplazableEditor, Tema.SUPERFICIE);
        derecha.add(desplazableEditor, BorderLayout.CENTER);

        Tarjeta vista = tarjeta();
        JPanel tituloVista = new JPanel(new BorderLayout());
        tituloVista.setOpaque(false);
        tituloVista.add(subtitulo("PREVISUALIZACIÓN SOBRE SU CÓDIGO"), BorderLayout.WEST);
        tituloVista.add(estadoVista, BorderLayout.EAST);
        tituloVista.setAlignmentX(Component.LEFT_ALIGNMENT);
        tituloVista.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        vista.add(tituloVista);
        vista.add(Box.createVerticalStrut(4));
        crVista.setFont(Tema.negrita(15f));
        crVista.setForeground(Tema.TEXTO);
        crVista.setAlignmentX(Component.LEFT_ALIGNMENT);
        vista.add(crVista);
        vista.add(Box.createVerticalStrut(4));
        hallazgosVista.setEditable(false);
        hallazgosVista.setLineWrap(true);
        hallazgosVista.setWrapStyleWord(true);
        hallazgosVista.setFont(Tema.texto(12f));
        hallazgosVista.setForeground(Tema.TEXTO_SUAVE);
        hallazgosVista.setBackground(Tema.SUPERFICIE);
        JScrollPane desplazableHallazgos = new JScrollPane(hallazgosVista);
        desplazableHallazgos.setBorder(BorderFactory.createEmptyBorder());
        BarraDesplazamiento.aplicar(desplazableHallazgos, Tema.SUPERFICIE);
        desplazableHallazgos.setAlignmentX(Component.LEFT_ALIGNMENT);
        desplazableHallazgos.setPreferredSize(new Dimension(200, 92));
        desplazableHallazgos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        vista.add(desplazableHallazgos);
        vista.add(Box.createVerticalStrut(8));
        previsualizar.addActionListener(e -> previsualizar());
        guardar.addActionListener(e -> guardar());
        vista.add(fila(previsualizar, guardar));
        derecha.add(vista, BorderLayout.SOUTH);
        limpiarVistaPrevia("Previsualice el catálogo para ver qué diagnóstico produciría sobre su código.");

        JSplitPane division = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, derecha);
        division.setResizeWeight(0.5);
        division.setBorder(BorderFactory.createEmptyBorder());
        division.setOpaque(false);
        division.setDividerSize(6);
        return division;
    }

    private JComponent construirEstado() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Tema.SUPERFICIE);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE), new EmptyBorder(6, 14, 6, 14)));
        mensaje.setFont(Tema.texto(12f));
        mensaje.setForeground(Tema.TEXTO_SUAVE);
        barra.add(mensaje, BorderLayout.CENTER);
        progreso.setIndeterminate(true);
        progreso.setVisible(false);
        progreso.setPreferredSize(new Dimension(160, 8));
        progreso.setBorderPainted(false);
        barra.add(progreso, BorderLayout.EAST);
        return barra;
    }

    // =========================================================== configuración

    private void cargarConfiguracion(ConfiguracionModelo c) {
        cargandoConfiguracion = true;
        proveedor.setSelectedItem(c.proveedor());
        url.setText(c.url());
        modelo.removeAllItems();
        if (!c.modelo().isEmpty()) {
            modelo.addItem(c.modelo());
        }
        modelo.setSelectedItem(c.modelo());
        clave.setText(c.clave());
        recordarClave.setSelected(!c.clave().isEmpty());
        cargandoConfiguracion = false;
        actualizarPrivacidad();
    }

    private void cambiarProveedor(Proveedor p) {
        url.setText(p.urlPorDefecto());
        modelo.removeAllItems();
        if (!p.modeloPorDefecto().isEmpty()) {
            modelo.addItem(p.modeloPorDefecto());
        }
        modelo.setSelectedItem(p.modeloPorDefecto());
        clave.setText("");
        estadoConexion.mostrar(p.local()
                ? "Pulse «Probar conexión» para listar los modelos instalados."
                : p.variableEntorno() != null && System.getenv(p.variableEntorno()) != null
                        ? "Se usará la clave de la variable " + p.variableEntorno() + " si no indica otra."
                        : "", Tema.TEXTO_SUAVE);
        actualizarPrivacidad();
    }

    private ConfiguracionModelo configuracion() {
        Object m = modelo.getEditor().getItem();
        return new ConfiguracionModelo((Proveedor) proveedor.getSelectedItem(), url.getText(),
                m == null ? "" : m.toString(), new String(clave.getPassword()));
    }

    private void guardarConfiguracion() {
        try {
            configuracion().guardar(recordarClave.isSelected());
        } catch (IOException e) {
            // La configuración se pedirá de nuevo; el trabajo no se pierde.
        }
    }

    private void actualizarPrivacidad() {
        Proveedor p = (Proveedor) proveedor.getSelectedItem();
        if (p != null && p.local()) {
            privacidad.mostrar("El modelo se ejecuta en este equipo: el perfil del proyecto no sale de él.",
                    Tema.EXITO);
        } else {
            privacidad.mostrar("Al consultar al asistente se envía a " + (p == null ? "el servicio" : p.etiqueta())
                    + " el perfil estructural del proyecto: nombres de paquetes, número de clases, hasta "
                    + Perfil.EJEMPLOS_POR_PAQUETE + " nombres de clase por paquete y las dependencias entre"
                    + " paquetes. Nunca el código fuente. Si su organización no lo permite, use un modelo"
                    + " local (Ollama o LM Studio).", Tema.TEXTO_SUAVE);
        }
    }

    private void probarConexion() {
        ConfiguracionModelo c = configuracion();
        tarea("Probando la conexión con " + c.proveedor().etiqueta() + "…",
                () -> new ClienteModelo(c).modelos(),
                modelos -> {
                    Object actual = modelo.getEditor().getItem();
                    modelo.removeAllItems();
                    modelos.forEach(modelo::addItem);
                    String elegido = actual != null && !actual.toString().isBlank() ? actual.toString()
                            : modelos.isEmpty() ? "" : modelos.get(0);
                    modelo.setSelectedItem(elegido);
                    estadoConexion.mostrar(modelos.isEmpty()
                            ? "Conexión correcta, pero el servicio no tiene modelos disponibles."
                                    + (c.proveedor() == Proveedor.OLLAMA ? " Instale uno, por ejemplo: ollama pull llama3.1" : "")
                            : "● Conexión correcta · " + modelos.size() + (modelos.size() == 1 ? " modelo" : " modelos")
                                    + " disponibles", modelos.isEmpty() ? Tema.AVISO : Tema.EXITO);
                    guardarConfiguracion();
                    mostrarMensaje("Conexión verificada.");
                });
    }

    // =========================================================== conversación

    private void cargarPerfil() {
        tarea("Leyendo la estructura del proyecto…", () -> Asistencia.perfil(proyecto), p -> {
            perfil = p;
            mostrarMensaje("Proyecto leído: " + p.paquetes().size() + " paquetes y " + p.dependencias().size()
                    + " dependencias bajo " + p.raiz() + ".");
            actualizarControles();
        });
    }

    private void proponer() {
        if (!listoParaConsultar()) {
            return;
        }
        String contexto = entrada.getText().strip();
        ConfiguracionModelo c = configuracion();
        conversacion = new Conversacion(new ClienteModelo(c), perfil.describir(), Asistencia.descripcionReglas());
        if (!contexto.isEmpty()) {
            agregarPersona(contexto);
        } else {
            agregarPersona("Proponga un catálogo para este proyecto.");
        }
        entrada.setText("");
        Conversacion actual = conversacion;
        tarea("El asistente está analizando la estructura del proyecto…", () -> actual.proponer(contexto),
                this::aplicarRespuesta);
    }

    private void enviarMensaje() {
        String texto = entrada.getText().strip();
        if (texto.isEmpty() || !listoParaConsultar()) {
            return;
        }
        if (conversacion == null) {
            conversacion = new Conversacion(new ClienteModelo(configuracion()), perfil.describir(),
                    Asistencia.descripcionReglas());
        }
        agregarPersona(texto);
        entrada.setText("");
        String catalogo = editor.getText();
        String vista = vistaPreviaVigente && vistaPrevia != null ? resumenVistaPrevia(vistaPrevia) : null;
        Conversacion actual = conversacion;
        tarea("El asistente está respondiendo…", () -> actual.responder(texto, catalogo, vista), this::aplicarRespuesta);
    }

    private boolean listoParaConsultar() {
        if (perfil == null) {
            mostrarMensaje("Todavía se está leyendo la estructura del proyecto.");
            return false;
        }
        String incompleta = configuracion().incompleta();
        if (incompleta != null) {
            estadoConexion.mostrar("● " + incompleta, Tema.ALERTA);
            return false;
        }
        guardarConfiguracion();
        return true;
    }

    private void aplicarRespuesta(Conversacion.Respuesta r) {
        agregarAsistente(r.explicacion(), r.propuesta());
        if (r.propuesta() != null) {
            editor.setText(r.propuesta().aCatalogo(perfil.nombre(), configuracion().modelo()));
            editor.setCaretPosition(0);
            if (Asistencia.validar(editor.getText()).isEmpty()) {
                previsualizar();
            }
        } else if (r.errorFormato() != null) {
            agregarNota("La respuesta no traía un catálogo legible (" + r.errorFormato()
                    + "). Puede pedirle que lo repita.");
        }
        mostrarMensaje("Respuesta recibida.");
    }

    // =========================================================== catálogo

    private void alEditar() {
        vistaPreviaVigente = false;
        mostrarCatalogo(editor.getText());
        if (vistaPrevia != null) {
            estadoVista.mostrar("Desactualizada", Tema.TEXTO_TENUE, Tema.NUCLEO_FONDO);
        }
        actualizarControles();
    }

    private void mostrarCatalogo(String texto) {
        if (texto == null || texto.isBlank()) {
            estadoCatalogo.mostrar("Sin catálogo", Tema.TEXTO_TENUE, Tema.NUCLEO_FONDO);
            detalleCatalogo.mostrar("La propuesta aparecerá aquí. Puede editarla libremente antes de guardarla.",
                    Tema.TEXTO_TENUE);
            return;
        }
        Optional<String> error = Asistencia.validar(texto);
        if (error.isPresent()) {
            estadoCatalogo.mostrar("No válido", Tema.ALERTA, Tema.ALERTA_FONDO);
            detalleCatalogo.mostrar(error.get(), Tema.ALERTA);
        } else {
            estadoCatalogo.mostrar("Válido", Tema.EXITO, Tema.EXITO_FONDO);
            detalleCatalogo.mostrar("El cargador de la herramienta lo acepta. Previsualícelo para ver el diagnóstico.",
                    Tema.TEXTO_SUAVE);
        }
    }

    private void previsualizar() {
        String texto = editor.getText();
        if (Asistencia.validar(texto).isPresent()) {
            return;
        }
        tarea("Analizando el proyecto con el catálogo propuesto…", () -> Asistencia.previsualizar(proyecto, texto),
                r -> {
                    vistaPrevia = r;
                    vistaPreviaVigente = editor.getText().equals(texto);
                    mostrarVistaPrevia(r);
                    mostrarMensaje("Previsualización lista.");
                });
    }

    private void mostrarVistaPrevia(ResultadoAnalisis r) {
        Resultado res = r.resultado();
        estadoVista.mostrar(res.estado().etiqueta(), Tema.colorEstado(res.estado()), Tema.fondoEstado(res.estado()));
        crVista.setText(String.format(ES, "CR %.2f %% · %d %s", res.conformidad(), res.totalViolaciones(),
                res.totalViolaciones() == 1 ? "hallazgo" : "hallazgos"));
        crVista.setForeground(Tema.colorConformidad(res.conformidad()));
        StringBuilder sb = new StringBuilder();
        for (Violacion v : res.violaciones()) {
            sb.append("• [").append(v.severidad().etiqueta()).append("] ").append(v.descripcion()).append('\n');
        }
        hallazgosVista.setText(sb.length() == 0
                ? "Sin hallazgos: el código respeta el catálogo propuesto."
                : sb.toString().strip() + "\n\nSi un hallazgo es deriva real, consérvelo; si delata un catálogo mal"
                        + " declarado, coméntelo con el asistente.");
        hallazgosVista.setCaretPosition(0);
    }

    private void limpiarVistaPrevia(String texto) {
        estadoVista.mostrar("Sin previsualizar", Tema.TEXTO_TENUE, Tema.NUCLEO_FONDO);
        crVista.setText(" ");
        hallazgosVista.setText(texto);
    }

    /** Lo que se comunica al asistente sobre la previsualización, para que interprete los hallazgos. */
    private static String resumenVistaPrevia(ResultadoAnalisis r) {
        Resultado res = r.resultado();
        StringBuilder sb = new StringBuilder(String.format(Locale.ROOT, "CR %.2f %%, estado %s, %d hallazgos.%n",
                res.conformidad(), res.estado().etiqueta(), res.totalViolaciones()));
        for (Violacion v : res.violaciones()) {
            sb.append("- [").append(v.severidad().etiqueta()).append("] ").append(v.regla()).append(": ")
                    .append(v.descripcion()).append('\n');
        }
        return sb.toString();
    }

    private void guardar() {
        String texto = editor.getText();
        Optional<String> error = Asistencia.validar(texto);
        if (error.isPresent()) {
            JOptionPane.showMessageDialog(this, "El catálogo no es válido:\n" + error.get(), "Catálogo no válido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Path carpeta = Files.isDirectory(proyecto) ? proyecto : proyecto.toAbsolutePath().getParent();
        JFileChooser selector = new JFileChooser(carpeta == null ? null : carpeta.toFile());
        selector.setDialogTitle("Guardar el catálogo de reglas");
        selector.setSelectedFile(new File(carpeta == null ? new File(".") : carpeta.toFile(), "arquitecta.properties"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destino = selector.getSelectedFile().toPath();
        if (!destino.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".properties")) {
            destino = destino.resolveSibling(destino.getFileName() + ".properties");
        }
        if (Files.exists(destino) && JOptionPane.showConfirmDialog(this, "Ya existe " + destino.getFileName()
                + ". ¿Desea reemplazarlo?", "Reemplazar catálogo", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            // Latin-1 es la codificación con que Java lee los archivos de propiedades.
            Files.write(destino, texto.getBytes(StandardCharsets.ISO_8859_1));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (usar != null) {
            usar.accept(destino);
        }
        dispose();
    }

    // =========================================================== transcripción

    private void agregarPersona(String texto) {
        transcripcion.add("<div style='margin:4px 0 10px 40px; padding:8px 10px; background:#E8EFFA;'>"
                + "<div style='color:#2F5BA8; font-weight:bold; font-size:8.5pt'>USTED</div>" + html(texto) + "</div>");
        refrescarTranscripcion();
    }

    private void agregarAsistente(String texto, Propuesta propuesta) {
        StringBuilder sb = new StringBuilder("<div style='margin:4px 40px 10px 0; padding:8px 10px; background:#FFFFFF;"
                + " border:1px solid #DCE2EB;'><div style='color:#5B6B82; font-weight:bold; font-size:8.5pt'>ASISTENTE</div>");
        sb.append(html(texto.isBlank() ? "(Sin explicación.)" : texto));
        if (propuesta != null) {
            if (!propuesta.derivasProbables().isEmpty()) {
                sb.append("<div style='margin-top:6px; color:#B7791F; font-weight:bold'>Probable deriva actual</div><ul>");
                propuesta.derivasProbables().forEach(d -> sb.append("<li>").append(html(d)).append("</li>"));
                sb.append("</ul>");
            }
            if (!propuesta.preguntas().isEmpty()) {
                sb.append("<div style='margin-top:6px; color:#2F5BA8; font-weight:bold'>Preguntas para usted</div><ul>");
                propuesta.preguntas().forEach(p -> sb.append("<li>").append(html(p)).append("</li>"));
                sb.append("</ul>");
            }
            sb.append("<div style='margin-top:6px; color:#2E7D4F'>El catálogo propuesto está a la derecha.</div>");
        }
        sb.append("</div>");
        transcripcion.add(sb.toString());
        refrescarTranscripcion();
    }

    private void agregarNota(String texto) {
        transcripcion.add("<div style='margin:2px 0 10px 0; color:#8A97AA'>" + html(texto) + "</div>");
        refrescarTranscripcion();
    }

    private void refrescarTranscripcion() {
        String fuente = Tema.texto(13f).getFamily();
        conversacionVista.setText("<html><head><style>body{font-family:'" + fuente + "';font-size:11.5pt;"
                + "color:#1F2D44;margin:0} ul{margin-top:2px;margin-bottom:2px} li{margin-bottom:2px}"
                + "</style></head><body>" + String.join("", transcripcion) + "</body></html>");
        SwingUtilities.invokeLater(() -> conversacionVista.setCaretPosition(conversacionVista.getDocument().getLength()));
    }

    /**
     * Texto a HTML: escapa, respeta los saltos de línea y admite **negrita** y
     * `código`. El espacio que precede a una etiqueta se fija como no separable,
     * porque el visor HTML de Swing lo descarta.
     */
    private static String html(String texto) {
        String t = texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        t = t.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>")
                .replaceAll("`([^`]+)`", "<font color='#2F5BA8'>$1</font>");
        t = t.replace(" <b>", "&nbsp;<b>").replace(" <font", "&nbsp;<font");
        return t.replace("\n", "<br>");
    }

    private void verPerfil() {
        if (perfil == null) {
            mostrarMensaje("Todavía se está leyendo la estructura del proyecto.");
            return;
        }
        JTextArea area = new JTextArea(perfil.describir());
        area.setEditable(false);
        area.setFont(Tema.codigo(12f));
        area.setCaretPosition(0);
        JScrollPane desplazable = new JScrollPane(area);
        desplazable.setPreferredSize(new Dimension(760, 480));
        JOptionPane.showMessageDialog(this, desplazable, "Perfil estructural que se envía al asistente",
                JOptionPane.PLAIN_MESSAGE);
    }

    // =========================================================== apoyo

    private <T> void tarea(String descripcion, Callable<T> trabajo, Consumer<T> alTerminar) {
        ocupado = true;
        progreso.setVisible(true);
        mostrarMensaje(descripcion);
        actualizarControles();
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return trabajo.call();
            }

            @Override
            protected void done() {
                ocupado = false;
                progreso.setVisible(false);
                try {
                    alTerminar.accept(get());
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    String texto = causa instanceof ErrorModelo || causa instanceof ErrorAnalisis
                            ? causa.getMessage() : String.valueOf(causa);
                    agregarNota("No se pudo completar: " + texto);
                    mostrarMensaje("No se pudo completar la operación.");
                    if (causa instanceof ErrorModelo) {
                        estadoConexion.mostrar("● " + texto, Tema.ALERTA);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                actualizarControles();
            }
        }.execute();
    }

    private void actualizarControles() {
        boolean conPerfil = perfil != null;
        proponer.setEnabled(!ocupado && conPerfil);
        proponer.setText(conversacion == null ? "Proponer catálogo" : "Nueva propuesta");
        enviar.setEnabled(!ocupado && conPerfil);
        boolean valido = !editor.getText().isBlank() && Asistencia.validar(editor.getText()).isEmpty();
        previsualizar.setEnabled(!ocupado && valido);
        guardar.setEnabled(!ocupado && valido);
    }

    private void mostrarMensaje(String texto) {
        mensaje.setText(texto);
    }

    private static Tarjeta tarjeta() {
        Tarjeta t = new Tarjeta(null);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        return t;
    }

    private static JLabel subtitulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Tema.negrita(10.5f));
        l.setForeground(Tema.TEXTO_TENUE);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 0, 0, 8));
        return l;
    }

    private static JComponent campo(String etiqueta, JComponent control) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel l = new JLabel(etiqueta);
        l.setFont(Tema.texto(12f));
        l.setForeground(Tema.TEXTO_SUAVE);
        p.add(l, BorderLayout.NORTH);
        control.setFont(Tema.texto(13f));
        control.setBackground(Tema.SUPERFICIE);
        control.setForeground(Tema.TEXTO);
        if (control instanceof javax.swing.text.JTextComponent t) {
            t.setCaretColor(Tema.TEXTO);
            t.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Tema.BORDE),
                    new EmptyBorder(5, 7, 5, 7)));
        }
        p.add(control, BorderLayout.CENTER);
        p.setBorder(new EmptyBorder(0, 0, 8, 0));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    private static JComponent fila(JComponent... componentes) {
        JPanel p = new JPanel(new GridLayout(1, componentes.length, 8, 0));
        p.setOpaque(false);
        for (JComponent c : componentes) {
            p.add(c);
        }
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height));
        return p;
    }

    private static TextoAjustado texto(String texto, java.awt.Color color) {
        TextoAjustado t = new TextoAjustado(texto, Tema.texto(12f), color, ANCHO_LATERAL - 70);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        return t;
    }
}
