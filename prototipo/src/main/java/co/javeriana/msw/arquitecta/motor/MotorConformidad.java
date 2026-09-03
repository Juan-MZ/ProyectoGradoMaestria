package co.javeriana.msw.arquitecta.motor;

import java.util.ArrayList;
import java.util.List;

import co.javeriana.msw.arquitecta.config.ModoClasificacion;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.reglas.Regla;
import co.javeriana.msw.arquitecta.reglas.ResultadoRegla;
import co.javeriana.msw.arquitecta.reglas.Violacion;

/**
 * Motor de validación. Ejecuta el catálogo de reglas sobre el grafo, agrega sus
 * verificaciones en una Tasa de Conformidad (CR) y clasifica el estado
 * arquitectónico resultante.
 *
 * <p>La CR se define como la proporción de verificaciones atómicas superadas:
 * {@code CR = 100 × (verificaciones − incumplimientos) / verificaciones}. Al ser
 * un cociente explícito, cualquier valor puede reconstruirse a partir de los
 * hallazgos individuales que lo produjeron.</p>
 *
 * <p>Sobre esa base se aplican dos criterios complementarios. El primero, la
 * <em>extensión</em> del incumplimiento, se lee de la CR frente a sus umbrales.
 * El segundo, la <em>gravedad</em>, se lee de la severidad del peor hallazgo.
 * En el modo {@link ModoClasificacion#CR_SEVERIDAD} el veredicto final adopta el
 * más restrictivo de ambos, porque una violación aislada pero severa no debería
 * quedar diluida por el tamaño del sistema que la contiene.</p>
 */
public class MotorConformidad {

    private final double umbralAceptable;
    private final double umbralObservacion;
    private final ModoClasificacion modo;

    /** Motor con los umbrales y el modo definidos por omisión (85 %, 70 %, CR + severidad). */
    public MotorConformidad() {
        this(85.0, 70.0, ModoClasificacion.CR_SEVERIDAD);
    }

    public MotorConformidad(double umbralAceptable, double umbralObservacion, ModoClasificacion modo) {
        if (umbralObservacion > umbralAceptable) {
            throw new IllegalArgumentException("El umbral de observación no puede superar al de aceptación.");
        }
        this.umbralAceptable = umbralAceptable;
        this.umbralObservacion = umbralObservacion;
        this.modo = modo == null ? ModoClasificacion.CR_SEVERIDAD : modo;
    }

    public Resultado evaluar(GrafoDependencias grafo, List<Regla> reglas) {
        List<ResultadoRegla> porRegla = new ArrayList<>();
        int verificaciones = 0;
        int incumplimientos = 0;
        Violacion.Severidad severidadMaxima = null;

        for (Regla regla : reglas) {
            ResultadoRegla resultado = regla.evaluar(grafo);
            porRegla.add(resultado);
            verificaciones += resultado.verificaciones();
            incumplimientos += resultado.incumplimientos();
            for (Violacion violacion : resultado.violaciones()) {
                severidadMaxima = Violacion.Severidad.masGrave(severidadMaxima, violacion.severidad());
            }
        }

        double cr = verificaciones == 0
                ? 100.0
                : 100.0 * (verificaciones - incumplimientos) / verificaciones;

        EstadoArquitectonico porCR = clasificarPorCR(cr);
        EstadoArquitectonico porSeveridad = clasificarPorSeveridad(severidadMaxima);
        EstadoArquitectonico estadoFinal = modo == ModoClasificacion.CR
                ? porCR
                : EstadoArquitectonico.masRestrictivo(porCR, porSeveridad);

        return new Resultado(cr, porCR, severidadMaxima, porSeveridad, estadoFinal, modo,
                porRegla, verificaciones, incumplimientos, umbralAceptable, umbralObservacion);
    }

    /** Estado deducido de la extensión del incumplimiento, medida por la CR. */
    public EstadoArquitectonico clasificarPorCR(double cr) {
        if (cr >= umbralAceptable) {
            return EstadoArquitectonico.ACEPTABLE;
        }
        if (cr >= umbralObservacion) {
            return EstadoArquitectonico.EN_OBSERVACION;
        }
        return EstadoArquitectonico.NO_CONFORME;
    }

    /** Estado deducido de la gravedad del peor hallazgo detectado. */
    public EstadoArquitectonico clasificarPorSeveridad(Violacion.Severidad severidad) {
        if (severidad == null) {
            return EstadoArquitectonico.ACEPTABLE;
        }
        return switch (severidad) {
            case CRITICA -> EstadoArquitectonico.NO_CONFORME;
            case MAYOR -> EstadoArquitectonico.EN_OBSERVACION;
            case MENOR -> EstadoArquitectonico.ACEPTABLE;
        };
    }
}
