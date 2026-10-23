package librosdigitales;

import java.io.IOException;

//esta clase sirve para la configuracion de la conexion del cliente. Se lee un archivo de texto con lineas.
public class Configuracion {

    private String rutaServidor;
    private String rutaLocal;

    public Configuracion(String rutaServidor, String rutaLocal) {
        this.rutaServidor = rutaServidor;
        this.rutaLocal = rutaLocal;
    }

    public String getRutaServidor() {
        return rutaServidor;
    }

    public void setRutaServidor(String rutaServidor) {
        this.rutaServidor = rutaServidor;
    }

    public String getRutaLocal() {
        return rutaLocal;
    }

    public void setRutaLocal(String rutaLocal) {
        this.rutaLocal = rutaLocal;
    }

    //Carga la configuracion desde el archivo indicado.
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
