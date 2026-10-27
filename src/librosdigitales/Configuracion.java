package librosdigitales;

import java.io.IOException;

// Esta clase guarda y carga las rutas del servidor y de la carpeta local del cliente desde un archivo de texto con formato clave=valor.
public class Configuracion {

    // Ruta de la carpeta del servidor, local o compartida por red.
    private String rutaServidor;
    // Ruta donde el cliente guardará sus archivos.
    private String rutaLocal;

    // Este constructor guarda las rutas del servidor y del almacenamiento local del cliente.
    public Configuracion(String rutaServidor, String rutaLocal) {
        this.rutaServidor = rutaServidor;
        this.rutaLocal = rutaLocal;
    }

    // Este método devuelve la ruta de la carpeta del servidor.
    public String getRutaServidor() {
        return rutaServidor;
    }

    // Este método actualiza la ruta de la carpeta del servidor.
    public void setRutaServidor(String rutaServidor) {
        this.rutaServidor = rutaServidor;
    }

    // Este método devuelve la ruta de almacenamiento local.
    public String getRutaLocal() {
        return rutaLocal;
    }

    // Este método actualiza la ruta de almacenamiento local.
    public void setRutaLocal(String rutaLocal) {
        this.rutaLocal = rutaLocal;
    }

    // Este método lee las líneas clave=valor y obtiene rutaServidor y rutaLocal. Las líneas vacías y los comentarios se omiten.
    public static Configuracion cargar(String rutaArchivo) throws IOException {
        String contenido = ArchivoUtil.leerTexto(rutaArchivo);
        String servidor = null;
        String local = null;
        String[] lineas = contenido.split("\\r?\\n");
        for (int i = 0; i < lineas.length; i++) {
            String linea = lineas[i].trim();
            if (linea.isEmpty() || linea.startsWith("#")) {
                continue;
            }
            int igual = linea.indexOf('=');
            if (igual < 0) {
                continue;
            }
            String clave = linea.substring(0, igual).trim();
            String valor = linea.substring(igual + 1).trim();
            if (clave.equals("rutaServidor")) {
                servidor = valor;
            } else if (clave.equals("rutaLocal")) {
                local = valor;
            }
        }
        if (servidor == null || local == null) {
            throw new IOException("El archivo de configuracion debe tener rutaServidor y rutaLocal.");
        }
        return new Configuracion(servidor, local);
    }
}