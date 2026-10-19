package com.caso.util;

import com.caso.dominio.Cliente;
import com.caso.dominio.Venta;
import com.caso.negocio.ServicioVenta;
import com.caso.persistencia.RepositorioVenta;
import com.caso.presentacion.ControladorVenta;

/**
 * Paquete de utilidades que concentra dependencias hacia todos los demás
 * paquetes del sistema.
 *
 * <p>Al no pertenecer a ninguna capa declarada no vulnera la estratificación, y
 * al no ser destino de ninguna dependencia tampoco cierra ciclos: la única regla
 * que debe activarse es la de concentración de dependencias salientes.</p>
 */
public class Utilidades {

    private final Cliente cliente = new Cliente("demo");
    private final Venta venta = new Venta(cliente, 0.0);
    private final ServicioVenta servicio = new ServicioVenta();
    private final ControladorVenta controlador = new ControladorVenta();
    private final RepositorioVenta repositorio = new RepositorioVenta();

    public Venta venta() {
        return venta;
    }

    public ServicioVenta servicio() {
        return servicio;
    }

    public ControladorVenta controlador() {
        return controlador;
    }

    public RepositorioVenta repositorio() {
        return repositorio;
    }
}
