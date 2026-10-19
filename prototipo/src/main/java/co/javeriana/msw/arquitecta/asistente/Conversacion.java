package co.javeriana.msw.arquitecta.asistente;

import co.javeriana.msw.arquitecta.asistente.ClienteModelo.ErrorModelo;
import co.javeriana.msw.arquitecta.asistente.ClienteModelo.Mensaje;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conversación con el modelo para redactar el catálogo de un proyecto.
 *
 * <p>El asistente no decide: propone y justifica, y la persona corrige, completa
 * o descarta. Por eso cada turno devuelve, además de la explicación, el catálogo
 * completo en un formato estructurado que la herramienta valida con el mismo
 * cargador que un catálogo escrito a mano, y cada mensaje de la persona viaja
 * con el catálogo tal como ella lo dejó: sus cambios prevalecen sobre los del
 * modelo.</p>
 */
public final class Conversacion {

    /**
     * Respuesta del asistente.
     *
     * @param explicacion   el texto dirigido a la persona.
     * @param propuesta     el catálogo propuesto, o {@code null} si no incluyó ninguno.
     * @param errorFormato  por qué no pudo leerse el catálogo, o {@code null}.
     */
    public record Respuesta(String explicacion, Propuesta propuesta, String errorFormato) {
    }

    private static final Pattern BLOQUE_JSON = Pattern.compile("```(?:json)?\\s*(\\{.*?})\\s*```", Pattern.DOTALL);

    private final ClienteModelo cliente;
    private final String sistema;
    private final String perfil;
    private final List<Mensaje> historial = new ArrayList<>();

    /**
     * @param perfil  descripción estructural del proyecto, tal como se mostró a la persona.
     * @param reglas  descripción de las <em>Fitness Functions</em> de la herramienta.
     */
    public Conversacion(ClienteModelo cliente, String perfil, String reglas) {
        this.cliente = cliente;
        this.perfil = perfil;
        this.sistema = instrucciones(reglas);
    }

    /** Primer turno: pide una propuesta a partir de la estructura del proyecto. */
    public Respuesta proponer(String contexto) throws ErrorModelo {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Proponga un catálogo inicial para este proyecto.\n\n");
        if (contexto != null && !contexto.isBlank()) {
            mensaje.append("Lo que la persona cuenta de su arquitectura:\n").append(contexto.strip()).append("\n\n");
        }
        mensaje.append("PERFIL ESTRUCTURAL DEL PROYECTO\n").append(perfil);
        return turno(mensaje.toString());
    }

    /**
     * Turno siguiente: la persona responde, corrige o pregunta.
     *
     * @param catalogoActual  el catálogo tal como está ahora en el editor, o {@code null}.
     * @param previsualizacion resumen del diagnóstico que produce ese catálogo, o {@code null}.
     */
    public Respuesta responder(String texto, String catalogoActual, String previsualizacion) throws ErrorModelo {
        StringBuilder mensaje = new StringBuilder(texto.strip());
        if (catalogoActual != null && !catalogoActual.isBlank()) {
            mensaje.append("\n\nCATÁLOGO ACTUAL (puede incluir cambios hechos a mano; prevalecen)\n")
                    .append(catalogoActual.strip());
        }
        if (previsualizacion != null && !previsualizacion.isBlank()) {
            mensaje.append("\n\nRESULTADO DE PREVISUALIZAR EL CATÁLOGO ACTUAL\n").append(previsualizacion.strip());
        }
        if (historial.isEmpty()) {
            mensaje.append("\n\nPERFIL ESTRUCTURAL DEL PROYECTO\n").append(perfil);
        }
        return turno(mensaje.toString());
    }

    public boolean iniciada() {
        return !historial.isEmpty();
    }

    private Respuesta turno(String mensaje) throws ErrorModelo {
        historial.add(new Mensaje("user", mensaje));
        String texto;
        try {
            texto = cliente.completar(sistema, historial);
        } catch (ErrorModelo e) {
            historial.remove(historial.size() - 1);
            throw e;
        }
        historial.add(new Mensaje("assistant", texto));
        return interpretar(texto);
    }

    /** Separa la explicación del bloque JSON con el catálogo y lo interpreta. */
    static Respuesta interpretar(String texto) {
        String json = null;
        Matcher m = BLOQUE_JSON.matcher(texto);
        int inicio = -1;
        int fin = -1;
        while (m.find()) {
            json = m.group(1);
            inicio = m.start();
            fin = m.end();
        }
        if (json == null) {
            int a = texto.indexOf('{');
            int b = texto.lastIndexOf('}');
            if (a >= 0 && b > a) {
                json = texto.substring(a, b + 1);
                inicio = a;
                fin = b + 1;
            }
        }
        if (json == null) {
            return new Respuesta(texto.strip(), null, null);
        }
        String explicacion = (texto.substring(0, inicio) + texto.substring(fin)).strip();
        try {
            return new Respuesta(explicacion, Propuesta.desdeJson(Json.leer(json)), null);
        } catch (IllegalArgumentException e) {
            return new Respuesta(explicacion, null, e.getMessage());
        }
    }

    // ------------------------------------------------------------- instrucciones

    static String instrucciones(String reglas) {
        return """
                Usted es el asistente de catálogo de Arquitecta, una herramienta que detecta deriva \
                arquitectónica en sistemas Java comparando el grafo de dependencias entre paquetes, \
                extraído del código compilado, con un catálogo de reglas. Su tarea es ayudar a una persona \
                a redactar el catálogo de SU proyecto, que es la declaración de su arquitectura PREVISTA.

                LÍMITES
                - Las Fitness Functions son tres, están fijas en la herramienta y se describen al final. \
                No puede crear otras ni cambiarlas: solo propone sus parámetros (raíz, capas y sus \
                paquetes, dependencias permitidas, umbral de concentración y exenciones).
                - El catálogo declara la arquitectura que el equipo pretende, no la que el código tiene \
                hoy. No permita una dependencia solo porque existe: si contradice la estratificación \
                plausible, no la permita y menciónela en "derivas_probables". Legitimar una deriva \
                existente haría que nunca se detectara.
                - Use únicamente paquetes que aparezcan en el perfil. No invente paquetes ni capas sin \
                paquetes.
                - Si no puede saber cuál es la intención del equipo, pregúntelo en "preguntas" en vez de \
                suponerlo.

                CÓMO INTERPRETA EL CATÁLOGO LA HERRAMIENTA
                - Cada capa agrupa uno o más prefijos de paquete y los subpaquetes heredan su capa. Se \
                asigna la primera capa cuyo prefijo coincide, en el orden declarado: si un prefijo \
                contiene a otro, declare antes la capa más específica. No declare la raíz como capa.
                - Los paquetes sin capa (un dominio compartido, utilidades) quedan fuera de la integridad \
                de capas, pero se evalúan con las reglas de ciclos y de concentración.
                - "permitido" indica de qué otras capas puede depender cada capa; dentro de una misma \
                capa siempre se permite. Una capa con la lista vacía no puede depender de ninguna otra.
                - "umbral_concentracion": entero; se señala el paquete cuyas dependencias salientes (Ce) \
                lo superen. Propóngalo a partir del Ce de los paquetes que parecen sanos y justifíquelo. \
                No lo eleve para dejar de señalar un paquete concreto: eso ocultaría la deriva. Si cree \
                que la concentración de un paquete es legítima por su papel, propóngalo como exención \
                "compartido", justificada, para que quede registrada; si no, debe quedar señalado.
                - Un paquete de entidades del dominio que usan todas las capas suele ser un núcleo \
                compartido: déjelo sin capa o, si lo declara como capa inferior, permita que dependan \
                de él las capas que lo usan por diseño. Si duda de si un uso es previsto, pregúntelo.
                - Exenciones, con moderación y siempre justificadas: "compartido" para un paquete del \
                sistema que concentra dependencias por su papel (raíz de composición, fachada, núcleo \
                de anotaciones); queda exento de ciclos y concentración, solo ese paquete exacto. \
                "excluir" para código que no es parte del sistema (generado, de terceros); sale del \
                análisis con sus subpaquetes.
                - Nombres de capa en minúsculas, sin espacios ni puntos (presentacion, negocio, ...).

                CÓMO RESPONDER
                - En español, breve y concreto. Primero una explicación de no más de diez renglones: qué \
                estilo arquitectónico reconoce y en qué se basa, y qué dudas tiene.
                - Después, el catálogo completo en un único bloque ```json con exactamente este esquema:
                {
                  "raiz": "com.ejemplo",
                  "capas": [ {"nombre": "presentacion", "paquetes": ["com.ejemplo.web"], "justificacion": "..."} ],
                  "permitido": { "presentacion": ["negocio"], "negocio": ["persistencia"], "persistencia": [] },
                  "umbral_concentracion": {"valor": 6, "justificacion": "..."},
                  "exenciones": [ {"paquete": "com.ejemplo", "tipo": "compartido", "justificacion": "..."} ],
                  "derivas_probables": ["com.ejemplo.persistencia -> com.ejemplo.web: ..."],
                  "preguntas": ["..."]
                }
                - Las capas, de la más alta (la que depende de las demás) a la más baja.
                - Cada justificación, una frase: quedará como comentario en el catálogo y es el registro \
                de la decisión.
                - Si la persona corrige o aporta contexto, devuelva el catálogo completo actualizado, no \
                solo el cambio. Si su mensaje incluye el catálogo actual, parta de él: los cambios que \
                hizo a mano prevalecen, salvo que sean inválidos, en cuyo caso explique por qué.
                - Si se incluye el resultado de previsualizar el catálogo, interprete los hallazgos: \
                distinga los que parecen deriva real de los que delatan un catálogo mal declarado, y \
                proponga el ajuste que corresponda sin legitimar la deriva.

                LAS TRES FITNESS FUNCTIONS DE LA HERRAMIENTA
                """ + reglas;
    }
}
