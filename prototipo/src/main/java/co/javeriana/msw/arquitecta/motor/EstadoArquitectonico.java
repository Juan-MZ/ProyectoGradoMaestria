package co.javeriana.msw.arquitecta.motor;

/**
 * Clasificación del estado arquitectónico de un sistema.
 *
 * <p>Las constantes se declaran de mejor a peor, de modo que el orden natural
 * del enumerado permita adoptar siempre el veredicto más restrictivo cuando dos
 * criterios discrepan.</p>
 */
public enum EstadoArquitectonico {

    ACEPTABLE("Aceptable"),
    EN_OBSERVACION("En observación"),
    NO_CONFORME("No conforme");

    private final String etiqueta;

    EstadoArquitectonico(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    /** Devuelve el más restrictivo (peor) de dos estados. */
    public static EstadoArquitectonico masRestrictivo(EstadoArquitectonico a, EstadoArquitectonico b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
