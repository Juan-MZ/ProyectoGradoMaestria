package com.caso.presentacion;

import com.caso.dominio.Venta;
import com.caso.negocio.ServicioVenta;

/**
 * Controlador de la capa de presentación. Solo conoce el negocio y el dominio
 * compartido, respetando la matriz de dependencias del caso base.
 */
public class ControladorVenta {

    private final ServicioVenta servicio = new ServicioVenta();

    public Venta registrarVenta() {
        return servicio.registrarVenta();
    }
}
