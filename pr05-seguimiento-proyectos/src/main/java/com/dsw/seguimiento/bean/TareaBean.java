package com.dsw.seguimiento.bean;

import com.dsw.seguimiento.dao.EvidenciaDAO;
import com.dsw.seguimiento.dao.ImpedimentoDAO;
import com.dsw.seguimiento.dao.IntegranteDAO;
import com.dsw.seguimiento.dao.TareaDAO;
import com.dsw.seguimiento.model.Evidencia;
import com.dsw.seguimiento.model.Impedimento;
import com.dsw.seguimiento.model.Integrante;
import com.dsw.seguimiento.model.Tarea;
import com.dsw.seguimiento.util.AlmacenEvidencias;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Managed Bean JSF para Tarea. Reutiliza TareaDAO, ImpedimentoDAO y
 * EvidenciaDAO tal cual se construyeron en P02 - la maquina de estados
 * y la regla de bloqueo por impedimento abierto viven en TareaDAO,
 * este Bean solo la invoca.
 *
 * @ViewScoped para que la seleccion de proyecto/tarea sobreviva entre
 * postbacks de la misma pagina sin cargar toda la sesion.
 */
@ManagedBean(name = "tareaBean")
@ViewScoped
public class TareaBean implements Serializable {

    private final TareaDAO tareaDAO = new TareaDAO();
    private final ImpedimentoDAO impedimentoDAO = new ImpedimentoDAO();
    private final EvidenciaDAO evidenciaDAO = new EvidenciaDAO();
    private final IntegranteDAO integranteDAO = new IntegranteDAO();

    private int proyectoId;
    private List<Tarea> tareas;
    private List<Integrante> integrantes;

    // Formulario de alta de tarea
    private String titulo;
    private Integer responsableId;
    private Date fechaLimite;

    // Tarea seleccionada para ver detalle / impedimentos / evidencia
    private Tarea tareaSeleccionada;
    private List<Impedimento> impedimentosDeTarea;
    private List<Evidencia> evidenciasDeTarea;

    private String descripcionImpedimento;
    // enlace y codigo se capturan como texto; captura y documento como archivo
    private String tipoEvidencia = "enlace";
    private String urlEvidencia;

    public void inicializar() {
        if (proyectoId > 0) {
            tareas = tareaDAO.listarPorProyecto(proyectoId);
            integrantes = integranteDAO.listarPorProyecto(proyectoId);
        }
    }

    public List<Tarea> getTareas() {
        if (tareas == null) {
            inicializar();
        }
        return tareas;
    }

    public List<Integrante> getIntegrantes() {
        if (integrantes == null) {
            inicializar();
        }
        return integrantes;
    }

    /** Estados posibles a mostrar en el selector de cambio de estado. */
    public String[] getEstadosDisponibles() {
        return new String[]{Tarea.PENDIENTE, Tarea.EN_PROGRESO, Tarea.BLOQUEADA, Tarea.COMPLETADA};
    }

    public String crearTarea() {
        if (titulo == null || titulo.trim().isEmpty() || responsableId == null) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "Titulo y responsable son obligatorios.");
            return null;
        }

        java.sql.Date fechaSql = (fechaLimite != null) ? new java.sql.Date(fechaLimite.getTime()) : null;
        Tarea tarea = new Tarea(proyectoId, titulo.trim(), responsableId, fechaSql);
        boolean creada = tareaDAO.crear(tarea);

        if (!creada) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "No se pudo crear la tarea. Intenta de nuevo.");
            return null;
        }

        agregarMensaje(FacesMessage.SEVERITY_INFO, "Tarea creada correctamente.");
        titulo = null;
        responsableId = null;
        fechaLimite = null;
        inicializar();
        return null;
    }

    /**
     * Cambia el estado de una tarea especifica. Se llama desde un
     * p:selectOneMenu con ajax por fila de la tabla (ver tareas.xhtml).
     * Toda la validacion (transicion valida + impedimento abierto)
     * vive en TareaDAO.cambiarEstado(), aqui solo se reporta el resultado.
     */
    public void cambiarEstado(Tarea tarea, String nuevoEstado) {
        boolean exito = tareaDAO.cambiarEstado(tarea.getId(), nuevoEstado);

        if (exito) {
            agregarMensaje(FacesMessage.SEVERITY_INFO,
                "Tarea #" + tarea.getId() + " actualizada a " + nuevoEstado + ".");
        } else {
            agregarMensaje(FacesMessage.SEVERITY_WARN,
                "No se pudo cambiar el estado (transicion invalida o hay un impedimento abierto).");
        }
        inicializar(); // refresca la tabla con el estado real desde BD
    }

    public void seleccionarTarea(Tarea tarea) {
        this.tareaSeleccionada = tarea;
        this.impedimentosDeTarea = impedimentoDAO.listarPorTarea(tarea.getId());
        this.evidenciasDeTarea = evidenciaDAO.listarPorTarea(tarea.getId());
    }

    public List<Impedimento> getImpedimentosDeTarea() {
        return impedimentosDeTarea;
    }

    public List<Evidencia> getEvidenciasDeTarea() {
        return evidenciasDeTarea;
    }

    public void crearImpedimento() {
        if (tareaSeleccionada == null) {
            return;
        }
        if (descripcionImpedimento == null || descripcionImpedimento.trim().isEmpty()) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "La descripcion del impedimento es obligatoria.");
            return;
        }

        Impedimento impedimento = new Impedimento(tareaSeleccionada.getId(), descripcionImpedimento.trim());
        boolean creado = impedimentoDAO.crear(impedimento);

        if (!creado) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "No se pudo registrar el impedimento.");
            return;
        }

        agregarMensaje(FacesMessage.SEVERITY_INFO, "Impedimento registrado.");
        descripcionImpedimento = null;
        seleccionarTarea(tareaSeleccionada); // refresca la lista de impedimentos
    }

    public void resolverImpedimento(Impedimento impedimento) {
        boolean resuelto = impedimentoDAO.resolver(impedimento.getId());

        if (!resuelto) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "No se pudo marcar el impedimento como resuelto.");
            return;
        }

        agregarMensaje(FacesMessage.SEVERITY_INFO, "Impedimento marcado como resuelto.");
        seleccionarTarea(tareaSeleccionada);
    }

    public void registrarEvidencia() {
        if (tareaSeleccionada == null) {
            return;
        }
        if (isEvidenciaConArchivo()) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "Para este tipo de evidencia selecciona un archivo.");
            return;
        }
        if (tipoEvidencia == null || tipoEvidencia.trim().isEmpty()
                || urlEvidencia == null || urlEvidencia.trim().isEmpty()) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "Tipo y URL/ruta de evidencia son obligatorios.");
            return;
        }

        Evidencia evidencia = new Evidencia(tareaSeleccionada.getId(), tipoEvidencia.trim(),
            urlEvidencia.trim(), new java.sql.Date(System.currentTimeMillis()));
        boolean registrada = evidenciaDAO.crear(evidencia);

        if (!registrada) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "No se pudo registrar la evidencia.");
            return;
        }

        agregarMensaje(FacesMessage.SEVERITY_INFO, "Evidencia registrada.");
        urlEvidencia = null;
        seleccionarTarea(tareaSeleccionada); // refresca la lista de evidencias
    }

    /**
     * Listener de p:fileUpload (tipos captura y documento). PrimeFaces ya
     * filtra extension y tamano en el navegador, pero se vuelve a validar
     * aqui porque la peticion puede llegar sin pasar por esa vista.
     */
    public void subirArchivoEvidencia(FileUploadEvent evento) {
        UploadedFile archivo = evento.getFile();
        if (tareaSeleccionada == null || archivo == null) {
            return;
        }
        if (!isEvidenciaConArchivo()) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "Elige primero el tipo Captura o Documento.");
            return;
        }
        if (archivo.getSize() > AlmacenEvidencias.TAMANO_MAXIMO) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "El archivo no debe superar 5 MB.");
            return;
        }
        if (!AlmacenEvidencias.extensionPermitida(tipoEvidencia, archivo.getFileName())) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "Tipo de archivo no permitido. Usa "
                + AlmacenEvidencias.extensionesPermitidas(tipoEvidencia) + ".");
            return;
        }

        String ruta;
        try (InputStream datos = archivo.getInputStream()) {
            ruta = AlmacenEvidencias.guardar(datos, archivo.getFileName());
        } catch (IOException e) {
            e.printStackTrace();
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "No se pudo guardar el archivo.");
            return;
        }

        Evidencia evidencia = new Evidencia(tareaSeleccionada.getId(), tipoEvidencia.trim(),
            ruta, new java.sql.Date(System.currentTimeMillis()));
        if (!evidenciaDAO.crear(evidencia)) {
            agregarMensaje(FacesMessage.SEVERITY_ERROR, "No se pudo registrar la evidencia.");
            return;
        }

        agregarMensaje(FacesMessage.SEVERITY_INFO, "Evidencia registrada: " + archivo.getFileName());
        seleccionarTarea(tareaSeleccionada);
    }

    public String[] getTiposEvidencia() {
        return new String[]{"enlace", "captura", "documento", "codigo"};
    }

    public boolean isEvidenciaConArchivo() {
        return AlmacenEvidencias.requiereArchivo(tipoEvidencia);
    }

    /** Extensiones para el texto de ayuda, p. ej. ".png, .jpg, .jpeg, .gif, .webp". */
    public String getExtensionesPermitidas() {
        return AlmacenEvidencias.extensionesPermitidas(tipoEvidencia);
    }

    /** Expresion para allowTypes de p:fileUpload, p. ej. /(\.|\/)(png|jpg)$/i */
    public String getPatronExtensiones() {
        String lista = getExtensionesPermitidas().replace(".", "").replace(", ", "|");
        return "/(\\.|\\/)(" + lista + ")$/i";
    }

    public boolean esArchivoSubido(Evidencia evidencia) {
        return evidencia.getUrlORuta() != null
            && evidencia.getUrlORuta().startsWith(AlmacenEvidencias.PREFIJO_URL);
    }

    public boolean esImagenSubida(Evidencia evidencia) {
        return esArchivoSubido(evidencia) && AlmacenEvidencias.esImagen(evidencia.getUrlORuta());
    }

    private void agregarMensaje(FacesMessage.Severity severidad, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidad, texto, null));
    }

    // Getters/setters para binding con la vista

    public int getProyectoId() { return proyectoId; }
    public void setProyectoId(int proyectoId) { this.proyectoId = proyectoId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public Integer getResponsableId() { return responsableId; }
    public void setResponsableId(Integer responsableId) { this.responsableId = responsableId; }

    public Date getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(Date fechaLimite) { this.fechaLimite = fechaLimite; }

    public Tarea getTareaSeleccionada() { return tareaSeleccionada; }
    public void setTareaSeleccionada(Tarea tareaSeleccionada) { this.tareaSeleccionada = tareaSeleccionada; }

    public String getDescripcionImpedimento() { return descripcionImpedimento; }
    public void setDescripcionImpedimento(String descripcionImpedimento) {
        this.descripcionImpedimento = descripcionImpedimento;
    }

    public String getTipoEvidencia() { return tipoEvidencia; }
    public void setTipoEvidencia(String tipoEvidencia) { this.tipoEvidencia = tipoEvidencia; }

    public String getUrlEvidencia() { return urlEvidencia; }
    public void setUrlEvidencia(String urlEvidencia) { this.urlEvidencia = urlEvidencia; }
}
