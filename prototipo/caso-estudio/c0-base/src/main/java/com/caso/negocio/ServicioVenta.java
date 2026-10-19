package com.caso.negocio;

import com.caso.dominio.Cliente;
import com.caso.dominio.Venta;
import com.caso.persistencia.RepositorioVenta;

/**
 * Servicio de la capa de negocio. Depende de la persistencia y del dominio,
 * pero no de la presentación.
 */
public class ServicioVenta {

    private final RepositorioVenta repositorio = new RepositorioVenta();

    public Venta registrarVenta() {
        Venta venta = new Venta(new Cliente("cliente-demo"), 100.0);
        return repositorio.guardar(venta);
    }
}
