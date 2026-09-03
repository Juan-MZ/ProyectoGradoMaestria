package co.javeriana.msw.arquitecta.reglas;

import java.util.List;

/**
 * Resultado de aplicar una {@link Regla} sobre el grafo en una única pasada.
 *
 * <p>Reunir en un solo objeto el conteo de verificaciones, el conteo de
 * incumplimientos y la lista de violaciones garantiza, por construcción, que la
 * Tasa de Conformidad y la evidencia reportada provengan de la misma
 * evaluación y no puedan discrepar.</p>
 *
 * @param regla           nombre de la regla evaluada.
 * @param verificaciones  cantidad de verificaciones atómicas realizadas.
 * @param incumplimientos cantidad de verificaciones atómicas que fallaron.
 * @param violaciones     hallazgos detallados con su evidencia estructural.
 */
public record ResultadoRegla(
        String regla,
        int verificaciones,
        int incumplimientos,
        List<Violacion> violaciones) {

    public ResultadoRegla {
        if (verificaciones < 0 || incumplimientos < 0) {
            throw new IllegalArgumentException("Los conteos no pueden ser negativos.");
        }
        if (incumplimientos > verificaciones) {
            throw new IllegalArgumentException(
                    "Los incumplimientos (" + incumplimientos + ") no pueden superar las "
                            + "verificaciones (" + verificaciones + ") en la regla '" + regla + "'.");
        }
        violaciones = List.copyOf(violaciones);
    }

    /** Verificaciones atómicas superadas por el sistema bajo análisis. */
    public int cumplidas() {
        return verificaciones - incumplimientos;
    }

    /** Conformidad local de la regla, en porcentaje. Una regla sin verificaciones es conforme. */
    public double conformidadLocal() {
        return verificaciones == 0 ? 100.0 : 100.0 * cumplidas() / verificaciones;
    }
}
