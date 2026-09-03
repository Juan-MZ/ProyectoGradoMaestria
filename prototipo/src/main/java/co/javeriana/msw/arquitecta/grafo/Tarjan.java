package co.javeriana.msw.arquitecta.grafo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementación del algoritmo de Tarjan para hallar componentes fuertemente
 * conexos (SCC). Un SCC de tamaño mayor a uno evidencia un ciclo de dependencias.
 */
public class Tarjan {

    private final GrafoDependencias grafo;
    private int indice = 0;
    private final Deque<String> pila = new ArrayDeque<>();
    private final Map<String, Integer> indices = new HashMap<>();
    private final Map<String, Integer> bajos = new HashMap<>();
    private final Set<String> enPila = new HashSet<>();
    private final List<List<String>> componentes = new ArrayList<>();

    public Tarjan(GrafoDependencias grafo) {
        this.grafo = grafo;
    }

    public List<List<String>> componentesFuertementeConexos() {
        for (String nodo : grafo.nodos()) {
            if (!indices.containsKey(nodo)) {
                fuerteConexo(nodo);
            }
        }
        return componentes;
    }

    private void fuerteConexo(String v) {
        indices.put(v, indice);
        bajos.put(v, indice);
        indice++;
        pila.push(v);
        enPila.add(v);

        for (String w : grafo.destinosDe(v)) {
            if (!indices.containsKey(w)) {
                fuerteConexo(w);
                bajos.put(v, Math.min(bajos.get(v), bajos.get(w)));
            } else if (enPila.contains(w)) {
                bajos.put(v, Math.min(bajos.get(v), indices.get(w)));
            }
        }

        if (bajos.get(v).equals(indices.get(v))) {
            List<String> componente = new ArrayList<>();
            String w;
            do {
                w = pila.pop();
                enPila.remove(w);
                componente.add(w);
            } while (!w.equals(v));
            componentes.add(componente);
        }
    }
}
