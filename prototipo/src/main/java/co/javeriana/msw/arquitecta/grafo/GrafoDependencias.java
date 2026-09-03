package co.javeriana.msw.arquitecta.grafo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grafo dirigido en el que cada nodo es un paquete y cada arista representa una
 * dependencia entre paquetes (el origen depende del destino).
 *
 * <p>Junto a cada arista el grafo conserva la evidencia que la originó —las
 * referencias concretas entre clases detectadas por el análisis estático—, de
 * modo que todo hallazgo pueda rastrearse hasta el punto exacto del código que
 * lo produjo y no se quede en una afirmación sobre paquetes.</p>
 */
public class GrafoDependencias {

    /** Separador interno de la clave de una arista. Un punto no aparece nunca solo. */
    private static final String SEP = " ==> ";

    private final Set<String> nodos = new LinkedHashSet<>();
    private final Map<String, Set<String>> dependencias = new LinkedHashMap<>();
    private final Map<String, Set<String>> evidencias = new LinkedHashMap<>();

    public void agregarNodo(String nodo) {
        nodos.add(nodo);
        dependencias.putIfAbsent(nodo, new LinkedHashSet<>());
    }

    public void agregarArista(String origen, String destino) {
        agregarArista(origen, destino, null);
    }

    /**
     * Registra una dependencia entre dos paquetes. Los autobucles (un paquete que
     * depende de sí mismo) se ignoran por carecer de significado arquitectónico.
     *
     * @param detalle evidencia concreta de la dependencia (por ejemplo, la
     *                referencia entre clases que la origina); puede ser nula.
     */
    public void agregarArista(String origen, String destino, String detalle) {
        agregarNodo(origen);
        agregarNodo(destino);
        if (origen.equals(destino)) {
            return;
        }
        dependencias.get(origen).add(destino);
        if (detalle != null && !detalle.isBlank()) {
            evidencias.computeIfAbsent(clave(origen, destino), k -> new LinkedHashSet<>()).add(detalle);
        }
    }

    public Set<String> nodos() {
        return Collections.unmodifiableSet(nodos);
    }

    public Set<String> destinosDe(String origen) {
        Set<String> destinos = dependencias.get(origen);
        return destinos == null ? Set.of() : Collections.unmodifiableSet(destinos);
    }

    /** Evidencia registrada para una arista concreta; vacía si no se capturó ninguna. */
    public List<String> evidenciaDe(String origen, String destino) {
        Set<String> detalles = evidencias.get(clave(origen, destino));
        return detalles == null ? List.of() : List.copyOf(detalles);
    }

    public boolean existeNodo(String nodo) {
        return nodos.contains(nodo);
    }

    public boolean existeArista(String origen, String destino) {
        return destinosDe(origen).contains(destino);
    }

    public int cantidadAristas() {
        int total = 0;
        for (Set<String> destinos : dependencias.values()) {
            total += destinos.size();
        }
        return total;
    }

    /** Acoplamiento eferente (Ce): paquetes distintos de los que depende un nodo. */
    public int acoplamientoEferente(String nodo) {
        return destinosDe(nodo).size();
    }

    /** Acoplamiento aferente (Ca): paquetes distintos que dependen de un nodo. */
    public int acoplamientoAferente(String nodo) {
        int total = 0;
        for (String origen : nodos) {
            if (!origen.equals(nodo) && destinosDe(origen).contains(nodo)) {
                total++;
            }
        }
        return total;
    }

    /** Aristas del grafo como pares ordenados origen-destino, en orden de inserción. */
    public List<String[]> aristas() {
        List<String[]> lista = new ArrayList<>();
        for (String origen : nodos) {
            for (String destino : destinosDe(origen)) {
                lista.add(new String[] {origen, destino});
            }
        }
        return lista;
    }

    private static String clave(String origen, String destino) {
        return origen + SEP + destino;
    }
}
