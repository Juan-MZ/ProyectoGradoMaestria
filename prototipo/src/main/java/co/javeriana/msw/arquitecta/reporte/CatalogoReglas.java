package co.javeriana.msw.arquitecta.reporte;

import co.javeriana.msw.arquitecta.reglas.Regla;

import java.util.List;

/**
 * Imprime el catálogo de <em>Fitness Functions</em> a partir de las propias
 * reglas cargadas.
 *
 * <p>El catálogo deja así de ser un documento que puede quedar desactualizado
 * respecto del código: se deriva de la implementación vigente, de modo que la
 * racionalidad, la unidad de verificación y el criterio de incumplimiento que se
 * publican son necesariamente los que el mecanismo aplica.</p>
 */
public final class CatalogoReglas {

    private CatalogoReglas() {
    }

    public static String imprimir(List<Regla> reglas) {
        StringBuilder sb = new StringBuilder();
        String linea = "=".repeat(78);
        sb.append(linea).append('\n');
        sb.append("  CATÁLOGO DE FITNESS FUNCTIONS\n");
        sb.append(linea).append('\n');
        int i = 1;
        for (Regla regla : reglas) {
            sb.append("\nFF").append(i++).append(". ").append(regla.nombre()).append('\n');
            sb.append("  Propiedad protegida : ").append(regla.propiedad()).append('\n');
            sb.append("  Racionalidad        : ").append(regla.racionalidad()).append('\n');
            sb.append("  Unidad de verificación: ").append(regla.unidadVerificacion()).append('\n');
            sb.append("  Criterio de incumplimiento: ").append(regla.criterioIncumplimiento()).append('\n');
            sb.append("  Severidad asignada  : ").append(regla.severidad().etiqueta()).append('\n');
        }
        sb.append('\n').append(linea).append('\n');
        return sb.toString();
    }
}
