package com.dsw.seguimiento.dao;

import com.dsw.seguimiento.model.Tarea;
import com.dsw.seguimiento.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TareaDAO {

    public boolean crear(Tarea tarea) {
        String sql = "INSERT INTO tarea (proyecto_id, titulo, responsable_id, estado, fecha_limite) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tarea.getProyectoId());
            ps.setString(2, tarea.getTitulo());
            ps.setInt(3, tarea.getResponsableId());
            ps.setString(4, tarea.getEstado());
            ps.setDate(5, tarea.getFechaLimite());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Regla de negocio central: no se puede completar una tarea si tiene
     * al menos un impedimento ABIERTO asociado.
     */
    public boolean tieneImpedimentoAbierto(int tareaId) {
        String sql = "SELECT COUNT(*) FROM impedimento WHERE tarea_id = ? AND estado = 'ABIERTO'";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tareaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Transicion de estado controlada. Valida la maquina de estados y,
     * si el destino es COMPLETADA, verifica que no haya impedimentos abiertos.
     */
    public boolean cambiarEstado(int tareaId, String nuevoEstado) {
        String estadoActual = obtenerEstado(tareaId);

        if (!transicionValida(estadoActual, nuevoEstado)) {
            return false;
        }

        if (Tarea.COMPLETADA.equals(nuevoEstado) && tieneImpedimentoAbierto(tareaId)) {
            return false; // no se puede completar con impedimentos abiertos
        }

        String sql = "UPDATE tarea SET estado = ? WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado);
            ps.setInt(2, tareaId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean transicionValida(String actual, String nuevo) {
        if (actual == null) return false;
        switch (actual) {
            case Tarea.PENDIENTE:
                return nuevo.equals(Tarea.EN_PROGRESO);
            case Tarea.EN_PROGRESO:
                return nuevo.equals(Tarea.BLOQUEADA) || nuevo.equals(Tarea.COMPLETADA);
            case Tarea.BLOQUEADA:
                return nuevo.equals(Tarea.EN_PROGRESO); // se desbloquea al resolver el impedimento
            default:
                return false; // COMPLETADA es estado final
        }
    }

    private String obtenerEstado(int tareaId) {
        String sql = "SELECT estado FROM tarea WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tareaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("estado");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Tarea> listarPorProyecto(int proyectoId) {
        List<Tarea> tareas = new ArrayList<>();
        String sql = "SELECT * FROM tarea WHERE proyecto_id = ? ORDER BY fecha_limite";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, proyectoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tareas.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tareas;
    }

    private Tarea mapear(ResultSet rs) throws SQLException {
        Tarea t = new Tarea();
        t.setId(rs.getInt("id"));
        t.setProyectoId(rs.getInt("proyecto_id"));
        t.setTitulo(rs.getString("titulo"));
        t.setResponsableId(rs.getInt("responsable_id"));
        t.setEstado(rs.getString("estado"));
        t.setFechaLimite(rs.getDate("fecha_limite"));
        return t;
    }
}