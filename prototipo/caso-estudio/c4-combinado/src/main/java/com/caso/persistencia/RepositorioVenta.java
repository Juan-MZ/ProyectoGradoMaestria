package com.caso.persistencia;

import com.caso.dominio.Venta;
import com.caso.negocio.ServicioVenta;
import com.caso.presentacion.ControladorVenta;

/**
 * Repositorio alterado. Introduce dos derivas deliberadas:
 * <ul>
 *   <li>Una dependencia hacia el negocio, que cierra un ciclo negocio-persistencia.</li>
 *   <li>Una dependencia hacia la presentación, que rompe la integridad de capas.</li>
 * </ul>
 */
public class RepositorioVenta {

    public Venta guardar(Venta venta) {
        return venta;
    }

    public ServicioVenta crearServicio() {
        return new ServicioVenta();
    }

    public ControladorVenta crearControlador() {
        return new ControladorVenta();
    }
}
