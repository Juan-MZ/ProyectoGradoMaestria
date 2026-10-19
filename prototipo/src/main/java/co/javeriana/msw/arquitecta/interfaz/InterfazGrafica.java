package co.javeriana.msw.arquitecta.interfaz;

import co.javeriana.msw.arquitecta.servicio.Analisis;
import co.javeriana.msw.arquitecta.servicio.ErrorAnalisis;
import co.javeriana.msw.arquitecta.servicio.ResultadoAnalisis;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Punto de entrada de la interfaz gráfica.
 *
 * <p>La interfaz se construye con Swing, incluido en el JDK, de modo que no añade
 * ninguna dependencia: el mismo requisito de ligereza que rige el mecanismo rige
 * también su presentación. Se usa el aspecto del sistema operativo, para que los
 * diálogos de selección de archivos sean los que el usuario ya conoce.</p>
 */
public final class InterfazGrafica {

    private InterfazGrafica() {
    }

    /** Abre la ventana principal, opcionalmente con un proyecto y un catálogo ya elegidos. */
    public static void abrir(Path proyecto, Path catalogo) {
        abrir(proyecto, catalogo, null);
    }

    /**
     * Abre la ventana principal ofreciendo, además, un asistente para redactar el
     * catálogo de reglas.
     */
    public static void abrir(Path proyecto, Path catalogo, AsistenteCatalogo asistente) {
        SwingUtilities.invokeLater(() -> {
            configurarAspecto();
            VentanaPrincipal ventana = new VentanaPrincipal(true);
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.usarAsistente(asistente);
            if (catalogo != null) {
                ventana.seleccionarCatalogo(catalogo);
            }
            if (proyecto != null) {
                ventana.seleccionarProyecto(proyecto);
            }
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }

    /**
     * Dibuja la interfaz fuera de pantalla, con un análisis ya realizado, y la
     * guarda como imagen PNG. Sirve para documentar la herramienta con capturas
     * reproducibles: la imagen se regenera con la misma orden cada vez que la
     * interfaz cambia.
     *
     * @param pestana   pestaña visible: {@code grafo}, {@code hallazgos} o {@code reporte}; con
     *                  {@code inicio} se captura la pantalla de bienvenida, sin proyecto.
     * @param seleccion paquete que se selecciona en el grafo, o {@code null}.
     */
    public static void capturar(Path proyecto, Path catalogo, String pestana, String seleccion, Path salida)
            throws ErrorAnalisis, IOException, InterruptedException {
        boolean inicio = "inicio".equals(pestana);
        if (proyecto == null && !inicio) {
            throw new ErrorAnalisis("La captura requiere un proyecto (--proyecto).");
        }
        ResultadoAnalisis resultado = inicio ? null
                : Analisis.ejecutar(new Analisis.Solicitud(proyecto, catalogo, null, null, null));
        AtomicReference<BufferedImage> imagen = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                configurarAspecto();
                VentanaPrincipal ventana = new VentanaPrincipal(false);
                if (!inicio) {
                    if (catalogo != null) {
                        ventana.seleccionarCatalogo(catalogo);
                    }
                    ventana.seleccionarProyectoAhora(proyecto);
                    ventana.mostrarResultado(resultado);
                }
                ventana.setSize(1440, 900);
                ventana.addNotify();
                validarTodo(ventana);
                if (!inicio) {
                    ventana.seleccionarPestana(pestana == null ? "grafo" : pestana);
                    validarTodo(ventana);
                    ventana.ajustarGrafo();
                    if (seleccion != null) {
                        ventana.seleccionarPaquete(seleccion);
                    }
                    validarTodo(ventana);
                }

                Container contenido = ventana.getContentPane();
                int escala = 2;
                BufferedImage img = new BufferedImage(contenido.getWidth() * escala, contenido.getHeight() * escala,
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.scale(escala, escala);
                contenido.printAll(g);
                g.dispose();
                imagen.set(img);
                ventana.dispose();
            });
        } catch (InvocationTargetException e) {
            throw new IOException("No se pudo dibujar la interfaz: " + e.getCause(), e.getCause());
        }
        Path absoluta = salida.toAbsolutePath().normalize();
        if (absoluta.getParent() != null) {
            Files.createDirectories(absoluta.getParent());
        }
        ImageIO.write(imagen.get(), "png", absoluta.toFile());
    }

    private static void validarTodo(JFrame ventana) {
        ventana.validate();
        ventana.getContentPane().doLayout();
        ventana.getRootPane().validate();
    }

    static void configurarAspecto() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Si el aspecto del sistema no está disponible se conserva el de Java.
        }
        UIManager.put("ToolTip.font", Tema.texto(12.5f));
        UIManager.put("ToolTip.background", Tema.SUPERFICIE);
        UIManager.put("ToolTip.foreground", Tema.TEXTO);
        UIManager.put("ToolTip.border", javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(Tema.BORDE),
                javax.swing.BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        UIManager.put("OptionPane.messageFont", Tema.texto(13f));
        UIManager.put("OptionPane.buttonFont", Tema.texto(12.5f));
        UIManager.put("TabbedPane.font", Tema.negrita(13f));
        ToolTipManager.sharedInstance().setInitialDelay(250);
        ToolTipManager.sharedInstance().setDismissDelay(15000);
    }
}
