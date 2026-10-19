package com.caso.dominio;

/** Entidad compartida del dominio del caso de estudio. */
public class Cliente {
    private final String nombre;

    public Cliente(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
