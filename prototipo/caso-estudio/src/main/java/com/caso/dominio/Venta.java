package com.caso.dominio;

/** Entidad de venta perteneciente al dominio compartido. */
public class Venta {
    private final Cliente cliente;
    private final double total;

    public Venta(Cliente cliente, double total) {
        this.cliente = cliente;
        this.total = total;
    }

    public Cliente cliente() {
        return cliente;
    }

    public double total() {
        return total;
    }
}
