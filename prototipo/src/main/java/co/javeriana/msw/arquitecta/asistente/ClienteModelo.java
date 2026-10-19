package co.javeriana.msw.arquitecta.asistente;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente de un modelo de lenguaje, sobre el cliente HTTP del propio JDK.
 *
 * <p>Implementa los dos protocolos que cubren la gran mayoría de los servicios:
 * el de la API de mensajes de Anthropic y el de {@code chat/completions} de
 * OpenAI, que también ofrecen Ollama y LM Studio para modelos locales.</p>
 */
public final class ClienteModelo {

    /** Un turno de la conversación: {@code user} o {@code assistant}. */
    public record Mensaje(String rol, String texto) {
    }

    /** Fallo al consultar el modelo, con un mensaje dirigido al usuario. */
    public static final class ErrorModelo extends Exception {
        private static final long serialVersionUID = 1L;

        public ErrorModelo(String mensaje) {
            super(mensaje);
        }

        public ErrorModelo(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }

    private static final String VERSION_ANTHROPIC = "2023-06-01";
    private static final int MAXIMO_TOKENS = 8192;

    private final ConfiguracionModelo config;
    private final HttpClient http;
    private final Duration espera;

    public ClienteModelo(ConfiguracionModelo config) {
        this(config, Duration.ofMinutes(5));
    }

    ClienteModelo(ConfiguracionModelo config, Duration espera) {
        this.config = config;
        this.espera = espera;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    /** Envía la conversación y devuelve el texto de la respuesta. */
    public String completar(String sistema, List<Mensaje> mensajes) throws ErrorModelo {
        String incompleta = config.incompleta();
        if (incompleta != null) {
            throw new ErrorModelo(incompleta);
        }
        return config.proveedor().protocolo() == Proveedor.Protocolo.ANTHROPIC
                ? completarAnthropic(sistema, mensajes)
                : completarOpenAI(sistema, mensajes);
    }

    /** Modelos que ofrece el servicio; sirve también para probar la conexión y la clave. */
    public List<String> modelos() throws ErrorModelo {
        if (config.url().isEmpty()) {
            throw new ErrorModelo("Indique la dirección del servicio.");
        }
        boolean anthropic = config.proveedor().protocolo() == Proveedor.Protocolo.ANTHROPIC;
        String url = anthropic ? base() + "/v1/models?limit=100" : base() + "/models";
        Map<?, ?> respuesta = enviar(peticion(url).GET().build());
        List<String> ids = new ArrayList<>();
        if (respuesta.get("data") instanceof List<?> datos) {
            for (Object d : datos) {
                if (d instanceof Map<?, ?> m && m.get("id") instanceof String id) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    // ------------------------------------------------------------- protocolos

    private String completarAnthropic(String sistema, List<Mensaje> mensajes) throws ErrorModelo {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("model", config.modelo());
        cuerpo.put("max_tokens", MAXIMO_TOKENS);
        cuerpo.put("system", sistema);
        List<Object> lista = new ArrayList<>();
        for (Mensaje m : mensajes) {
            lista.add(Map.of("role", m.rol(), "content", m.texto()));
        }
        cuerpo.put("messages", lista);
        Map<?, ?> respuesta = enviar(peticion(base() + "/v1/messages")
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.escribir(cuerpo), StandardCharsets.UTF_8))
                .build());
        StringBuilder texto = new StringBuilder();
        if (respuesta.get("content") instanceof List<?> bloques) {
            for (Object b : bloques) {
                if (b instanceof Map<?, ?> m && "text".equals(m.get("type")) && m.get("text") instanceof String t) {
                    texto.append(t);
                }
            }
        }
        if (texto.length() == 0) {
            throw new ErrorModelo("El modelo no devolvió texto.");
        }
        return texto.toString();
    }

    private String completarOpenAI(String sistema, List<Mensaje> mensajes) throws ErrorModelo {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("model", config.modelo());
        List<Object> lista = new ArrayList<>();
        lista.add(Map.of("role", "system", "content", sistema));
        for (Mensaje m : mensajes) {
            lista.add(Map.of("role", m.rol(), "content", m.texto()));
        }
        cuerpo.put("messages", lista);
        Map<?, ?> respuesta = enviar(peticion(base() + "/chat/completions")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.escribir(cuerpo), StandardCharsets.UTF_8))
                .build());
        if (respuesta.get("choices") instanceof List<?> opciones && !opciones.isEmpty()
                && opciones.get(0) instanceof Map<?, ?> opcion
                && opcion.get("message") instanceof Map<?, ?> mensaje
                && mensaje.get("content") instanceof String texto && !texto.isBlank()) {
            return texto;
        }
        throw new ErrorModelo("El modelo no devolvió texto.");
    }

    // ------------------------------------------------------------- transporte

    private String base() {
        String url = config.url();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private HttpRequest.Builder peticion(String url) throws ErrorModelo {
        HttpRequest.Builder b;
        try {
            b = HttpRequest.newBuilder(URI.create(url)).timeout(espera);
        } catch (IllegalArgumentException e) {
            throw new ErrorModelo("La dirección del servicio no es válida: " + url);
        }
        String clave = config.claveEfectiva();
        if (config.proveedor().protocolo() == Proveedor.Protocolo.ANTHROPIC) {
            b.header("x-api-key", clave).header("anthropic-version", VERSION_ANTHROPIC);
        } else if (!clave.isEmpty()) {
            b.header("Authorization", "Bearer " + clave);
        }
        return b;
    }

    private Map<?, ?> enviar(HttpRequest peticion) throws ErrorModelo {
        HttpResponse<String> respuesta;
        try {
            respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpConnectTimeoutException | ConnectException e) {
            throw new ErrorModelo(config.proveedor().local()
                    ? "No se pudo conectar con " + config.url() + ". ¿Está en ejecución " + config.proveedor().etiqueta()
                            .replace(" · modelo local", "") + "?"
                    : "No se pudo conectar con " + config.url() + ". Revise la conexión a internet.", e);
        } catch (HttpTimeoutException e) {
            throw new ErrorModelo("El modelo tardó demasiado en responder.", e);
        } catch (IOException e) {
            throw new ErrorModelo("Fallo de comunicación con el servicio: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErrorModelo("Consulta interrumpida.", e);
        }
        Object cuerpo;
        try {
            cuerpo = respuesta.body() == null || respuesta.body().isBlank() ? Map.of() : Json.leer(respuesta.body());
        } catch (IllegalArgumentException e) {
            cuerpo = Map.of();
        }
        int estado = respuesta.statusCode();
        if (estado / 100 != 2) {
            throw new ErrorModelo(describirFallo(estado, cuerpo));
        }
        if (!(cuerpo instanceof Map<?, ?> mapa)) {
            throw new ErrorModelo("Respuesta inesperada del servicio.");
        }
        return mapa;
    }

    private static String describirFallo(int estado, Object cuerpo) {
        String detalle = "";
        if (cuerpo instanceof Map<?, ?> m && m.get("error") instanceof Map<?, ?> e && e.get("message") instanceof String s) {
            detalle = s;
        } else if (cuerpo instanceof Map<?, ?> m && m.get("error") instanceof String s) {
            detalle = s;
        }
        String motivo = switch (estado) {
            case 401, 403 -> "La clave de API no es válida o no tiene permiso para este modelo.";
            case 404 -> "El servicio no reconoce la dirección o el modelo indicado.";
            case 429 -> "El servicio rechazó la consulta por exceso de uso o falta de saldo.";
            default -> estado >= 500 ? "El servicio no está disponible en este momento." : "El servicio rechazó la consulta.";
        };
        return motivo + " (HTTP " + estado + (detalle.isEmpty() ? "" : ": " + detalle) + ")";
    }
}
