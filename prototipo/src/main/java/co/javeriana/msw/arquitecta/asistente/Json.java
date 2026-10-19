package co.javeriana.msw.arquitecta.asistente;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lectura y escritura de JSON, lo justo para hablar con las API de los modelos
 * de lenguaje sin añadir una biblioteca: objetos como {@link Map}, listas como
 * {@link List}, números como {@link Double} y el resto con sus tipos naturales.
 */
public final class Json {

    private final String texto;
    private int pos;

    private Json(String texto) {
        this.texto = texto;
    }

    /** Interpreta un documento JSON completo. */
    public static Object leer(String texto) {
        Json lector = new Json(texto);
        lector.espacios();
        Object valor = lector.valor();
        lector.espacios();
        if (lector.pos != texto.length()) {
            throw lector.error("contenido inesperado tras el documento");
        }
        return valor;
    }

    /** Serializa mapas, listas, cadenas, números, booleanos y {@code null}. */
    public static String escribir(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribir(valor, sb);
        return sb.toString();
    }

    // ------------------------------------------------------------- lectura

    private Object valor() {
        if (pos >= texto.length()) {
            throw error("documento incompleto");
        }
        char c = texto.charAt(pos);
        return switch (c) {
            case '{' -> objeto();
            case '[' -> lista();
            case '"' -> cadena();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> numero();
        };
    }

    private Map<String, Object> objeto() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        pos++;
        espacios();
        if (consumir('}')) {
            return mapa;
        }
        do {
            espacios();
            if (pos >= texto.length() || texto.charAt(pos) != '"') {
                throw error("se esperaba el nombre de una propiedad");
            }
            String clave = cadena();
            espacios();
            if (!consumir(':')) {
                throw error("se esperaba ':'");
            }
            espacios();
            mapa.put(clave, valor());
            espacios();
        } while (consumir(','));
        if (!consumir('}')) {
            throw error("se esperaba '}'");
        }
        return mapa;
    }

    private List<Object> lista() {
        List<Object> lista = new ArrayList<>();
        pos++;
        espacios();
        if (consumir(']')) {
            return lista;
        }
        do {
            espacios();
            lista.add(valor());
            espacios();
        } while (consumir(','));
        if (!consumir(']')) {
            throw error("se esperaba ']'");
        }
        return lista;
    }

    private String cadena() {
        StringBuilder sb = new StringBuilder();
        pos++;
        while (pos < texto.length()) {
            char c = texto.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (pos >= texto.length()) {
                break;
            }
            char e = texto.charAt(pos++);
            switch (e) {
                case 'n' -> sb.append('\n');
                case 't' -> sb.append('\t');
                case 'r' -> sb.append('\r');
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'u' -> {
                    if (pos + 4 > texto.length()) {
                        throw error("secuencia \\u incompleta");
                    }
                    sb.append((char) Integer.parseInt(texto.substring(pos, pos + 4), 16));
                    pos += 4;
                }
                default -> sb.append(e);
            }
        }
        throw error("cadena sin cerrar");
    }

    private Object literal(String palabra, Object valor) {
        if (!texto.startsWith(palabra, pos)) {
            throw error("valor no reconocido");
        }
        pos += palabra.length();
        return valor;
    }

    private Double numero() {
        int inicio = pos;
        while (pos < texto.length() && "+-0123456789.eE".indexOf(texto.charAt(pos)) >= 0) {
            pos++;
        }
        if (inicio == pos) {
            throw error("valor no reconocido");
        }
        try {
            return Double.valueOf(texto.substring(inicio, pos));
        } catch (NumberFormatException e) {
            throw error("número mal formado");
        }
    }

    private boolean consumir(char c) {
        if (pos < texto.length() && texto.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }

    private void espacios() {
        while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) {
            pos++;
        }
    }

    private IllegalArgumentException error(String motivo) {
        return new IllegalArgumentException("JSON no válido (posición " + pos + "): " + motivo);
    }

    // ------------------------------------------------------------- escritura

    private static void escribir(Object valor, StringBuilder sb) {
        if (valor == null) {
            sb.append("null");
        } else if (valor instanceof String s) {
            cadena(s, sb);
        } else if (valor instanceof Number || valor instanceof Boolean) {
            sb.append(valor);
        } else if (valor instanceof Map<?, ?> mapa) {
            sb.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> e : mapa.entrySet()) {
                if (!primero) {
                    sb.append(',');
                }
                primero = false;
                cadena(String.valueOf(e.getKey()), sb);
                sb.append(':');
                escribir(e.getValue(), sb);
            }
            sb.append('}');
        } else if (valor instanceof List<?> lista) {
            sb.append('[');
            for (int i = 0; i < lista.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                escribir(lista.get(i), sb);
            }
            sb.append(']');
        } else {
            throw new IllegalArgumentException("Tipo no serializable: " + valor.getClass().getName());
        }
    }

    private static void cadena(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
