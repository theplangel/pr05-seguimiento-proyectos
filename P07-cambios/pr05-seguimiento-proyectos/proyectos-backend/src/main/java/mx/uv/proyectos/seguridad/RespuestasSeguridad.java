package mx.uv.proyectos.seguridad;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Cuerpos de error JSON para respuestas generadas dentro de la cadena de filtros (401, 403, 429). */
final class RespuestasSeguridad {
    private RespuestasSeguridad() {
    }

    static void escribir(HttpServletRequest req, HttpServletResponse res, int status, String error, String mensaje)
        throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"error\":\"" + error + "\",\"mensaje\":\"" + mensaje
            + "\",\"ruta\":\"" + escapar(req.getRequestURI()) + "\"}");
    }

    private static String escapar(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replaceAll("[\\p{Cntrl}]", "_");
    }
}
