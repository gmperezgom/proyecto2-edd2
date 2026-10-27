package librosdigitales;

// Esta clase implementa un montículo mínimo con un arreglo que aumenta su capacidad cuando se llena. Permite extraer el nodo de Huffman con menor frecuencia.
public class MonticuloMinimo {

    // Arreglo que almacena los nodos del montículo.
    private NodoHuffman[] datos;
    // Cantidad de elementos que contiene la estructura.
    private int tamano;

    // Este constructor reserva espacio inicial para 16 nodos y deja el montículo vacío.
    public MonticuloMinimo() {
        this.datos = new NodoHuffman[16];
        this.tamano = 0;
    }

    // Este método devuelve la cantidad de elementos.
    public int getTamano() {
        return tamano;
    }

    // Este método agrega un nodo y lo sube hasta conservar el orden del montículo.
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

    // Este método retira el nodo de menor frecuencia y reorganiza el montículo. Devuelve null si está vacío.
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

    // Este método compara el nodo con su padre y lo intercambia mientras su frecuencia sea menor.
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

    // Este método compara el nodo con sus hijos y lo intercambia con el de menor frecuencia cuando corresponde.
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

    // Este método intercambia dos posiciones del arreglo del montículo.
    private void intercambiar(int a, int b) {
        NodoHuffman temp = datos[a];
        datos[a] = datos[b];
        datos[b] = temp;
    }
}