package co.javeriana.msw.arquitecta.config;

import java.util.List;

/**
 * Representa una capa arquitectónica: un nombre lógico y el conjunto de
 * prefijos de paquete que se consideran parte de ella.
 */
public record Capa(String nombre, List<String> paquetes) {

    public boolean contiene(String paquete) {
        for (String prefijo : paquetes) {
            if (paquete.startsWith(prefijo)) {
                return true;
            }
        }
        return false;
    }
}
