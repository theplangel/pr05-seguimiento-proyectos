package com.dsw.seguimiento.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class ConexionBD {

    private static final Properties CONFIGURACION = cargarConfiguracion();

    // Cargar explícitamente el driver JDBC de PostgreSQL para Tomcat
    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el driver de PostgreSQL.");
            e.printStackTrace();
        }
    }

    private ConexionBD() {
    }

    public static Connection obtenerConexion() throws SQLException {
        String url = System.getProperty("db.url", CONFIGURACION.getProperty("db.url"));
        String usuario = System.getProperty("db.user", CONFIGURACION.getProperty("db.user"));
        String password = System.getProperty("db.password", System.getenv("DB_PASSWORD"));
        if (password == null || password.isEmpty()) {
            password = CONFIGURACION.getProperty("db.password");
        }
        if (password == null || password.isEmpty()) {
            throw new SQLException("Configura la contraseña con -Ddb.password o DB_PASSWORD");
        }
        return DriverManager.getConnection(url, usuario, password);
    }

    private static Properties cargarConfiguracion() {
        Properties propiedades = new Properties();
        try (InputStream entrada = ConexionBD.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (entrada != null) {
                propiedades.load(entrada);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer db.properties", e);
        }
        return propiedades;
    }
}