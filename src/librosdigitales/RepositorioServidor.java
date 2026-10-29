package librosdigitales;

import java.io.IOException;
import java.nio.file.*;

// Esta clase consulta los archivos de la carpeta compartida desde el proceso servidor.
// Los registros inválidos se reportan con su línea en vez de omitirse silenciosamente.
public final class RepositorioServidor {
    private String rutaServidor;
    public RepositorioServidor(String ruta) { rutaServidor = ruta; }
    public String getRutaServidor() { return rutaServidor; }
    public void setRutaServidor(String ruta) { rutaServidor = ruta; }

    // Lee usuarios; el servidor los vuelve a consultar en cada solicitud autenticada.
    public ListaEnlazada<Usuario> cargarUsuarios() throws IOException {
        ListaEnlazada<Usuario> lista = new ListaEnlazada<>();
        int numero = 0;
        for (String linea : ArchivoUtil.leerTexto(ArchivoUtil.unir(rutaServidor,"usuarios.txt")).split("\\r?\\n", -1)) {
            numero++;
            if (linea.trim().isEmpty()) continue;
            String[] p = linea.split("\\|", -1);
            if (p.length != 3 || !p[0].trim().matches("[\\p{L}\\p{N}_.-]{1,64}")
                    || !p[1].trim().matches("[a-fA-F0-9]{64}")
                    || !(p[2].trim().equals("true") || p[2].trim().equals("false")))
                throw new IOException("Registro de usuarios inválido en línea " + numero);
            for (int i=0;i<lista.getTamano();i++)
                if (lista.obtener(i).getNombre().equals(p[0].trim())) throw new IOException("Usuario duplicado en línea " + numero);
            lista.agregar(new Usuario(p[0].trim(),p[1].trim().toLowerCase(),Boolean.parseBoolean(p[2].trim())));
            if (lista.getTamano()>10000) throw new IOException("Demasiados usuarios.");
        }
        return lista;
    }

    public ListaEnlazada<Libro> cargarCatalogo() throws IOException {
        return analizarCatalogo(ArchivoUtil.leerTexto(ArchivoUtil.unir(rutaServidor,"catalogo.txt")));
    }

    // Analiza ISBN|título para el servidor, la respuesta de red y el catálogo local.
    public static ListaEnlazada<Libro> analizarCatalogo(String contenido) throws IOException {
        ListaEnlazada<Libro> lista = new ListaEnlazada<>();
        int numero=0;
        for(String linea:contenido.split("\\r?\\n",-1)) {
            numero++;
            if(linea.trim().isEmpty()) continue;
            String[] p=linea.split("\\|",-1);
            if(p.length!=2 || !isbnValido(p[0].trim()) || p[1].trim().isEmpty() || p[1].length()>300)
                throw new IOException("Registro de catálogo inválido en línea "+numero);
            for(int i=0;i<lista.getTamano();i++)
                if(lista.obtener(i).getIsbn().equals(p[0].trim())) throw new IOException("ISBN duplicado en línea "+numero);
            lista.agregar(new Libro(p[0].trim(),p[1].trim()));
            if(lista.getTamano()>10000) throw new IOException("Demasiados libros.");
        }
        return lista;
    }

    // Conserva un formato de texto simple para transportar o almacenar el catálogo.
    public static String serializarCatalogo(ListaEnlazada<Libro> lista) {
        StringBuilder s=new StringBuilder();
        for(int i=0;i<lista.getTamano();i++) s.append(lista.obtener(i).getIsbn()).append('|').append(lista.obtener(i).getTitulo()).append('\n');
        return s.toString();
    }
    public static boolean isbnValido(String isbn) { return isbn != null && isbn.matches("[0-9Xx-]{10,20}"); }

    // El ISBN no admite separadores de ruta y el archivo real debe permanecer dentro de libros.
    public String leerLibroCifrado(String isbn) throws IOException {
        if(!isbnValido(isbn)) throw new IOException("ISBN inválido.");
        Path carpeta=Path.of(rutaServidor,"libros").toRealPath();
        Path archivo=carpeta.resolve(isbn+".txt").toRealPath();
        if(!archivo.startsWith(carpeta)) throw new IOException("Ruta del libro fuera de la carpeta permitida.");
        return ArchivoUtil.leerTexto(archivo.toString()).trim();
    }
}
