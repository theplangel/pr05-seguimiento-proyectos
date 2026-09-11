package com.dsw.seguimiento.model;

import java.sql.Date;

public class Evidencia {
    private int id;
    private int tareaId;
    private String tipo;       // ej. captura, documento, enlace
    private String urlORuta;
    private Date fecha;

    public Evidencia() {}

    public Evidencia(int tareaId, String tipo, String urlORuta, Date fecha) {
        this.tareaId = tareaId;
        this.tipo = tipo;
        this.urlORuta = urlORuta;
        this.fecha = fecha;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTareaId() { return tareaId; }
    public void setTareaId(int tareaId) { this.tareaId = tareaId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getUrlORuta() { return urlORuta; }
    public void setUrlORuta(String urlORuta) { this.urlORuta = urlORuta; }

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }
}
