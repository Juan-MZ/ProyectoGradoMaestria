package co.javeriana.msw.arquitecta.grafo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reconstruye una ruta cíclica concreta dentro de un componente fuertemente
 * conexo.
 *
 * <p>Saber que existe un ciclo entre tres paquetes es un diagnóstico incompleto:
 * para actuar sobre él hace falta conocer el recorrido exacto que lo cierra. Esta
 * clase realiza una búsqueda en profundidad restringida al componente y devuelve
 * el primer camino que regresa al nodo de partida.</p>
 */
public final class BuscadorCiclos {

    private BuscadorCiclos() {
    }

    /**
     * Devuelve una ruta cíclica del componente, expresada como la secuencia de
     * paquetes que la recorre y que termina de nuevo en el paquete inicial. Si el
     * componente no contiene un ciclo alcanzable, devuelve una lista vacía.
     */
    public static List<String> rutaDe(GrafoDependencias grafo, List<String> componente) {
        if (componente.size() < 2) {
            return List.of();
        }
        Set<String> ambito = new HashSet<>(componente);
        String inicio = componente.get(0);
        List<String> camino = new ArrayList<>();
        camino.add(inicio);
        if (buscar(grafo, ambito, inicio, inicio, new HashSet<>(Set.of(inicio)), camino)) {
            return List.copyOf(camino);
        }
        return List.of();
    }

    private static boolean buscar(GrafoDependencias grafo, Set<String> ambito, String actual,
            String objetivo, Set<String> visitados, List<String> camino) {
        for (String siguiente : grafo.destinosDe(actual)) {
            if (!ambito.contains(siguiente)) {
                continue;
            }
            if (siguiente.equals(objetivo)) {
                camino.add(objetivo);
                return true;
            }
            if (visitados.add(siguiente)) {
                camino.add(siguiente);
                if (buscar(grafo, ambito, siguiente, objetivo, visitados, camino)) {
                    return true;
                }
                camino.remove(camino.size() - 1);
            }
        }
        return false;
    }
}
