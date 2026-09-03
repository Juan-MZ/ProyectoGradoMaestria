package co.javeriana.msw.arquitecta.reglas;

import java.util.List;

/**
 * Hallazgo concreto producido por una regla al evaluar el grafo.
 *
 * <p>Separa deliberadamente la <em>ubicación</em> —los paquetes implicados, que
 * responden a la pregunta «dónde»— de la <em>evidencia</em> —las referencias
 * entre clases y los valores medidos, que responden a «por qué»—. Esa distinción
 * es la que permite que un reporte sea auditable y no una simple alarma.</p>
 *
 * @param regla       nombre de la regla que produjo el hallazgo.
 * @param descripcion enunciado legible del incumplimiento.
 * @param ubicacion   paquetes implicados en el hallazgo.
 * @param evidencia   trazas estructurales que sustentan el hallazgo.
 * @param severidad   gravedad asignada por la regla.
 */
public record Violacion(
        String regla,
        String descripcion,
        List<String> ubicacion,
        List<String> evidencia,
        Severidad severidad) {

    public Violacion {
        ubicacion = List.copyOf(ubicacion);
        evidencia = List.copyOf(evidencia);
    }

    /**
     * Gravedad de un hallazgo. El orden de declaración va de mayor a menor
     * gravedad, de modo que {@code compareTo} permita hallar la más severa.
     */
    public enum Severidad {

        CRITICA("Crítica"),
        MAYOR("Mayor"),
        MENOR("Menor");

        private final String etiqueta;

        Severidad(String etiqueta) {
            this.etiqueta = etiqueta;
        }

        public String etiqueta() {
            return etiqueta;
        }

        /** Devuelve la más grave de dos severidades, admitiendo nulos. */
        public static Severidad masGrave(Severidad a, Severidad b) {
            if (a == null) {
                return b;
            }
            if (b == null) {
                return a;
            }
            return a.ordinal() <= b.ordinal() ? a : b;
        }
    }
}
