package co.javeriana.msw.arquitecta.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catálogo de reglas expresado como configuración: raíz de análisis, capas
 * declaradas, matriz de dependencias permitidas, exenciones, umbral de
 * concentración y parámetros de clasificación.
 *
 * <p>Todo el conocimiento sobre qué es admisible en un sistema concreto reside
 * aquí y no en el código de las reglas; ese deslinde es lo que permite reutilizar
 * el mismo motor sobre arquitecturas distintas sin recompilarlo.</p>
 *
 * <p>Las exenciones merecen una nota. Un catálogo que no admita excepciones
 * obliga a elegir entre tolerar falsos positivos o apagar la regla entera, y
 * ninguna de las dos salidas es aceptable. La configuración distingue por ello
 * dos situaciones: el código que no pertenece al sistema evaluado
 * ({@code exclusiones}) y el que sí pertenece pero cuyo papel arquitectónico
 * justifica un trato distinto ({@code compartidos}). Ambas se declaran de forma
 * explícita y el reporte las enumera, de modo que la excepción quede registrada
 * y sea auditable en lugar de quedar oculta en el código.</p>
 */
public record ConfiguracionReglas(
        String raiz,
        List<Capa> capas,
        Map<String, Set<String>> permitidos,
        List<String> exclusiones,
        List<String> compartidos,
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
        exclusiones = List.copyOf(exclusiones);
        compartidos = List.copyOf(compartidos);
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

    /**
     * Indica si un paquete queda fuera del análisis por no formar parte del
     * sistema evaluado: código vendorizado, generado o de terceros que el
     * empaquetado sitúa bajo la misma raíz.
     */
    public boolean excluido(String paquete) {
        if (paquete == null) {
            return false;
        }
        for (String prefijo : exclusiones) {
            if (paquete.equals(prefijo) || paquete.startsWith(prefijo + ".")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Indica si un paquete desempeña el papel de núcleo compartido. Un paquete de
     * anotaciones, una raíz de composición o una fachada concentran dependencias
     * por diseño; medirlos con el mismo rasero que a un componente de negocio
     * produce hallazgos que no corresponden a ninguna deriva.
     *
     * <p>A diferencia de las exclusiones, la comparación es exacta: un papel lo
     * desempeña un paquete concreto y no se hereda hacia los subpaquetes.</p>
     */
    public boolean esCompartido(String paquete) {
        return paquete != null && compartidos.contains(paquete);
    }

    /** Indica si hay alguna exención declarada, de cualquiera de los dos tipos. */
    public boolean tieneExenciones() {
        return !exclusiones.isEmpty() || !compartidos.isEmpty();
    }
}
