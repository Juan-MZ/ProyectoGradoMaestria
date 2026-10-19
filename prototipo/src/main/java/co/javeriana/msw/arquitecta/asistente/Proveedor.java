package co.javeriana.msw.arquitecta.asistente;

/**
 * Servicios de modelos de lenguaje con que puede trabajar el asistente.
 *
 * <p>Se distinguen por el protocolo que hablan ---el de Anthropic o el de OpenAI,
 * que también implementan DeepSeek, Ollama, LM Studio y muchos otros--- y por si el modelo
 * se ejecuta en el propio equipo. Un modelo local es la opción para quien no
 * pueda enviar la estructura de su sistema a un tercero.</p>
 */
public enum Proveedor {

    ANTHROPIC("Anthropic · Claude", Protocolo.ANTHROPIC, "https://api.anthropic.com",
            "claude-opus-5-5", true, false, "ANTHROPIC_API_KEY"),
    OPENAI("OpenAI", Protocolo.OPENAI, "https://api.openai.com/v1", "", true, false, "OPENAI_API_KEY"),
    DEEPSEEK("DeepSeek", Protocolo.OPENAI, "https://api.deepseek.com", "deepseek-v4-pro", true, false,
            "DEEPSEEK_API_KEY"),
    OLLAMA("Ollama · modelo local", Protocolo.OPENAI, "http://localhost:11434/v1", "", false, true, null),
    LM_STUDIO("LM Studio · modelo local", Protocolo.OPENAI, "http://localhost:1234/v1", "", false, true, null),
    COMPATIBLE("Otro servicio compatible con OpenAI", Protocolo.OPENAI, "", "", false, false, null);

    /** Formato de las peticiones. */
    public enum Protocolo { ANTHROPIC, OPENAI }

    private final String etiqueta;
    private final Protocolo protocolo;
    private final String urlPorDefecto;
    private final String modeloPorDefecto;
    private final boolean requiereClave;
    private final boolean local;
    private final String variableEntorno;

    Proveedor(String etiqueta, Protocolo protocolo, String urlPorDefecto, String modeloPorDefecto,
            boolean requiereClave, boolean local, String variableEntorno) {
        this.etiqueta = etiqueta;
        this.protocolo = protocolo;
        this.urlPorDefecto = urlPorDefecto;
        this.modeloPorDefecto = modeloPorDefecto;
        this.requiereClave = requiereClave;
        this.local = local;
        this.variableEntorno = variableEntorno;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public Protocolo protocolo() {
        return protocolo;
    }

    public String urlPorDefecto() {
        return urlPorDefecto;
    }

    public String modeloPorDefecto() {
        return modeloPorDefecto;
    }

    public boolean requiereClave() {
        return requiereClave;
    }

    /** Verdadero si el modelo se ejecuta en este equipo y nada sale de él. */
    public boolean local() {
        return local;
    }

    /** Variable de entorno de la que tomar la clave si no se indicó otra, o {@code null}. */
    public String variableEntorno() {
        return variableEntorno;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
