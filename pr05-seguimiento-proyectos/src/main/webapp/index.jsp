<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="false" %>
<!DOCTYPE html>
<html lang="es">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Seguimiento de Proyectos | M02 Web 1.0</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css">
</head>

<body>

    <!-- Barra de Navegación -->
    <header class="navbar">
        <div class="navbar-inner">
            <a href="${pageContext.request.contextPath}/" class="navbar-brand">
                Seguimiento de Proyectos
            </a>
            <nav>
                <ul class="navbar-nav">
                    <li><a href="${pageContext.request.contextPath}/" class="nav-link active">Inicio</a></li>
                    <li><a href="${pageContext.request.contextPath}/proyectos" class="nav-link">Catálogo de
                            Proyectos</a></li>
                </ul>
            </nav>
        </div>
    </header>

    <main class="container">
        <!-- Hero / Presentación -->
        <section class="card" style="border-top: 4px solid var(--primary-light);">
            <div class="card-body" style="padding: 2.5rem 2rem; text-align: center;">
                <h1 style="color: var(--primary-dark); font-size: 2.2rem; margin-bottom: 0.75rem;">
                    Sistema de Seguimiento de Proyectos Web
                </h1>
                <div class="actions" style="justify-content: center;">
                    <a href="${pageContext.request.contextPath}/proyectos" class="btn btn-primary"
                        style="padding: 0.75rem 1.75rem; font-size: 1.05rem;">
                        Catálogo de Proyectos &rarr;
                    </a>
                </div>
            </div>
        </section>

        <!-- Arquitectura y Funcionalidades del M02 -->
        <div class="grid-3">
            <div class="card">
                <div class="card-header">
                    <h3>Proyectos</h3>
                </div>
                <div class="card-body">
                    <p style="font-size: 0.92rem; color: var(--text-muted); margin-bottom: 1rem;">
                        Consulta y alta de proyectos con seguimiento de fechas y estados (<code>ACTIVO</code>,
                        <code>PAUSADO</code>, <code>CERRADO</code>).
                    </p>
                    <a href="${pageContext.request.contextPath}/proyectos" class="btn btn-sm btn-outline">Ver
                        proyectos &rarr;</a>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h3>📋 2. Tareas y Flujo</h3>
                </div>
                <div class="card-body">
                    <p style="font-size: 0.92rem; color: var(--text-muted); margin-bottom: 1rem;">
                        Asignación a integrantes del equipo y transiciones de estado estrictas:
                        <code>PENDIENTE</code> &rarr; <code>EN_PROGRESO</code> &rarr; <code>BLOQUEADA</code> /
                        <code>COMPLETADA</code>.
                    </p>
                    <span class="badge badge-en_progreso">Máquina de Estados</span>
                </div>
            </div>

            <div class="card">
                <div class="card-header">
                    <h3>3. Bloqueos y Evidencias</h3>
                </div>
                <div class="card-body">
                    <p style="font-size: 0.92rem; color: var(--text-muted); margin-bottom: 1rem;">
                        Regla de negocio: <strong>No se permite completar una tarea con impedimentos
                            abiertos</strong>.
                        Adjunte evidencias verificables para trazabilidad.
                    </p>
                    <span class="badge badge-bloqueada">Regla de Negocio</span>
                </div>
            </div>
        </div>
    </main>

</body>

</html>