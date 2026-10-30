package librosdigitales;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

// Esta clase mantiene los archivos separados por usuario y no guarda texto plano de los libros leídos.
public final class RepositorioLocal {
    private final Path base;
    public RepositorioLocal(String ruta) { base=Path.of(ruta).toAbsolutePath().normalize(); }

    // El hash del nombre evita caracteres de ruta y colisiones entre usuarios en el mismo cliente.
    public Path carpetaUsuario(String nombre) {
        return base.resolve("usuarios").resolve(HashUtil.sha256(nombre));
    }
    public Path rutaCuenta(String nombre) { return base.resolve("cuentas").resolve(HashUtil.sha256(nombre)+".cuenta"); }
    public Path rutaDescarga(String nombre,String isbn) {
        if(!RepositorioServidor.isbnValido(isbn)) throw new IllegalArgumentException("ISBN inválido.");
        return carpetaUsuario(nombre).resolve("descargas").resolve(isbn+".huf.txt");
    }

    // Guarda el archivo original del servidor y la entrega cifrada negociada, sin guardar el secreto.
    public void guardarCifrados(String nombre,String isbn,String original,String entrega) throws IOException {
        if(!RepositorioServidor.isbnValido(isbn)) throw new IOException("ISBN inválido.");
        ArchivoUtil.escribirTexto(carpetaUsuario(nombre).resolve("copias_cifradas").resolve(isbn+".txt").toString(),original);
        ArchivoUtil.escribirTexto(carpetaUsuario(nombre).resolve("entregas_cifradas").resolve(isbn+".txt").toString(),entrega);
    }

    // Solo el flujo Premium usa este método; valida el resultado antes de persistir la descarga.
    public Path guardarDescarga(String nombre,String isbn,String texto) throws IOException {
        String comprimido=Huffman.comprimir(texto);
        if(!texto.equals(Huffman.descomprimir(comprimido))) throw new IOException("La comprobación de Huffman falló.");
        Path destino=rutaDescarga(nombre,isbn);
        ArchivoUtil.escribirTexto(destino.toString(),comprimido);
        return destino;
    }
    public String leerDescarga(String nombre,String isbn) throws IOException {
        return Huffman.descomprimir(ArchivoUtil.leerTexto(rutaDescarga(nombre,isbn).toString()));
    }

    // Enumera archivos regulares del usuario; el contenido se valida al seleccionarlos.
    public ListaEnlazada<String> listarDescargas(String nombre) throws IOException {
        ListaEnlazada<String> lista=new ListaEnlazada<>();
        Path carpeta=carpetaUsuario(nombre).resolve("descargas");
        if(!Files.exists(carpeta)) return lista;
        try(Stream<Path> archivos=Files.list(carpeta)) {
            java.util.Iterator<Path> iterador=archivos.sorted().iterator();
            while(iterador.hasNext()) {
                Path archivo=iterador.next(); String n=archivo.getFileName().toString();
                if(Files.isRegularFile(archivo,LinkOption.NOFOLLOW_LINKS) && n.endsWith(".huf.txt")) {
                    String isbn=n.substring(0,n.length()-8);
                    if(RepositorioServidor.isbnValido(isbn)) lista.agregar(isbn);
                }
            }
        }
        return lista;
    }
}
