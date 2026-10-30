package librosdigitales;

import java.io.IOException;
import java.nio.file.Path;

// Esta clase carga y valida la configuración del cliente sin depender del servidor al arrancar.
public final class Configuracion {
    private String rutaServidor;
    private String rutaLocal;
    private String host;
    private int puerto;
    private int tiempoEspera;
    private String huellaServidor;

    public Configuracion(String rutaServidor, String rutaLocal) {
        this.rutaServidor = rutaServidor; this.rutaLocal = rutaLocal;
        host = "localhost"; puerto = 5050; tiempoEspera = 5000; huellaServidor = "";
    }
    public String getRutaServidor() { return rutaServidor; }
    public void setRutaServidor(String valor) { rutaServidor = valor; }
    public String getRutaLocal() { return rutaLocal; }
    public void setRutaLocal(String valor) { rutaLocal = valor; }
    public String getHost() { return host; }
    public int getPuerto() { return puerto; }
    public int getTiempoEspera() { return tiempoEspera; }
    public String getHuellaServidor() { return huellaServidor; }

    // Lee las claves y rechaza valores vacíos, duplicados, puertos inválidos y huellas incompletas.
    public static Configuracion cargar(String ruta) throws IOException {
        String contenido = ArchivoUtil.leerTexto(ruta);
        Configuracion c = new Configuracion(valor(contenido, "rutaServidor", ""), valor(contenido, "rutaLocal", null));
        c.host = valor(contenido, "hostServidor", null);
        c.puerto = entero(valor(contenido, "puertoServidor", "5050"), 1, 65535, "puertoServidor");
        c.tiempoEspera = entero(valor(contenido, "tiempoEsperaMs", "5000"), 500, 30000, "tiempoEsperaMs");
        c.huellaServidor = valor(contenido, "huellaServidor", null).toLowerCase();
        if (!c.huellaServidor.matches("[a-f0-9]{64}")) throw new IOException("huellaServidor debe contener 64 dígitos hexadecimales.");
        try { Path.of(c.rutaLocal); } catch (RuntimeException e) { throw new IOException("rutaLocal no es válida.", e); }
        return c;
    }

    // Analiza clave=valor sin usar colecciones para los registros de configuración.
    static String valor(String contenido, String clave, String defecto) throws IOException {
        String resultado = null;
        int numero = 0;
        for (String original : contenido.split("\\r?\\n", -1)) {
            numero++;
            String linea = original.trim();
            if (linea.isEmpty() || linea.startsWith("#")) continue;
            int igual = linea.indexOf('=');
            if (igual <= 0) throw new IOException("Configuración inválida en línea " + numero);
            if (linea.substring(0, igual).trim().equals(clave)) {
                if (resultado != null) throw new IOException("Clave de configuración duplicada: " + clave);
                resultado = linea.substring(igual + 1).trim();
                if (resultado.isEmpty()) throw new IOException("Valor vacío: " + clave);
            }
        }
        if (resultado != null) return resultado;
        if (defecto != null) return defecto;
        throw new IOException("Falta configurar: " + clave);
    }
    static int entero(String texto, int minimo, int maximo, String nombre) throws IOException {
        try {
            int n = Integer.parseInt(texto);
            if (n < minimo || n > maximo) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) { throw new IOException("Valor inválido para " + nombre + ".", e); }
    }
}
