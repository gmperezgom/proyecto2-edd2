package librosdigitales;

import java.io.IOException;

/**
 * Acceso a la carpeta compartida del servidor: archivo de usuarios,
 * archivo de catalogo y carpeta de libros cifrados.
 *
 * Estructura esperada dentro de la carpeta compartida:
 *   usuarios.txt   -> usuario|hash|boolean_premium
 *   catalogo.txt   -> ISBN|Titulo
 *   libros/ISBN.txt -> texto cifrado del libro
 */
public class RepositorioServidor {

    private String rutaServidor;

    public RepositorioServidor(String rutaServidor) {
        this.rutaServidor = rutaServidor;
    }

    public String getRutaServidor() {
        return rutaServidor;
    }

    public void setRutaServidor(String rutaServidor) {
        this.rutaServidor = rutaServidor;
    }

    /** Lee el archivo de usuarios y devuelve la lista de usuarios registrados. */
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

    /** Lee el archivo de catalogo y devuelve la lista de libros disponibles. */
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

    /** Lee el contenido cifrado de un libro desde la carpeta de libros del servidor. */
    public String leerLibroCifrado(String isbn) throws IOException {
        String carpeta = ArchivoUtil.unir(rutaServidor, "libros");
        return ArchivoUtil.leerTexto(ArchivoUtil.unir(carpeta, isbn + ".txt"));
    }
}
