package com.dsw.seguimiento.servlet;

import com.dsw.seguimiento.dao.TareaDAO;
import com.dsw.seguimiento.model.Tarea;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Date;
import java.util.List;

@WebServlet("/tareas")
public class TareaServlet extends HttpServlet {

    private final TareaDAO tareaDAO = new TareaDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String proyectoIdParam = request.getParameter("proyectoId");
        Integer proyectoId = entero(proyectoIdParam);
        if (proyectoId == null) {
            request.setAttribute("error", "Falta el parametro proyectoId.");
            request.getRequestDispatcher("/WEB-INF/views/tareas.jsp").forward(request, response);
            return;
        }
        List<Tarea> tareas = tareaDAO.listarPorProyecto(proyectoId);
        request.setAttribute("tareas", tareas);
        request.setAttribute("proyectoId", proyectoId);
        request.getRequestDispatcher("/WEB-INF/views/tareas.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        Integer proyectoId = entero(request.getParameter("proyectoId"));
        if (proyectoId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "proyectoId es obligatorio");
            return;
        }

        String accion = request.getParameter("accion");
        if ("crear".equals(accion)) {
            crearTarea(request, response);
        } else if ("cambiarEstado".equals(accion)) {
            cambiarEstado(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Accion no valida");
        }
    }

    private void crearTarea(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String proyectoIdParam = request.getParameter("proyectoId");
        Integer proyectoId = entero(proyectoIdParam);
        String titulo = request.getParameter("titulo");
        Integer responsableId = entero(request.getParameter("responsableId"));
        String fechaLimiteParam = request.getParameter("fechaLimite");
        Date fechaLimite = fecha(fechaLimiteParam);

        if (proyectoId == null || estaVacio(titulo) || responsableId == null
                || (!estaVacio(fechaLimiteParam) && fechaLimite == null)) {
            response.sendRedirect(request.getContextPath() + "/tareas?proyectoId="
                    + (proyectoIdParam == null ? "" : proyectoIdParam)
                    + "&resultado=camposInvalidos");
            return;
        }

        boolean creado = tareaDAO.crear(
                new Tarea(proyectoId, titulo.trim(), responsableId, fechaLimite));
        String resultado = creado ? "ok" : "error";
        response.sendRedirect(request.getContextPath() + "/tareas?proyectoId="
                + proyectoId + "&resultado=" + resultado);
    }

    private void cambiarEstado(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Integer tareaId = entero(request.getParameter("tareaId"));
        String proyectoId = request.getParameter("proyectoId");
        String nuevoEstado = request.getParameter("nuevoEstado");
        if (tareaId == null || estaVacio(proyectoId) || estaVacio(nuevoEstado)) {
            response.sendRedirect(request.getContextPath() + "/tareas?proyectoId="
                    + (proyectoId == null ? "" : proyectoId)
                    + "&resultado=transicionInvalida");
            return;
        }

        boolean exito = tareaDAO.cambiarEstado(tareaId, nuevoEstado.trim());
        String resultado = exito ? "ok" : "transicionInvalida";
        response.sendRedirect(request.getContextPath() + "/tareas?proyectoId="
                + proyectoId + "&resultado=" + resultado);
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

    private Date fecha(String valor) {
        if (estaVacio(valor)) {
            return null;
        }
        try {
            return Date.valueOf(valor.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}