package com.caso.presentacion;

import com.caso.dominio.Venta;
import com.caso.negocio.ServicioVenta;
import com.caso.persistencia.RepositorioVenta;

/**
 * Controlador con una deriva deliberada de tipo <em>salto de capa</em>.
 *
 * <p>Junto a su dependencia legítima hacia el negocio, accede directamente al
 * repositorio para "consultar rápido" sin pasar por el servicio. La dependencia
 * presentacion -> persistencia no figura en la matriz de adyacencias permitidas
 * y, al no invertir el sentido del flujo, tampoco cierra ningún ciclo: aísla la
 * violación de capas de las otras dos familias de deriva.</p>
 */
public class ControladorVenta {

    private final ServicioVenta servicio = new ServicioVenta();
    private final RepositorioVenta repositorio = new RepositorioVenta();

    public Venta registrarVenta() {
        return servicio.registrarVenta();
    }

    public Venta consultaDirecta(Venta venta) {
        return repositorio.guardar(venta);
    }
}
