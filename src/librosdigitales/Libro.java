package librosdigitales;

// Esta clase representa un libro del catálogo mediante su ISBN y su título.
public class Libro {

    // Identificador utilizado para buscar el archivo del libro.
    private String isbn;
    // Título que se muestra en el catálogo.
    private String titulo;

    // Este constructor asigna el ISBN y el título del libro.
    public Libro(String isbn, String titulo) {
        this.isbn = isbn;
        this.titulo = titulo;
    }

    // Este método devuelve el ISBN del libro.
    public String getIsbn() {
        return isbn;
    }

    // Este método actualiza el ISBN del libro.
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    // Este método devuelve el título del libro.
    public String getTitulo() {
        return titulo;
    }

    // Este método actualiza el título del libro.
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }
}