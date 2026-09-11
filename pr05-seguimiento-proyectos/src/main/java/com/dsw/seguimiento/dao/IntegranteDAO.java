package com.dsw.seguimiento.dao;

import com.dsw.seguimiento.model.Integrante;
import com.dsw.seguimiento.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IntegranteDAO {

    public boolean crear(Integrante integrante) {
        String sql = "INSERT INTO integrante (proyecto_id, nombre, rol_equipo) VALUES (?, ?, ?)";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, integrante.getProyectoId());
            ps.setString(2, integrante.getNombre());
            ps.setString(3, integrante.getRolEquipo());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Integrante> listarPorProyecto(int proyectoId) {
        List<Integrante> integrantes = new ArrayList<>();
        String sql = "SELECT * FROM integrante WHERE proyecto_id = ? ORDER BY nombre";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, proyectoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    integrantes.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return integrantes;
    }

    private Integrante mapear(ResultSet rs) throws SQLException {
        Integrante i = new Integrante();
        i.setId(rs.getInt("id"));
        i.setProyectoId(rs.getInt("proyecto_id"));
        i.setNombre(rs.getString("nombre"));
        i.setRolEquipo(rs.getString("rol_equipo"));
        return i;
    }
}