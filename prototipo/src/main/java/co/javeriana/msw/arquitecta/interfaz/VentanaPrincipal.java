package co.javeriana.msw.arquitecta.interfaz;

import co.javeriana.msw.arquitecta.reglas.Violacion;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo;
import co.javeriana.msw.arquitecta.reporte.ExportadorSvg;
import co.javeriana.msw.arquitecta.servicio.Analisis;
import co.javeriana.msw.arquitecta.servicio.ErrorAnalisis;
import co.javeriana.msw.arquitecta.servicio.ResolutorProyecto;
import co.javeriana.msw.arquitecta.servicio.ResultadoAnalisis;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

/**
 * Ventana principal de la interfaz gráfica.
 *
 * <p>El recorrido sigue tres pasos explícitos en la barra lateral ---elegir el
 * proyecto, elegir su catálogo de reglas, elegir el criterio de clasificación---
 * y uno de acción, analizar. El resultado se presenta en tres vistas
 * complementarias: el grafo, para ver dónde está cada problema respecto de la
 * arquitectura; los hallazgos, para recorrerlos uno a uno con su evidencia; y el
 * reporte, idéntico al de la consola, para quien necesite el texto completo.</p>
 */
final class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final int ANCHO_LATERAL = 330;
    /** Ancho útil del texto dentro de las tarjetas de la barra lateral. */
    private static final int ANCHO_TEXTO = ANCHO_LATERAL - 36 - 32 - 4;
    private static final Path HISTORIAL =
            Path.of(System.getProperty("user.home"), ".arquitecta", "historial.csv");

    private final boolean persistir;
    private final Preferences preferencias;

    private Path proyecto;
    private Path catalogo;
    /** El catálogo vigente lo propuso el proyecto, no el usuario: se reemplaza al cambiar de proyecto. */
    private boolean catalogoDelProyecto;
    /** Analizar en cuanto se confirme que el proyecto elegido puede analizarse. */
    private boolean analizarAlDetectar;
    private boolean proyectoValido;
    private ResultadoAnalisis resultado;

    private final JLabel nombreProyecto = etiqueta(Tema.negrita(14f), Tema.TEXTO);
    private final JLabel rutaProyecto = etiqueta(Tema.texto(11.5f), Tema.TEXTO_TENUE);
    private final TextoAjustado deteccion = new TextoAjustado("", Tema.texto(12f), Tema.TEXTO_SUAVE, ANCHO_TEXTO);
    private final JLabel nombreCatalogo = etiqueta(Tema.negrita(14f), Tema.TEXTO);
    private final JLabel rutaCatalogo = etiqueta(Tema.texto(11.5f), Tema.TEXTO_TENUE);
    private final TextoAjustado notaCatalogo = new TextoAjustado("", Tema.texto(11.5f), Tema.TEXTO_TENUE, ANCHO_TEXTO);
    private final JRadioButton conSeveridad = radio("Extensión y gravedad");
    private final JRadioButton soloCr = radio("Solo Tasa de Conformidad");
    private final Boton analizar = new Boton("Analizar arquitectura", Boton.Estilo.PRIMARIO);
    private final Boton exportar = new Boton("Exportar evidencia…", Boton.Estilo.SECUNDARIO);
    private final Boton guardarSvg = new Boton("Guardar grafo SVG…", Boton.Estilo.SECUNDARIO);
    private final Boton quitarCatalogo = new Boton("Quitar", Boton.Estilo.DISCRETO);
    private final Boton botonAsistente = new Boton("Definir con el asistente…", Boton.Estilo.SECUNDARIO);
    private final JComponent filaAsistente = filaBotones(botonAsistente);
    /** Asistente de catálogo, si la aplicación lo incluye. */
    private AsistenteCatalogo asistente;

    private final CardLayout vistas = new CardLayout();
    private final JPanel centro = new JPanel(vistas);
    private final JLabel tituloResultado = etiqueta(Tema.negrita(19f), Tema.TEXTO);
    private final JLabel subtituloResultado = etiqueta(Tema.texto(12.5f), Tema.TEXTO_SUAVE);
    private final JLabel avisoResultado = etiqueta(Tema.texto(12.5f), Tema.AVISO);
    private final PanelResumen resumen = new PanelResumen();
    private final LienzoGrafo lienzo = new LienzoGrafo();
    private final JEditorPane detalle = new JEditorPane("text/html", "");
    private final TablaHallazgos tabla = new TablaHallazgos();
    private final JTextArea evidencia = areaCodigo();
    private final JTextArea reporte = areaCodigo();
    private final Pestanas pestanas = new Pestanas();

    private final JLabel mensaje = etiqueta(Tema.texto(12f), Tema.TEXTO_SUAVE);
    private final JProgressBar progreso = new JProgressBar();

    VentanaPrincipal(boolean persistir) {
        super("Arquitecta · Detección de deriva arquitectónica");
        this.persistir = persistir;
        this.preferencias = persistir ? Preferences.userNodeForPackage(VentanaPrincipal.class) : null;

        setIconImages(Cabecera.iconos());
        setMinimumSize(new Dimension(1100, 720));
        setSize(1400, 880);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Tema.FONDO);
        raiz.add(new Cabecera(), BorderLayout.NORTH);
        raiz.add(construirLateral(), BorderLayout.WEST);
        centro.setOpaque(false);
        centro.add(construirVacio(), "vacio");
        centro.add(construirResultados(), "resultados");
        raiz.add(centro, BorderLayout.CENTER);
        raiz.add(construirBarraEstado(), BorderLayout.SOUTH);
        setContentPane(raiz);

        configurarArrastre();
        configurarAtajos();
        actualizarControles();
        mostrarMensaje("Seleccione un proyecto para comenzar.  Ctrl+O abre el selector; F5 analiza.");

        if (persistir) {
            restaurarPreferencias();
        }
    }

    // =========================================================== construcción

    private JComponent construirLateral() {
        PanelLateral lateral = new PanelLateral();
        lateral.setBorder(new EmptyBorder(18, 18, 14, 18));

        // Paso 1: proyecto
        lateral.add(paso(1, "Proyecto", "Carpeta del proyecto compilado o archivo .jar"));
        lateral.add(Box.createVerticalStrut(8));
        Tarjeta tarjetaProyecto = tarjetaApilada();
        nombreProyecto.setText("Ningún proyecto seleccionado");
        nombreProyecto.setForeground(Tema.TEXTO_TENUE);
        rutaProyecto.setText("Arrastre aquí una carpeta o pulse Examinar.");
        tarjetaProyecto.add(nombreProyecto);
        tarjetaProyecto.add(Box.createVerticalStrut(3));
        tarjetaProyecto.add(rutaProyecto);
        tarjetaProyecto.add(Box.createVerticalStrut(8));
        tarjetaProyecto.add(deteccion);
        tarjetaProyecto.add(Box.createVerticalStrut(10));
        Boton examinarProyecto = new Boton("Examinar…", Boton.Estilo.SECUNDARIO);
        examinarProyecto.addActionListener(e -> elegirProyecto());
        tarjetaProyecto.add(filaBotones(examinarProyecto));
        lateral.add(tarjetaProyecto);
        lateral.add(Box.createVerticalStrut(14));

        // Paso 2: catálogo
        lateral.add(paso(2, "Catálogo de reglas", "Capas, adyacencias, umbrales y exenciones"));
        lateral.add(Box.createVerticalStrut(8));
        Tarjeta tarjetaCatalogo = tarjetaApilada();
        tarjetaCatalogo.add(nombreCatalogo);
        tarjetaCatalogo.add(Box.createVerticalStrut(3));
        tarjetaCatalogo.add(rutaCatalogo);
        tarjetaCatalogo.add(notaCatalogo);
        tarjetaCatalogo.add(Box.createVerticalStrut(10));
        Boton examinarCatalogo = new Boton("Examinar…", Boton.Estilo.SECUNDARIO);
        examinarCatalogo.addActionListener(e -> elegirCatalogo());
        quitarCatalogo.addActionListener(e -> seleccionarCatalogo(null));
        tarjetaCatalogo.add(filaBotones(examinarCatalogo, quitarCatalogo));
        botonAsistente.setToolTipText("Un modelo de lenguaje propone capas, dependencias permitidas, umbral y"
                + " exenciones a partir de la estructura del proyecto; usted revisa y decide");
        botonAsistente.addActionListener(e -> abrirAsistente());
        filaAsistente.setBorder(new EmptyBorder(8, 0, 0, 0));
        filaAsistente.setVisible(false);
        tarjetaCatalogo.add(filaAsistente);
        lateral.add(tarjetaCatalogo);
        mostrarCatalogo();
        lateral.add(Box.createVerticalStrut(14));

        // Paso 3: clasificación
        lateral.add(paso(3, "Clasificación", "Cómo se traduce el resultado en un estado"));
        lateral.add(Box.createVerticalStrut(8));
        Tarjeta tarjetaModo = tarjetaApilada();
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(conSeveridad);
        grupo.add(soloCr);
        conSeveridad.setSelected(true);
        tarjetaModo.add(conSeveridad);
        tarjetaModo.add(nota("Recomendado. Combina la Tasa de Conformidad con la severidad del peor hallazgo:"
                + " una violación aislada no queda diluida."));
        tarjetaModo.add(Box.createVerticalStrut(8));
        tarjetaModo.add(soloCr);
        tarjetaModo.add(nota("Formulación original: el estado depende solo de la CR."));
        conSeveridad.addActionListener(e -> guardarPreferencia("modo", modo()));
        soloCr.addActionListener(e -> guardarPreferencia("modo", modo()));
        lateral.add(tarjetaModo);
        lateral.add(Box.createVerticalStrut(18));

        // Acciones
        analizar.addActionListener(e -> analizar());
        exportar.addActionListener(e -> exportarEvidencia());
        guardarSvg.addActionListener(e -> guardarSvg());
        for (Boton b : List.of(analizar, exportar, guardarSvg)) {
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, b.getPreferredSize().height));
        }
        lateral.add(analizar);
        lateral.add(Box.createVerticalStrut(10));
        JPanel secundarios = new JPanel(new java.awt.GridLayout(1, 2, 8, 0));
        secundarios.setOpaque(false);
        secundarios.setAlignmentX(Component.LEFT_ALIGNMENT);
        secundarios.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        exportar.setText("Exportar…");
        guardarSvg.setText("Guardar SVG…");
        exportar.setToolTipText("Escribe reporte, JSON, CSV y el grafo (SVG, TikZ, DOT) en una carpeta");
        guardarSvg.setToolTipText("Guarda el grafo como imagen SVG, que se abre en cualquier navegador");
        secundarios.add(exportar);
        secundarios.add(guardarSvg);
        lateral.add(secundarios);

        lateral.add(Box.createVerticalStrut(14));
        lateral.add(Box.createVerticalGlue());
        lateral.add(new TextoAjustado("Arquitecta analiza el código compilado: no ejecuta el sistema ni envía nada"
                + " fuera de este equipo.", Tema.texto(11f), Tema.TEXTO_TENUE, ANCHO_LATERAL - 36));

        JScrollPane desplazable = new JScrollPane(lateral, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        desplazable.setBorder(BorderFactory.createEmptyBorder());
        BarraDesplazamiento.aplicar(desplazable, Tema.LATERAL);
        desplazable.setPreferredSize(new Dimension(ANCHO_LATERAL, 600));
        return desplazable;
    }

    private JComponent construirVacio() {
        JPanel vacio = new JPanel(new GridBagLayout());
        vacio.setOpaque(false);
        JPanel bloque = new JPanel();
        bloque.setOpaque(false);
        bloque.setLayout(new BoxLayout(bloque, BoxLayout.Y_AXIS));

        Ilustracion ilustracion = new Ilustracion();
        ilustracion.setAlignmentX(Component.CENTER_ALIGNMENT);
        bloque.add(ilustracion);
        bloque.add(Box.createVerticalStrut(18));
        JLabel titulo = etiqueta(Tema.negrita(22f), Tema.TEXTO);
        titulo.setText("Analice la arquitectura de un proyecto Java");
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        bloque.add(titulo);
        bloque.add(Box.createVerticalStrut(10));
        JLabel texto = etiqueta(Tema.texto(14f), Tema.TEXTO_SUAVE);
        texto.setText("<html><div style='text-align:center; width:520px'>Elija la carpeta del proyecto compilado"
                + " —se reconocen Maven, Gradle, IntelliJ y Eclipse— o directamente su archivo .jar. Si el proyecto"
                + " trae su catálogo de reglas, se carga solo; si no, se evalúan los ciclos y la concentración con"
                + " una configuración mínima.</div></html>");
        texto.setAlignmentX(Component.CENTER_ALIGNMENT);
        bloque.add(texto);
        bloque.add(Box.createVerticalStrut(22));
        Boton seleccionar = new Boton("Seleccionar proyecto…", Boton.Estilo.PRIMARIO);
        seleccionar.setAlignmentX(Component.CENTER_ALIGNMENT);
        seleccionar.setMaximumSize(new Dimension(260, 44));
        seleccionar.addActionListener(e -> elegirProyecto());
        bloque.add(seleccionar);
        bloque.add(Box.createVerticalStrut(12));
        JLabel arrastre = etiqueta(Tema.texto(12.5f), Tema.TEXTO_TENUE);
        arrastre.setText("También puede arrastrar una carpeta o un .jar a esta ventana.");
        arrastre.setAlignmentX(Component.CENTER_ALIGNMENT);
        bloque.add(arrastre);

        List<Ejemplos.Caso> casos = Ejemplos.disponibles();
        if (!casos.isEmpty()) {
            bloque.add(Box.createVerticalStrut(30));
            JLabel invitacion = etiqueta(Tema.negrita(12f), Tema.TEXTO_TENUE);
            invitacion.setText("¿SIN UN PROYECTO A MANO? PRUEBE CON EL CASO DE ESTUDIO");
            // Holgura lateral: con el ancho exacto, el texto se recorta al escalar.
            invitacion.setBorder(new EmptyBorder(0, 12, 0, 12));
            invitacion.setAlignmentX(Component.CENTER_ALIGNMENT);
            bloque.add(invitacion);
            bloque.add(Box.createVerticalStrut(8));
            JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
            botones.setOpaque(false);
            for (Ejemplos.Caso caso : casos) {
                Boton b = new Boton(caso.nombre(), Boton.Estilo.SECUNDARIO);
                b.setToolTipText(caso.descripcion());
                b.addActionListener(e -> probarEjemplo(caso));
                botones.add(b);
            }
            botones.setAlignmentX(Component.CENTER_ALIGNMENT);
            botones.setMaximumSize(botones.getPreferredSize());
            bloque.add(botones);
        }
        vacio.add(bloque);
        return vacio;
    }

    private JComponent construirResultados() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(18, 20, 14, 20));

        JPanel encabezado = new JPanel();
        encabezado.setOpaque(false);
        encabezado.setLayout(new BoxLayout(encabezado, BoxLayout.Y_AXIS));
        tituloResultado.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtituloResultado.setAlignmentX(Component.LEFT_ALIGNMENT);
        avisoResultado.setAlignmentX(Component.LEFT_ALIGNMENT);
        encabezado.add(tituloResultado);
        encabezado.add(Box.createVerticalStrut(3));
        encabezado.add(subtituloResultado);
        encabezado.add(avisoResultado);
        encabezado.add(Box.createVerticalStrut(12));
        resumen.setAlignmentX(Component.LEFT_ALIGNMENT);
        encabezado.add(resumen);
        panel.add(encabezado, BorderLayout.NORTH);

        pestanas.agregar("Grafo", construirPestanaGrafo());
        pestanas.agregar("Hallazgos", construirPestanaHallazgos());
        pestanas.agregar("Reporte", construirPestanaReporte());
        panel.add(pestanas, BorderLayout.CENTER);
        return panel;
    }

    private JComponent construirPestanaGrafo() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Tema.SUPERFICIE);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        barra.setBackground(Tema.SUPERFICIE);
        barra.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE_SUAVE));
        Boton ajustar = new Boton("Ajustar vista", Boton.Estilo.DISCRETO);
        Boton acercar = new Boton("Acercar +", Boton.Estilo.DISCRETO);
        Boton alejar = new Boton("Alejar −", Boton.Estilo.DISCRETO);
        ajustar.addActionListener(e -> lienzo.ajustar());
        acercar.addActionListener(e -> lienzo.acercar(1.25));
        alejar.addActionListener(e -> lienzo.acercar(0.8));
        // En pantallas bajas el resumen le roba altura al grafo: se puede plegar.
        Boton ampliar = new Boton("Ampliar grafo", Boton.Estilo.DISCRETO);
        ampliar.setToolTipText("Oculta los indicadores para dedicar al grafo toda la altura de la ventana");
        ampliar.addActionListener(e -> {
            boolean visible = !resumen.isVisible();
            resumen.setVisible(visible);
            ampliar.setText(visible ? "Ampliar grafo" : "Mostrar resumen");
            ampliar.setToolTipText(visible ? "Oculta los indicadores para dedicar al grafo toda la altura de la ventana"
                    : "Vuelve a mostrar los indicadores del análisis");
            lienzo.ajustar();
        });
        barra.add(ajustar);
        barra.add(acercar);
        barra.add(alejar);
        barra.add(ampliar);
        JLabel ayuda = etiqueta(Tema.texto(12f), Tema.TEXTO_TENUE);
        ayuda.setText("   Rueda: zoom · Arrastrar: mover · Clic: ver un paquete · Doble clic: ajustar");
        ayuda.setBorder(new EmptyBorder(0, 0, 0, 16));
        barra.add(ayuda);
        panel.add(barra, BorderLayout.NORTH);

        panel.add(lienzo, BorderLayout.CENTER);

        detalle.setEditable(false);
        detalle.setOpaque(true);
        detalle.setBackground(Tema.LATERAL);
        detalle.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        detalle.setFont(Tema.texto(13f));
        detalle.setBorder(new EmptyBorder(14, 16, 14, 16));
        JScrollPane desplazable = new JScrollPane(detalle);
        desplazable.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Tema.BORDE_SUAVE));
        BarraDesplazamiento.aplicar(desplazable, Tema.LATERAL);
        desplazable.setPreferredSize(new Dimension(290, 300));
        panel.add(desplazable, BorderLayout.EAST);
        lienzo.alSeleccionar(this::mostrarDetalle);
        mostrarDetalle(null);
        return panel;
    }

    private JComponent construirPestanaHallazgos() {
        JScrollPane superior = new JScrollPane(tabla);
        superior.setBorder(BorderFactory.createEmptyBorder());
        BarraDesplazamiento.aplicar(superior, Tema.SUPERFICIE);

        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setBackground(Tema.SUPERFICIE);
        JLabel titulo = etiqueta(Tema.negrita(12f), Tema.TEXTO_TENUE);
        titulo.setText("EVIDENCIA DEL HALLAZGO SELECCIONADO");
        titulo.setBorder(new EmptyBorder(10, 12, 6, 12));
        inferior.add(titulo, BorderLayout.NORTH);
        JScrollPane desplazable = new JScrollPane(evidencia);
        desplazable.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE_SUAVE));
        BarraDesplazamiento.aplicar(desplazable, Tema.SUPERFICIE);
        inferior.add(desplazable, BorderLayout.CENTER);

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarEvidencia(tabla.seleccionada());
            }
        });

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, superior, inferior);
        division.setResizeWeight(0.55);
        division.setDividerSize(6);
        division.setOpaque(true);
        division.setBackground(Tema.LATERAL);
        division.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void paint(Graphics g) {
                        g.setColor(Tema.LATERAL);
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.setColor(Tema.BORDE_SUAVE);
                        g.fillRect(0, getHeight() / 2, getWidth(), 1);
                    }
                };
            }
        });
        division.setBorder(BorderFactory.createEmptyBorder());
        return division;
    }

    private JComponent construirPestanaReporte() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Tema.SUPERFICIE);
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        barra.setBackground(Tema.SUPERFICIE);
        barra.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDE_SUAVE));
        Boton copiar = new Boton("Copiar reporte", Boton.Estilo.DISCRETO);
        copiar.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(reporte.getText()), null);
            mostrarMensaje("Reporte copiado al portapapeles.");
        });
        barra.add(copiar);
        JLabel ayuda = etiqueta(Tema.texto(12f), Tema.TEXTO_TENUE);
        ayuda.setText("   Idéntico al que emite la línea de comandos.");
        barra.add(ayuda);
        panel.add(barra, BorderLayout.NORTH);
        JScrollPane desplazable = new JScrollPane(reporte);
        desplazable.setBorder(BorderFactory.createEmptyBorder());
        BarraDesplazamiento.aplicar(desplazable, Tema.SUPERFICIE);
        panel.add(desplazable, BorderLayout.CENTER);
        return panel;
    }

    private JComponent construirBarraEstado() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Tema.SUPERFICIE);
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDE), new EmptyBorder(6, 14, 6, 14)));
        barra.add(mensaje, BorderLayout.CENTER);
        progreso.setIndeterminate(true);
        progreso.setVisible(false);
        progreso.setPreferredSize(new Dimension(180, 8));
        progreso.setBorderPainted(false);
        JPanel contenedor = new JPanel(new GridBagLayout());
        contenedor.setOpaque(false);
        contenedor.add(progreso);
        barra.add(contenedor, BorderLayout.EAST);
        return barra;
    }

    // ============================================================== acciones

    void elegirProyecto() {
        JFileChooser selector = new JFileChooser(directorioInicial(proyecto));
        selector.setDialogTitle("Seleccione la carpeta del proyecto o su archivo .jar");
        selector.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        selector.setAcceptAllFileFilterUsed(false);
        selector.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith(".jar");
            }

            @Override
            public String getDescription() {
                return "Carpetas de proyecto y archivos .jar";
            }
        });
        if (selector.showDialog(this, "Seleccionar") == JFileChooser.APPROVE_OPTION) {
            seleccionarProyecto(selector.getSelectedFile().toPath());
        }
    }

    void elegirCatalogo() {
        JFileChooser selector = new JFileChooser(directorioInicial(catalogo != null ? catalogo : proyecto));
        selector.setDialogTitle("Seleccione el catálogo de reglas (.properties)");
        selector.setFileSelectionMode(JFileChooser.FILES_ONLY);
        selector.setAcceptAllFileFilterUsed(true);
        FileFilter propiedades = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase(Locale.ROOT).endsWith(".properties");
            }

            @Override
            public String getDescription() {
                return "Catálogos de reglas (*.properties)";
            }
        };
        selector.addChoosableFileFilter(propiedades);
        selector.setFileFilter(propiedades);
        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            seleccionarCatalogo(selector.getSelectedFile().toPath());
        }
    }

    /** Ofrece en la barra lateral el acceso al asistente de catálogo. */
    void usarAsistente(AsistenteCatalogo asistente) {
        this.asistente = asistente;
        filaAsistente.setVisible(asistente != null);
        actualizarControles();
    }

    private void abrirAsistente() {
        if (asistente == null || proyecto == null) {
            return;
        }
        asistente.abrir(this, proyecto, catalogo, ruta -> {
            seleccionarCatalogo(ruta);
            mostrarMensaje("Se adoptó el catálogo " + ruta.getFileName() + " definido con el asistente.");
            if (proyectoValido) {
                analizar();
            }
        });
    }

    /** Abre un escenario del caso de estudio, con su catálogo, y lo analiza. */
    private void probarEjemplo(Ejemplos.Caso caso) {
        Optional<Path> reglas = ResolutorProyecto.catalogoSugerido(caso.carpeta());
        seleccionarCatalogo(reglas.orElse(null));
        catalogoDelProyecto = true;
        analizarAlDetectar = true;
        seleccionarProyecto(caso.carpeta());
    }

    /** Selecciona el proyecto y comprueba en segundo plano qué se analizará realmente. */
    void seleccionarProyecto(Path ruta) {
        proyecto = ruta.toAbsolutePath().normalize();
        proyectoValido = false;
        Path nombre = proyecto.getFileName();
        nombreProyecto.setText(nombre == null ? proyecto.toString() : nombre.toString());
        nombreProyecto.setForeground(Tema.TEXTO);
        rutaProyecto.setText(abreviar(proyecto.toString()));
        rutaProyecto.setToolTipText(proyecto.toString());
        deteccion.mostrar("Examinando la carpeta…", Tema.TEXTO_SUAVE);
        guardarPreferencia("proyecto", proyecto.toString());
        actualizarControles();

        new SwingWorker<ResolutorProyecto.Origen, Void>() {
            @Override
            protected ResolutorProyecto.Origen doInBackground() throws Exception {
                return ResolutorProyecto.resolver(ruta);
            }

            @Override
            protected void done() {
                if (!ruta.toAbsolutePath().normalize().equals(proyecto)) {
                    return;
                }
                try {
                    aplicarDeteccion(get(), null);
                } catch (ExecutionException e) {
                    aplicarDeteccion(null, e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Variante síncrona, para la captura de pantalla fuera de línea. */
    void seleccionarProyectoAhora(Path ruta) {
        proyecto = ruta.toAbsolutePath().normalize();
        Path nombre = proyecto.getFileName();
        nombreProyecto.setText(nombre == null ? proyecto.toString() : nombre.toString());
        nombreProyecto.setForeground(Tema.TEXTO);
        rutaProyecto.setText(abreviar(proyecto.toString()));
        rutaProyecto.setToolTipText(proyecto.toString());
        try {
            aplicarDeteccion(ResolutorProyecto.resolver(ruta), null);
        } catch (ErrorAnalisis e) {
            aplicarDeteccion(null, e);
        }
    }

    private void aplicarDeteccion(ResolutorProyecto.Origen origen, Throwable error) {
        boolean analizarAhora = analizarAlDetectar && origen != null;
        analizarAlDetectar = false;
        if (origen != null) {
            proyectoValido = true;
            deteccion.mostrar("● " + origen.descripcion(), Tema.EXITO);
            String aviso = "";
            // Un catálogo elegido por el usuario se respeta; uno que propuso el
            // proyecto anterior se sustituye por el que traiga el nuevo, o se retira.
            if (catalogo == null || catalogoDelProyecto) {
                Optional<Path> sugerido = ResolutorProyecto.catalogoSugerido(proyecto);
                seleccionarCatalogo(sugerido.orElse(null));
                catalogoDelProyecto = sugerido.isPresent();
                if (sugerido.isPresent()) {
                    aviso = "  Se cargó el catálogo que trae el proyecto: " + sugerido.get().getFileName() + ".";
                }
            }
            mostrarMensaje("Proyecto listo para analizar." + aviso + "  Pulse «Analizar arquitectura» o F5.");
        } else {
            proyectoValido = false;
            String texto = error == null ? "No se pudo examinar la carpeta." : error.getMessage();
            deteccion.mostrar("● " + texto, Tema.ALERTA);
            mostrarMensaje("El proyecto seleccionado no puede analizarse todavía.");
        }
        actualizarControles();
        if (analizarAhora) {
            analizar();
        }
    }

    void seleccionarCatalogo(Path ruta) {
        catalogoDelProyecto = false;
        catalogo = ruta == null ? null : ruta.toAbsolutePath().normalize();
        guardarPreferencia("catalogo", catalogo == null ? "" : catalogo.toString());
        mostrarCatalogo();
        actualizarControles();
    }

    private void mostrarCatalogo() {
        if (catalogo == null) {
            nombreCatalogo.setText("Configuración mínima");
            rutaCatalogo.setVisible(false);
            notaCatalogo.mostrar("Sin catálogo se infiere la raíz y se evalúan ciclos y concentración. Para"
                    + " verificar las capas, elija un archivo de reglas.", Tema.TEXTO_TENUE);
            notaCatalogo.setVisible(true);
            quitarCatalogo.setVisible(false);
        } else {
            nombreCatalogo.setText(catalogo.getFileName().toString());
            rutaCatalogo.setText(abreviar(catalogo.toString()));
            rutaCatalogo.setToolTipText(catalogo.toString());
            rutaCatalogo.setVisible(true);
            notaCatalogo.setVisible(false);
            quitarCatalogo.setVisible(true);
        }
    }

    void analizar() {
        if (proyecto == null) {
            elegirProyecto();
            return;
        }
        if (!proyectoValido) {
            mostrarError("No se puede analizar el proyecto", deteccionTexto());
            return;
        }
        ocupado(true, "Analizando " + nombreProyecto.getText() + "…");
        Analisis.Solicitud solicitud = new Analisis.Solicitud(proyecto, catalogo, modo(), null,
                persistir ? HISTORIAL : null);
        new SwingWorker<ResultadoAnalisis, Void>() {
            @Override
            protected ResultadoAnalisis doInBackground() throws Exception {
                return Analisis.ejecutar(solicitud);
            }

            @Override
            protected void done() {
                ocupado(false, null);
                try {
                    ResultadoAnalisis r = get();
                    mostrarResultado(r);
                    if (persistir) {
                        try {
                            r.registrarEn(HISTORIAL);
                        } catch (IOException e) {
                            // Sin historial se pierde la tendencia, no el análisis.
                        }
                    }
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    mostrarError("No se pudo analizar el proyecto",
                            causa instanceof ErrorAnalisis ? causa.getMessage() : String.valueOf(causa));
                    mostrarMensaje("El análisis no se completó.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    void mostrarResultado(ResultadoAnalisis r) {
        resultado = r;
        mostrarMensaje(String.format(ES, "Análisis completado en %.1f s · %s · %d hallazgos.",
                r.milisegundos() / 1000.0, r.resultado().estado().etiqueta(), r.resultado().totalViolaciones()));
        tituloResultado.setText(r.etiqueta());
        subtituloResultado.setText(r.origen() + "  ·  raíz " + r.raiz() + "  ·  "
                + (r.configuracionMinima() ? "configuración mínima" : "catálogo " + r.catalogo().getFileName()));
        if (r.configuracionMinima()) {
            avisoResultado.setText("Sin capas declaradas no se verifica la estratificación: elija un catálogo de"
                    + " reglas para evaluarla.");
            avisoResultado.setBorder(new EmptyBorder(6, 0, 0, 0));
            avisoResultado.setVisible(true);
        } else {
            avisoResultado.setVisible(false);
        }
        resumen.mostrar(r);
        lienzo.mostrar(r.disposicion());
        tabla.mostrar(r.resultado().violaciones());
        pestanas.titulo(1, "Hallazgos (" + r.resultado().totalViolaciones() + ")");
        reporte.setText(r.reporte());
        reporte.setCaretPosition(0);
        evidencia.setText(r.resultado().totalViolaciones() == 0
                ? "Sin hallazgos: el sistema cumple todas las reglas evaluadas."
                : "Seleccione un hallazgo para ver su evidencia y resaltarlo en el grafo.");
        mostrarDetalle(null);
        vistas.show(centro, "resultados");
        actualizarControles();
        javax.swing.SwingUtilities.invokeLater(lienzo::ajustar);
    }

    private void mostrarEvidencia(Violacion v) {
        if (v == null) {
            lienzo.resaltar(java.util.Set.of());
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(v.severidad().etiqueta().toUpperCase(ES)).append("] ").append(v.descripcion()).append("\n\n");
        sb.append("Regla      : ").append(v.regla()).append('\n');
        sb.append("Ubicación  : ").append(String.join(", ", v.ubicacion())).append("\n\n");
        sb.append("Evidencia:\n");
        for (String linea : v.evidencia()) {
            sb.append("  · ").append(linea).append('\n');
        }
        evidencia.setText(sb.toString());
        evidencia.setCaretPosition(0);
        lienzo.resaltar(new LinkedHashSet<>(v.ubicacion()));
    }

    /** Ficha del paquete seleccionado en el grafo: capa, acoplamientos, vecinos y hallazgos. */
    private void mostrarDetalle(String paquete) {
        if (resultado == null || paquete == null) {
            detalle.setText(estilo() + "<div class='tenue'>PAQUETE</div><p style='margin-top:6px'>Haga clic en un"
                    + " paquete del grafo para ver su capa, sus acoplamientos, de quién depende, quién depende de él"
                    + " y los hallazgos en que interviene.</p><p class='tenue'>Al seleccionarlo, todo lo que no le"
                    + " concierne se atenúa.</p>");
            return;
        }
        DisposicionGrafo d = resultado.disposicion();
        Map<String, DisposicionGrafo.Nodo> porPaquete = new HashMap<>();
        d.nodos().forEach(n -> porPaquete.put(n.paquete(), n));
        DisposicionGrafo.Nodo n = porPaquete.get(paquete);
        if (n == null) {
            return;
        }
        List<String> salientes = new ArrayList<>();
        List<String> entrantes = new ArrayList<>();
        for (DisposicionGrafo.Arista a : d.aristas()) {
            if (a.origen().paquete().equals(paquete)) {
                salientes.add(marca(a) + html(a.destino().etiqueta()) + aviso(a));
            }
            if (a.destino().paquete().equals(paquete)) {
                entrantes.add(marca(a) + html(a.origen().etiqueta()) + aviso(a));
            }
        }
        StringBuilder sb = new StringBuilder(estilo());
        sb.append("<div class='tenue'>PAQUETE</div>");
        sb.append("<div class='titulo'>").append(html(n.etiqueta())).append("</div>");
        sb.append("<div class='tenue'>").append(html(n.paquete())).append("</div>");
        sb.append("<table cellpadding='2' style='margin-top:10px'>");
        fila(sb, "Capa", n.capa() == null ? "sin capa" : n.capa());
        fila(sb, "Estado", ExportadorSvg.nombreEstado(n.estado()));
        fila(sb, "Ce (salientes)", Integer.toString(n.ce()));
        fila(sb, "Ca (entrantes)", Integer.toString(n.ca()));
        sb.append("</table>");
        lista(sb, "Depende de", salientes);
        lista(sb, "Dependen de él", entrantes);
        List<String> hallazgos = new ArrayList<>();
        for (Violacion v : resultado.resultado().violaciones()) {
            if (v.ubicacion().contains(paquete)) {
                String color = v.severidad() == Violacion.Severidad.CRITICA ? "#C0392B" : "#B7791F";
                hallazgos.add("<span style='color:" + color + "'>&#8226;</span> " + html(v.descripcion()));
            }
        }
        lista(sb, "Hallazgos", hallazgos);
        detalle.setText(sb.toString());
        detalle.setCaretPosition(0);
    }

    void exportarEvidencia() {
        if (resultado == null) {
            return;
        }
        JFileChooser selector = new JFileChooser(directorioInicial(proyecto));
        selector.setDialogTitle("Carpeta donde escribir la evidencia");
        selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (selector.showDialog(this, "Exportar aquí") != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destino = selector.getSelectedFile().toPath();
        ResultadoAnalisis r = resultado;
        ocupado(true, "Exportando evidencia…");
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return r.exportarEvidencia(destino);
            }

            @Override
            protected void done() {
                ocupado(false, null);
                try {
                    String base = get();
                    mostrarMensaje("Evidencia escrita en " + destino);
                    int opcion = JOptionPane.showOptionDialog(VentanaPrincipal.this,
                            "<html>Se escribieron el reporte, el JSON, los hallazgos en CSV y el grafo en SVG, TikZ"
                                    + " y DOT<br>(<b>" + html(base) + ".*</b>) en:<br><br>" + html(destino.toString())
                                    + "</html>",
                            "Evidencia exportada", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                            new Object[] {"Abrir carpeta", "Cerrar"}, "Cerrar");
                    if (opcion == 0) {
                        abrir(destino);
                    }
                } catch (ExecutionException e) {
                    mostrarError("No se pudo exportar la evidencia", e.getCause().getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    void guardarSvg() {
        if (resultado == null) {
            return;
        }
        JFileChooser selector = new JFileChooser(directorioInicial(proyecto));
        selector.setDialogTitle("Guardar el grafo como imagen SVG");
        String base = resultado.etiqueta().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");
        selector.setSelectedFile(new File(selector.getCurrentDirectory(), base + ".svg"));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destino = selector.getSelectedFile().toPath();
        if (!destino.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".svg")) {
            destino = destino.resolveSibling(destino.getFileName() + ".svg");
        }
        try {
            Files.writeString(destino, resultado.svg(), StandardCharsets.UTF_8);
            mostrarMensaje("Grafo guardado en " + destino);
            int opcion = JOptionPane.showOptionDialog(this,
                    "<html>El grafo se guardó en:<br><br>" + html(destino.toString())
                            + "<br><br>El archivo SVG se abre en cualquier navegador.</html>",
                    "Grafo guardado", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
                    new Object[] {"Abrir en el navegador", "Cerrar"}, "Cerrar");
            if (opcion == 0) {
                abrir(destino);
            }
        } catch (IOException e) {
            mostrarError("No se pudo guardar el grafo", e.getMessage());
        }
    }

    void seleccionarPestana(String nombre) {
        switch (nombre.toLowerCase(Locale.ROOT)) {
            case "hallazgos" -> {
                pestanas.seleccionar(1);
                if (tabla.getRowCount() > 0) {
                    tabla.setRowSelectionInterval(0, 0);
                }
            }
            case "reporte" -> pestanas.seleccionar(2);
            default -> pestanas.seleccionar(0);
        }
    }

    /** Selecciona un paquete en el grafo; admite el nombre completo o el relativo a la raíz. */
    void seleccionarPaquete(String nombre) {
        if (resultado == null || nombre == null) {
            return;
        }
        for (DisposicionGrafo.Nodo n : resultado.disposicion().nodos()) {
            if (n.paquete().equals(nombre) || n.etiqueta().equals(nombre)) {
                lienzo.seleccionar(n.paquete());
                return;
            }
        }
    }

    void ajustarGrafo() {
        lienzo.ajustar();
    }

    // ============================================================= auxiliares

    private void ocupado(boolean si, String texto) {
        progreso.setVisible(si);
        analizar.setEnabled(!si && proyecto != null);
        exportar.setEnabled(!si && resultado != null);
        guardarSvg.setEnabled(!si && resultado != null);
        if (texto != null) {
            mostrarMensaje(texto);
        }
        setCursor(java.awt.Cursor.getPredefinedCursor(si ? java.awt.Cursor.WAIT_CURSOR : java.awt.Cursor.DEFAULT_CURSOR));
    }

    private void actualizarControles() {
        analizar.setEnabled(proyecto != null);
        botonAsistente.setEnabled(proyecto != null && proyectoValido);
        botonAsistente.setToolTipText(proyecto != null && proyectoValido
                ? "Un modelo de lenguaje propone capas, dependencias permitidas, umbral y exenciones a partir"
                        + " de la estructura del proyecto; usted revisa y decide"
                : "Seleccione primero un proyecto compilado");
        exportar.setEnabled(resultado != null);
        guardarSvg.setEnabled(resultado != null);
    }

    private void mostrarMensaje(String texto) {
        mensaje.setText(texto);
    }

    private void mostrarError(String titulo, String texto) {
        JOptionPane.showMessageDialog(this, "<html><div style='width:420px'>" + html(texto).replace("\n", "<br>")
                + "</div></html>", titulo, JOptionPane.ERROR_MESSAGE);
    }

    private String deteccionTexto() {
        return deteccion.getText().replace("●", "").trim();
    }

    private String modo() {
        return conSeveridad.isSelected() ? "cr+severidad" : "cr";
    }

    private void configurarArrastre() {
        getRootPane().setTransferHandler(new TransferHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean canImport(TransferSupport soporte) {
                return soporte.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport soporte) {
                try {
                    @SuppressWarnings("unchecked")
                    List<File> archivos = (List<File>) soporte.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (archivos.isEmpty()) {
                        return false;
                    }
                    Path ruta = archivos.get(0).toPath();
                    if (ruta.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".properties")) {
                        seleccionarCatalogo(ruta);
                    } else {
                        seleccionarProyecto(ruta);
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
    }

    private void configurarAtajos() {
        JComponent raiz = getRootPane();
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), "abrir");
        raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "analizar");
        raiz.getActionMap().put("abrir", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                elegirProyecto();
            }
        });
        raiz.getActionMap().put("analizar", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (analizar.isEnabled()) {
                    analizar();
                }
            }
        });
    }

    private void restaurarPreferencias() {
        String modoGuardado = preferencias.get("modo", "cr+severidad");
        (modoGuardado.equals("cr") ? soloCr : conSeveridad).setSelected(true);
        String catalogoGuardado = preferencias.get("catalogo", "");
        if (!catalogoGuardado.isBlank() && Files.isRegularFile(Path.of(catalogoGuardado))) {
            seleccionarCatalogo(Path.of(catalogoGuardado));
        }
        String proyectoGuardado = preferencias.get("proyecto", "");
        if (!proyectoGuardado.isBlank() && Files.exists(Path.of(proyectoGuardado))) {
            seleccionarProyecto(Path.of(proyectoGuardado));
        }
    }

    private void guardarPreferencia(String clave, String valor) {
        if (preferencias != null) {
            try {
                preferencias.put(clave, valor);
            } catch (RuntimeException e) {
                // Las preferencias son una comodidad: si no se pueden guardar, no se interrumpe nada.
            }
        }
    }

    private static File directorioInicial(Path ruta) {
        if (ruta == null) {
            return null;
        }
        Path carpeta = ruta.getParent();
        return carpeta == null ? null : carpeta.toFile();
    }

    private void abrir(Path ruta) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(ruta.toFile());
            }
        } catch (IOException | UnsupportedOperationException e) {
            mostrarMensaje("No se pudo abrir " + ruta);
        }
    }

    // --------------------------------------------------------- piezas visuales

    private static JLabel etiqueta(java.awt.Font fuente, Color color) {
        JLabel l = new JLabel();
        l.setFont(fuente);
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JComponent nota(String texto) {
        TextoAjustado t = new TextoAjustado(texto, Tema.texto(11.5f), Tema.TEXTO_TENUE, ANCHO_TEXTO);
        t.setBorder(new EmptyBorder(2, 26, 0, 0));
        return t;
    }

    private static JRadioButton radio(String texto) {
        JRadioButton r = new JRadioButton(texto);
        r.setFont(Tema.negrita(13f));
        r.setForeground(Tema.TEXTO);
        r.setOpaque(false);
        r.setFocusPainted(false);
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.setIcon(new IconoOpcion(false));
        r.setSelectedIcon(new IconoOpcion(true));
        r.setRolloverIcon(new IconoOpcion(false));
        r.setRolloverSelectedIcon(new IconoOpcion(true));
        r.setIconTextGap(8);
        r.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return r;
    }

    private static JTextArea areaCodigo() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setFont(Tema.codigo(13f));
        a.setForeground(Tema.TEXTO);
        a.setBackground(Tema.SUPERFICIE);
        a.setBorder(new EmptyBorder(12, 14, 12, 14));
        return a;
    }

    private static Tarjeta tarjetaApilada() {
        Tarjeta t = new Tarjeta(null);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return t;
    }

    private static JPanel filaBotones(Boton... botones) {
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fila.setOpaque(false);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < botones.length; i++) {
            if (i > 0) {
                fila.add(Box.createHorizontalStrut(6));
            }
            fila.add(botones[i]);
        }
        return fila;
    }

    /** Encabezado de un paso de la barra lateral: número en un círculo, título y descripción. */
    private static JComponent paso(int numero, String titulo, String descripcion) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JComponent insignia = new JComponent() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                Tema.suavizar(g);
                g.setColor(Tema.PRIMARIO);
                g.fill(new Ellipse2D.Double(1, 5, 26, 26));
                g.setColor(Color.WHITE);
                g.setFont(Tema.negrita(13f));
                String n = Integer.toString(numero);
                java.awt.FontMetrics fm = g.getFontMetrics();
                g.drawString(n, 14 - fm.stringWidth(n) / 2f, 18 + fm.getAscent() / 2f - 2);
                g.dispose();
            }
        };
        insignia.setPreferredSize(new Dimension(28, 36));
        panel.add(insignia, BorderLayout.WEST);
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JLabel t = etiqueta(Tema.negrita(14.5f), Tema.TEXTO);
        t.setText(titulo);
        JLabel d = etiqueta(Tema.texto(11.5f), Tema.TEXTO_TENUE);
        d.setText(descripcion);
        textos.add(t);
        textos.add(d);
        panel.add(textos, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Abrevia una ruta larga por la izquierda, conservando su final ---que es lo
     * que identifica al proyecto---; la ruta completa queda en el texto emergente.
     */
    private static String abreviar(String ruta) {
        int maximo = 38;
        if (ruta.length() <= maximo) {
            return ruta;
        }
        String cola = ruta.substring(ruta.length() - maximo + 1);
        int separador = Math.max(cola.indexOf('\\'), cola.indexOf('/'));
        if (separador > 0 && separador < cola.length() - 8) {
            cola = cola.substring(separador);
        }
        return "…" + cola;
    }

    private static String estilo() {
        return "<html><head><style>"
                + "body{font-family:'" + Tema.texto(13f).getFamily() + "';font-size:12.5pt;color:#1F2D44;margin:0}"
                + ".titulo{font-size:16pt;font-weight:bold;margin-top:2px}"
                + ".tenue{color:#8A97AA;font-size:10.5pt}"
                + "h4{font-size:11pt;color:#5B6B82;margin-top:14px;margin-bottom:2px}"
                + "td{font-size:12pt} .clave{color:#5B6B82}"
                + "</style></head><body>";
    }

    private static void fila(StringBuilder sb, String clave, String valor) {
        sb.append("<tr><td class='clave'>").append(html(clave)).append("</td><td><b>")
          .append(html(valor)).append("</b></td></tr>");
    }

    private static void lista(StringBuilder sb, String titulo, List<String> elementos) {
        sb.append("<h4>").append(html(titulo)).append(" (").append(elementos.size()).append(")</h4>");
        if (elementos.isEmpty()) {
            sb.append("<div class='tenue'>ninguno</div>");
            return;
        }
        for (String e : elementos) {
            sb.append("<div style='margin-top:2px'>").append(e).append("</div>");
        }
    }

    private static String marca(DisposicionGrafo.Arista a) {
        return switch (a.tipo()) {
            case VIOLA_CAPAS, CICLO -> "<span style='color:#C0392B'>&#8226;</span> ";
            case NUCLEO -> "<span style='color:#8A94A6'>&#8226;</span> ";
            default -> "<span style='color:#7F93B5'>&#8226;</span> ";
        };
    }

    private static String aviso(DisposicionGrafo.Arista a) {
        return switch (a.tipo()) {
            case VIOLA_CAPAS -> " <span style='color:#C0392B'>(viola capas)</span>";
            case CICLO -> " <span style='color:#C0392B'>(ciclo)</span>";
            default -> "";
        };
    }

    private static String html(String texto) {
        return texto == null ? "" : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Barra lateral: ocupa siempre el ancho de su visor y crece en altura lo que
     * necesite; si la ventana es baja, se desplaza en lugar de comprimirse.
     */
    private static final class PanelLateral extends JPanel implements javax.swing.Scrollable {

        private static final long serialVersionUID = 1L;

        PanelLateral() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Tema.LATERAL);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Tema.BORDE);
            g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientacion, int direccion) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientacion, int direccion) {
            return Math.max(16, visible.height - 32);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof javax.swing.JViewport v && v.getHeight() > getPreferredSize().height;
        }
    }

    /** Botón de opción redondo, coherente con el resto de la interfaz. */
    private static final class IconoOpcion implements javax.swing.Icon {

        private final boolean marcado;

        IconoOpcion(boolean marcado) {
            this.marcado = marcado;
        }

        @Override
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            Tema.suavizar(g);
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(x + 1, y + 1, 16, 16));
            g.setColor(marcado ? Tema.PRIMARIO : new Color(0xA9B5C7));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new Ellipse2D.Double(x + 1, y + 1, 16, 16));
            if (marcado) {
                g.fill(new Ellipse2D.Double(x + 5, y + 5, 8, 8));
            }
            g.dispose();
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }
    }

    /** Ilustración del estado vacío: tres capas y una dependencia que remonta la estratificación. */
    private static final class Ilustracion extends JComponent {

        private static final long serialVersionUID = 1L;

        Ilustracion() {
            setPreferredSize(new Dimension(220, 150));
            setMaximumSize(new Dimension(220, 150));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Tema.suavizar(g);
            String[] capas = {"presentacion", "negocio", "persistencia"};
            double[] ys = {8, 58, 108};
            for (int i = 0; i < 3; i++) {
                g.setColor(i % 2 == 0 ? new Color(0xEAF0F8) : new Color(0xE1E8F3));
                g.fill(new RoundRectangle2D.Double(6, ys[i] - 4, 208, 42, 10, 10));
            }
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(0x7F93B5));
            g.draw(new Line2D.Double(110, 34, 110, 62));
            g.draw(new Line2D.Double(110, 84, 110, 112));
            g.setColor(Tema.ALERTA);
            g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            java.awt.geom.Path2D.Double rodeo = new java.awt.geom.Path2D.Double();
            rodeo.moveTo(160, 123);
            rodeo.curveTo(196, 123, 196, 21, 160, 21);
            g.draw(rodeo);
            for (int i = 0; i < 3; i++) {
                boolean infractor = i != 1;
                g.setColor(infractor ? Tema.ALERTA_FONDO : Color.WHITE);
                RoundRectangle2D caja = new RoundRectangle2D.Double(60, ys[i] + 6, 100, 22, 8, 8);
                g.fill(caja);
                g.setColor(infractor ? Tema.ALERTA : Tema.BORDE_GRAFO);
                g.setStroke(new BasicStroke(1.6f));
                g.draw(caja);
                g.setFont(Tema.codigo(10.5f));
                g.setColor(Tema.TEXTO);
                java.awt.FontMetrics fm = g.getFontMetrics();
                g.drawString(capas[i], (float) (110 - fm.stringWidth(capas[i]) / 2.0),
                        (float) (ys[i] + 17 + fm.getAscent() / 2.0 - 2));
            }
            g.dispose();
        }
    }
}
