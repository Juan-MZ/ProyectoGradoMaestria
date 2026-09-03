package com.caso.persistencia;

import com.caso.dominio.Venta;

/**
 * Repositorio de la capa de persistencia. En la versión base solo conoce el
 * dominio compartido.
 */
public class RepositorioVenta {

    public Venta guardar(Venta venta) {
        // Persistencia simulada.
        return venta;
    }
}
