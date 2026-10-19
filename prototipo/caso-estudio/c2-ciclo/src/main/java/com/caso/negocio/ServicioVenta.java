package com.caso.negocio;

import com.caso.dominio.Cliente;
import com.caso.dominio.Venta;
import com.caso.negocio.calculo.CalculadoraTotales;
import com.caso.persistencia.RepositorioVenta;

/**
 * Servicio de negocio que referencia a su propio subpaquete de cálculo, el cual
 * a su vez lo referencia de vuelta. El ciclo queda contenido dentro de la capa.
 */
public class ServicioVenta {

    private final RepositorioVenta repositorio = new RepositorioVenta();

    public Venta registrarVenta() {
        Venta venta = new Venta(new Cliente("cliente-demo"), 100.0);
        return repositorio.guardar(venta);
    }

    public double totalConCalculadora() {
        return new CalculadoraTotales(this).totalRegistrado();
    }
}
