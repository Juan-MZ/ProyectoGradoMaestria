package com.caso.presentacion;

import com.caso.dominio.Venta;
import com.caso.negocio.ServicioVenta;

/** Controlador de la capa de presentación (versión alterada). */
public class ControladorVenta {

    private final ServicioVenta servicio = new ServicioVenta();

    public Venta registrarVenta() {
        return servicio.registrarVenta();
    }
}
