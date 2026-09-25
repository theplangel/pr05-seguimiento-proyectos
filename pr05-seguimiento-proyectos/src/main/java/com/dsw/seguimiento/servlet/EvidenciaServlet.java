package com.dsw.seguimiento.servlet;

import com.dsw.seguimiento.dao.EvidenciaDAO;
import com.dsw.seguimiento.model.Evidencia;
import com.dsw.seguimiento.util.AlmacenEvidencias;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;

/**
 * Registra evidencias de una tarea. Los tipos "enlace" y "codigo" reciben
 * una URL o ruta en texto; "captura" y "documento" reciben un archivo
 * (multipart/form-data) que se guarda con AlmacenEvidencias.
 *
 * maxRequestSize es mayor que AlmacenEvidencias.TAMANO_MAXIMO para que un
 * archivo apenas pasado del limite llegue aqui y reciba un mensaje claro.
 */
@WebServlet("/evidencias")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxRequestSize = 12L * 1024 * 1024)
public class EvidenciaServlet extends HttpServlet {

    private final EvidenciaDAO evidenciaDAO = new EvidenciaDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        // proyectoId viaja en la URL del action, asi se conserva aunque falle el multipart
        String proyectoId = request.getParameter("proyectoId");

        if (esMultipart(request)) {
            try {
                request.getParts();
            } catch (IllegalStateException e) {
                // la peticion supero maxRequestSize: el cuerpo no se leyo
                redirigirConResultado(request, response, proyectoId, "archivoGrande");
                return;
            }
        }

        Integer tareaId = entero(request.getParameter("tareaId"));
        String tipo = request.getParameter("tipo");

        if (entero(proyectoId) == null || tareaId == null || estaVacio(tipo)) {
            redirigirConResultado(request, response, proyectoId, "evidenciaIncompleta");
            return;
        }

        String urlORuta;
        if (AlmacenEvidencias.requiereArchivo(tipo)) {
            Part archivo = esMultipart(request) ? request.getPart("archivo") : null;
            String nombreOriginal = (archivo == null) ? null : archivo.getSubmittedFileName();
            if (archivo == null || archivo.getSize() == 0 || estaVacio(nombreOriginal)) {
                redirigirConResultado(request, response, proyectoId, "archivoFaltante");
                return;
            }
            if (archivo.getSize() > AlmacenEvidencias.TAMANO_MAXIMO) {
                redirigirConResultado(request, response, proyectoId, "archivoGrande");
                return;
            }
            if (!AlmacenEvidencias.extensionPermitida(tipo, nombreOriginal)) {
                // tipo ya se valido contra la lista de tipos con archivo, es seguro ponerlo en la URL
                redirigirConResultado(request, response, proyectoId,
                        "archivoInvalido&tipo=" + tipo.trim().toLowerCase());
                return;
            }

            try (InputStream datos = archivo.getInputStream()) {
                urlORuta = AlmacenEvidencias.guardar(datos, nombreOriginal);
            } catch (IOException e) {
                e.printStackTrace();
                redirigirConResultado(request, response, proyectoId, "error");
                return;
            }
        } else {
            urlORuta = request.getParameter("urlORuta");
            if (estaVacio(urlORuta)) {
                redirigirConResultado(request, response, proyectoId, "evidenciaIncompleta");
                return;
            }
        }

        Evidencia evidencia = new Evidencia(
                tareaId, tipo.trim(), urlORuta.trim(), new Date(System.currentTimeMillis()));
        String resultado = evidenciaDAO.crear(evidencia) ? "ok" : "error";
        redirigirConResultado(request, response, proyectoId, resultado);
    }

    private void redirigirConResultado(HttpServletRequest request,
                                       HttpServletResponse response,
                                       String proyectoId,
                                       String resultado) throws IOException {
        response.sendRedirect(request.getContextPath() + "/tareas?proyectoId="
                + (proyectoId == null ? "" : proyectoId) + "&resultado=" + resultado);
    }

    private boolean esMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    private Integer entero(String valor) {
        if (estaVacio(valor)) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
