package co.javeriana.msw.arquitecta.config;

import java.util.List;

/**
 * Representa una capa arquitectónica: un nombre lógico y el conjunto de
 * prefijos de paquete que se consideran parte de ella.
 *
 * <p>La pertenencia se resuelve por prefijo, de modo que los subpaquetes heredan
 * la capa de su padre. La comparación respeta la frontera del separador: el
 * prefijo {@code com.caso.negocio} reclama a {@code com.caso.negocio.calculo}
 * pero no a {@code com.caso.negociador}, que es un paquete distinto y que una
 * comparación textual ingenua habría absorbido por error.</p>
 */
public record Capa(String nombre, List<String> paquetes) {

    public Capa {
        paquetes = List.copyOf(paquetes);
    }

    public boolean contiene(String paquete) {
        if (paquete == null) {
            return false;
        }
        for (String prefijo : paquetes) {
            if (paquete.equals(prefijo) || paquete.startsWith(prefijo + ".")) {
                return true;
            }
        }
        return false;
    }
}
