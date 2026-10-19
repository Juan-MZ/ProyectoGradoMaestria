package co.javeriana.msw.arquitecta;

import co.javeriana.msw.arquitecta.motor.EstadoArquitectonico;
import co.javeriana.msw.arquitecta.servicio.Analisis;
import co.javeriana.msw.arquitecta.servicio.ErrorAnalisis;
import co.javeriana.msw.arquitecta.servicio.ResolutorProyecto;
import co.javeriana.msw.arquitecta.servicio.ResultadoAnalisis;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

/**
 * Pruebas del servicio de análisis que comparten la línea de comandos y la
 * interfaz gráfica.
 *
 * <p>A diferencia de las pruebas del núcleo, estas recorren la ruta real de
 * extracción sobre los escenarios compilados en {@code out/}: lo que se verifica
 * es que el usuario pueda señalar la carpeta de un proyecto tal como la deja su
 * herramienta de construcción y obtenga exactamente el mismo diagnóstico que el
 * experimento controlado.</p>
 */
final class PruebasServicio {

    private static final Path BASE = Path.of("out", "caso-estudio");
    private static final Path C4 = Path.of("out", "caso-estudio-c4-combinado");
    private static final Path NUCLEO = Path.of("out", "arquitecta-core");
    private static final Path CATALOGO = Path.of("rules.properties");
    private static final double EPS = 0.001;

    private PruebasServicio() {
    }

    static void ejecutar(Verificador v) throws IOException {
        v.grupo("Servicio de análisis: selección del proyecto");
        if (!Files.isDirectory(BASE) || !Files.isDirectory(C4)) {
            v.comprueba("los escenarios compilados existen (ejecute build antes de las pruebas)", false);
            return;
        }
        Path temporal = Files.createTempDirectory("arquitecta-servicio");
        try {
            resolucion(v, temporal);
            analisis(v, temporal);
        } finally {
            borrar(temporal);
        }
    }

    private static void resolucion(Verificador v, Path temporal) throws IOException {
        Path maven = temporal.resolve("proyecto-maven");
        copiar(BASE, maven.resolve("target/classes"));
        Files.createDirectories(maven.resolve("src/main/java"));
        ResolutorProyecto.Origen origenMaven = resolverSinError(maven);
        v.comprueba("un proyecto Maven se resuelve a target/classes",
                origenMaven != null && origenMaven.ruta().endsWith(Path.of("target", "classes")));
        v.comprueba("el origen describe la convención reconocida",
                origenMaven != null && origenMaven.descripcion().startsWith("Proyecto Maven"));

        Path gradle = temporal.resolve("proyecto-gradle");
        copiar(BASE, gradle.resolve("build/classes/java/main"));
        ResolutorProyecto.Origen origenGradle = resolverSinError(gradle);
        v.comprueba("un proyecto Gradle se resuelve a build/classes/java/main",
                origenGradle != null && origenGradle.ruta().endsWith(Path.of("build", "classes", "java", "main")));

        ResolutorProyecto.Origen directo = resolverSinError(BASE);
        v.comprueba("una carpeta con clases se analiza tal cual",
                directo != null && directo.ruta().equals(BASE.toAbsolutePath().normalize()));

        Path fuente = temporal.resolve("sin-compilar");
        Files.createDirectories(fuente.resolve("src/main/java/com/demo"));
        Files.writeString(fuente.resolve("src/main/java/com/demo/A.java"), "package com.demo; class A {}");
        String mensaje = error(() -> ResolutorProyecto.resolver(fuente));
        v.comprueba("un proyecto sin compilar se rechaza pidiendo compilarlo",
                mensaje != null && mensaje.contains("no está compilado"));

        Path vacia = Files.createDirectories(temporal.resolve("vacia"));
        v.comprueba("una carpeta sin clases se rechaza", error(() -> ResolutorProyecto.resolver(vacia)) != null);
        v.comprueba("una ruta inexistente se rechaza",
                error(() -> ResolutorProyecto.resolver(temporal.resolve("no-existe"))) != null);
        Path texto = Files.writeString(temporal.resolve("notas.txt"), "x");
        v.comprueba("un archivo que no es .jar se rechaza", error(() -> ResolutorProyecto.resolver(texto)) != null);

        v.comprueba("sin catálogo en la carpeta no se sugiere ninguno",
                ResolutorProyecto.catalogoSugerido(maven).isEmpty());
        Files.copy(CATALOGO, maven.resolve("rules.properties"));
        v.comprueba("el catálogo que trae el proyecto se sugiere",
                ResolutorProyecto.catalogoSugerido(maven).map(p -> p.endsWith("rules.properties")).orElse(false));
    }

    private static void analisis(Verificador v, Path temporal) throws IOException {
        v.grupo("Servicio de análisis: paridad con el experimento");

        ResultadoAnalisis c4 = analizar(C4, CATALOGO);
        v.comprueba("el escenario combinado se analiza", c4 != null);
        if (c4 == null) {
            return;
        }
        v.igual("la CR coincide con la del experimento (C4)", 57.14, c4.resultado().conformidad(), 0.01);
        v.igual("se reportan los cuatro hallazgos inducidos", 4, c4.resultado().totalViolaciones());
        v.igual("el estado es No conforme", EstadoArquitectonico.NO_CONFORME, c4.resultado().estado());
        v.igual("la etiqueta por defecto es el nombre de la carpeta", "caso-estudio-c4-combinado", c4.etiqueta());
        v.igual("se dispone el grafo de los cinco paquetes", 5, c4.disposicion().nodos().size());

        Path maven = temporal.resolve("c4-maven");
        copiar(C4, maven.resolve("target/classes"));
        ResultadoAnalisis desdeMaven = analizar(maven, CATALOGO);
        v.comprueba("analizar la carpeta del proyecto Maven da el mismo diagnóstico",
                desdeMaven != null && Math.abs(desdeMaven.resultado().conformidad() - c4.resultado().conformidad()) < EPS
                        && desdeMaven.resultado().totalViolaciones() == 4);

        Path jar = temporal.resolve("c4.jar");
        empaquetar(C4, jar);
        ResultadoAnalisis desdeJar = analizar(jar, CATALOGO);
        v.comprueba("analizar el .jar da el mismo diagnóstico que la carpeta",
                desdeJar != null && Math.abs(desdeJar.resultado().conformidad() - c4.resultado().conformidad()) < EPS
                        && desdeJar.dependencias() == c4.dependencias());

        Path jarVacio = temporal.resolve("vacio.jar");
        empaquetar(Files.createDirectories(temporal.resolve("nada")), jarVacio);
        v.comprueba("un .jar sin clases se rechaza en lugar de dar un 100 % vacío",
                error(() -> Analisis.ejecutar(new Analisis.Solicitud(jarVacio, CATALOGO, null, null, null))) != null);

        ResultadoAnalisis minima = analizar(BASE, null);
        v.comprueba("sin catálogo se aplica la configuración mínima", minima != null && minima.configuracionMinima());
        if (minima != null) {
            v.igual("la raíz se infiere del código", "com.caso", minima.raiz());
            v.comprueba("la configuración mínima no declara capas", minima.capas().isEmpty());
            v.igual("la línea base no tiene ciclos ni concentración", 0, minima.resultado().totalViolaciones());
        }
        ResultadoAnalisis minimaC4 = analizar(C4, null);
        v.comprueba("la configuración mínima detecta el ciclo inducido sin catálogo",
                minimaC4 != null && minimaC4.resultado().violaciones().stream()
                        .anyMatch(x -> x.regla().startsWith("Ciclos")));

        try {
            v.igual("la raíz del propio mecanismo se infiere", "co.javeriana.msw.arquitecta",
                    Analisis.inferirRaiz(NUCLEO));
        } catch (ErrorAnalisis e) {
            v.comprueba("la raíz del propio mecanismo se infiere: " + e.getMessage(), false);
        }

        Path evidencia = temporal.resolve("evidencia");
        String base = c4.exportarEvidencia(evidencia);
        v.comprueba("la exportación escribe reporte, JSON, hallazgos y grafo",
                Stream.of(".txt", ".json", "-hallazgos.csv", ".svg", ".tikz", ".dot")
                        .allMatch(ext -> Files.isRegularFile(evidencia.resolve(base + ext))));
        v.igual("la exportación registra una fila en el resumen", 2,
                Files.readAllLines(evidencia.resolve("resumen.csv")).size());

        Path historico = temporal.resolve("historial.csv");
        ResultadoAnalisis primero = analizar(BASE, CATALOGO, historico);
        if (primero != null) {
            primero.registrarEn(historico);
        }
        ResultadoAnalisis segundo = analizar(BASE, CATALOGO, historico);
        v.comprueba("el segundo análisis conoce la CR del anterior",
                segundo != null && segundo.crAnterior().isPresent()
                        && Math.abs(segundo.crAnterior().getAsDouble() - 100.0) < EPS);
    }

    // ------------------------------------------------------------ utilidades

    private interface Accion {
        void ejecutar() throws Exception;
    }

    /** Mensaje del {@link ErrorAnalisis} que lanza la acción, o {@code null} si no lanza ninguno. */
    private static String error(Accion accion) {
        try {
            accion.ejecutar();
            return null;
        } catch (ErrorAnalisis e) {
            return e.getMessage();
        } catch (Exception e) {
            return null;
        }
    }

    private static ResolutorProyecto.Origen resolverSinError(Path ruta) {
        try {
            return ResolutorProyecto.resolver(ruta);
        } catch (ErrorAnalisis e) {
            return null;
        }
    }

    private static ResultadoAnalisis analizar(Path sistema, Path catalogo) {
        return analizar(sistema, catalogo, null);
    }

    private static ResultadoAnalisis analizar(Path sistema, Path catalogo, Path historico) {
        try {
            return Analisis.ejecutar(new Analisis.Solicitud(sistema, catalogo, null, null, historico));
        } catch (ErrorAnalisis e) {
            return null;
        }
    }

    private static void copiar(Path origen, Path destino) throws IOException {
        try (Stream<Path> rutas = Files.walk(origen)) {
            for (Path p : (Iterable<Path>) rutas::iterator) {
                Path d = destino.resolve(origen.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(d);
                } else {
                    Files.copy(p, d);
                }
            }
        }
    }

    private static void empaquetar(Path clases, Path jar) throws IOException {
        try (OutputStream salida = Files.newOutputStream(jar);
             JarOutputStream j = new JarOutputStream(salida);
             Stream<Path> rutas = Files.walk(clases)) {
            List<Path> archivos = rutas.filter(Files::isRegularFile).sorted().toList();
            for (Path p : archivos) {
                j.putNextEntry(new JarEntry(clases.relativize(p).toString().replace('\\', '/')));
                Files.copy(p, j);
                j.closeEntry();
            }
        }
    }

    private static void borrar(Path raiz) {
        try (Stream<Path> rutas = Files.walk(raiz)) {
            rutas.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException | UncheckedIOException e) {
            // Un temporal que no se pudo borrar no invalida las pruebas.
        }
    }
}
