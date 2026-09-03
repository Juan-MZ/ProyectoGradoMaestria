package co.javeriana.msw.arquitecta.reglas;

import java.util.ArrayList;
import java.util.List;

import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;

/**
 * <em>Fitness Function</em> de concentración excesiva de dependencias salientes.
 *
 * <p>Marca como candidato a «componente dios» todo paquete cuyo acoplamiento
 * eferente (Ce) supere el umbral configurado. Un paquete que conoce a casi todos
 * los demás se convierte en el centro gravitacional del sistema: cualquier
 * cambio en él repercute en cascada y ninguna parte puede evolucionar sin
 * atravesarlo.</p>
 */
public class ReglaConcentracion implements Regla {

    private final int umbral;

    public ReglaConcentracion(int umbral) {
        if (umbral < 0) {
            throw new IllegalArgumentException("El umbral de concentración no puede ser negativo.");
        }
        this.umbral = umbral;
    }

    public int umbral() {
        return umbral;
    }

    @Override
    public String nombre() {
        return "Concentración de dependencias salientes";
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
        return "Cada paquete del grafo.";
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
        for (String nodo : grafo.nodos()) {
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
        return new ResultadoRegla(nombre(), grafo.nodos().size(), violaciones.size(), violaciones);
    }
}
