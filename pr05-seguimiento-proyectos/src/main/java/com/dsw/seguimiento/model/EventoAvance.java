package com.dsw.seguimiento.model;

import java.sql.Timestamp;

public class EventoAvance {
    private int id;
    private int proyectoId;
    private String descripcion;
    private Timestamp creadoEn;

    public EventoAvance() {
    }

    public EventoAvance(int proyectoId, String descripcion) {
        this.proyectoId = proyectoId;
        this.descripcion = descripcion;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProyectoId() { return proyectoId; }
    public void setProyectoId(int proyectoId) { this.proyectoId = proyectoId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Timestamp getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Timestamp creadoEn) { this.creadoEn = creadoEn; }
}
