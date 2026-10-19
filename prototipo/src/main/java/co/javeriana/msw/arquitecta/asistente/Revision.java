package co.javeriana.msw.arquitecta.asistente;

import co.javeriana.msw.arquitecta.servicio.Perfil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Comprobaciones deterministas sobre una propuesta del asistente.
 *
 * <p>Las instrucciones piden al modelo que no legitime la deriva, pero un modelo
 * de lenguaje no garantiza cumplirlas: en la evaluación con un modelo real hubo
 * propuestas que elevaban el umbral de concentración justo por encima del paquete
 * que lo excedía. Lo que no puede garantizarse con instrucciones se comprueba
 * aquí, con reglas fijas, y se advierte a la persona antes de que adopte el
 * catálogo.</p>
 */
public final class Revision {

    private Revision() {
    }

    /** Advertencias sobre la propuesta; vacía si no hay nada que señalar. */
    public static List<String> avisos(Propuesta propuesta, Perfil perfil) {
        List<String> avisos = new ArrayList<>();
        avisarExencionDelMasAcoplado(propuesta, perfil, avisos);
        Integer umbral = propuesta.umbralConcentracion();
        if (umbral == null) {
            return avisos;
        }
        List<Perfil.Paquete> candidatos = perfil.paquetes().stream()
                .filter(p -> propuesta.exenciones().stream().noneMatch(e -> cubre(e, p.nombre())))
                .sorted(Comparator.comparingInt(Perfil.Paquete::ce).reversed())
                .toList();
        if (candidatos.isEmpty()) {
            return avisos;
        }
        Perfil.Paquete mayor = candidatos.get(0);
        int segundo = candidatos.size() > 1 ? candidatos.get(1).ce() : 0;
        // Solo cuando el paquete más acoplado destaca sobre el resto: si el umbral
        // queda justo por encima de un caso atípico, es probable que se eligiera
        // para no señalarlo.
        if (umbral >= mayor.ce() && mayor.ce() > segundo) {
            avisos.add("Con el umbral propuesto (" + umbral + ") ningún paquete actual se señala por concentración."
                    + " El más acoplado es " + mayor.nombre() + " (Ce = " + mayor.ce() + "; el siguiente, " + segundo
                    + "): confirme que el umbral no se eligió para no señalarlo. Si su concentración es legítima,"
                    + " declárelo como exención justificada; si no lo es, baje el umbral.");
        }
        return avisos;
    }

    /**
     * Una exención retira el paquete de FF2 y FF3, de modo que eximir al paquete
     * que más concentra oculta la concentración igual que un umbral alto. Con un
     * modelo local se observó exactamente eso: el paquete concentrador declarado
     * como «compartido». Se advierte solo cuando el paquete destaca sobre el resto.
     */
    private static void avisarExencionDelMasAcoplado(Propuesta propuesta, Perfil perfil, List<String> avisos) {
        List<Perfil.Paquete> todos = perfil.paquetes().stream()
                .sorted(Comparator.comparingInt(Perfil.Paquete::ce).reversed())
                .toList();
        if (todos.isEmpty()) {
            return;
        }
        Perfil.Paquete mayor = todos.get(0);
        int segundo = todos.size() > 1 ? todos.get(1).ce() : 0;
        if (mayor.ce() <= segundo) {
            return;
        }
        propuesta.exenciones().stream().filter(e -> cubre(e, mayor.nombre())).findFirst().ifPresent(e -> avisos.add(
                "La propuesta exime a " + mayor.nombre() + ", el paquete que más dependencias salientes concentra (Ce = "
                        + mayor.ce() + "; el siguiente, " + segundo + ")" + (e.justificacion() == null
                                || e.justificacion().isBlank() ? "" : ", con el motivo «" + e.justificacion() + "»")
                        + ". Una exención lo retira de los ciclos y de la concentración: confirme que concentra por su"
                        + " papel (raíz de composición, fachada) y que no es la deriva que se quiere detectar."));
    }

    private static boolean cubre(Propuesta.Exencion e, String paquete) {
        return e.tipo().equals("compartido") ? e.paquete().equals(paquete)
                : paquete.equals(e.paquete()) || paquete.startsWith(e.paquete() + ".");
    }
}
