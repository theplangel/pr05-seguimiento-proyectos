package com.dsw.seguimiento.dao;

import com.dsw.seguimiento.model.EventoAvance;
import com.dsw.seguimiento.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventoAvanceDAO {

    public boolean registrar(int proyectoId, String descripcion) {
        String sql = "INSERT INTO evento_avance (proyecto_id, descripcion) VALUES (?, ?)";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, proyectoId);
            ps.setString(2, descripcion);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lista los eventos de avance mas recientes primero: es lo que
     * alimenta el tablero (RF07) para reconocer estado y bloqueos.
     */
    public List<EventoAvance> listarPorProyecto(int proyectoId) {
        List<EventoAvance> eventos = new ArrayList<>();
        String sql = "SELECT * FROM evento_avance WHERE proyecto_id = ? ORDER BY creado_en DESC";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, proyectoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    eventos.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return eventos;
    }

    private EventoAvance mapear(ResultSet rs) throws SQLException {
        EventoAvance e = new EventoAvance();
        e.setId(rs.getInt("id"));
        e.setProyectoId(rs.getInt("proyecto_id"));
        e.setDescripcion(rs.getString("descripcion"));
        e.setCreadoEn(rs.getTimestamp("creado_en"));
        return e;
    }
}