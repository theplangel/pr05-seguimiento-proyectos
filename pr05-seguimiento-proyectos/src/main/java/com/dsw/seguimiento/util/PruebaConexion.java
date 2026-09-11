package com.dsw.seguimiento.util;

import com.dsw.seguimiento.dao.ProyectoDAO;
import com.dsw.seguimiento.model.Proyecto;

import java.util.List;

/**
 * Clase de prueba desechable — NO forma parte del producto final.
 * Sirve solo para confirmar que ConexionBD y ProyectoDAO funcionan
 * contra la base real antes de construir los Servlets.
 *
 * Borra este archivo (o muevelo fuera de src/main) antes de la entrega
 * final para no ensuciar el WAR con codigo de prueba.
 */
public class PruebaConexion {

    public static void main(String[] args) {
        System.out.println("Probando conexion a PostgreSQL...");

        ProyectoDAO dao = new ProyectoDAO();
        List<Proyecto> proyectos = dao.listarTodos();

        if (proyectos.isEmpty()) {
            System.out.println("Conexion establecida, pero no se encontraron proyectos. " +
                "Verifica que el script DDL con los datos de prueba se haya ejecutado.");
            return;
        }

        System.out.println("Conexion exitosa. Proyectos encontrados: " + proyectos.size());
        for (Proyecto p : proyectos) {
            System.out.println("  - [" + p.getId() + "] " + p.getNombre() +
                " (" + p.getEstado() + ") desde " + p.getFechaInicio());
        }
    }
}