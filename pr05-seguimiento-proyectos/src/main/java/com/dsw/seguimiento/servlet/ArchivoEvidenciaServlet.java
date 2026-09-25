package com.dsw.seguimiento.servlet;

import com.dsw.seguimiento.util.AlmacenEvidencias;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Entrega los archivos subidos como evidencia:
 *   GET /archivos/evidencias/<uuid>.<ext>
 * Las imagenes y PDF se muestran en el navegador; el resto se descarga.
 */
@WebServlet("/archivos/evidencias/*")
public class ArchivoEvidenciaServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String ruta = request.getPathInfo();
        String nombre = (ruta == null) ? null : ruta.substring(1);
        Path archivo = AlmacenEvidencias.resolver(nombre);

        if (archivo == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Evidencia no encontrada.");
            return;
        }

        boolean verEnLinea = AlmacenEvidencias.esImagen(nombre) || nombre.endsWith(".pdf");
        response.setContentType(AlmacenEvidencias.contentType(nombre));
        response.setContentLengthLong(Files.size(archivo));
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Disposition",
                (verEnLinea ? "inline" : "attachment") + "; filename=\"" + nombre + "\"");
        Files.copy(archivo, response.getOutputStream());
    }
}
