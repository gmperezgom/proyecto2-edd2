package librosdigitales;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;

//Esta clase contiene las utilidades para leer y escribir archivos de texto en UTF-8.

public class ArchivoUtil{
    private ArchivoUtil(){
    }

    //metodo para leer todo un archivo de texto
    public static String leerTexto(String ruta) throws IOException{
        try (RandomAccessFile raf = new RandomAccessFile(ruta, "r");
             FileChannel canal = raf.getChannel()) {
            FileLock bloqueo = null;
            try {
                //este es el bloqueo para solo lectura
                bloqueo = canal.lock(0L, Long.MAX_VALUE, true);
            } catch (Exception e) {
                // Algunas carpetas compartidas no soportan bloqueos; se lee igual.
            }
            byte[] datos = new byte[(int) raf.length()];
            raf.readFully(datos);
            if (bloqueo != null) {
                bloqueo.release();
            }
            return new String(datos, StandardCharsets.UTF_8);
        }
    }

    //este metodo escribe un texto en un archivo (se hace un reemplazo si existe) y crea las carpetas que son necesarias
    public static void escribirTexto(String ruta, String texto) throws IOException{
        File archivo = new File(ruta);
        File padre = archivo.getParentFile();
        if (padre != null && !padre.exists()) {
            padre.mkdirs();
        }
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            salida.write(texto.getBytes(StandardCharsets.UTF_8));
        }

    }

    //metodo para unir dos partes de una ruta con el separador del sistema
    public static String unir(String base, String parte){
        return new File(base,parte).getPath();
    }

}