package librosdigitales;

// Esta clase representa un nodo del árbol de Huffman. Una hoja guarda un carácter y un nodo interno conecta dos nodos y suma sus frecuencias.
public class NodoHuffman {

    // Unidad de texto de tipo char almacenada en una hoja.
    private char caracter;
    // Frecuencia del carácter o suma de las frecuencias de los hijos.
    private int frecuencia;
    // Referencia al hijo izquierdo.
    private NodoHuffman izquierdo;
    // Referencia al hijo derecho.
    private NodoHuffman derecho;

    // Este constructor crea una hoja con el carácter y su frecuencia.
    public NodoHuffman(char caracter, int frecuencia) {
        this.caracter = caracter;
        this.frecuencia = frecuencia;
    }

    // Este constructor crea un nodo interno y suma las frecuencias de sus hijos.
    public NodoHuffman(NodoHuffman izquierdo, NodoHuffman derecho) {
        this.izquierdo = izquierdo;
        this.derecho = derecho;
        this.frecuencia = izquierdo.frecuencia + derecho.frecuencia;
    }

    // Este método indica si el nodo no tiene hijos.
    public boolean esHoja() {
        return izquierdo == null && derecho == null;
    }

    // Este método devuelve el carácter del nodo.
    public char getCaracter() {
        return caracter;
    }

    // Este método actualiza el carácter del nodo.
    public void setCaracter(char caracter) {
        this.caracter = caracter;
    }

    // Este método devuelve la frecuencia del nodo.
    public int getFrecuencia() {
        return frecuencia;
    }

    // Este método actualiza la frecuencia del nodo.
    public void setFrecuencia(int frecuencia) {
        this.frecuencia = frecuencia;
    }

    // Este método devuelve el hijo izquierdo.
    public NodoHuffman getIzquierdo() {
        return izquierdo;
    }

    // Este método actualiza el hijo izquierdo.
    public void setIzquierdo(NodoHuffman izquierdo) {
        this.izquierdo = izquierdo;
    }

    // Este método devuelve el hijo derecho.
    public NodoHuffman getDerecho() {
        return derecho;
    }

    // Este método actualiza el hijo derecho.
    public void setDerecho(NodoHuffman derecho) {
        this.derecho = derecho;
    }
}