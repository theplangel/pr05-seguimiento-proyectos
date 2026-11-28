package com.dsw.seguimiento;

import com.dsw.seguimiento.dao.ProyectoDAO;
import com.dsw.seguimiento.model.Proyecto;

import java.util.List;

public final class PruebaProyectoDAO {

    private PruebaProyectoDAO() {
    }

    public static void main(String[] args) {
        List<Proyecto> proyectos = new ProyectoDAO().listarTodos();
        if (proyectos.isEmpty()) {
            throw new IllegalStateException("La consulta no devolvio proyectos");
        }
        for (Proyecto proyecto : proyectos) {
            System.out.printf("%d - %s - %s%n",
                    proyecto.getId(), proyecto.getNombre(), proyecto.getEstado());
        }
    }
}
