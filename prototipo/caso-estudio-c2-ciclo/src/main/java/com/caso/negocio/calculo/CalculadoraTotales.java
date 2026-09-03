package com.caso.negocio.calculo;

import com.caso.negocio.ServicioVenta;

/**
 * Subpaquete de la capa de negocio que cierra un ciclo con su paquete padre.
 *
 * <p>La deriva es puramente cíclica: ambos paquetes pertenecen a la capa
 * negocio, de modo que la integridad de la estratificación permanece intacta y
 * la única regla que debe activarse es la de ausencia de ciclos.</p>
 */
public class CalculadoraTotales {

    private final ServicioVenta servicio;

    public CalculadoraTotales(ServicioVenta servicio) {
        this.servicio = servicio;
    }

    public double totalRegistrado() {
        return servicio.registrarVenta().total();
    }
}
