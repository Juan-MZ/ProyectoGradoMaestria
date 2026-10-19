package co.javeriana.msw.arquitecta.servicio;

import co.javeriana.msw.arquitecta.config.ConfiguracionReglas;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.motor.Resultado;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo;
import co.javeriana.msw.arquitecta.reporte.ExportadorCsv;
import co.javeriana.msw.arquitecta.reporte.ExportadorDot;
import co.javeriana.msw.arquitecta.reporte.ExportadorJson;
import co.javeriana.msw.arquitecta.reporte.ExportadorSvg;
import co.javeriana.msw.arquitecta.reporte.ExportadorTikz;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Todo lo que produce un análisis, reunido para que la consola y la interfaz
 * gráfica lo presenten sin tener que reconstruirlo.
 *
 * <p>La configuración y el grafo se conservan como detalle interno: quien
 * presenta el resultado solo necesita el diagnóstico, el dibujo y los textos, y
 * no debería depender de cómo se modeló el catálogo ni de cómo se extrajeron las
 * dependencias.</p>
 */
public final class ResultadoAnalisis {

    private final String etiqueta;
    private final Path rutaAnalizada;
    private final String origen;
    private final Path catalogo;
    private final ConfiguracionReglas config;
    private final GrafoDependencias grafo;
    private final Resultado resultado;
    private final DisposicionGrafo disposicion;
    private final String reporte;
    private final OptionalDouble crAnterior;
    private final long milisegundos;

    ResultadoAnalisis(String etiqueta, Path rutaAnalizada, String origen, Path catalogo,
            ConfiguracionReglas config, GrafoDependencias grafo, Resultado resultado,
            DisposicionGrafo disposicion, String reporte, OptionalDouble crAnterior, long milisegundos) {
        this.etiqueta = etiqueta;
        this.rutaAnalizada = rutaAnalizada;
        this.origen = origen;
        this.catalogo = catalogo;
        this.config = config;
        this.grafo = grafo;
        this.resultado = resultado;
        this.disposicion = disposicion;
        this.reporte = reporte;
        this.crAnterior = crAnterior;
        this.milisegundos = milisegundos;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Ruta efectivamente analizada: el directorio de clases o el JAR. */
    public Path rutaAnalizada() {
        return rutaAnalizada;
    }

    /** Cómo se identificó la ruta analizada a partir de la selección del usuario. */
    public String origen() {
        return origen;
    }

    /** Archivo de catálogo empleado, o {@code null} si se usó una configuración mínima inferida. */
    public Path catalogo() {
        return catalogo;
    }

    public boolean configuracionMinima() {
        return catalogo == null;
    }

    public String raiz() {
        return config.raiz();
    }

    public List<String> capas() {
        return config.nombresCapas();
    }

    public List<String> exclusiones() {
        return config.exclusiones();
    }

    public List<String> compartidos() {
        return config.compartidos();
    }

    public int umbralConcentracion() {
        return config.umbralConcentracion();
    }

    public int paquetes() {
        return grafo.nodos().size();
    }

    public int dependencias() {
        return grafo.cantidadAristas();
    }

    public Resultado resultado() {
        return resultado;
    }

    public DisposicionGrafo disposicion() {
        return disposicion;
    }

    /** Reporte textual completo, idéntico al que emite la consola. */
    public String reporte() {
        return reporte;
    }

    /** Tasa de Conformidad del análisis anterior del mismo sistema, si se registró. */
    public OptionalDouble crAnterior() {
        return crAnterior;
    }

    public long milisegundos() {
        return milisegundos;
    }

    public String svg() {
        return ExportadorSvg.exportar(disposicion, resultado, etiqueta);
    }

    public String tikz() {
        return ExportadorTikz.exportar(disposicion, etiqueta);
    }

    public String dot() {
        return ExportadorDot.exportar(disposicion, etiqueta);
    }

    public String json() {
        return ExportadorJson.exportar(resultado, grafo, config, etiqueta);
    }

    /**
     * Escribe la evidencia completa en un directorio: reporte, JSON, hallazgos,
     * el grafo en sus tres formatos y una fila en el resumen acumulado.
     *
     * @return el nombre base con que se escribieron los archivos.
     */
    public String exportarEvidencia(Path directorio) throws IOException {
        Files.createDirectories(directorio);
        String base = etiqueta.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");
        escribir(directorio.resolve(base + ".txt"), reporte);
        escribir(directorio.resolve(base + ".json"), json());
        escribir(directorio.resolve(base + ".svg"), svg());
        escribir(directorio.resolve(base + ".tikz"), tikz());
        escribir(directorio.resolve(base + ".dot"), dot());
        escribir(directorio.resolve(base + "-hallazgos.csv"),
                ExportadorCsv.CABECERA_HALLAZGOS + "\n" + ExportadorCsv.filasHallazgos(resultado, etiqueta));
        registrarEn(directorio.resolve("resumen.csv"));
        return base;
    }

    /**
     * Añade una fila al resumen acumulado indicado, creándolo con su cabecera si no
     * existe. Conservar ese archivo entre análisis es lo que permite reportar la
     * tendencia de la Tasa de Conformidad.
     */
    public void registrarEn(Path resumen) throws IOException {
        if (resumen.getParent() != null) {
            Files.createDirectories(resumen.getParent());
        }
        if (!Files.exists(resumen)) {
            escribir(resumen, ExportadorCsv.CABECERA_RESUMEN + "\n");
        }
        Files.writeString(resumen, ExportadorCsv.filaResumen(resultado, grafo, etiqueta) + "\n",
                StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private static void escribir(Path destino, String contenido) throws IOException {
        Files.writeString(destino, contenido, StandardCharsets.UTF_8);
    }
}
