# PR05 — Sistema de Seguimiento de Proyectos Web

[![Java 11](https://img.shields.io/badge/Java-11-blue.svg)](https://www.oracle.com/java/)
[![Servlet 4.0](https://img.shields.io/badge/Servlet-4.0-orange.svg)](https://jakarta.ee/)
[![Tomcat 9](https://img.shields.io/badge/Tomcat-9.0-yellow.svg)](https://tomcat.apache.org/)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Rúbrica R02](https://img.shields.io/badge/Evaluaci%C3%B3n-R02%20(10%20pts)-brightgreen.svg)]()

> **Experiencia Educativa:** Desarrollo de Sistemas Web (DSW-19559 / NRC 19559)  
> **Facilitador:** Dr. Gabriel Rodríguez Vásquez  
> **Módulo:** M02. Web 1.0 con JSP, Tomcat y PostgreSQL  
> **Actividad:** P02 (Incremento 1 del PR05 confirmado en F02 y delimitado en C11)

---

## 1. Identificación y Distribución del Equipo

| Rol | Responsable | Componente y Alcance Evaluado |
| :--- | :--- | :--- |
| **Persona A** | **Vistas JSP** | **Formularios, listados, alertas, navegación y validaciones (positivas y negativas)** |
| **Persona B** | Servlets | Controladores HTTP (`ProyectoServlet`, `TareaServlet`, `ImpedimentoServlet`, `EvidenciaServlet`) |
| **Persona C** | Persistencia | Modelo relacional (6 entidades), DAOs JDBC parametrizados y scripts DDL/DML |

---

## 2. Arquitectura del Incremento (Web 1.0 MVC)

El incremento implementa una arquitectura Web 1.0 dinámica desacoplada:
- **Capa de Presentación (JSP 2.3 + CSS 100% Autocontenido):**
  - `index.jsp`: Landing page con navegación institucional, presentación del sistema y accesos directos.
  - `WEB-INF/views/proyectos.jsp`: Listado de proyectos y formulario de alta (`POST /proyectos`).
  - `WEB-INF/views/tareas.jsp`: Tablero de seguimiento por proyecto, máquina de estados, gestión de impedimentos y evidencias.
  - `css/estilos.css`: Estilos visuales puros y responsivos, sin dependencias CDN de terceros.
- **Capa de Control (Servlets 4.0):**
  - Procesamiento del ciclo de vida HTTP (`GET` para consulta, `POST` para cambios de estado y persistencia).
  - Manejo de redirecciones (`sendRedirect`) y reenvíos (`RequestDispatcher.forward`).
- **Capa de Modelo y Persistencia (PostgreSQL 16 + JDBC):**
  - Modelo de 6 entidades: `proyecto`, `integrante`, `tarea`, `impedimento`, `evidencia`, `evento_avance`.
  - Consultas SQL parametrizadas (`PreparedStatement`) protegidas contra inyección SQL.

---

## 3. Requisitos de Entorno

- **Java JDK:** 11 LTS (o superior compatible, ej. 11 a 17)
- **Apache Tomcat:** Versión 9.0.x
- **PostgreSQL:** Versión 16 (o superior)
- **Apache Maven:** 3.8 o 3.9

---

## 4. Base de Datos y Persistencia

### 4.1. Configuración de Conexión (`src/main/resources/db.properties`)

```properties
db.url=jdbc:postgresql://localhost:5432/seguimiento_proyectos
db.user=app_seguimiento
db.password=una_clave_local
```

> *Nota de Seguridad:* Las credenciales pueden sobreescribirse mediante variables de entorno del sistema (`DB_PASSWORD`) o propiedades de JVM (`-Ddb.password`), cumpliendo con la política de no exponer secretos reales en el repositorio.

### 4.2. Ejecución de Scripts SQL

Para inicializar la base de datos con las 6 entidades y datos de prueba:

```bash
# Crear base de datos y usuario (desde psql con superusuario)
psql -U postgres -c "CREATE DATABASE seguimiento_proyectos;"
psql -U postgres -c "CREATE USER app_seguimiento WITH PASSWORD 'una_clave_local';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE seguimiento_proyectos TO app_seguimiento;"

# Ejecutar DDL y datos de prueba reproducibles
psql -U app_seguimiento -d seguimiento_proyectos -f sql/init.sql
```

---

## 5. Compilación y Despliegue en Tomcat 9

### 5.1. Construcción del WAR con Maven

Desde la raíz del proyecto (`pr05-seguimiento-proyectos`):

```bash
mvn clean package
```

Se generará el archivo empaquetado:
`target/pr05-seguimiento-proyectos.war`

### 5.2. Despliegue en Tomcat 9

1. Copiar `target/pr05-seguimiento-proyectos.war` al directorio `webapps/` de Apache Tomcat:
   ```bash
   cp target/pr05-seguimiento-proyectos.war $CATALINA_HOME/webapps/
   ```
2. Iniciar Tomcat (`bin/startup.sh` en Linux/macOS o `bin/startup.bat` en Windows).
3. Acceder desde el navegador:
   ```
   http://localhost:8080/pr05-seguimiento-proyectos/
   ```

---

## 6. Recorrido Reproducible y Casos de Prueba (Rúbrica R02)

### 6.1. Flujo Principal y Pruebas Positivas (2.5 pts Navegación)

1. **Acceso al Portal Principal:**
   - URL: `http://localhost:8080/pr05-seguimiento-proyectos/`
   - Resultado esperado: Página de bienvenida con descripción del proyecto PR05 y botón para acceder al catálogo.
2. **Consulta del Catálogo de Proyectos:**
   - Navegar a: `http://localhost:8080/pr05-seguimiento-proyectos/proyectos`
   - Resultado esperado: Se despliega la tabla con los proyectos existentes y sus estados (`ACTIVO`, `PAUSADO`).
3. **Registro Exitoso de un Nuevo Proyecto (POST):**
   - Llenar el formulario:
     * Nombre: `Auditoría de Sistemas 2026`
     * Descripción: `Proyecto de revisión de calidad y seguridad.`
     * Fecha de Inicio: `2026-09-15`
   - Enviar formulario.
   - Resultado esperado: Redirección automática a `/proyectos` y el nuevo proyecto aparece en el listado.
4. **Seguimiento de Tareas:**
   - Hacer clic en el botón `Tareas →` del proyecto `#1`.
   - URL: `http://localhost:8080/pr05-seguimiento-proyectos/tareas?proyectoId=1`
   - Resultado esperado: Se muestran los detalles del proyecto `#1` y la lista de sus tareas asociadas con sus responsables y estados.
5. **Transición Válida de Estado:**
   - En una tarea con estado `PENDIENTE`, hacer clic en `▶ Iniciar (EN_PROGRESO)`.
   - Resultado esperado: La tarea pasa inmediatamente al estado `EN_PROGRESO` con banner verde de confirmación (`param.resultado=ok`).

---

### 6.2. Protocolo de Pruebas Negativas (Validaciones de Entrada y Reglas de Negocio)

Para la verificación de la rúbrica R02, la interfaz y controladores implementan validaciones negativas controladas:

| Prueba Negativa | Acción del Usuario | Resultado Esperado en Vista JSP |
| :--- | :--- | :--- |
| **Campos Vacíos en Proyecto** | Enviar el formulario en `/proyectos` dejando el nombre o fecha vacíos. | Alerta roja en vista: `"Nombre y fecha de inicio son obligatorios."` |
| **Fecha Inválida en Proyecto** | Enviar formulario con fecha no válida (ej. `32-13-2026`). | Alerta roja en vista: `"Formato de fecha invalido. Usa aaaa-mm-dd."` |
| **Parámetro proyectoId Ausente** | Acceder directamente a `/tareas` sin especificar `?proyectoId=X`. | Alerta roja: `"Error en la Solicitud: Falta el parametro proyectoId."` con botón de retorno. |
| **Campos Inválidos en Tarea** | En `/tareas`, intentar crear una tarea con título vacío. | Redirección con `?resultado=camposInvalidos` y mensaje rojo de validación negativa. |
| **Violación de Regla de Negocio (Impedimento Abierto)** | En una tarea con impedimento abierto (ej. Tarea `#5`), intentar presionar `✔ Completar`. | El DAO y Servlet rechazan la transición. La vista JSP despliega: `"No se puede ejecutar la transición... Una tarea no puede completarse si cuenta con al menos un impedimento ABIERTO."` |
| **Evidencia Incompleta** | Intentar adjuntar una evidencia sin URL o ruta. | Redirección con `?resultado=evidenciaIncompleta` y banner amarillo de advertencia. |

---

## 7. Trazabilidad con Criterios de Evaluación R02 (10 pts)

- **Navegación JSP/Servlet y flujo funcional (2.5 pts):** Navegación fluida entre inicio, proyectos y tareas con retroalimentación completa y controles contextuales.
- **Despliegue correcto en Tomcat (2.0 pts):** Empaquetado WAR estandarizado en Maven con descriptor Servlet 3.1.
- **Persistencia en PostgreSQL (2.5 pts):** Esquema relacional de 6 entidades en `sql/init.sql` con datos de prueba funcionales.
- **README y trazabilidad (2.0 pts):** Esta guía técnica reproducible con comandos de instalación y casos de prueba.
- **Orden y limpieza técnica (1.0 pt):** Estructura estándar Maven, separación estricta de capas MVC y ausencia de archivos basura.

