package com.dsw.seguimiento.dao;

import com.dsw.seguimiento.model.Evidencia;
import com.dsw.seguimiento.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvidenciaDAO {

    public boolean crear(Evidencia evidencia) {
        String sql = "INSERT INTO evidencia (tarea_id, tipo, url_o_ruta, fecha) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, evidencia.getTareaId());
            ps.setString(2, evidencia.getTipo());
            ps.setString(3, evidencia.getUrlORuta());
            ps.setDate(4, evidencia.getFecha());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Evidencia> listarPorTarea(int tareaId) {
        List<Evidencia> evidencias = new ArrayList<>();
        String sql = "SELECT * FROM evidencia WHERE tarea_id = ? ORDER BY fecha DESC";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tareaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    evidencias.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return evidencias;
    }

    private Evidencia mapear(ResultSet rs) throws SQLException {
        Evidencia e = new Evidencia();
        e.setId(rs.getInt("id"));
        e.setTareaId(rs.getInt("tarea_id"));
        e.setTipo(rs.getString("tipo"));
        e.setUrlORuta(rs.getString("url_o_ruta"));
        e.setFecha(rs.getDate("fecha"));
        return e;
    }
}