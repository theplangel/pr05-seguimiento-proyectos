package com.dsw.seguimiento.model;

public class Impedimento {

    public static final String ABIERTO = "ABIERTO";
    public static final String RESUELTO = "RESUELTO";

    private int id;
    private int tareaId;
    private String descripcion;
    private String estado;

    public Impedimento() {}

    public Impedimento(int tareaId, String descripcion) {
        this.tareaId = tareaId;
        this.descripcion = descripcion;
        this.estado = ABIERTO;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTareaId() { return tareaId; }
    public void setTareaId(int tareaId) { this.tareaId = tareaId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
