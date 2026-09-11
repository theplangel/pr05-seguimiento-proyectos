package com.dsw.seguimiento.dao;

import com.dsw.seguimiento.model.Proyecto;
import com.dsw.seguimiento.util.ConexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProyectoDAO {

    public boolean crear(Proyecto proyecto) {
        String sql = "INSERT INTO proyecto (nombre, descripcion, fecha_inicio, estado) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, proyecto.getNombre());
            ps.setString(2, proyecto.getDescripcion());
            ps.setDate(3, proyecto.getFechaInicio());
            ps.setString(4, proyecto.getEstado());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Proyecto buscarPorId(int id) {
        String sql = "SELECT * FROM proyecto WHERE id = ?";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Proyecto> listarTodos() {
        List<Proyecto> proyectos = new ArrayList<>();
        String sql = "SELECT * FROM proyecto ORDER BY fecha_inicio DESC";

        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                proyectos.add(mapear(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return proyectos;
    }

    public boolean actualizarEstado(int proyectoId, String nuevoEstado) {
        if (!esEstadoValido(nuevoEstado)) {
            return false;
        }

        String sql = "UPDATE proyecto SET estado = ? WHERE id = ?";
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado);
            ps.setInt(2, proyectoId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean esEstadoValido(String estado) {
        return Proyecto.ACTIVO.equals(estado)
            || Proyecto.PAUSADO.equals(estado)
            || Proyecto.CERRADO.equals(estado);
    }

    private Proyecto mapear(ResultSet rs) throws SQLException {
        Proyecto p = new Proyecto();
        p.setId(rs.getInt("id"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setFechaInicio(rs.getDate("fecha_inicio"));
        p.setEstado(rs.getString("estado"));
        return p;
    }
}