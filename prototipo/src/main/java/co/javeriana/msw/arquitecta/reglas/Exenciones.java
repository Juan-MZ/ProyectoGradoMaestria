package co.javeriana.msw.arquitecta.reglas;

import java.util.List;

/**
 * Conjunto de prefijos de paquete a los que una regla no se aplica.
 *
 * <p>Una exención no es una excusa para silenciar un hallazgo incómodo: es el
 * reconocimiento de que ciertos paquetes desempeñan un papel ---núcleo de
 * anotaciones, raíz de composición, fachada--- para el que la regla no fue
 * pensada. Por eso se declara de forma explícita en la configuración y el reporte
 * la enumera: la excepción queda registrada y puede discutirse, que es
 * exactamente lo contrario de codificarla en silencio dentro de la regla.</p>
 */
public record Exenciones(List<String> paquetes) {

    /** Ninguna exención: la regla se aplica a todo el grafo. */
    public static final Exenciones NINGUNA = new Exenciones(List.of());

    public Exenciones {
        paquetes = List.copyOf(paquetes);
    }

    /**
     * Indica si el paquete está exento.
     *
     * <p>La comparación es exacta y no se propaga a los subpaquetes, y la razón es
     * conceptual: un papel arquitectónico lo desempeña un paquete concreto, no un
     * subárbol. Que {@code com.caso} aloje las anotaciones comunes no dice nada
     * sobre {@code com.caso.negocio}, y propagar la exención hacia abajo eximiría
     * al sistema entero cada vez que el núcleo compartido resultara ser la raíz,
     * que es justamente el caso más frecuente.</p>
     */
    public boolean cubre(String paquete) {
        return paquete != null && paquetes.contains(paquete);
    }

    public boolean vacias() {
        return paquetes.isEmpty();
    }
}
