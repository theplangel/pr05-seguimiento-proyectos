package com.dsw.seguimiento.servlet;

import com.dsw.seguimiento.dao.EvidenciaDAO;
import com.dsw.seguimiento.model.Evidencia;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Date;

@WebServlet("/evidencias")
public class EvidenciaServlet extends HttpServlet {

    private final EvidenciaDAO evidenciaDAO = new EvidenciaDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        String proyectoId = request.getParameter("proyectoId");
        Integer tareaId = entero(request.getParameter("tareaId"));
        String tipo = request.getParameter("tipo");
        String urlORuta = request.getParameter("urlORuta");

        if (entero(proyectoId) == null || tareaId == null
                || estaVacio(tipo) || estaVacio(urlORuta)) {
            redirigirConResultado(request, response, proyectoId, "evidenciaIncompleta");
            return;
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
