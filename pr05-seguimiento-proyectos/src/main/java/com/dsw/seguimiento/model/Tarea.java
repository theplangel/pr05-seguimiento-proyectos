package com.dsw.seguimiento.model;

import java.sql.Date;

public class Tarea {

    // Estados válidos — deben coincidir exactamente con los usados en TareaDAO
    public static final String PENDIENTE = "PENDIENTE";
    public static final String EN_PROGRESO = "EN_PROGRESO";
    public static final String BLOQUEADA = "BLOQUEADA";
    public static final String COMPLETADA = "COMPLETADA";

    private int id;
    private int proyectoId;
    private String titulo;
    private int responsableId; // referencia a Integrante
    private String estado;
    private Date fechaLimite;

    public Tarea() {}

    public Tarea(int proyectoId, String titulo, int responsableId, Date fechaLimite) {
        this.proyectoId = proyectoId;
        this.titulo = titulo;
        this.responsableId = responsableId;
        this.fechaLimite = fechaLimite;
        this.estado = PENDIENTE;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProyectoId() { return proyectoId; }
    public void setProyectoId(int proyectoId) { this.proyectoId = proyectoId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public int getResponsableId() { return responsableId; }
    public void setResponsableId(int responsableId) { this.responsableId = responsableId; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Date getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(Date fechaLimite) { this.fechaLimite = fechaLimite; }
}
