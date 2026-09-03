package co.javeriana.msw.arquitecta.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catálogo de reglas expresado como configuración: raíz de análisis, capas
 * declaradas, matriz de dependencias permitidas, umbral de concentración y
 * parámetros de clasificación.
 *
 * <p>Todo el conocimiento sobre qué es admisible en un sistema concreto reside
 * aquí y no en el código de las reglas; ese deslinde es lo que permite reutilizar
 * el mismo motor sobre arquitecturas distintas sin recompilarlo.</p>
 */
public record ConfiguracionReglas(
        String raiz,
        List<Capa> capas,
        Map<String, Set<String>> permitidos,
        int umbralConcentracion,
        double umbralAceptable,
        double umbralObservacion,
        ModoClasificacion modoClasificacion) {

    public ConfiguracionReglas {
        if (raiz == null || raiz.isBlank()) {
            throw new IllegalArgumentException("La raíz de análisis es obligatoria.");
        }
        if (umbralObservacion > umbralAceptable) {
            throw new IllegalArgumentException("El umbral de observación (" + umbralObservacion
                    + ") no puede superar al de aceptación (" + umbralAceptable + ").");
        }
        capas = List.copyOf(capas);
        permitidos = Map.copyOf(permitidos);
    }

    /**
     * Nombre de la capa a la que pertenece un paquete, o {@code null} si no fue
     * asignado a ninguna (por ejemplo, un dominio compartido).
     */
    public String capaDe(String paquete) {
        for (Capa capa : capas) {
            if (capa.contiene(paquete)) {
                return capa.nombre();
            }
        }
        return null;
    }

    /** Indica si está permitida una dependencia directa entre dos capas. */
    public boolean permitido(String desde, String hacia) {
        return destinosPermitidos(desde).contains(hacia);
    }

    /** Capas hacia las que la capa indicada puede depender según la matriz declarada. */
    public Set<String> destinosPermitidos(String capa) {
        Set<String> destinos = permitidos.get(capa);
        return destinos == null ? Set.of() : destinos;
    }

    /** Nombres de las capas declaradas, en el orden en que fueron definidas. */
    public List<String> nombresCapas() {
        return capas.stream().map(Capa::nombre).toList();
    }
}
