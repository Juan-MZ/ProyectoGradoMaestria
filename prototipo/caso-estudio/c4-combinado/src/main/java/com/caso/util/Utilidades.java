package com.caso.util;

import com.caso.dominio.Cliente;
import com.caso.dominio.Venta;
import com.caso.negocio.ServicioVenta;
import com.caso.presentacion.ControladorVenta;
import com.caso.persistencia.RepositorioVenta;

/**
 * Paquete de utilidades que concentra dependencias salientes hacia casi todos
 * los demás paquetes del sistema: representa un "componente dios" inducido.
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

    public ControladorVenta controlador() {
        return controlador;
    }

    public RepositorioVenta repositorio() {
        return repositorio;
    }

    public ServicioVenta servicio() {
        return servicio;
    }
}
