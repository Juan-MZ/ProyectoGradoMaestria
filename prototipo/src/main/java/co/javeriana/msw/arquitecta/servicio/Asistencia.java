package co.javeriana.msw.arquitecta.servicio;

import co.javeriana.msw.arquitecta.config.CargadorConfiguracion;
import co.javeriana.msw.arquitecta.config.ConfiguracionReglas;
import co.javeriana.msw.arquitecta.grafo.ConstructorGrafo;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.reporte.CatalogoReglas;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Operaciones que necesita quien ayuda a redactar el catálogo de reglas de un
 * proyecto: describir su estructura, validar un catálogo propuesto y
 * previsualizar el diagnóstico que produciría.
 *
 * <p>Ninguna de ellas decide nada. Un catálogo propuesto ---lo redacte una
 * persona o un asistente--- se valida con el mismo cargador que cualquier otro y
 * se evalúa con las mismas tres reglas: la ayuda puede equivocarse al proponer,
 * pero no puede alterar lo que el mecanismo verifica ni cómo lo hace.</p>
 */
public final class Asistencia {

    private Asistencia() {
    }

    /** Describe la estructura del proyecto: paquetes, clases y dependencias, sin código fuente. */
    public static Perfil perfil(Path proyecto) throws ErrorAnalisis {
        ResolutorProyecto.Origen origen = ResolutorProyecto.resolver(proyecto);
        String raiz = Analisis.inferirRaiz(origen.ruta());
        GrafoDependencias grafo;
        Map<String, List<String>> nombres;
        try {
            grafo = new ConstructorGrafo(raiz).desdeRuta(origen.ruta());
            nombres = ConstructorGrafo.nombresDeClases(origen.ruta());
        } catch (UncheckedIOException e) {
            throw new ErrorAnalisis(e.getMessage(), e);
        }
        Map<String, Integer> clases = ConstructorGrafo.contarPaquetes(origen.ruta());

        List<Perfil.Paquete> paquetes = new ArrayList<>();
        grafo.nodos().stream().sorted().forEach(p -> {
            List<String> todos = nombres.getOrDefault(p, List.of());
            paquetes.add(new Perfil.Paquete(p, clases.getOrDefault(p, todos.size()),
                    grafo.acoplamientoEferente(p), grafo.acoplamientoAferente(p),
                    todos.subList(0, Math.min(todos.size(), Perfil.EJEMPLOS_POR_PAQUETE))));
        });
        List<Perfil.Dependencia> dependencias = new ArrayList<>();
        for (String[] arista : grafo.aristas()) {
            dependencias.add(new Perfil.Dependencia(arista[0], arista[1],
                    Math.max(1, grafo.evidenciaDe(arista[0], arista[1]).size())));
        }
        dependencias.sort((a, b) -> (a.origen() + " " + a.destino()).compareTo(b.origen() + " " + b.destino()));
        Path nombre = proyecto.toAbsolutePath().normalize().getFileName();
        return new Perfil(nombre == null ? proyecto.toString() : nombre.toString(), raiz, paquetes, dependencias);
    }

    /**
     * Descripción de las tres <em>Fitness Functions</em>, derivada de la
     * implementación vigente: es lo que el asistente debe saber de ellas.
     */
    public static String descripcionReglas() {
        Properties props = new Properties();
        props.setProperty("raiz", "x");
        return CatalogoReglas.imprimir(Analisis.reglasDe(CargadorConfiguracion.desde(props)));
    }

    /**
     * Valida un catálogo escrito como texto {@code .properties}.
     *
     * @return el motivo por el que no es válido, o vacío si lo es.
     */
    public static Optional<String> validar(String catalogo) {
        try {
            Properties props = new Properties();
            props.load(new StringReader(catalogo));
            ConfiguracionReglas config = CargadorConfiguracion.desde(props);
            if (config.capas().isEmpty()) {
                return Optional.of("El catálogo no declara capas: sin ellas no se verifica la integridad de capas.");
            }
            return Optional.empty();
        } catch (IOException | IllegalArgumentException e) {
            return Optional.of(e.getMessage());
        }
    }

    /**
     * Analiza el proyecto con un catálogo todavía no guardado, para ver qué
     * diagnóstico produciría antes de adoptarlo.
     */
    public static ResultadoAnalisis previsualizar(Path proyecto, String catalogo) throws ErrorAnalisis {
        Optional<String> error = validar(catalogo);
        if (error.isPresent()) {
            throw new ErrorAnalisis("El catálogo propuesto no es válido: " + error.get());
        }
        Path temporal = null;
        try {
            temporal = Files.createTempFile("catalogo-propuesto", ".properties");
            // Latin-1, como lee Java los archivos de propiedades; lo que no
            // representa se sustituye en lugar de abortar.
            Files.write(temporal, catalogo.getBytes(StandardCharsets.ISO_8859_1));
            return Analisis.ejecutar(new Analisis.Solicitud(proyecto, temporal, null, null, null));
        } catch (IOException e) {
            throw new ErrorAnalisis("No se pudo preparar la previsualización: " + e.getMessage(), e);
        } finally {
            if (temporal != null) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException e) {
                    // Un temporal que no se pudo borrar no afecta al resultado.
                }
            }
        }
    }
}
