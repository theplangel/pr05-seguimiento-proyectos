package com.dsw.seguimiento.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Guarda y localiza los archivos que se suben como evidencia (capturas y
 * documentos). Lo usan tanto EvidenciaServlet (Web 1.0) como TareaBean
 * (Web 2.0) para aplicar las mismas reglas en ambas versiones.
 *
 * Los archivos se guardan FUERA del WAR para que no se pierdan al
 * redesplegar. La carpeta se toma, en este orden, de:
 *   1. propiedad de JVM  -Devidencias.dir=...
 *   2. variable de entorno EVIDENCIAS_DIR
 *   3. ~/pr05-seguimiento/evidencias
 *
 * En la tabla evidencia solo se guarda la ruta relativa
 * "archivos/evidencias/<uuid>.<ext>", que sirve ArchivoEvidenciaServlet.
 */
public final class AlmacenEvidencias {

    public static final long TAMANO_MAXIMO = 5L * 1024 * 1024; // 5 MB
    public static final String PREFIJO_URL = "archivos/evidencias/";

    public static final String TIPO_CAPTURA = "captura";
    public static final String TIPO_DOCUMENTO = "documento";

    private static final Map<String, Set<String>> EXTENSIONES_POR_TIPO = new HashMap<>();
    private static final Map<String, String> CONTENT_TYPES = new HashMap<>();

    // Nombre generado por guardar(): uuid + extension permitida. Evita rutas como ../../
    private static final Pattern NOMBRE_VALIDO =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.[a-z0-9]{2,5}");

    static {
        EXTENSIONES_POR_TIPO.put(TIPO_CAPTURA, conjunto("png", "jpg", "jpeg", "gif", "webp"));
        EXTENSIONES_POR_TIPO.put(TIPO_DOCUMENTO, conjunto("pdf", "doc", "docx", "odt", "txt", "md"));

        CONTENT_TYPES.put("png", "image/png");
        CONTENT_TYPES.put("jpg", "image/jpeg");
        CONTENT_TYPES.put("jpeg", "image/jpeg");
        CONTENT_TYPES.put("gif", "image/gif");
        CONTENT_TYPES.put("webp", "image/webp");
        CONTENT_TYPES.put("pdf", "application/pdf");
        CONTENT_TYPES.put("doc", "application/msword");
        CONTENT_TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        CONTENT_TYPES.put("odt", "application/vnd.oasis.opendocument.text");
        CONTENT_TYPES.put("txt", "text/plain; charset=UTF-8");
        CONTENT_TYPES.put("md", "text/plain; charset=UTF-8");
    }

    private AlmacenEvidencias() {}

    /** captura y documento se suben como archivo; enlace y codigo siguen siendo URL/ruta. */
    public static boolean requiereArchivo(String tipo) {
        return tipo != null && EXTENSIONES_POR_TIPO.containsKey(tipo.trim().toLowerCase(Locale.ROOT));
    }

    /** Texto para mensajes y para el atributo accept: ".png, .jpg, ..." */
    public static String extensionesPermitidas(String tipo) {
        Set<String> extensiones = extensionesDe(tipo);
        StringBuilder sb = new StringBuilder();
        for (String ext : extensiones) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append('.').append(ext);
        }
        return sb.toString();
    }

    public static boolean extensionPermitida(String tipo, String nombreOriginal) {
        return extensionesDe(tipo).contains(extension(nombreOriginal));
    }

    /**
     * Copia el archivo a la carpeta de evidencias con un nombre aleatorio y
     * devuelve la ruta relativa que se guarda en la BD. Se asume que el
     * llamador ya valido tipo y tamano.
     */
    public static String guardar(InputStream datos, String nombreOriginal) throws IOException {
        Path carpeta = directorio();
        Files.createDirectories(carpeta);
        String nombre = UUID.randomUUID() + "." + extension(nombreOriginal);
        Files.copy(datos, carpeta.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        return PREFIJO_URL + nombre;
    }

    /** Devuelve el archivo guardado o null si el nombre no es valido o no existe. */
    public static Path resolver(String nombreArchivo) {
        if (nombreArchivo == null || !NOMBRE_VALIDO.matcher(nombreArchivo).matches()
                || !CONTENT_TYPES.containsKey(extension(nombreArchivo))) {
            return null;
        }
        Path carpeta = directorio();
        Path archivo = carpeta.resolve(nombreArchivo).normalize();
        if (!archivo.startsWith(carpeta) || !Files.isRegularFile(archivo)) {
            return null;
        }
        return archivo;
    }

    public static String contentType(String nombreArchivo) {
        return CONTENT_TYPES.getOrDefault(extension(nombreArchivo), "application/octet-stream");
    }

    public static boolean esImagen(String nombreArchivo) {
        return EXTENSIONES_POR_TIPO.get(TIPO_CAPTURA).contains(extension(nombreArchivo));
    }

    public static String extension(String nombre) {
        if (nombre == null) {
            return "";
        }
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto + 1).trim().toLowerCase(Locale.ROOT);
    }

    private static Set<String> extensionesDe(String tipo) {
        if (tipo == null) {
            return Collections.emptySet();
        }
        return EXTENSIONES_POR_TIPO.getOrDefault(tipo.trim().toLowerCase(Locale.ROOT), Collections.emptySet());
    }

    private static Path directorio() {
        String configurado = System.getProperty("evidencias.dir");
        if (configurado == null || configurado.trim().isEmpty()) {
            configurado = System.getenv("EVIDENCIAS_DIR");
        }
        if (configurado == null || configurado.trim().isEmpty()) {
            return Paths.get(System.getProperty("user.home"), "pr05-seguimiento", "evidencias")
                    .toAbsolutePath().normalize();
        }
        return Paths.get(configurado.trim()).toAbsolutePath().normalize();
    }

    private static Set<String> conjunto(String... valores) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(valores)));
    }
}
