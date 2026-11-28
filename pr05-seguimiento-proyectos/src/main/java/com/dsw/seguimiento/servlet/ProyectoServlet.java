package com.dsw.seguimiento.servlet;

import com.dsw.seguimiento.dao.ProyectoDAO;
import com.dsw.seguimiento.model.Proyecto;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Date;
import java.util.List;

@WebServlet("/proyectos")
public class ProyectoServlet extends HttpServlet {

    private final ProyectoDAO proyectoDAO = new ProyectoDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Proyecto> proyectos = proyectoDAO.listarTodos();
        request.setAttribute("proyectos", proyectos);
        request.getRequestDispatcher("/WEB-INF/views/proyectos.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String nombre = request.getParameter("nombre");
        String descripcion = request.getParameter("descripcion");
        String fechaInicioTexto = request.getParameter("fechaInicio");

        if (estaVacio(nombre) || estaVacio(fechaInicioTexto)) {
            request.setAttribute("error", "Nombre y fecha de inicio son obligatorios.");
            doGet(request, response);
            return;
        }

        Date fechaInicio;
        try {
            fechaInicio = Date.valueOf(fechaInicioTexto.trim());
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", "Formato de fecha invalido. Usa aaaa-mm-dd.");
            doGet(request, response);
            return;
        }

        if (!proyectoDAO.crear(new Proyecto(nombre.trim(), descripcion, fechaInicio))) {
            request.setAttribute("error", "No se pudo crear el proyecto. Intenta de nuevo.");
            doGet(request, response);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/proyectos");
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}