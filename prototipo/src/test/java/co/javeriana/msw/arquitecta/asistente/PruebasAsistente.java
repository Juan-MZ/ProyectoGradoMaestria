package co.javeriana.msw.arquitecta.asistente;

import co.javeriana.msw.arquitecta.Verificador;
import co.javeriana.msw.arquitecta.asistente.ClienteModelo.ErrorModelo;
import co.javeriana.msw.arquitecta.asistente.ClienteModelo.Mensaje;
import co.javeriana.msw.arquitecta.servicio.Asistencia;
import co.javeriana.msw.arquitecta.servicio.ErrorAnalisis;
import co.javeriana.msw.arquitecta.servicio.Perfil;
import co.javeriana.msw.arquitecta.servicio.ResultadoAnalisis;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pruebas del asistente de catálogo.
 *
 * <p>No consultan ningún modelo real: un servidor HTTP local, incluido en el JDK,
 * imita las API de Anthropic y de OpenAI y devuelve respuestas fijadas. Así se
 * verifica lo que es responsabilidad de la herramienta ---qué se envía, cómo se
 * interpreta la respuesta, que la propuesta pase por el mismo cargador que un
 * catálogo escrito a mano y que su diagnóstico sea el del experimento--- sin
 * depender de la red, de una clave ni de la variabilidad de un modelo.</p>
 */
public final class PruebasAsistente {

    private static final Path C4 = Path.of("caso-estudio", "c4-combinado");
    private static final Path C0 = Path.of("caso-estudio", "c0-base");

    /** Propuesta equivalente al catálogo del caso de estudio. */
    private static final String PROPUESTA = """
            Reconozco una arquitectura en tres capas con un dominio compartido.

            ```json
            {
              "raiz": "com.caso",
              "capas": [
                {"nombre": "Presentación", "paquetes": ["com.caso.presentacion"], "justificacion": "Controladores — punto de entrada."},
                {"nombre": "negocio", "paquetes": ["com.caso.negocio"], "justificacion": "Servicios de la aplicación."},
                {"nombre": "persistencia", "paquetes": ["com.caso.persistencia"], "justificacion": "Repositorios."}
              ],
              "permitido": {"presentacion": ["negocio"], "negocio": ["persistencia"], "persistencia": []},
              "umbral_concentracion": {"valor": 3, "justificacion": "Ningún paquete sano supera tres dependencias."},
              "exenciones": [],
              "derivas_probables": ["com.caso.persistencia -> com.caso.negocio: invierte la estratificación"],
              "preguntas": ["¿util es una utilidad transversal?"]
            }
            ```
            """;

    private PruebasAsistente() {
    }

    public static void ejecutar(Verificador v) throws IOException {
        json(v);
        propuesta(v);
        conversacion(v);
        clientes(v);
        servicio(v);
    }

    private static void json(Verificador v) {
        v.grupo("Asistente: lectura y escritura de JSON");
        Object leido = Json.leer("{\"a\": [1, 2.5, true, null, \"x\\n\\u00e1\"], \"b\": {}}");
        v.comprueba("se lee un documento con todos los tipos",
                leido instanceof Map<?, ?> m && m.get("a") instanceof List<?> l && l.size() == 5
                        && "x\ná".equals(l.get(4)) && Boolean.TRUE.equals(l.get(2)));
        String escrito = Json.escribir(Map.of("texto", "comillas \" y\nsalto"));
        v.comprueba("la escritura escapa comillas y saltos de línea",
                escrito.equals("{\"texto\":\"comillas \\\" y\\nsalto\"}"));
        v.comprueba("lo escrito se vuelve a leer igual",
                Json.leer(escrito) instanceof Map<?, ?> m && "comillas \" y\nsalto".equals(m.get("texto")));
        v.rechaza("un documento truncado se rechaza", IllegalArgumentException.class, () -> Json.leer("{\"a\": [1, 2"));
    }

    private static void propuesta(Verificador v) {
        v.grupo("Asistente: propuesta de catálogo");
        Conversacion.Respuesta r = Conversacion.interpretar(PROPUESTA);
        v.comprueba("se separa la explicación del bloque JSON",
                r.explicacion().startsWith("Reconozco") && !r.explicacion().contains("```"));
        v.comprueba("se interpreta la propuesta", r.propuesta() != null);
        if (r.propuesta() == null) {
            return;
        }
        Propuesta p = r.propuesta();
        v.igual("el nombre de capa se normaliza (minúsculas, sin acentos)", "presentacion", p.capas().get(0).nombre());
        v.igual("se conservan las preguntas para la persona", 1, p.preguntas().size());
        String catalogo = p.aCatalogo("c4-combinado", "modelo-de-prueba");
        v.comprueba("el catálogo generado es válido para el cargador de la herramienta",
                Asistencia.validar(catalogo).isEmpty());
        v.comprueba("las justificaciones quedan como comentarios", catalogo.contains("# presentacion: Controladores"));
        v.comprueba("los comentarios solo usan caracteres Latin-1",
                catalogo.chars().allMatch(c -> c <= 0xFF) && catalogo.contains("Controladores - punto"));
        v.comprueba("la deriva probable queda registrada en el catálogo",
                catalogo.contains("com.caso.persistencia -> com.caso.negocio"));

        v.comprueba("una respuesta sin catálogo se acepta como solo explicación",
                Conversacion.interpretar("¿Qué papel cumple el paquete util?").propuesta() == null);
        v.comprueba("un JSON sin raíz se informa como error de formato",
                Conversacion.interpretar("```json\n{\"capas\": []}\n```").errorFormato() != null);
        v.comprueba("una dependencia hacia una capa inexistente vuelve inválido el catálogo",
                Asistencia.validar(Propuesta.desdeJson(Json.leer("""
                        {"raiz": "com.caso", "capas": [{"nombre": "a", "paquetes": ["com.caso.a"]}],
                         "permitido": {"a": ["fantasma"]}}""")).aCatalogo("x", "y")).isPresent());
        v.comprueba("las instrucciones prohíben legitimar la deriva existente",
                Conversacion.instrucciones("").contains("No permita una dependencia solo porque existe"));
    }

    private static void conversacion(Verificador v) throws IOException {
        v.grupo("Asistente: conversación");
        AtomicReference<String> cuerpo = new AtomicReference<>();
        HttpServer servidor = servidor(cuerpo, 200, anthropic(PROPUESTA));
        try {
            ClienteModelo cliente = new ClienteModelo(new ConfiguracionModelo(Proveedor.ANTHROPIC,
                    url(servidor), "modelo-de-prueba", "clave"), Duration.ofSeconds(10));
            Conversacion c = new Conversacion(cliente, "PERFIL-DE-PRUEBA", "REGLAS-DE-PRUEBA");
            Conversacion.Respuesta r1 = c.proponer("Usamos MVC");
            v.comprueba("el primer turno envía el perfil y el contexto de la persona",
                    cuerpo.get().contains("PERFIL-DE-PRUEBA") && cuerpo.get().contains("Usamos MVC"));
            v.comprueba("las instrucciones describen las Fitness Functions de la herramienta",
                    cuerpo.get().contains("REGLAS-DE-PRUEBA"));
            v.comprueba("el primer turno devuelve una propuesta", r1.propuesta() != null);
            c.responder("util es transversal", "raiz=com.caso\n# editado a mano", "CR 57.14 %");
            Object enviado = Json.leer(cuerpo.get());
            List<?> mensajes = enviado instanceof Map<?, ?> m && m.get("messages") instanceof List<?> l ? l : List.of();
            v.igual("el segundo turno conserva el historial (persona, asistente, persona)", 3, mensajes.size());
            v.comprueba("el segundo turno envía el catálogo editado y la previsualización",
                    cuerpo.get().contains("editado a mano") && cuerpo.get().contains("CR 57.14"));
        } catch (ErrorModelo e) {
            v.comprueba("la conversación no falla: " + e.getMessage(), false);
        } finally {
            servidor.stop(0);
        }
    }

    private static void clientes(Verificador v) throws IOException {
        v.grupo("Asistente: clientes de modelos");
        AtomicReference<String> cuerpo = new AtomicReference<>();
        AtomicReference<Map<String, List<String>>> cabeceras = new AtomicReference<>();

        HttpServer anthropic = servidor(cuerpo, cabeceras, 200, anthropic("hola"));
        try {
            ClienteModelo c = new ClienteModelo(new ConfiguracionModelo(Proveedor.ANTHROPIC, url(anthropic),
                    "claude-de-prueba", "sk-prueba"), Duration.ofSeconds(10));
            String texto = c.completar("sistema", List.of(new Mensaje("user", "pregunta")));
            v.igual("Anthropic: se devuelve el texto de la respuesta", "hola", texto);
            v.comprueba("Anthropic: la clave viaja en x-api-key, con la versión de la API",
                    "sk-prueba".equals(primera(cabeceras.get(), "x-api-key"))
                            && primera(cabeceras.get(), "anthropic-version") != null);
            Object enviado = Json.leer(cuerpo.get());
            v.comprueba("Anthropic: el sistema y el modelo van en el cuerpo",
                    enviado instanceof Map<?, ?> m && "sistema".equals(m.get("system"))
                            && "claude-de-prueba".equals(m.get("model")) && m.get("max_tokens") != null);
        } catch (ErrorModelo e) {
            v.comprueba("Anthropic: la consulta no falla: " + e.getMessage(), false);
        } finally {
            anthropic.stop(0);
        }

        HttpServer openai = servidor(cuerpo, cabeceras, 200,
                "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"hola local\"}}],"
                        + "\"data\":[{\"id\":\"llama3.1\"},{\"id\":\"qwen2.5\"}]}");
        try {
            ConfiguracionModelo local = new ConfiguracionModelo(Proveedor.OLLAMA, url(openai) + "/v1", "llama3.1", "");
            ClienteModelo c = new ClienteModelo(local, Duration.ofSeconds(10));
            v.igual("OpenAI/Ollama: se devuelve el texto de la respuesta", "hola local",
                    c.completar("sistema", List.of(new Mensaje("user", "pregunta"))));
            Object enviado = Json.leer(cuerpo.get());
            v.comprueba("OpenAI/Ollama: el sistema viaja como primer mensaje",
                    enviado instanceof Map<?, ?> m && m.get("messages") instanceof List<?> l
                            && l.get(0) instanceof Map<?, ?> primero && "system".equals(primero.get("role")));
            v.comprueba("OpenAI/Ollama: un modelo local no requiere clave ni envía Authorization",
                    local.incompleta() == null && primera(cabeceras.get(), "Authorization") == null);
            v.igual("OpenAI/Ollama: se listan los modelos instalados", List.of("llama3.1", "qwen2.5"), c.modelos());
        } catch (ErrorModelo e) {
            v.comprueba("OpenAI/Ollama: la consulta no falla: " + e.getMessage(), false);
        } finally {
            openai.stop(0);
        }

        HttpServer rechazo = servidor(cuerpo, cabeceras, 401,
                "{\"type\":\"error\",\"error\":{\"type\":\"authentication_error\",\"message\":\"invalid x-api-key\"}}");
        try {
            new ClienteModelo(new ConfiguracionModelo(Proveedor.ANTHROPIC, url(rechazo), "m", "mala"),
                    Duration.ofSeconds(10)).completar("s", List.of(new Mensaje("user", "x")));
            v.comprueba("una clave inválida se informa", false);
        } catch (ErrorModelo e) {
            v.comprueba("una clave inválida se informa en términos de la persona",
                    e.getMessage().startsWith("La clave de API no es válida") && e.getMessage().contains("invalid x-api-key"));
        } finally {
            rechazo.stop(0);
        }

        try {
            new ClienteModelo(new ConfiguracionModelo(Proveedor.OLLAMA, "http://127.0.0.1:9/v1", "m", ""),
                    Duration.ofSeconds(5)).completar("s", List.of(new Mensaje("user", "x")));
            v.comprueba("un servicio local apagado se informa", false);
        } catch (ErrorModelo e) {
            v.comprueba("un servicio local apagado se informa preguntando si está en ejecución",
                    e.getMessage().contains("¿Está en ejecución Ollama?"));
        }

        ConfiguracionModelo sinClave = new ConfiguracionModelo(Proveedor.OPENAI, Proveedor.OPENAI.urlPorDefecto(), "m", "");
        v.comprueba("un proveedor remoto sin clave no se consulta",
                System.getenv("OPENAI_API_KEY") != null || sinClave.incompleta() != null);

        Path temporal = Files.createTempFile("asistente", ".properties");
        try {
            new ConfiguracionModelo(Proveedor.LM_STUDIO, "http://localhost:1234/v1", "qwen", "secreta").guardar(temporal, false);
            ConfiguracionModelo leida = ConfiguracionModelo.cargar(temporal);
            v.comprueba("la configuración se conserva entre sesiones",
                    leida.proveedor() == Proveedor.LM_STUDIO && leida.modelo().equals("qwen"));
            v.comprueba("la clave no se guarda si la persona no lo pide",
                    leida.clave().isEmpty() && !Files.readString(temporal).contains("secreta"));
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private static void servicio(Verificador v) {
        v.grupo("Asistente: perfil y previsualización");
        if (!Files.isDirectory(C4.resolve("target/classes"))) {
            v.comprueba("los escenarios compilados existen (compile antes de las pruebas)", false);
            return;
        }
        try {
            Perfil perfil = Asistencia.perfil(C4);
            v.igual("el perfil infiere la raíz", "com.caso", perfil.raiz());
            v.igual("el perfil reúne los cinco paquetes", 5, perfil.paquetes().size());
            v.igual("el perfil reúne las once dependencias", 11, perfil.dependencias().size());
            String texto = perfil.describir();
            v.comprueba("el perfil incluye nombres de clase que delatan el papel de cada paquete",
                    texto.contains("ControladorVenta") && texto.contains("RepositorioVenta"));
            v.comprueba("el perfil no incluye código fuente ni firmas de métodos",
                    !texto.contains("void ") && !texto.contains("return ") && !texto.contains("registrarVenta"));
            v.comprueba("la descripción de las reglas sale de la implementación",
                    Asistencia.descripcionReglas().contains("Integridad de capas"));

            String catalogo = Conversacion.interpretar(PROPUESTA).propuesta().aCatalogo("c4", "prueba");
            ResultadoAnalisis c4 = Asistencia.previsualizar(C4, catalogo);
            v.igual("previsualizar la propuesta sobre C4 reproduce el diagnóstico del experimento", 57.14,
                    c4.resultado().conformidad(), 0.01);
            ResultadoAnalisis c0 = Asistencia.previsualizar(C0, catalogo);
            v.igual("y sobre la línea base no encuentra deriva", 0, c0.resultado().totalViolaciones());
            try {
                Asistencia.previsualizar(C4, "capas=a");
                v.comprueba("un catálogo inválido no se previsualiza", false);
            } catch (ErrorAnalisis e) {
                v.comprueba("un catálogo inválido no se previsualiza", e.getMessage().contains("no es válido"));
            }
        } catch (ErrorAnalisis e) {
            v.comprueba("el servicio de asistencia no falla: " + e.getMessage(), false);
        }
    }

    // ------------------------------------------------------------- apoyo

    private static String anthropic(String texto) {
        return Json.escribir(Map.of("type", "message", "role", "assistant",
                "content", List.of(Map.of("type", "text", "text", texto))));
    }

    private static HttpServer servidor(AtomicReference<String> cuerpo, int estado, String respuesta) throws IOException {
        return servidor(cuerpo, new AtomicReference<>(), estado, respuesta);
    }

    private static HttpServer servidor(AtomicReference<String> cuerpo, AtomicReference<Map<String, List<String>>> cabeceras,
            int estado, String respuesta) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/", intercambio -> {
            cuerpo.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            cabeceras.set(Map.copyOf(intercambio.getRequestHeaders()));
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length);
            try (OutputStream out = intercambio.getResponseBody()) {
                out.write(bytes);
            }
        });
        s.start();
        return s;
    }

    private static String url(HttpServer s) {
        return "http://127.0.0.1:" + s.getAddress().getPort();
    }

    private static String primera(Map<String, List<String>> cabeceras, String nombre) {
        for (Map.Entry<String, List<String>> e : cabeceras.entrySet()) {
            if (e.getKey().equalsIgnoreCase(nombre) && !e.getValue().isEmpty()) {
                return e.getValue().get(0);
            }
        }
        return null;
    }
}
