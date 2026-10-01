package co.javeriana.msw.arquitecta.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Lee el catálogo de reglas desde un archivo {@code .properties}.
 *
 * <p>La carga es estricta con lo que no puede inferirse (la raíz de análisis) y
 * permisiva con lo que tiene un valor razonable por omisión (umbrales y modo de
 * clasificación), de modo que un archivo mínimo siga siendo válido y el
 * comportamiento por defecto coincida con el definido en el anteproyecto.</p>
 */
public final class CargadorConfiguracion {

    /** Umbral de conformidad a partir del cual el estado se considera aceptable. */
    public static final double UMBRAL_ACEPTABLE_POR_DEFECTO = 85.0;

    /** Umbral de conformidad a partir del cual el estado se considera en observación. */
    public static final double UMBRAL_OBSERVACION_POR_DEFECTO = 70.0;

    /** Umbral de acoplamiento eferente por defecto. */
    public static final int UMBRAL_CONCENTRACION_POR_DEFECTO = 10;

    private CargadorConfiguracion() {
    }

    public static ConfiguracionReglas cargar(Path archivo) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(archivo)) {
            props.load(in);
        }
        return desde(props);
    }

    static ConfiguracionReglas desde(Properties props) {
        String raiz = texto(props, "raiz", null);
        if (raiz == null) {
            throw new IllegalArgumentException("La propiedad 'raiz' es obligatoria.");
        }

        List<Capa> capas = new ArrayList<>();
        for (String nombre : lista(props.getProperty("capas", ""))) {
            List<String> paquetes = lista(props.getProperty("capa." + nombre, ""));
            if (paquetes.isEmpty()) {
                throw new IllegalArgumentException("La capa '" + nombre
                        + "' está declarada en 'capas' pero no define la propiedad 'capa." + nombre + "'.");
            }
            capas.add(new Capa(nombre, paquetes));
        }

        Map<String, Set<String>> permitidos = new LinkedHashMap<>();
        List<String> nombresCapas = capas.stream().map(Capa::nombre).toList();
        for (Capa capa : capas) {
            Set<String> destinos = new LinkedHashSet<>(lista(props.getProperty("permitido." + capa.nombre(), "")));
            for (String destino : destinos) {
                if (!nombresCapas.contains(destino)) {
                    throw new IllegalArgumentException("La matriz de dependencias permitidas de la capa '"
                            + capa.nombre() + "' referencia una capa no declarada: '" + destino + "'.");
                }
            }
            permitidos.put(capa.nombre(), destinos);
        }

        List<String> exclusiones = lista(props.getProperty("excluir", ""));
        List<String> compartidos = lista(props.getProperty("rol.compartido", ""));
        validarExenciones(raiz, exclusiones, "excluir");
        validarExenciones(raiz, compartidos, "rol.compartido");
        for (String compartido : compartidos) {
            if (exclusiones.contains(compartido)) {
                throw new IllegalArgumentException("El paquete '" + compartido
                        + "' figura a la vez en 'excluir' y en 'rol.compartido'; son tratamientos"
                        + " incompatibles: el primero lo saca del análisis y el segundo lo mantiene"
                        + " dentro con exenciones.");
            }
        }

        return new ConfiguracionReglas(
                raiz,
                capas,
                permitidos,
                exclusiones,
                compartidos,
                entero(props, "umbral.concentracion", UMBRAL_CONCENTRACION_POR_DEFECTO),
                decimal(props, "umbral.cr.aceptable", UMBRAL_ACEPTABLE_POR_DEFECTO),
                decimal(props, "umbral.cr.observacion", UMBRAL_OBSERVACION_POR_DEFECTO),
                ModoClasificacion.desde(props.getProperty("clasificacion.modo")));
    }

    /**
     * Una exención que no cuelga de la raíz de análisis no puede tener efecto,
     * porque el grafo nunca contendrá ese paquete. Casi siempre es una errata, y
     * fallar es preferible a aplicar en silencio una excepción que el usuario
     * cree activa.
     */
    private static void validarExenciones(String raiz, List<String> prefijos, String propiedad) {
        for (String prefijo : prefijos) {
            if (!prefijo.equals(raiz) && !prefijo.startsWith(raiz + ".")) {
                throw new IllegalArgumentException("La propiedad '" + propiedad + "' declara '" + prefijo
                        + "', que no pertenece a la raíz de análisis '" + raiz + "' y por tanto no"
                        + " tendría ningún efecto.");
            }
        }
    }

    private static List<String> lista(String csv) {
        List<String> valores = new ArrayList<>();
        for (String parte : csv.split(",")) {
            String limpio = parte.trim();
            if (!limpio.isEmpty()) {
                valores.add(limpio);
            }
        }
        return valores;
    }

    private static String texto(Properties props, String clave, String porDefecto) {
        String valor = props.getProperty(clave);
        return (valor == null || valor.isBlank()) ? porDefecto : valor.trim();
    }

    private static int entero(Properties props, String clave, int porDefecto) {
        String valor = texto(props, clave, null);
        if (valor == null) {
            return porDefecto;
        }
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La propiedad '" + clave
                    + "' debe ser un número entero; se encontró: '" + valor + "'.");
        }
    }

    private static double decimal(Properties props, String clave, double porDefecto) {
        String valor = texto(props, clave, null);
        if (valor == null) {
            return porDefecto;
        }
        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La propiedad '" + clave
                    + "' debe ser un número; se encontró: '" + valor + "'.");
        }
    }
}
