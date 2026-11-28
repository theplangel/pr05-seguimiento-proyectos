<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="false" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="com.dsw.seguimiento.model.*" %>
<%@ page import="com.dsw.seguimiento.dao.*" %>
<%
    Integer proyectoId = (Integer) request.getAttribute("proyectoId");
    String errorServlet = (String) request.getAttribute("error");
    String resultadoParam = request.getParameter("resultado");

    Proyecto proyectoActual = null;
    List<Integrante> integrantes = new ArrayList<>();
    Map<Integer, String> mapaIntegrantes = new HashMap<>();

    ProyectoDAO proyectoDAO = new ProyectoDAO();
    IntegranteDAO integranteDAO = new IntegranteDAO();
    ImpedimentoDAO impedimentoDAO = new ImpedimentoDAO();
    EvidenciaDAO evidenciaDAO = new EvidenciaDAO();
    TareaDAO tareaDAO = new TareaDAO();

    if (proyectoId != null) {
        proyectoActual = proyectoDAO.buscarPorId(proyectoId);
        integrantes = integranteDAO.listarPorProyecto(proyectoId);
        for (Integrante itg : integrantes) {
            mapaIntegrantes.put(itg.getId(), itg.getNombre() + " (" + itg.getRolEquipo() + ")");
        }
    }

    @SuppressWarnings("unchecked")
    List<Tarea> tareas = (List<Tarea>) request.getAttribute("tareas");
    if (tareas == null) {
        tareas = new ArrayList<>();
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Seguimiento de Tareas - PR05</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css">
</head>
<body>

    <!-- Navegación -->
    <header class="navbar">
        <div class="navbar-inner">
            <a href="${pageContext.request.contextPath}/" class="navbar-brand">
                <span class="brand-logo">P</span>
                Seguimiento de Proyectos
            </a>
            <nav>
                <ul class="navbar-nav">
                    <li><a href="${pageContext.request.contextPath}/" class="nav-link">Inicio</a></li>
                    <li><a href="${pageContext.request.contextPath}/proyectos" class="nav-link active">Proyectos</a></li>
                </ul>
            </nav>
        </div>
    </header>

    <main class="container">

        <% if (errorServlet != null && !errorServlet.trim().isEmpty()) { %>
            <div class="alert alert-danger" role="alert">
                <div>
                    <div class="alert-title">Aviso:</div>
                    <%= errorServlet %>
                    <div style="margin-top: 0.5rem;">
                        <a href="${pageContext.request.contextPath}/proyectos" class="btn btn-sm btn-outline">&larr; Volver a Proyectos</a>
                    </div>
                </div>
            </div>
        <% } %>

        <!-- Mensajes de Retroalimentación -->
        <% if ("ok".equals(resultadoParam)) { %>
            <div class="alert alert-success" role="alert">
                <div>
                    <div class="alert-title">Operación completada:</div>
                    Los cambios han sido actualizados con éxito.
                </div>
            </div>
        <% } else if ("camposInvalidos".equals(resultadoParam)) { %>
            <div class="alert alert-danger" role="alert">
                <div>
                    <div class="alert-title">Campos incompletos:</div>
                    El título y el responsable son obligatorios, y la fecha límite debe tener un formato válido (aaaa-mm-dd).
                </div>
            </div>
        <% } else if ("transicionInvalida".equals(resultadoParam)) { %>
            <div class="alert alert-danger" role="alert">
                <div>
                    <div class="alert-title">No es posible realizar este cambio de estado:</div>
                    Asegúrese de que todos los impedimentos abiertos estén resueltos antes de marcar una tarea como completada.
                </div>
            </div>
        <% } else if ("evidenciaIncompleta".equals(resultadoParam)) { %>
            <div class="alert alert-warning" role="alert">
                <div>
                    <div class="alert-title">Datos de evidencia requeridos:</div>
                    Debe seleccionar el tipo y escribir el enlace o ruta de la evidencia.
                </div>
            </div>
        <% } else if ("error".equals(resultadoParam)) { %>
            <div class="alert alert-danger" role="alert">
                <div>
                    <div class="alert-title">Error en el servidor:</div>
                    Ocurrió un problema al procesar la información en la base de datos.
                </div>
            </div>
        <% } %>

        <% if (proyectoActual != null) { %>
            <!-- Encabezado del Proyecto -->
            <div class="card" style="margin-bottom: 1.5rem;">
                <div class="card-body">
                    <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem;">
                        <div>
                            <div style="display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.35rem;">
                                <h1 style="font-size: 1.5rem; font-weight: 700; color: var(--text-primary); margin: 0;">
                                    <%= proyectoActual.getNombre() %>
                                </h1>
                                <%
                                    String estadoBadge = "badge-activo";
                                    if (Proyecto.PAUSADO.equalsIgnoreCase(proyectoActual.getEstado())) estadoBadge = "badge-pausado";
                                    else if (Proyecto.CERRADO.equalsIgnoreCase(proyectoActual.getEstado())) estadoBadge = "badge-cerrado";
                                %>
                                <span class="badge <%= estadoBadge %>"><%= proyectoActual.getEstado() %></span>
                            </div>
                            <p style="color: var(--text-secondary); font-size: 0.92rem; margin-bottom: 0.35rem;">
                                <%= (proyectoActual.getDescripcion() != null && !proyectoActual.getDescripcion().isEmpty()) ? proyectoActual.getDescripcion() : "Sin descripción adicional." %>
                            </p>
                            <small style="color: var(--text-muted); font-size: 0.82rem;">
                                Fecha de inicio: <strong><%= proyectoActual.getFechaInicio() %></strong> &bull; Ref: #<%= proyectoActual.getId() %>
                            </small>
                        </div>
                        <div>
                            <a href="${pageContext.request.contextPath}/proyectos" class="btn btn-outline btn-sm">
                                &larr; Volver a Proyectos
                            </a>
                        </div>
                    </div>
                </div>
            </div>

            <div class="grid-2">

                <!-- Columna Izquierda: Listado de Tareas -->
                <section>
                    <div class="card">
                        <div class="card-header">
                            <h2>Tareas Asociadas</h2>
                            <span class="badge badge-en_progreso"><%= tareas.size() %> <%= tareas.size() == 1 ? "tarea" : "tareas" %></span>
                        </div>
                        <div class="card-body">
                            <% if (tareas.isEmpty()) { %>
                                <div class="empty-state">
                                    <h3>No hay tareas registradas</h3>
                                    <p>Utiliza el formulario de la derecha para agregar la primera tarea del proyecto.</p>
                                </div>
                            <% } else { %>
                                <% for (Tarea t : tareas) {
                                    boolean tieneBloqueo = tareaDAO.tieneImpedimentoAbierto(t.getId());
                                    List<Impedimento> impedimentos = impedimentoDAO.listarPorTarea(t.getId());
                                    List<Evidencia> evidencias = evidenciaDAO.listarPorTarea(t.getId());
                                    
                                    String claseCard = "item-card";
                                    if (tieneBloqueo) {
                                        claseCard += " is-blocked";
                                    } else if (Tarea.COMPLETADA.equals(t.getEstado())) {
                                        claseCard += " is-completed";
                                    }

                                    String badgeTarea = "badge-pendiente";
                                    if (Tarea.EN_PROGRESO.equals(t.getEstado())) badgeTarea = "badge-en_progreso";
                                    else if (Tarea.BLOQUEADA.equals(t.getEstado())) badgeTarea = "badge-bloqueada";
                                    else if (Tarea.COMPLETADA.equals(t.getEstado())) badgeTarea = "badge-completada";

                                    String nombreResponsable = mapaIntegrantes.get(t.getResponsableId());
                                    if (nombreResponsable == null) {
                                        nombreResponsable = "Integrante #" + t.getResponsableId();
                                    }
                                %>
                                    <div class="<%= claseCard %>">
                                        <div class="item-header">
                                            <div>
                                                <div class="item-title"><%= t.getTitulo() %></div>
                                                <div style="font-size: 0.82rem; color: var(--text-muted); margin-top: 0.2rem;">
                                                    Asignado a: <strong><%= nombreResponsable %></strong> &bull; 
                                                    Límite: <%= (t.getFechaLimite() != null) ? t.getFechaLimite().toString() : "<em>Sin fecha</em>" %>
                                                </div>
                                            </div>
                                            <div>
                                                <span class="badge <%= badgeTarea %>"><%= t.getEstado() %></span>
                                            </div>
                                        </div>

                                        <% if (tieneBloqueo) { %>
                                            <div style="background-color: #fef2f2; color: #991b1b; padding: 0.45rem 0.75rem; border-radius: 6px; font-size: 0.82rem; margin-bottom: 0.65rem; border: 1px solid #fecaca;">
                                                <strong>Atención:</strong> Esta tarea tiene impedimentos abiertos que deben resolverse para poder completarla.
                                            </div>
                                        <% } %>

                                        <!-- Transiciones de Estado -->
                                        <div class="actions" style="margin-top: 0.65rem; padding-top: 0.65rem; border-top: 1px solid var(--border-light);">
                                            <span style="font-size: 0.8rem; font-weight: 600; color: var(--text-muted); margin-right: 0.35rem;">Cambiar estado:</span>

                                            <% if (Tarea.PENDIENTE.equals(t.getEstado())) { %>
                                                <form method="POST" action="${pageContext.request.contextPath}/tareas" style="display:inline;">
                                                    <input type="hidden" name="accion" value="cambiarEstado">
                                                    <input type="hidden" name="tareaId" value="<%= t.getId() %>">
                                                    <input type="hidden" name="proyectoId" value="<%= proyectoId %>">
                                                    <input type="hidden" name="nuevoEstado" value="<%= Tarea.EN_PROGRESO %>">
                                                    <button type="submit" class="btn btn-sm btn-primary">Iniciar Tarea</button>
                                                </form>
                                            <% } else if (Tarea.EN_PROGRESO.equals(t.getEstado())) { %>
                                                <form method="POST" action="${pageContext.request.contextPath}/tareas" style="display:inline;">
                                                    <input type="hidden" name="accion" value="cambiarEstado">
                                                    <input type="hidden" name="tareaId" value="<%= t.getId() %>">
                                                    <input type="hidden" name="proyectoId" value="<%= proyectoId %>">
                                                    <input type="hidden" name="nuevoEstado" value="<%= Tarea.BLOQUEADA %>">
                                                    <button type="submit" class="btn btn-sm btn-warning">Bloquear</button>
                                                </form>

                                                <form method="POST" action="${pageContext.request.contextPath}/tareas" style="display:inline;">
                                                    <input type="hidden" name="accion" value="cambiarEstado">
                                                    <input type="hidden" name="tareaId" value="<%= t.getId() %>">
                                                    <input type="hidden" name="proyectoId" value="<%= proyectoId %>">
                                                    <input type="hidden" name="nuevoEstado" value="<%= Tarea.COMPLETADA %>">
                                                    <button type="submit" class="btn btn-sm btn-success">
                                                        Completar
                                                    </button>
                                                </form>
                                            <% } else if (Tarea.BLOQUEADA.equals(t.getEstado())) { %>
                                                <form method="POST" action="${pageContext.request.contextPath}/tareas" style="display:inline;">
                                                    <input type="hidden" name="accion" value="cambiarEstado">
                                                    <input type="hidden" name="tareaId" value="<%= t.getId() %>">
                                                    <input type="hidden" name="proyectoId" value="<%= proyectoId %>">
                                                    <input type="hidden" name="nuevoEstado" value="<%= Tarea.EN_PROGRESO %>">
                                                    <button type="submit" class="btn btn-sm btn-primary">Reanudar</button>
                                                </form>
                                            <% } else if (Tarea.COMPLETADA.equals(t.getEstado())) { %>
                                                <span style="font-size: 0.82rem; color: #16a34a; font-weight: 600;">Finalizada</span>
                                            <% } %>
                                        </div>

                                        <!-- Sub-panel: Impedimentos -->
                                        <div class="sub-panel">
                                            <h4>
                                                <span>Impedimentos (<%= impedimentos.size() %>)</span>
                                            </h4>

                                            <% if (!impedimentos.isEmpty()) { %>
                                                <ul class="sub-list">
                                                    <% for (Impedimento imp : impedimentos) { %>
                                                        <li>
                                                            <div>
                                                                <span class="badge <%= Impedimento.ABIERTO.equals(imp.getEstado()) ? "badge-abierto" : "badge-resuelto" %>" style="margin-right: 0.4rem;">
                                                                    <%= imp.getEstado() %>
                                                                </span>
                                                                <span><%= imp.getDescripcion() %></span>
                                                            </div>
                                                            <% if (Impedimento.ABIERTO.equals(imp.getEstado())) { %>
                                                                <form method="POST" action="${pageContext.request.contextPath}/impedimentos" style="margin: 0;">
                                                                    <input type="hidden" name="accion" value="resolver">
                                                                    <input type="hidden" name="impedimentoId" value="<%= imp.getId() %>">
                                                                    <input type="hidden" name="proyectoId" value="<%= proyectoId %>">
                                                                    <button type="submit" class="btn btn-sm btn-outline" style="font-size: 0.75rem; padding: 0.2rem 0.45rem;">
                                                                        Resolver
                                                                    </button>
                                                                </form>
                                                            <% } %>
                                                        </li>
                                                    <% } %>
                                                </ul>
                                            <% } else { %>
                                                <p style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 0.4rem;">Sin impedimentos reportados.</p>
                                            <% } %>

                                            <!-- Formulario Inline: Agregar Impedimento -->
                                            <form method="POST" action="${pageContext.request.contextPath}/impedimentos" style="margin-top: 0.5rem; display: flex; gap: 0.45rem; align-items: center; flex-wrap: wrap;">
                                                <input type="hidden" name="accion" value="crear">
                                                <input type="hidden" name="tareaId" value="<%= t.getId() %>">
                                                <input type="hidden" name="proyectoId" value="<%= proyectoId %>">
                                                <input type="text" name="descripcion" class="form-control" style="flex: 1; min-width: 180px; font-size: 0.82rem; padding: 0.35rem 0.6rem;" placeholder="Reportar nuevo impedimento..." required>
                                                <button type="submit" class="btn btn-sm btn-outline" style="white-space: nowrap;">
                                                    + Reportar
                                                </button>
                                            </form>
                                        </div>

                                        <!-- Sub-panel: Evidencias -->
                                        <div class="sub-panel">
                                            <h4>
                                                <span>Evidencias y Enlaces (<%= evidencias.size() %>)</span>
                                            </h4>

                                            <% if (!evidencias.isEmpty()) { %>
                                                <ul class="sub-list">
                                                    <% for (Evidencia ev : evidencias) { %>
                                                        <li>
                                                            <div>
                                                                <span class="badge badge-activo" style="margin-right: 0.4rem; font-size: 0.7rem;"><%= ev.getTipo() %></span>
                                                                <a href="<%= ev.getUrlORuta() %>" target="_blank" rel="noopener noreferrer" style="color: var(--primary); text-decoration: underline;">
                                                                    <%= ev.getUrlORuta() %>
                                                                </a>
                                                            </div>
                                                            <span style="font-size: 0.75rem; color: var(--text-muted);"><%= ev.getFecha() %></span>
                                                        </li>
                                                    <% } %>
                                                </ul>
                                            <% } else { %>
                                                <p style="font-size: 0.8rem; color: var(--text-muted); margin-bottom: 0.4rem;">Sin evidencias adjuntas.</p>
                                            <% } %>

                                            <!-- Formulario Inline: Adjuntar Evidencia -->
                                            <form method="POST" action="${pageContext.request.contextPath}/evidencias" style="margin-top: 0.5rem; display: flex; gap: 0.45rem; align-items: center; flex-wrap: wrap;">
                                                <input type="hidden" name="tareaId" value="<%= t.getId() %>">
                                                <input type="hidden" name="proyectoId" value="<%= proyectoId %>">
                                                <select name="tipo" class="form-control select-tipo-evidencia" style="width: auto; min-width: 105px; font-size: 0.82rem; padding: 0.35rem 0.5rem;" required>
                                                    <option value="enlace">Enlace</option>
                                                    <option value="captura">Captura</option>
                                                    <option value="documento">Documento</option>
                                                    <option value="codigo">Código</option>
                                                </select>
                                                <input type="text" name="urlORuta" class="form-control input-ruta-evidencia" style="flex: 1; min-width: 170px; font-size: 0.82rem; padding: 0.35rem 0.6rem;" placeholder="URL o enlace del recurso..." required>
                                                <button type="submit" class="btn btn-sm btn-outline" style="white-space: nowrap;">
                                                    + Adjuntar
                                                </button>
                                            </form>
                                        </div>

                                    </div>
                                <% } %>
                            <% } %>
                        </div>
                    </div>
                </section>

                <!-- Columna Derecha: Formulario para Crear Tarea -->
                <aside>
                    <div class="card">
                        <div class="card-header">
                            <h2>Nueva Tarea</h2>
                        </div>
                        <div class="card-body">
                            <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 1.25rem;">
                                Agrega una actividad para el seguimiento de este proyecto.
                            </p>

                            <form method="POST" action="${pageContext.request.contextPath}/tareas" accept-charset="UTF-8">
                                <input type="hidden" name="accion" value="crear">
                                <input type="hidden" name="proyectoId" value="<%= proyectoId %>">

                                <div class="form-group">
                                    <label for="titulo">Título de la Tarea <span class="required">*</span></label>
                                    <input type="text" id="titulo" name="titulo" class="form-control" placeholder="Nombre de la actividad" required>
                                </div>

                                <div class="form-group">
                                    <label for="responsableId">Responsable Asignado <span class="required">*</span></label>
                                    <% if (!integrantes.isEmpty()) { %>
                                        <select id="responsableId" name="responsableId" class="form-control" required>
                                            <option value="">-- Seleccionar integrante --</option>
                                            <% for (Integrante itg : integrantes) { %>
                                                <option value="<%= itg.getId() %>">
                                                    <%= itg.getNombre() %> &mdash; <%= itg.getRolEquipo() %>
                                                </option>
                                            <% } %>
                                        </select>
                                    <% } else { %>
                                        <input type="number" id="responsableId" name="responsableId" class="form-control" placeholder="ID del integrante" required>
                                    <% } %>
                                </div>

                                <div class="form-group">
                                    <label for="fechaLimite">Fecha Límite</label>
                                    <input type="date" id="fechaLimite" name="fechaLimite" class="form-control">
                                    <div class="form-hint">Opcional. Formato: aaaa-mm-dd.</div>
                                </div>

                                <div style="margin-top: 1.5rem;">
                                    <button type="submit" class="btn btn-primary btn-block">
                                        Crear Tarea
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </aside>

            </div>
        <% } %>

    </main>

    <footer class="footer">
        <p><strong>PR05 &bull; Seguimiento de Proyectos</strong></p>
    </footer>

    <!-- Script dinámico para cambiar el placeholder según el tipo de evidencia -->
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            const selectores = document.querySelectorAll('.select-tipo-evidencia');
            selectores.forEach(function(select) {
                select.addEventListener('change', function() {
                    const inputRuta = this.form.querySelector('.input-ruta-evidencia');
                    if (this.value === 'documento' || this.value === 'codigo') {
                        inputRuta.placeholder = 'Ruta o nombre del archivo (ej. docs/manual.pdf)...';
                    } else if (this.value === 'captura') {
                        inputRuta.placeholder = 'Ruta de imagen (ej. img/captura.png)...';
                    } else {
                        inputRuta.placeholder = 'URL o enlace del recurso...';
                    }
                });
            });
        });
    </script>

</body>
</html>