package co.javeriana.msw.arquitecta;

import co.javeriana.msw.arquitecta.grafo.ConstructorGrafo;
import co.javeriana.msw.arquitecta.grafo.GrafoDependencias;
import co.javeriana.msw.arquitecta.servicio.ErrorAnalisis;
import co.javeriana.msw.arquitecta.servicio.ResolutorProyecto;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Reglas de extracción del grafo, verificadas sobre código compilado.
 *
 * <p>Un paquete {@code x.origen} referencia, de una forma distinta cada vez, a
 * diecinueve paquetes; las pruebas compilan ese código y comprueban cuáles de
 * esas formas se convierten en una arista del grafo. Documentan, por tanto, qué
 * cuenta como dependencia para el mecanismo, y fallarían si una actualización de
 * la biblioteca de extracción cambiara esas reglas.</p>
 */
final class PruebasExtraccion {

    private PruebasExtraccion() {
    }

    /** Forma de referencia → si debe producir arista. */
    private static final Map<String, Boolean> ESPERADO = new LinkedHashMap<>();

    static {
        for (String s : List.of("herencia", "interfaz", "campo", "parametro", "retorno", "excepcion", "llamada",
                "instancia", "accesocampo", "anotacion", "generico", "literal", "instanceofp", "arreglo")) {
            ESPERADO.put(s, true);
        }
        for (String s : List.of("anotacionfuente", "conversion", "constante", "local", "importsolo")) {
            ESPERADO.put(s, false);
        }
    }

    static void ejecutar(Verificador v) throws IOException {
        v.grupo("Reglas de extracción del grafo");
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) {
            v.comprueba("se dispone de un compilador (ejecute con un JDK)", false);
            return;
        }
        Path temporal = Files.createTempDirectory("arquitecta-extraccion");
        try {
            Path fuentes = temporal.resolve("src");
            escribirFuentes(fuentes);
            Path clases = temporal.resolve("clases-17");
            compilar(javac, fuentes, clases, "17");
            GrafoDependencias g = new ConstructorGrafo("x").desdeRuta(clases);
            for (Map.Entry<String, Boolean> e : ESPERADO.entrySet()) {
                boolean hay = g.existeArista("x.origen", "x." + e.getKey());
                v.igual((e.getValue() ? "produce arista: " : "no produce arista: ") + descripcion(e.getKey()),
                        e.getValue(), hay);
            }
            v.comprueba("cada arista conserva la referencia concreta entre clases que la origina",
                    g.evidenciaDe("x.origen", "x.llamada").stream().anyMatch(l -> l.contains("calls method")));

            Path recientes = temporal.resolve("clases-recientes");
            Files.createDirectories(recientes.resolve("x"));
            // Cabecera de un .class de Java 26 (versión principal 70).
            Files.write(recientes.resolve("x/Reciente.class"),
                    new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0, 0, 0, 70});
            String mensaje;
            try {
                ResolutorProyecto.resolver(recientes);
                mensaje = null;
            } catch (ErrorAnalisis ex) {
                mensaje = ex.getMessage();
            }
            v.comprueba("un código compilado para una versión de Java que la extracción no reconoce se rechaza"
                    + " en lugar de analizarse a medias", mensaje != null && mensaje.contains("Java 26"));
        } finally {
            borrar(temporal);
        }
    }

    private static String descripcion(String forma) {
        return switch (forma) {
            case "herencia" -> "herencia de clase";
            case "interfaz" -> "implementación de interfaz";
            case "campo" -> "tipo de un campo";
            case "parametro" -> "tipo de un parámetro";
            case "retorno" -> "tipo de retorno";
            case "excepcion" -> "excepción declarada";
            case "llamada" -> "llamada a método";
            case "instancia" -> "creación de instancia";
            case "accesocampo" -> "acceso a un campo";
            case "anotacion" -> "anotación conservada en el bytecode";
            case "generico" -> "argumento de tipo genérico";
            case "literal" -> "literal de clase (X.class)";
            case "instanceofp" -> "comprobación instanceof";
            case "arreglo" -> "tipo de componente de un arreglo";
            case "anotacionfuente" -> "anotación solo de código fuente";
            case "conversion" -> "conversión de tipo (cast)";
            case "constante" -> "constante de tiempo de compilación";
            case "local" -> "variable local sin uso";
            default -> "importación sin uso";
        };
    }

    private static void escribirFuentes(Path raiz) throws IOException {
        Map<String, String> destinos = new LinkedHashMap<>();
        destinos.put("herencia/Base", "public class Base {}");
        destinos.put("interfaz/Contrato", "public interface Contrato {}");
        destinos.put("campo/TipoCampo", "public class TipoCampo {}");
        destinos.put("parametro/TipoParametro", "public class TipoParametro {}");
        destinos.put("retorno/TipoRetorno", "public class TipoRetorno {}");
        destinos.put("excepcion/Fallo", "public class Fallo extends Exception {}");
        destinos.put("llamada/Servicio", "public class Servicio { public static void hacer() {} }");
        destinos.put("instancia/Producto", "public class Producto {}");
        destinos.put("accesocampo/Estado", "public class Estado { public static Object valor = null; }");
        destinos.put("anotacion/Marca", "import java.lang.annotation.*;"
                + " @Retention(RetentionPolicy.RUNTIME) public @interface Marca {}");
        destinos.put("anotacionfuente/MarcaFuente", "import java.lang.annotation.*;"
                + " @Retention(RetentionPolicy.SOURCE) public @interface MarcaFuente {}");
        destinos.put("generico/Elemento", "public class Elemento {}");
        destinos.put("literal/Referida", "public class Referida {}");
        destinos.put("instanceofp/Comprobada", "public class Comprobada {}");
        destinos.put("conversion/Convertida", "public class Convertida {}");
        destinos.put("arreglo/Componente", "public class Componente {}");
        destinos.put("constante/Constantes", "public class Constantes { public static final int LIMITE = 7; }");
        destinos.put("local/SoloLocal", "public class SoloLocal {}");
        destinos.put("importsolo/Importada", "public class Importada {}");
        for (Map.Entry<String, String> d : destinos.entrySet()) {
            String paquete = d.getKey().substring(0, d.getKey().indexOf('/'));
            Path archivo = raiz.resolve("x/" + d.getKey() + ".java");
            Files.createDirectories(archivo.getParent());
            Files.writeString(archivo, "package x." + paquete + ";\n" + d.getValue() + "\n", StandardCharsets.UTF_8);
        }
        String origen = """
                package x.origen;
                import x.importsolo.Importada;
                @x.anotacion.Marca @x.anotacionfuente.MarcaFuente
                public class Origen extends x.herencia.Base implements x.interfaz.Contrato {
                    private x.campo.TipoCampo campo;
                    private java.util.List<x.generico.Elemento> lista;
                    private x.arreglo.Componente[] arreglo;
                    public void parametro(x.parametro.TipoParametro p) {}
                    public x.retorno.TipoRetorno retorno() { return null; }
                    public void lanza() throws x.excepcion.Fallo {}
                    public void cuerpo(Object o) {
                        x.llamada.Servicio.hacer();
                        Object nuevo = new x.instancia.Producto();
                        Object leido = x.accesocampo.Estado.valor;
                        Class<?> c = x.literal.Referida.class;
                        boolean b = o instanceof x.instanceofp.Comprobada;
                        Object conv = (x.conversion.Convertida) o;
                        int limite = x.constante.Constantes.LIMITE;
                        x.local.SoloLocal soloLocal = null;
                    }
                }
                """;
        Path archivo = raiz.resolve("x/origen/Origen.java");
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, origen, StandardCharsets.UTF_8);
    }

    private static void compilar(JavaCompiler javac, Path fuentes, Path destino, String version) throws IOException {
        Files.createDirectories(destino);
        List<String> argumentos = new ArrayList<>(List.of("-g", "--release", version, "-encoding", "UTF-8",
                "-d", destino.toString()));
        try (Stream<Path> archivos = Files.walk(fuentes)) {
            archivos.filter(p -> p.toString().endsWith(".java")).forEach(p -> argumentos.add(p.toString()));
        }
        OutputStream silencio = OutputStream.nullOutputStream();
        int codigo = javac.run(null, silencio, silencio, argumentos.toArray(String[]::new));
        if (codigo != 0) {
            throw new IOException("No se pudo compilar el código de prueba.");
        }
    }

    private static void borrar(Path raiz) {
        try (Stream<Path> rutas = Files.walk(raiz)) {
            rutas.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            // Un temporal que no se pudo borrar no invalida las pruebas.
        }
    }
}
