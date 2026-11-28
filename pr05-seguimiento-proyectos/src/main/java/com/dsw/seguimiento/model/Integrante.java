package com.dsw.seguimiento.model;

public class Integrante {
    private int id;
    private int proyectoId;
    private String nombre;
    private String rolEquipo; // ej. líder, backend, BD, frontend/QA

    public Integrante() {}

    public Integrante(int proyectoId, String nombre, String rolEquipo) {
        this.proyectoId = proyectoId;
        this.nombre = nombre;
        this.rolEquipo = rolEquipo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProyectoId() { return proyectoId; }
    public void setProyectoId(int proyectoId) { this.proyectoId = proyectoId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getRolEquipo() { return rolEquipo; }
    public void setRolEquipo(String rolEquipo) { this.rolEquipo = rolEquipo; }
}
