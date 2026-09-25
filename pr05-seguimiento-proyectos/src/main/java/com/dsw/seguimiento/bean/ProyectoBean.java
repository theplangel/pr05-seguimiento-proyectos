package com.dsw.seguimiento.bean;

import com.dsw.seguimiento.dao.ProyectoDAO;
import com.dsw.seguimiento.model.Proyecto;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Managed Bean JSF para Proyecto. Reutiliza ProyectoDAO tal cual se
 * construyo en P02 - la capa de persistencia no cambia entre Web 1.0 y
 * Web 2.0, solo cambia quien la invoca (Bean en vez de Servlet).
 */
@ManagedBean(name = "proyectoBean")
@SessionScoped
public class ProyectoBean implements Serializable {

    private final ProyectoDAO proyectoDAO = new ProyectoDAO();

    private List<Proyecto> proyectos;

    // Campos del formulario de alta (bindeados con h:inputText / p:inputText)
    private String nombre;
    private String descripcion;
    private Date fechaInicio;

    private Proyecto proyectoSeleccionado;

    public void cargarProyectos() {
        proyectos = proyectoDAO.listarTodos();
    }

    public List<Proyecto> getProyectos() {
        if (proyectos == null) {
            cargarProyectos();
        }
        return proyectos;
    }

    /**
     * Accion del formulario de alta. Reutiliza la misma validacion de
     * campos obligatorios que ya vivia en ProyectoServlet.doPost().
     */
    public String crear() {
        if (nombre == null || nombre.trim().isEmpty() || fechaInicio == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Nombre y fecha de inicio son obligatorios.", null));
            return null; // se queda en la misma vista, PrimeFaces muestra el mensaje
        }

        java.sql.Date fechaSql = new java.sql.Date(fechaInicio.getTime());
        Proyecto proyecto = new Proyecto(nombre.trim(), descripcion, fechaSql);
        boolean creado = proyectoDAO.crear(proyecto);

        if (!creado) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "No se pudo crear el proyecto. Intenta de nuevo.", null));
            return null;
        }

        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Proyecto creado correctamente.", null));

        limpiarFormulario();
        cargarProyectos(); // refresca la tabla sin recargar toda la pagina

        return null; // navegacion implicita: se queda en proyectos.xhtml
    }

    private void limpiarFormulario() {
        nombre = null;
        descripcion = null;
        fechaInicio = null;
    }

    // Getters/setters usados por los componentes PrimeFaces (value binding)

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Date getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Date fechaInicio) { this.fechaInicio = fechaInicio; }

    public Proyecto getProyectoSeleccionado() { return proyectoSeleccionado; }
    public void setProyectoSeleccionado(Proyecto proyectoSeleccionado) {
        this.proyectoSeleccionado = proyectoSeleccionado;
    }
}
