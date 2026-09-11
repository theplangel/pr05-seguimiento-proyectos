package com.dsw.seguimiento.servlet;

import com.dsw.seguimiento.dao.ImpedimentoDAO;
import com.dsw.seguimiento.model.Impedimento;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/impedimentos")
public class ImpedimentoServlet extends HttpServlet {

    private final ImpedimentoDAO impedimentoDAO = new ImpedimentoDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        String accion = request.getParameter("accion");
        Integer proyectoId = entero(request.getParameter("proyectoId"));
        if (proyectoId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "proyectoId es obligatorio");
            return;
        }

        boolean resultado;
        if ("crear".equals(accion)) {
            resultado = crear(request);
        } else if ("resolver".equals(accion)) {
            resultado = resolver(request);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Accion no valida");
            return;
        }

        String estado = resultado ? "ok" : "error";
        response.sendRedirect(request.getContextPath() + "/tareas?proyectoId="
                + proyectoId + "&resultado=" + estado);
    }

    private boolean crear(HttpServletRequest request) {
        Integer tareaId = entero(request.getParameter("tareaId"));
        String descripcion = request.getParameter("descripcion");
        if (tareaId == null || estaVacio(descripcion)) {
            return false;
        }
        return impedimentoDAO.crear(new Impedimento(tareaId, descripcion.trim()));
    }

    private boolean resolver(HttpServletRequest request) {
        Integer impedimentoId = entero(request.getParameter("impedimentoId"));
        return impedimentoId != null && impedimentoDAO.resolver(impedimentoId);
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
