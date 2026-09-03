package co.javeriana.msw.arquitecta.config;

/**
 * Modo con el que el motor traduce los resultados en un estado arquitectónico.
 *
 * <p>{@link #CR} corresponde a la formulación original: el estado depende
 * únicamente de la Tasa de Conformidad y de sus umbrales. {@link #CR_SEVERIDAD}
 * incorpora, además, la gravedad del peor hallazgo, y adopta como veredicto el
 * más restrictivo de los dos criterios. La distinción se mantiene explícita y
 * configurable para que ambos resultados puedan compararse.</p>
 */
public enum ModoClasificacion {

    CR("cr"),
    CR_SEVERIDAD("cr+severidad");

    private final String clave;

    ModoClasificacion(String clave) {
        this.clave = clave;
    }

    public String clave() {
        return clave;
    }

    public static ModoClasificacion desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return CR_SEVERIDAD;
        }
        String normalizado = valor.trim().toLowerCase();
        for (ModoClasificacion modo : values()) {
            if (modo.clave.equals(normalizado)) {
                return modo;
            }
        }
        throw new IllegalArgumentException("Modo de clasificación desconocido: '" + valor
                + "'. Valores admitidos: cr, cr+severidad.");
    }
}
