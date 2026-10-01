package co.javeriana.msw.arquitecta;

import co.javeriana.msw.arquitecta.config.CargadorConfiguracion;
import co.javeriana.msw.arquitecta.config.ConfiguracionReglas;
import co.javeriana.msw.arquitecta.config.ModoClasificacion;
import co.javeriana.msw.arquitecta.grafo.ConstructorGrafo;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.motor.EstadoArquitectonico;
import co.javeriana.msw.arquitecta.motor.MotorConformidad;
import co.javeriana.msw.arquitecta.motor.Resultado;
import co.javeriana.msw.arquitecta.reglas.Exenciones;
import co.javeriana.msw.arquitecta.reglas.Regla;
import co.javeriana.msw.arquitecta.reglas.ReglaCiclos;
import co.javeriana.msw.arquitecta.reglas.ReglaConcentracion;
import co.javeriana.msw.arquitecta.reglas.ReglaIntegridadCapas;
import co.javeriana.msw.arquitecta.reporte.CatalogoReglas;
import co.javeriana.msw.arquitecta.reporte.ExportadorCsv;
import co.javeriana.msw.arquitecta.reporte.ExportadorDot;
import co.javeriana.msw.arquitecta.reporte.ExportadorJson;
import co.javeriana.msw.arquitecta.reporte.GeneradorReporte;
import co.javeriana.msw.arquitecta.reporte.Historico;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Punto de entrada de la línea de comandos.
 *
 * <pre>
 * arquitecta &lt;directorio-clases&gt; &lt;archivo-config&gt; [opciones]
 *
 *   --etiqueta &lt;nombre&gt;    nombre del escenario en el reporte y en los archivos
 *   --salida &lt;directorio&gt;  escribe reporte, JSON, CSV y DOT en ese directorio
 *   --historico &lt;csv&gt;      compara con la última corrida registrada para la misma
 *                          etiqueta; si se omite y hay --salida, se usa su resumen.csv
 *   --modo cr|cr+severidad sobrescribe el modo de clasificación del archivo de configuración
 *   --fallar-si &lt;estado&gt;   devuelve código de salida 2 si el estado alcanza esa gravedad
 *   --catalogo             imprime el catálogo de reglas y termina
 * </pre>
 */
public final class ArquitectaApp {

    private static final PrintStream SALIDA =
            new PrintStream(System.out, true, StandardCharsets.UTF_8);

    public static void main(String[] args) throws Exception {
        Map<String, String> opciones = new LinkedHashMap<>();
        List<String> posicionales = new ArrayList<>();
        if (!parsear(args, opciones, posicionales)) {
            System.exit(1);
        }

        Path directorioClases = Path.of(posicionales.size() > 0 ? posicionales.get(0) : "out");
        Path archivoConfig = Path.of(posicionales.size() > 1 ? posicionales.get(1) : "rules.properties");

        if (!Files.isRegularFile(archivoConfig)) {
            System.err.println("No se encontró el archivo de configuración: " + archivoConfig);
            System.exit(1);
        }

        ConfiguracionReglas config;
        try {
            config = CargadorConfiguracion.cargar(archivoConfig);
        } catch (IllegalArgumentException e) {
            System.err.println("Configuración inválida: " + e.getMessage());
            System.exit(1);
            return;
        }

        Exenciones compartidos = new Exenciones(config.compartidos());
        List<Regla> reglas = List.of(
                new ReglaIntegridadCapas(config),
                new ReglaCiclos(compartidos),
                new ReglaConcentracion(config.umbralConcentracion(), compartidos));

        if (opciones.containsKey("catalogo")) {
            SALIDA.println(CatalogoReglas.imprimir(reglas));
            return;
        }

        if (!Files.isDirectory(directorioClases)) {
            System.err.println("No se encontró el directorio de clases: " + directorioClases);
            System.exit(1);
        }

        ModoClasificacion modo = opciones.containsKey("modo")
                ? ModoClasificacion.desde(opciones.get("modo"))
                : config.modoClasificacion();
        String etiqueta = opciones.getOrDefault("etiqueta", directorioClases.getFileName().toString());

        GrafoDependencias grafo = new ConstructorGrafo(config.raiz(), config.exclusiones())
                .desdeDirectorio(directorioClases);
        Resultado resultado = new MotorConformidad(
                config.umbralAceptable(), config.umbralObservacion(), modo).evaluar(grafo, reglas);

        OptionalDouble crAnterior = Historico.crAnterior(rutaHistorico(opciones), etiqueta);

        SALIDA.println("Sistema bajo análisis   : " + directorioClases.toAbsolutePath().normalize());
        SALIDA.println("Configuración de reglas : " + archivoConfig.toAbsolutePath().normalize());
        SALIDA.println();
        String reporte = GeneradorReporte.generar(resultado, grafo, reglas, config, etiqueta, crAnterior);
        SALIDA.println(reporte);

        if (opciones.containsKey("salida")) {
            escribirEvidencia(Path.of(opciones.get("salida")), etiqueta, reporte,
                    resultado, grafo, config);
        }

        String fallarSi = opciones.get("fallar-si");
        if (fallarSi != null && alcanza(resultado.estado(), fallarSi)) {
            System.exit(2);
        }
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

    private static void escribirEvidencia(Path directorio, String etiqueta, String reporte,
            Resultado resultado, GrafoDependencias grafo, ConfiguracionReglas config) throws Exception {

        Files.createDirectories(directorio);
        String base = etiqueta.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");

        escribir(directorio.resolve(base + ".txt"), reporte);
        escribir(directorio.resolve(base + ".json"),
                ExportadorJson.exportar(resultado, grafo, config, etiqueta));
        escribir(directorio.resolve(base + ".dot"),
                ExportadorDot.exportar(grafo, config, resultado, etiqueta));
        escribir(directorio.resolve(base + "-hallazgos.csv"),
                ExportadorCsv.CABECERA_HALLAZGOS + "\n" + ExportadorCsv.filasHallazgos(resultado, etiqueta));

        Path resumen = directorio.resolve("resumen.csv");
        if (!Files.exists(resumen)) {
            escribir(resumen, ExportadorCsv.CABECERA_RESUMEN + "\n");
        }
        Files.writeString(resumen, ExportadorCsv.filaResumen(resultado, grafo, etiqueta) + "\n",
                StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);

        SALIDA.println("Evidencia escrita en    : " + directorio.toAbsolutePath().normalize());
    }

    private static void escribir(Path destino, String contenido) throws Exception {
        Files.writeString(destino, contenido, StandardCharsets.UTF_8);
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
            if (nombre.equals("catalogo")) {
                opciones.put(nombre, "true");
                continue;
            }
            if (i + 1 >= args.length) {
                System.err.println("La opción --" + nombre + " requiere un valor.");
                return false;
            }
            opciones.put(nombre, args[++i]);
        }
        return true;
    }
}
