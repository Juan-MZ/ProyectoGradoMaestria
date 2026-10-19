package co.javeriana.msw.arquitecta.servicio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Traduce la carpeta de un proyecto en la ruta que realmente debe analizarse.
 *
 * <p>Quien usa la herramienta piensa en su proyecto, no en el directorio donde su
 * herramienta de construcción deja las clases compiladas. Pedirle que conozca esa
 * ruta ---{@code target/classes} en Maven, {@code build/classes/java/main} en
 * Gradle, {@code out/production} en IntelliJ, {@code bin} en Eclipse--- es una
 * fricción innecesaria. El resolutor reconoce esas convenciones y, si el proyecto
 * todavía no se ha compilado, lo dice con claridad en lugar de devolver un
 * análisis vacío.</p>
 */
public final class ResolutorProyecto {

    /** Profundidad máxima de búsqueda de clases, para no recorrer árboles enormes. */
    private static final int PROFUNDIDAD = 14;

    private static final List<String> CATALOGOS = List.of(
            "rules.properties", "arquitecta.properties", "reglas.properties", ".arquitecta.properties");

    /** Ruta efectivamente analizable y descripción de cómo se identificó. */
    public record Origen(Path ruta, String descripcion) {
    }

    private ResolutorProyecto() {
    }

    /**
     * Versión de Java más reciente cuyo código compilado reconoce la biblioteca de
     * extracción (ArchUnit 1.3.0). Los archivos de versiones posteriores no se
     * rechazan: se omiten sin aviso, de modo que un análisis sobre ellos quedaría
     * incompleto ---o vacío--- sin que nada lo advirtiera.
     */
    public static final int VERSION_JAVA_MAXIMA = 23;

    /**
     * Localiza el código compilado del proyecto y comprueba que la biblioteca de
     * extracción puede leerlo por completo.
     */
    public static Origen resolver(Path seleccion) throws ErrorAnalisis {
        Origen origen = localizar(seleccion);
        int version = versionJava(origen.ruta());
        if (version > VERSION_JAVA_MAXIMA) {
            throw new ErrorAnalisis("Hay clases compiladas para Java " + version + ", y la biblioteca de extracción"
                    + " solo reconoce hasta Java " + VERSION_JAVA_MAXIMA + ": las clases más recientes se omitirían y el"
                    + " diagnóstico quedaría incompleto. Compile el proyecto con una versión de destino igual o"
                    + " anterior (por ejemplo, --release 21, o <maven.compiler.release>21</maven.compiler.release>"
                    + " en Maven) y vuelva a analizarlo.");
        }
        return origen;
    }

    /**
     * Versión de Java más alta entre las clases de la ruta, leída de la cabecera de
     * cada archivo {@code .class} (versión principal menos 44); 0 si no hay clases.
     */
    static int versionJava(Path ruta) {
        int maxima = 0;
        try {
            if (Files.isRegularFile(ruta)) {
                try (java.util.jar.JarFile jar = new java.util.jar.JarFile(ruta.toFile())) {
                    var entradas = jar.entries();
                    while (entradas.hasMoreElements()) {
                        var e = entradas.nextElement();
                        // Las clases de META-INF/versions son variantes opcionales de un
                        // .jar multiversión: la JVM las ignora si no las necesita.
                        if (e.getName().endsWith(".class") && !e.getName().startsWith("META-INF/")) {
                            try (java.io.InputStream in = jar.getInputStream(e)) {
                                maxima = Math.max(maxima, version(in.readNBytes(8)));
                            }
                        }
                    }
                }
            } else {
                try (Stream<Path> archivos = Files.walk(ruta, PROFUNDIDAD)) {
                    for (Path p : (Iterable<Path>) archivos::iterator) {
                        if (p.toString().endsWith(".class") && Files.isRegularFile(p)) {
                            try (java.io.InputStream in = Files.newInputStream(p)) {
                                maxima = Math.max(maxima, version(in.readNBytes(8)));
                            }
                        }
                    }
                }
            }
        } catch (IOException | UncheckedIOException e) {
            // Si no puede leerse la cabecera, el importador informará del problema.
        }
        return maxima;
    }

    private static int version(byte[] cabecera) {
        if (cabecera.length < 8 || (cabecera[0] & 0xFF) != 0xCA || (cabecera[1] & 0xFF) != 0xFE) {
            return 0;
        }
        return (((cabecera[6] & 0xFF) << 8) | (cabecera[7] & 0xFF)) - 44;
    }

    private static Origen localizar(Path seleccion) throws ErrorAnalisis {
        if (seleccion == null) {
            throw new ErrorAnalisis("No se ha indicado ningún proyecto para analizar.");
        }
        Path ruta = seleccion.toAbsolutePath().normalize();
        if (!Files.exists(ruta)) {
            throw new ErrorAnalisis("No existe la ruta seleccionada: " + ruta);
        }
        if (Files.isRegularFile(ruta)) {
            if (esJar(ruta)) {
                return new Origen(ruta, "Archivo JAR");
            }
            throw new ErrorAnalisis("El archivo seleccionado no es un .jar ni una carpeta de proyecto: " + ruta);
        }

        record Convencion(String subruta, String descripcion) {
        }
        List<Convencion> convenciones = List.of(
                new Convencion("target/classes", "Proyecto Maven · target/classes"),
                new Convencion("build/classes/java/main", "Proyecto Gradle · build/classes/java/main"),
                new Convencion("build/classes/kotlin/main", "Proyecto Gradle · build/classes/kotlin/main"),
                new Convencion("bin", "Proyecto Eclipse · bin"));
        for (Convencion c : convenciones) {
            Path candidata = ruta.resolve(c.subruta());
            if (Files.isDirectory(candidata) && contieneClases(candidata)) {
                return new Origen(candidata, c.descripcion());
            }
        }
        Path produccion = ruta.resolve("out").resolve("production");
        if (Files.isDirectory(produccion) && contieneClases(produccion)) {
            return new Origen(produccion, "Proyecto IntelliJ IDEA · out/production");
        }
        if (contieneClases(ruta)) {
            return new Origen(ruta, "Clases compiladas en la carpeta seleccionada");
        }
        Optional<Path> jar = jarGenerado(ruta);
        if (jar.isPresent()) {
            return new Origen(jar.get(), "JAR generado por el proyecto · " + ruta.relativize(jar.get()));
        }
        if (contieneFuentes(ruta)) {
            throw new ErrorAnalisis("El proyecto no está compilado: contiene archivos .java pero ninguna clase"
                    + " compilada. Compílelo primero (por ejemplo, con «mvn compile» o «gradle build») y vuelva"
                    + " a analizarlo; el mecanismo examina el código compilado, no el fuente.");
        }
        throw new ErrorAnalisis("No se encontraron clases compiladas (.class) ni archivos .jar en " + ruta + ".");
    }

    /**
     * Catálogo de reglas que el proyecto trae consigo, si lo hay: un archivo con
     * alguno de los nombres habituales en la carpeta del proyecto o, en su
     * defecto, en la carpeta que la contiene. Lo segundo cubre el caso de varios
     * módulos ---o varias versiones de un mismo sistema--- que comparten un único
     * catálogo declarado en su carpeta común.
     */
    public static Optional<Path> catalogoSugerido(Path seleccion) {
        if (seleccion == null) {
            return Optional.empty();
        }
        Path absoluta = seleccion.toAbsolutePath().normalize();
        Path carpeta = Files.isRegularFile(absoluta) ? absoluta.getParent() : absoluta;
        List<Path> lugares = new ArrayList<>();
        if (carpeta != null) {
            lugares.add(carpeta);
            if (carpeta.getParent() != null) {
                lugares.add(carpeta.getParent());
            }
        }
        for (Path lugar : lugares) {
            for (String nombre : CATALOGOS) {
                Path candidato = lugar.resolve(nombre);
                if (Files.isRegularFile(candidato)) {
                    return Optional.of(candidato);
                }
            }
        }
        return Optional.empty();
    }

    static boolean esJar(Path ruta) {
        return ruta.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar");
    }

    static boolean contieneClases(Path carpeta) {
        return existeArchivo(carpeta, n -> n.endsWith(".class") && !n.equals("module-info.class"));
    }

    private static boolean contieneFuentes(Path carpeta) {
        return existeArchivo(carpeta, n -> n.endsWith(".java"));
    }

    private static boolean existeArchivo(Path carpeta, java.util.function.Predicate<String> nombre) {
        try (Stream<Path> recorrido = Files.walk(carpeta, PROFUNDIDAD)) {
            return recorrido.anyMatch(p -> Files.isRegularFile(p)
                    && nombre.test(p.getFileName().toString().toLowerCase(Locale.ROOT)));
        } catch (IOException | UncheckedIOException e) {
            return false;
        }
    }

    /** JAR producido por la construcción, excluidos los de fuentes, documentación y pruebas. */
    private static Optional<Path> jarGenerado(Path ruta) {
        List<Path> candidatos = new ArrayList<>();
        for (String carpeta : List.of("target", "build/libs")) {
            Path dir = ruta.resolve(carpeta);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> archivos = Files.list(dir)) {
                archivos.filter(p -> Files.isRegularFile(p) && esJar(p))
                        .filter(p -> {
                            String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                            return !n.endsWith("-sources.jar") && !n.endsWith("-javadoc.jar")
                                    && !n.endsWith("-tests.jar") && !n.endsWith("-test.jar");
                        })
                        .forEach(candidatos::add);
            } catch (IOException e) {
                // Una carpeta ilegible simplemente no aporta candidatos.
            }
        }
        candidatos.sort(Comparator.comparing(Path::toString));
        return candidatos.stream().findFirst();
    }
}
