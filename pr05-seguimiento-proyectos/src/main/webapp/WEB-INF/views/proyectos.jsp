<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="false" %>
<%@ page import="java.util.List" %>
<%@ page import="com.dsw.seguimiento.model.Proyecto" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Proyectos - Seguimiento de Proyectos</title>
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
        <!-- Título de Sección -->
        <div style="margin-bottom: 1.5rem; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem;">
            <div>
                <h1 style="color: var(--text-primary); font-size: 1.6rem; font-weight: 700;">Proyectos</h1>
                <p style="color: var(--text-secondary); font-size: 0.92rem;">
                    Administración y supervisión de las iniciativas en curso.
                </p>
            </div>
            <div>
                <a href="${pageContext.request.contextPath}/" class="btn btn-outline btn-sm">&larr; Inicio</a>
            </div>
        </div>

        <!-- Mensajes de Error de Validación -->
        <%
            String errorMsg = (String) request.getAttribute("error");
            if (errorMsg != null && !errorMsg.trim().isEmpty()) {
        %>
            <div class="alert alert-danger" role="alert">
                <div>
                    <div class="alert-title">No fue posible registrar el proyecto:</div>
                    <%= errorMsg %>
                </div>
            </div>
        <%
            }
        %>

        <div class="grid-2">
            <!-- Columna 1: Listado de Proyectos -->
            <section class="card">
                <div class="card-header">
                    <h2>Iniciativas Registradas</h2>
                    <%
                        @SuppressWarnings("unchecked")
                        List<Proyecto> proyectos = (List<Proyecto>) request.getAttribute("proyectos");
                        int total = (proyectos != null) ? proyectos.size() : 0;
                    %>
                    <span class="badge badge-activo"><%= total %> <%= total == 1 ? "proyecto" : "proyectos" %></span>
                </div>
                <div class="card-body" style="padding: 0;">
                    <%
                        if (proyectos == null || proyectos.isEmpty()) {
                    %>
                        <div class="empty-state">
                            <h3>No hay proyectos registrados</h3>
                            <p>Utiliza el formulario de la derecha para dar de alta tu primera iniciativa.</p>
                        </div>
                    <%
                        } else {
                    %>
                        <div class="table-responsive">
                            <table class="table">
                                <thead>
                                    <tr>
                                        <th style="width: 8%;">ID</th>
                                        <th style="width: 32%;">Nombre</th>
                                        <th style="width: 26%;">Descripción</th>
                                        <th style="width: 14%;">Fecha Inicio</th>
                                        <th style="width: 10%;">Estado</th>
                                        <th style="width: 10%; text-align: center;">Acción</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <%
                                        for (Proyecto p : proyectos) {
                                            String estadoClase = "badge-activo";
                                            if (Proyecto.PAUSADO.equalsIgnoreCase(p.getEstado())) {
                                                estadoClase = "badge-pausado";
                                            } else if (Proyecto.CERRADO.equalsIgnoreCase(p.getEstado())) {
                                                estadoClase = "badge-cerrado";
                                            }
                                    %>
                                        <tr>
                                            <td><span style="color: var(--text-muted); font-size: 0.85rem;">#<%= p.getId() %></span></td>
                                            <td>
                                                <a href="${pageContext.request.contextPath}/tareas?proyectoId=<%= p.getId() %>" style="color: var(--primary); font-weight: 600; text-decoration: none;">
                                                    <%= p.getNombre() %>
                                                </a>
                                            </td>
                                            <td style="color: var(--text-secondary); font-size: 0.88rem;">
                                                <%= (p.getDescripcion() != null && !p.getDescripcion().trim().isEmpty()) ? p.getDescripcion() : "<span style='color: var(--text-muted); font-style: italic;'>Sin descripción</span>" %>
                                            </td>
                                            <td style="white-space: nowrap; font-size: 0.88rem; color: var(--text-secondary);"><%= p.getFechaInicio() %></td>
                                            <td>
                                                <span class="badge <%= estadoClase %>"><%= p.getEstado() %></span>
                                            </td>
                                            <td style="text-align: center; white-space: nowrap;">
                                                <a href="${pageContext.request.contextPath}/tareas?proyectoId=<%= p.getId() %>" class="btn btn-sm btn-outline" title="Ver tareas del proyecto">
                                                    Tareas &rarr;
                                                </a>
                                            </td>
                                        </tr>
                                    <%
                                        }
                                    %>
                                </tbody>
                            </table>
                        </div>
                    <%
                        }
                    %>
                </div>
            </section>

            <!-- Columna 2: Formulario de Registro -->
            <aside class="card">
                <div class="card-header">
                    <h2>Registrar Proyecto</h2>
                </div>
                <div class="card-body">
                    <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 1.25rem;">
                        Completa la información básica para dar de alta una nueva iniciativa.
                    </p>

                    <%
                        String paramNombre = request.getParameter("nombre");
                        String paramDesc = request.getParameter("descripcion");
                        String paramFecha = request.getParameter("fechaInicio");
                    %>

                    <form method="POST" action="${pageContext.request.contextPath}/proyectos" accept-charset="UTF-8">
                        <div class="form-group">
                            <label for="nombre">Nombre <span class="required">*</span></label>
                            <input type="text"
                                   id="nombre"
                                   name="nombre"
                                   class="form-control"
                                   placeholder="Nombre del proyecto"
                                   value="<%= (paramNombre != null) ? paramNombre : "" %>">
                            <div class="form-hint">Campo obligatorio.</div>
                        </div>

                        <div class="form-group">
                            <label for="descripcion">Descripción</label>
                            <textarea id="descripcion"
                                      name="descripcion"
                                      class="form-control"
                                      rows="3"
                                      placeholder="Objetivos o alcance general"><%= (paramDesc != null) ? paramDesc : "" %></textarea>
                        </div>

                        <div class="form-group">
                            <label for="fechaInicio">Fecha de Inicio <span class="required">*</span></label>
                            <input type="date"
                                   id="fechaInicio"
                                   name="fechaInicio"
                                   class="form-control"
                                   value="<%= (paramFecha != null) ? paramFecha : "" %>">
                            <div class="form-hint">Formato: aaaa-mm-dd.</div>
                        </div>

                        <div style="margin-top: 1.5rem;">
                            <button type="submit" class="btn btn-primary btn-block">
                                Guardar Proyecto
                            </button>
                        </div>
                    </form>
                </div>
            </aside>
        </div>
    </main>

    <footer class="footer">
        <p><strong>PR05 &bull; Seguimiento de Proyectos</strong></p>
    </footer>

</body>
</html>