package com.dsw.seguimiento.model;

import java.sql.Date;

public class Proyecto {

    public static final String ACTIVO = "ACTIVO";
    public static final String PAUSADO = "PAUSADO";
    public static final String CERRADO = "CERRADO";

    private int id;
    private String nombre;
    private String descripcion;
    private Date fechaInicio;
    private String estado;

    public Proyecto() {}

    public Proyecto(String nombre, String descripcion, Date fechaInicio) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.estado = ACTIVO;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Date getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Date fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
