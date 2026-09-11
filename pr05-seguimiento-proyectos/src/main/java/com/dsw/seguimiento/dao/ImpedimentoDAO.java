package com.dsw.seguimiento.dao;

import com.dsw.seguimiento.model.Impedimento;
import com.dsw.seguimiento.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ImpedimentoDAO {

    public boolean crear(Impedimento impedimento) {
        String sql = "INSERT INTO impedimento (tarea_id, descripcion, estado) VALUES (?, ?, ?)";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, impedimento.getTareaId());
            ps.setString(2, impedimento.getDescripcion());
            ps.setString(3, impedimento.getEstado());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Marca un impedimento como RESUELTO. No cambia el estado de la tarea
     * automaticamente: eso lo decide el usuario via TareaDAO.cambiarEstado().
     */
    public boolean resolver(int impedimentoId) {
        String sql = "UPDATE impedimento SET estado = ? WHERE id = ?";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, Impedimento.RESUELTO);
            ps.setInt(2, impedimentoId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Impedimento> listarPorTarea(int tareaId) {
        List<Impedimento> impedimentos = new ArrayList<>();
        String sql = "SELECT * FROM impedimento WHERE tarea_id = ? ORDER BY id DESC";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, tareaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    impedimentos.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return impedimentos;
    }

    private Impedimento mapear(ResultSet rs) throws SQLException {
        Impedimento i = new Impedimento();
        i.setId(rs.getInt("id"));
        i.setTareaId(rs.getInt("tarea_id"));
        i.setDescripcion(rs.getString("descripcion"));
        i.setEstado(rs.getString("estado"));
        return i;
    }
}