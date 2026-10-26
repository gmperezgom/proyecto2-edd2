package librosdigitales;

/** Nodo del arbol de Huffman. Las hojas guardan un caracter; los internos solo suman frecuencias. */
public class NodoHuffman {

    private char caracter;
    private int frecuencia;
    private NodoHuffman izquierdo;
    private NodoHuffman derecho;

    /** Constructor de hoja. */
    public NodoHuffman(char caracter, int frecuencia) {
        this.caracter = caracter;
        this.frecuencia = frecuencia;
    }

    /** Constructor de nodo interno. */
    public NodoHuffman(NodoHuffman izquierdo, NodoHuffman derecho) {
        this.izquierdo = izquierdo;
        this.derecho = derecho;
        this.frecuencia = izquierdo.frecuencia + derecho.frecuencia;
    }

    public boolean esHoja() {
        return izquierdo == null && derecho == null;
    }

    public char getCaracter() {
        return caracter;
    }

    public void setCaracter(char caracter) {
        this.caracter = caracter;
    }

    public int getFrecuencia() {
        return frecuencia;
    }

    public void setFrecuencia(int frecuencia) {
        this.frecuencia = frecuencia;
    }

    public NodoHuffman getIzquierdo() {
        return izquierdo;
    }

    public void setIzquierdo(NodoHuffman izquierdo) {
        this.izquierdo = izquierdo;
    }

    public NodoHuffman getDerecho() {
        return derecho;
    }

    public void setDerecho(NodoHuffman derecho) {
        this.derecho = derecho;
    }
}
