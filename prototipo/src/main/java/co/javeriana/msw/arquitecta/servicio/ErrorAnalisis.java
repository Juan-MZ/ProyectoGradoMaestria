package co.javeriana.msw.arquitecta.servicio;

/**
 * Error que impide completar un análisis, con un mensaje pensado para quien usa
 * la herramienta y no para quien la programó.
 *
 * <p>Los fallos esperables ---un proyecto sin compilar, una configuración mal
 * escrita, una ruta inexistente--- no son defectos del mecanismo sino situaciones
 * que el usuario puede corregir, y por eso se comunican explicando qué ocurrió y
 * qué hacer, tanto en la consola como en la interfaz gráfica.</p>
 */
public class ErrorAnalisis extends Exception {

    private static final long serialVersionUID = 1L;

    public ErrorAnalisis(String mensaje) {
        super(mensaje);
    }

    public ErrorAnalisis(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
