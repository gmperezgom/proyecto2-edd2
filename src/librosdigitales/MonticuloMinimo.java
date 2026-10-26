package librosdigitales;

/**
 * Monticulo minimo (cola de prioridad) de nodos de Huffman, implementado a mano
 * sobre un arreglo que crece cuando se llena. Siempre saca primero el nodo
 * con menor frecuencia.
 */
public class MonticuloMinimo {

    private NodoHuffman[] datos;
    private int tamano;

    public MonticuloMinimo() {
        this.datos = new NodoHuffman[16];
        this.tamano = 0;
    }

    public int getTamano() {
        return tamano;
    }

    public void insertar(NodoHuffman nodo) {
        if (tamano == datos.length) {
            NodoHuffman[] nuevo = new NodoHuffman[datos.length * 2];
            for (int i = 0; i < tamano; i++) {
                nuevo[i] = datos[i];
            }
            datos = nuevo;
        }
        datos[tamano] = nodo;
        subir(tamano);
        tamano++;
    }

    /** Saca y devuelve el nodo con menor frecuencia (null si esta vacio). */
    public NodoHuffman extraerMinimo() {
        if (tamano == 0) {
            return null;
        }
        NodoHuffman minimo = datos[0];
        tamano--;
        datos[0] = datos[tamano];
        datos[tamano] = null;
        if (tamano > 0) {
            bajar(0);
        }
        return minimo;
    }

    private void subir(int i) {
        while (i > 0) {
            int padre = (i - 1) / 2;
            if (datos[i].getFrecuencia() < datos[padre].getFrecuencia()) {
                intercambiar(i, padre);
                i = padre;
            } else {
                break;
            }
        }
    }

    private void bajar(int i) {
        while (true) {
            int izq = 2 * i + 1;
            int der = 2 * i + 2;
            int menor = i;
            if (izq < tamano && datos[izq].getFrecuencia() < datos[menor].getFrecuencia()) {
                menor = izq;
            }
            if (der < tamano && datos[der].getFrecuencia() < datos[menor].getFrecuencia()) {
                menor = der;
            }
            if (menor == i) {
                break;
            }
            intercambiar(i, menor);
            i = menor;
        }
    }

    private void intercambiar(int a, int b) {
        NodoHuffman temp = datos[a];
        datos[a] = datos[b];
        datos[b] = temp;
    }
}
