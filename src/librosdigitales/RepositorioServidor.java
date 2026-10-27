package librosdigitales;

import java.io.IOException;

// Esta clase consulta usuarios.txt, catalogo.txt y los libros cifrados de la carpeta del servidor. No ejecuta un servicio de red independiente.
public class RepositorioServidor {

    // Ruta de la carpeta del servidor, local o compartida por red.
    private String rutaServidor;

    // Este constructor guarda la ruta base donde se consultarán los archivos del servidor.
    public RepositorioServidor(String rutaServidor) {
        this.rutaServidor = rutaServidor;
    }

    // Este método devuelve la ruta de la carpeta del servidor.
    public String getRutaServidor() {
        return rutaServidor;
    }

    // Este método actualiza la ruta de la carpeta del servidor.
    public void setRutaServidor(String rutaServidor) {
        this.rutaServidor = rutaServidor;
    }

    // Este método lee registros usuario|hash|boolean_premium y los almacena en una lista. Omite registros con menos de tres campos.
    public ListaEnlazada<Usuario> cargarUsuarios() throws IOException {
        ListaEnlazada<Usuario> usuarios = new ListaEnlazada<>();
        String contenido = ArchivoUtil.leerTexto(ArchivoUtil.unir(rutaServidor, "usuarios.txt"));
        String[] lineas = contenido.split("\\r?\\n");
        for (int i = 0; i < lineas.length; i++) {
            String[] partes = lineas[i].split("\\|");
            if (partes.length < 3) {
                continue;
            }
            usuarios.agregar(new Usuario(partes[0].trim(), partes[1].trim(),
                    Boolean.parseBoolean(partes[2].trim())));
        }
        return usuarios;
    }

    // Este método lee registros ISBN|Título y los almacena en una lista de libros.
    public ListaEnlazada<Libro> cargarCatalogo() throws IOException {
        ListaEnlazada<Libro> libros = new ListaEnlazada<>();
        String contenido = ArchivoUtil.leerTexto(ArchivoUtil.unir(rutaServidor, "catalogo.txt"));
        String[] lineas = contenido.split("\\r?\\n");
        for (int i = 0; i < lineas.length; i++) {
            String[] partes = lineas[i].split("\\|", 2);
            if (partes.length < 2) {
                continue;
            }
            libros.agregar(new Libro(partes[0].trim(), partes[1].trim()));
        }
        return libros;
    }

    // Este método obtiene el texto cifrado del archivo libros/ISBN.txt del servidor.
    public String leerLibroCifrado(String isbn) throws IOException {
        String carpeta = ArchivoUtil.unir(rutaServidor, "libros");
        return ArchivoUtil.leerTexto(ArchivoUtil.unir(carpeta, isbn + ".txt"));
    }
}