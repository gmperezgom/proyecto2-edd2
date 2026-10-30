package librosdigitales;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

// Esta clase lee UTF-8 y escribe mediante archivos temporales para evitar copias parciales.
public final class ArchivoUtil {
    public static final int MAX_BYTES = 32 * 1024 * 1024;
    private ArchivoUtil() { }

    // Lee un archivo con un límite de tamaño y rechaza texto UTF-8 dañado.
    public static String leerTexto(String ruta) throws IOException {
        Path archivo = Path.of(ruta);
        if (!Files.isRegularFile(archivo)) throw new IOException("No existe el archivo: " + archivo.toAbsolutePath());
        if (Files.size(archivo) > MAX_BYTES) throw new IOException("Archivo demasiado grande: " + archivo);
        try (java.io.InputStream entrada = Files.newInputStream(archivo)) {
            byte[] datos = entrada.readNBytes(MAX_BYTES + 1);
            if (datos.length > MAX_BYTES) throw new IOException("Archivo demasiado grande.");
            String texto = decodificar(datos);
            return texto.startsWith("\uFEFF") ? texto.substring(1) : texto;
        }
    }

    // Decodifica sin sustituir silenciosamente bytes inválidos.
    public static String decodificar(byte[] datos) throws IOException {
        try {
            String texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(datos)).toString();
            return texto;
        } catch (CharacterCodingException e) { throw new IOException("El contenido no es UTF-8 válido.", e); }
    }

    // Reemplaza el destino después de escribir completamente un temporal en la misma carpeta.
    public static synchronized void escribirTexto(String ruta, String texto) throws IOException {
        byte[] datos = texto.getBytes(StandardCharsets.UTF_8);
        if (datos.length > MAX_BYTES) throw new IOException("El contenido excede el límite de tamaño.");
        Path destino = Path.of(ruta).toAbsolutePath().normalize();
        Files.createDirectories(destino.getParent());
        // El bloqueo coordina escrituras de procesos que comparten la misma carpeta local.
        Path cerrojo = destino.resolveSibling("." + destino.getFileName() + ".lock");
        try (java.nio.channels.FileChannel canal = java.nio.channels.FileChannel.open(cerrojo,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             java.nio.channels.FileLock bloqueo = canal.lock()) {
        if (!bloqueo.isValid()) throw new IOException("No se pudo bloquear el destino.");
        Path temporal = Files.createTempFile(destino.getParent(), ".libros-", ".tmp");
        try {
            Files.write(temporal, datos);
            // Windows puede mantener un destino abierto brevemente mientras otro proceso lo lee.
            for (int intento = 0; ; intento++) {
                try {
                    try { Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                    catch (AtomicMoveNotSupportedException e) { Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING); }
                    break;
                } catch (AccessDeniedException e) {
                    if (intento == 5) throw e;
                    try { Thread.sleep(50L); }
                    catch (InterruptedException interrumpida) {
                        Thread.currentThread().interrupt();
                        throw new java.io.InterruptedIOException("Escritura interrumpida.");
                    }
                }
            }
        } finally { Files.deleteIfExists(temporal); }
        }
    }

    // Une rutas según el separador del sistema operativo.
    public static String unir(String base, String parte) { return Path.of(base, parte).toString(); }
}
