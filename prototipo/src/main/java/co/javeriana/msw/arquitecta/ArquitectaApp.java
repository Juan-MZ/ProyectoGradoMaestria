package co.javeriana.msw.arquitecta;

import co.javeriana.msw.arquitecta.interfaz.InterfazGrafica;
import co.javeriana.msw.arquitecta.motor.EstadoArquitectonico;
import co.javeriana.msw.arquitecta.servicio.Analisis;
import co.javeriana.msw.arquitecta.servicio.ErrorAnalisis;
import co.javeriana.msw.arquitecta.servicio.ResultadoAnalisis;

import java.awt.GraphicsEnvironment;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Punto de entrada.
 *
 * <p>Sin argumentos abre la interfaz gráfica. Con argumentos funciona en línea de
 * comandos, que es la forma adecuada para automatizar el análisis:</p>
 *
 * <pre>
 * arquitecta &lt;proyecto|clases|archivo.jar&gt; [archivo-config] [opciones]
 *
 *   --etiqueta &lt;nombre&gt;    nombre del escenario en el reporte y en los archivos
 *   --salida &lt;directorio&gt;  escribe reporte, JSON, CSV y el grafo (SVG, TikZ y DOT)
 *   --svg &lt;archivo&gt;        dibuja el grafo como imagen SVG, que se abre en cualquier navegador
 *   --tikz &lt;archivo&gt;       dibuja el grafo como figura TikZ, lista para incluirse en LaTeX
 *   --historico &lt;csv&gt;      compara con la última corrida registrada para la misma
 *                          etiqueta; si se omite y hay --salida, se usa su resumen.csv
 *   --modo cr|cr+severidad sobrescribe el modo de clasificación del archivo de configuración
 *   --fallar-si &lt;estado&gt;   devuelve código de salida 2 si el estado alcanza esa gravedad
 *   --catalogo             imprime el catálogo de reglas y termina
 *   --gui                  abre la interfaz gráfica
 * </pre>
 *
 * <p>El sistema puede indicarse como la carpeta del proyecto ---se reconocen las
 * convenciones de Maven, Gradle, IntelliJ y Eclipse---, como un directorio de
 * clases compiladas o como un archivo {@code .jar}. Si se omite el archivo de
 * configuración se infiere una configuración mínima, sin capas.</p>
 */
public final class ArquitectaApp {

    private static final PrintStream SALIDA =
            new PrintStream(System.out, true, StandardCharsets.UTF_8);
    private static final PrintStream ERRORES =
            new PrintStream(System.err, true, StandardCharsets.UTF_8);

    public static void main(String[] args) throws Exception {
        Map<String, String> opciones = new LinkedHashMap<>();
        List<String> posicionales = new ArrayList<>();
        if (!parsear(args, opciones, posicionales)) {
            System.exit(1);
        }

        if (args.length == 0 || opciones.containsKey("gui")) {
            abrirInterfaz(opciones, posicionales);
            return;
        }

        Path sistema = Path.of(posicionales.isEmpty() ? "." : posicionales.get(0));
        Path catalogo = posicionales.size() > 1 ? Path.of(posicionales.get(1)) : null;

        if (opciones.containsKey("catalogo")) {
            if (catalogo == null) {
                ERRORES.println("La opción --catalogo requiere un archivo de configuración.");
                System.exit(1);
            }
            try {
                SALIDA.println(Analisis.catalogo(catalogo));
            } catch (ErrorAnalisis e) {
                ERRORES.println(e.getMessage());
                System.exit(1);
            }
            return;
        }

        ResultadoAnalisis r;
        try {
            r = Analisis.ejecutar(new Analisis.Solicitud(sistema, catalogo, opciones.get("modo"),
                    opciones.get("etiqueta"), rutaHistorico(opciones)));
        } catch (ErrorAnalisis e) {
            ERRORES.println(e.getMessage());
            System.exit(1);
            return;
        }

        SALIDA.println("Sistema bajo análisis   : " + r.rutaAnalizada());
        SALIDA.println("Configuración de reglas : " + (r.configuracionMinima()
                ? "mínima inferida (raíz " + r.raiz() + ", sin capas declaradas)"
                : r.catalogo().toString()));
        SALIDA.println();
        SALIDA.println(r.reporte());

        if (opciones.containsKey("salida")) {
            Path directorio = Path.of(opciones.get("salida"));
            r.exportarEvidencia(directorio);
            SALIDA.println("Evidencia escrita en    : " + directorio.toAbsolutePath().normalize());
        }
        if (opciones.containsKey("svg")) {
            SALIDA.println("Grafo (SVG) escrito en  : " + escribirFigura(Path.of(opciones.get("svg")), r.svg()));
        }
        if (opciones.containsKey("tikz")) {
            SALIDA.println("Grafo (TikZ) escrito en : " + escribirFigura(Path.of(opciones.get("tikz")), r.tikz()));
        }

        String fallarSi = opciones.get("fallar-si");
        if (fallarSi != null && alcanza(r.resultado().estado(), fallarSi)) {
            System.exit(2);
        }
    }

    /**
     * Abre la interfaz gráfica. Las opciones {@code --proyecto} y {@code --reglas}
     * la abren con un sistema ya seleccionado; {@code --captura} la dibuja fuera de
     * pantalla en una imagen PNG, lo que sirve para documentarla.
     */
    private static void abrirInterfaz(Map<String, String> opciones, List<String> posicionales) throws Exception {
        Path proyecto = opciones.containsKey("proyecto") ? Path.of(opciones.get("proyecto"))
                : posicionales.isEmpty() ? null : Path.of(posicionales.get(0));
        Path reglas = opciones.containsKey("reglas") ? Path.of(opciones.get("reglas"))
                : posicionales.size() > 1 ? Path.of(posicionales.get(1)) : null;

        if (GraphicsEnvironment.isHeadless()) {
            ERRORES.println("No hay un entorno gráfico disponible. Use la línea de comandos:");
            ERRORES.println("  arquitecta <proyecto|clases|archivo.jar> [archivo-config] [opciones]");
            System.exit(1);
        }
        if (opciones.containsKey("captura")) {
            InterfazGrafica.capturar(proyecto, reglas, opciones.getOrDefault("pestana", "grafo"),
                    opciones.get("seleccion"), Path.of(opciones.get("captura")));
            return;
        }
        InterfazGrafica.abrir(proyecto, reglas);
    }

    /**
     * Archivo del que se lee la historia. Si no se indica uno explícito pero se
     * pidió escribir evidencia, se usa el resumen acumulado de ese directorio: así
     * basta con conservarlo entre corridas para obtener la tendencia sin
     * configurar nada más.
     */
    private static Path rutaHistorico(Map<String, String> opciones) {
        if (opciones.containsKey("historico")) {
            return Path.of(opciones.get("historico"));
        }
        if (opciones.containsKey("salida")) {
            return Path.of(opciones.get("salida")).resolve("resumen.csv");
        }
        return null;
    }

    /** Escribe una figura en la ruta pedida, creando los directorios intermedios si faltan. */
    private static Path escribirFigura(Path destino, String contenido) throws Exception {
        Path absoluta = destino.toAbsolutePath().normalize();
        if (absoluta.getParent() != null) {
            Files.createDirectories(absoluta.getParent());
        }
        Files.writeString(absoluta, contenido, StandardCharsets.UTF_8);
        return absoluta;
    }

    private static boolean alcanza(EstadoArquitectonico estado, String minimo) {
        EstadoArquitectonico umbral = switch (minimo.toLowerCase(Locale.ROOT)) {
            case "aceptable" -> EstadoArquitectonico.ACEPTABLE;
            case "observacion", "en-observacion" -> EstadoArquitectonico.EN_OBSERVACION;
            case "no-conforme", "noconforme" -> EstadoArquitectonico.NO_CONFORME;
            default -> throw new IllegalArgumentException("Estado desconocido para --fallar-si: " + minimo);
        };
        return estado.ordinal() >= umbral.ordinal();
    }

    private static boolean parsear(String[] args, Map<String, String> opciones, List<String> posicionales) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                posicionales.add(arg);
                continue;
            }
            String nombre = arg.substring(2);
            if (nombre.equals("catalogo") || nombre.equals("gui")) {
                opciones.put(nombre, "true");
                continue;
            }
            if (i + 1 >= args.length) {
                ERRORES.println("La opción --" + nombre + " requiere un valor.");
                return false;
            }
            opciones.put(nombre, args[++i]);
        }
        return true;
    }
}
