package co.javeriana.msw.arquitecta.motor;

import java.util.List;

import co.javeriana.msw.arquitecta.config.ModoClasificacion;
import co.javeriana.msw.arquitecta.reglas.ResultadoRegla;
import co.javeriana.msw.arquitecta.reglas.Violacion;

/**
 * Resultado agregado de una evaluación de conformidad.
 *
 * <p>Conserva por separado el estado deducido de la Tasa de Conformidad y el
 * deducido de la severidad del peor hallazgo, además del veredicto final. Esa
 * separación permite auditar de dónde procede la clasificación y comparar ambos
 * criterios sin volver a ejecutar el análisis.</p>
 *
 * @param conformidad      Tasa de Conformidad (CR), en porcentaje.
 * @param estadoPorCR      estado deducido solo de la CR y sus umbrales.
 * @param severidadMaxima  severidad del hallazgo más grave, o {@code null} si no hubo hallazgos.
 * @param estadoPorSeveridad estado deducido solo de la severidad máxima.
 * @param estado           veredicto final, según el modo de clasificación aplicado.
 * @param modo             modo de clasificación empleado.
 * @param porRegla         resultado detallado de cada regla del catálogo.
 * @param verificaciones   total de verificaciones atómicas realizadas.
 * @param incumplimientos  total de verificaciones atómicas fallidas.
 * @param umbralAceptable  umbral de CR a partir del cual el estado es aceptable.
 * @param umbralObservacion umbral de CR a partir del cual el estado es en observación.
 */
public record Resultado(
        double conformidad,
        EstadoArquitectonico estadoPorCR,
        Violacion.Severidad severidadMaxima,
        EstadoArquitectonico estadoPorSeveridad,
        EstadoArquitectonico estado,
        ModoClasificacion modo,
        List<ResultadoRegla> porRegla,
        int verificaciones,
        int incumplimientos,
        double umbralAceptable,
        double umbralObservacion) {

    public Resultado {
        porRegla = List.copyOf(porRegla);
    }

    public int totalViolaciones() {
        return porRegla.stream().mapToInt(r -> r.violaciones().size()).sum();
    }

    public List<Violacion> violaciones() {
        return porRegla.stream().flatMap(r -> r.violaciones().stream()).toList();
    }

    /** Indica si el veredicto final difiere del que habría producido la CR por sí sola. */
    public boolean severidadAlteroElVeredicto() {
        return estado != estadoPorCR;
    }
}
