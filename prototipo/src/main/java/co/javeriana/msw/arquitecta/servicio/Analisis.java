package co.javeriana.msw.arquitecta.servicio;

import co.javeriana.msw.arquitecta.config.CargadorConfiguracion;
import co.javeriana.msw.arquitecta.config.ConfiguracionReglas;
import co.javeriana.msw.arquitecta.config.ModoClasificacion;
import co.javeriana.msw.arquitecta.grafo.ConstructorGrafo;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.motor.MotorConformidad;
import co.javeriana.msw.arquitecta.motor.Resultado;
import co.javeriana.msw.arquitecta.reglas.Exenciones;
import co.javeriana.msw.arquitecta.reglas.Regla;
import co.javeriana.msw.arquitecta.reglas.ReglaCiclos;
import co.javeriana.msw.arquitecta.reglas.ReglaConcentracion;
import co.javeriana.msw.arquitecta.reglas.ReglaIntegridadCapas;
import co.javeriana.msw.arquitecta.reporte.CatalogoReglas;
import co.javeriana.msw.arquitecta.reporte.DisposicionGrafo;
import co.javeriana.msw.arquitecta.reporte.GeneradorReporte;
import co.javeriana.msw.arquitecta.reporte.Historico;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Properties;

/**
 * Fachada que ejecuta un análisis completo: resolver el proyecto, cargar o
 * inferir el catálogo, extraer el grafo, evaluar las reglas, clasificar y
 * preparar la presentación.
 *
 * <p>Existe para que haya un único camino de análisis. La consola y la interfaz
 * gráfica la invocan por igual, de modo que es imposible que ambas den
 * resultados distintos para el mismo sistema: cualquier divergencia sería un
 * defecto de presentación, nunca de cálculo.</p>
 */
public final class Analisis {

    /** Umbral de concentración con que se evalúa un sistema del que no hay catálogo. */
    public static final int UMBRAL_CONFIGURACION_MINIMA = CargadorConfiguracion.UMBRAL_CONCENTRACION_POR_DEFECTO;

    /**
     * Parámetros de un análisis.
     *
     * @param sistema   carpeta del proyecto, directorio de clases o archivo {@code .jar}.
     * @param catalogo  archivo de reglas; si es nulo se infiere una configuración mínima.
     * @param modo      {@code cr} o {@code cr+severidad}; si es nulo se usa el del catálogo.
     * @param etiqueta  nombre del sistema en reportes y figuras; si es nulo se deriva de la ruta.
     * @param historico resumen acumulado del que leer la corrida anterior; puede ser nulo.
     */
    public record Solicitud(Path sistema, Path catalogo, String modo, String etiqueta, Path historico) {
    }

    private Analisis() {
    }

    public static ResultadoAnalisis ejecutar(Solicitud solicitud) throws ErrorAnalisis {
        long inicio = System.nanoTime();
        ResolutorProyecto.Origen origen = ResolutorProyecto.resolver(solicitud.sistema());

        ConfiguracionReglas config = solicitud.catalogo() == null
                ? configuracionMinima(origen.ruta())
                : cargar(solicitud.catalogo());

        ModoClasificacion modo;
        try {
            modo = solicitud.modo() == null ? config.modoClasificacion() : ModoClasificacion.desde(solicitud.modo());
        } catch (IllegalArgumentException e) {
            throw new ErrorAnalisis(e.getMessage(), e);
        }

        List<Regla> reglas = reglasDe(config);
        GrafoDependencias grafo;
        try {
            grafo = new ConstructorGrafo(config.raiz(), config.exclusiones()).desdeRuta(origen.ruta());
        } catch (UncheckedIOException e) {
            throw new ErrorAnalisis(e.getMessage(), e);
        }
        if (grafo.nodos().isEmpty()) {
            // Un grafo vacío no es un sistema conforme: es un análisis que no encontró
            // nada que analizar. Declararlo aceptable sería un falso negativo silencioso.
            throw new ErrorAnalisis("No se encontraron clases bajo la raíz '" + config.raiz() + "' en "
                    + origen.ruta() + ".\nCompruebe que la ruta contiene clases compiladas (.class) o un .jar,"
                    + " y no el código fuente, y que la propiedad 'raiz' coincide con sus paquetes.");
        }

        Resultado resultado = new MotorConformidad(config.umbralAceptable(), config.umbralObservacion(), modo)
                .evaluar(grafo, reglas);
        String etiqueta = solicitud.etiqueta() != null && !solicitud.etiqueta().isBlank()
                ? solicitud.etiqueta()
                : nombreDe(solicitud.sistema());
        OptionalDouble crAnterior = Historico.crAnterior(solicitud.historico(), etiqueta);
        String reporte = GeneradorReporte.generar(resultado, grafo, reglas, config, etiqueta, crAnterior);
        DisposicionGrafo disposicion = DisposicionGrafo.calcular(grafo, config, resultado);

        long milisegundos = (System.nanoTime() - inicio) / 1_000_000;
        Path catalogo = solicitud.catalogo() == null ? null : solicitud.catalogo().toAbsolutePath().normalize();
        return new ResultadoAnalisis(etiqueta, origen.ruta(), origen.descripcion(), catalogo, config, grafo,
                resultado, disposicion, reporte, crAnterior, milisegundos);
    }

    /** Catálogo de reglas, tal como lo describe la implementación vigente. */
    public static String catalogo(Path archivo) throws ErrorAnalisis {
        return CatalogoReglas.imprimir(reglasDe(cargar(archivo)));
    }

    static List<Regla> reglasDe(ConfiguracionReglas config) {
        Exenciones compartidos = new Exenciones(config.compartidos());
        return List.of(
                new ReglaIntegridadCapas(config),
                new ReglaCiclos(compartidos),
                new ReglaConcentracion(config.umbralConcentracion(), compartidos));
    }

    private static ConfiguracionReglas cargar(Path archivo) throws ErrorAnalisis {
        if (!Files.isRegularFile(archivo)) {
            throw new ErrorAnalisis("No se encontró el archivo de configuración: " + archivo);
        }
        try {
            return CargadorConfiguracion.cargar(archivo);
        } catch (IllegalArgumentException e) {
            throw new ErrorAnalisis("Configuración inválida: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ErrorAnalisis("No se pudo leer el archivo de configuración " + archivo + ": "
                    + e.getMessage(), e);
        }
    }

    /**
     * Configuración para un sistema del que no se dispone de catálogo: la raíz se
     * infiere y no se declaran capas, de modo que se evalúan los ciclos y la
     * concentración. Es un punto de partida, no un sustituto del catálogo: sin
     * capas declaradas no puede verificarse la estratificación.
     */
    static ConfiguracionReglas configuracionMinima(Path ruta) throws ErrorAnalisis {
        Properties props = new Properties();
        props.setProperty("raiz", inferirRaiz(ruta));
        return CargadorConfiguracion.desde(props);
    }

    /**
     * Raíz de análisis inferida: el prefijo de paquete común a todas las clases. Si
     * las clases no comparten ningún prefijo, se toma el de dos segmentos que
     * reúne más clases, que en la práctica identifica a la organización autora.
     */
    public static String inferirRaiz(Path ruta) throws ErrorAnalisis {
        Map<String, Integer> paquetes;
        try {
            paquetes = ConstructorGrafo.contarPaquetes(ruta);
        } catch (UncheckedIOException e) {
            throw new ErrorAnalisis(e.getMessage(), e);
        }
        if (paquetes.isEmpty()) {
            throw new ErrorAnalisis("No se encontraron clases en paquetes con nombre en " + ruta + ".");
        }
        List<String[]> segmentos = new ArrayList<>();
        for (String paquete : paquetes.keySet()) {
            segmentos.add(paquete.split("\\."));
        }
        List<String> comun = new ArrayList<>();
        for (int i = 0; ; i++) {
            String candidato = null;
            boolean coincide = true;
            for (String[] s : segmentos) {
                if (i >= s.length || (candidato != null && !candidato.equals(s[i]))) {
                    coincide = false;
                    break;
                }
                candidato = s[i];
            }
            if (!coincide || candidato == null) {
                break;
            }
            comun.add(candidato);
        }
        if (!comun.isEmpty()) {
            return String.join(".", comun);
        }
        Map<String, Integer> porPrefijo = new HashMap<>();
        paquetes.forEach((paquete, clases) -> {
            String[] s = paquete.split("\\.");
            String prefijo = s.length >= 2 ? s[0] + "." + s[1] : s[0];
            porPrefijo.merge(prefijo, clases, Integer::sum);
        });
        return porPrefijo.entrySet().stream()
                .max(Comparator.comparingInt((Map.Entry<String, Integer> e) -> e.getValue())
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElseThrow();
    }

    private static String nombreDe(Path ruta) {
        Path nombre = ruta.toAbsolutePath().normalize().getFileName();
        return nombre == null ? ruta.toString() : nombre.toString();
    }
}
