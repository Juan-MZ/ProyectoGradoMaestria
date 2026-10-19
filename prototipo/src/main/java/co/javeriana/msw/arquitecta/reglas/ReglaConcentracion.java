package co.javeriana.msw.arquitecta.reglas;

import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;

import java.util.ArrayList;
import java.util.List;

/**
 * <em>Fitness Function</em> de concentración excesiva de dependencias salientes.
 *
 * <p>Marca como candidato a «componente dios» todo paquete cuyo acoplamiento
 * eferente (Ce) supere el umbral configurado. Un paquete que conoce a casi todos
 * los demás se convierte en el centro gravitacional del sistema: cualquier
 * cambio en él repercute en cascada y ninguna parte puede evolucionar sin
 * atravesarlo.</p>
 *
 * <p>La regla es puramente topológica y, por tanto, ciega al papel que cada
 * paquete desempeña: una raíz de composición o una fachada presentan un Ce
 * elevado por diseño, no por deterioro. Las exenciones declaradas corrigen esa
 * ceguera sin desactivar la regla para el resto del sistema.</p>
 */
public class ReglaConcentracion implements Regla {

    /** Nombre de la regla, con el que se identifican sus hallazgos. */
    public static final String NOMBRE = "Concentración de dependencias salientes";

    private final int umbral;
    private final Exenciones exentos;

    public ReglaConcentracion(int umbral) {
        this(umbral, Exenciones.NINGUNA);
    }

    public ReglaConcentracion(int umbral, Exenciones exentos) {
        if (umbral < 0) {
            throw new IllegalArgumentException("El umbral de concentración no puede ser negativo.");
        }
        this.umbral = umbral;
        this.exentos = exentos == null ? Exenciones.NINGUNA : exentos;
    }

    public int umbral() {
        return umbral;
    }

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public String propiedad() {
        return "Ningún paquete concentra más dependencias salientes de las que el umbral "
                + "declarado considera admisible.";
    }

    @Override
    public String racionalidad() {
        return "Un acoplamiento eferente elevado señala un paquete que asumió más "
                + "responsabilidades de las que le corresponden; su modificación arrastra a "
                + "todo el sistema y su reemplazo se vuelve inviable.";
    }

    @Override
    public String unidadVerificacion() {
        return "Cada paquete del grafo no exento.";
    }

    @Override
    public String criterioIncumplimiento() {
        return "El acoplamiento eferente del paquete (Ce) supera el umbral configurado (" + umbral + ").";
    }

    @Override
    public Violacion.Severidad severidad() {
        return Violacion.Severidad.MAYOR;
    }

    @Override
    public ResultadoRegla evaluar(GrafoDependencias grafo) {
        List<Violacion> violaciones = new ArrayList<>();
        int verificaciones = 0;

        for (String nodo : grafo.nodos()) {
            if (exentos.cubre(nodo)) {
                continue;
            }
            verificaciones++;
            int ce = grafo.acoplamientoEferente(nodo);
            if (ce <= umbral) {
                continue;
            }
            List<String> evidencia = new ArrayList<>();
            evidencia.add("Ce = " + ce + " (umbral: " + umbral + "), Ca = " + grafo.acoplamientoAferente(nodo));
            evidencia.add("Depende de: " + String.join(", ", grafo.destinosDe(nodo)));
            violaciones.add(new Violacion(
                    nombre(),
                    "El paquete '" + nodo + "' concentra " + ce
                            + " dependencias salientes (umbral: " + umbral + ").",
                    List.of(nodo),
                    evidencia,
                    severidad()));
        }
        return new ResultadoRegla(nombre(), verificaciones, violaciones.size(), violaciones);
    }
}
