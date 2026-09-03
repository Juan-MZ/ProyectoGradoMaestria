package co.javeriana.msw.arquitecta.reporte;

import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.motor.Resultado;
import co.javeriana.msw.arquitecta.reglas.ResultadoRegla;
import co.javeriana.msw.arquitecta.reglas.Violacion;

import java.util.Locale;

/**
 * Exporta los resultados en CSV para su tabulación directa.
 *
 * <p>Produce dos vistas complementarias: una fila por escenario, que alimenta la
 * tabla comparativa del experimento, y una fila por hallazgo, que alimenta la
 * matriz de validación. Ambas comparten encabezado estable, de modo que las
 * corridas sucesivas puedan concatenarse sin postprocesamiento.</p>
 */
public final class ExportadorCsv {

    public static final String CABECERA_RESUMEN =
            "escenario,paquetes,dependencias,verificaciones,incumplimientos,cr,"
            + "estado_por_cr,severidad_maxima,estado_por_severidad,estado_final";

    public static final String CABECERA_HALLAZGOS =
            "escenario,regla,severidad,descripcion,ubicacion";

    private ExportadorCsv() {
    }

    public static String filaResumen(Resultado resultado, GrafoDependencias grafo, String etiqueta) {
        return String.join(",",
                celda(etiqueta),
                Integer.toString(grafo.nodos().size()),
                Integer.toString(grafo.cantidadAristas()),
                Integer.toString(resultado.verificaciones()),
                Integer.toString(resultado.incumplimientos()),
                String.format(Locale.ROOT, "%.2f", resultado.conformidad()),
                celda(resultado.estadoPorCR().etiqueta()),
                celda(resultado.severidadMaxima() == null ? "" : resultado.severidadMaxima().etiqueta()),
                celda(resultado.estadoPorSeveridad().etiqueta()),
                celda(resultado.estado().etiqueta()));
    }

    public static String filasHallazgos(Resultado resultado, String etiqueta) {
        StringBuilder sb = new StringBuilder();
        for (ResultadoRegla rr : resultado.porRegla()) {
            for (Violacion v : rr.violaciones()) {
                sb.append(String.join(",",
                        celda(etiqueta),
                        celda(v.regla()),
                        celda(v.severidad().etiqueta()),
                        celda(v.descripcion()),
                        celda(String.join(" | ", v.ubicacion())))).append('\n');
            }
        }
        return sb.toString();
    }

    private static String celda(String valor) {
        String texto = valor == null ? "" : valor;
        if (texto.contains(",") || texto.contains("\"") || texto.contains("\n")) {
            return '"' + texto.replace("\"", "\"\"") + '"';
        }
        return texto;
    }
}
