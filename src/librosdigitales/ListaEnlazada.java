package librosdigitales;

// Esta clase implementa una lista enlazada simple y genérica para almacenar elementos sin utilizar las listas de la biblioteca de Java.
public class ListaEnlazada<T> {

    private class Nodo {
        // Elemento almacenado en el nodo de la lista.
        private T dato;
        // Referencia al siguiente nodo de la lista.
        private Nodo siguiente;

        // Este constructor guarda el dato del nodo y deja su enlace siguiente vacío.
        private Nodo(T dato) {
            this.dato = dato;
            this.siguiente = null;
        }
    }

    // Referencia al primer nodo de la lista.
    private Nodo cabeza;
    // Referencia al último nodo para agregar elementos al final.
    private Nodo cola;
    // Cantidad de elementos que contiene la estructura.
    private int tamano;

    // Este constructor crea una lista vacía, sin cabeza ni cola y con tamaño cero.
    public ListaEnlazada() {
        this.cabeza = null;
        this.cola = null;
        this.tamano = 0;
    }

    // Este método agrega un elemento al final de la lista y actualiza su tamaño.
    public void agregar(T dato) {
        Nodo nuevo = new Nodo(dato);
        if (cabeza == null) {
            cabeza = nuevo;
            cola = nuevo;
        } else {
            cola.siguiente = nuevo;
            cola = nuevo;
        }
        tamano++;
    }

    // Este método devuelve el elemento de una posición, comenzando en cero, o null si el índice no existe.
    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano) {
            return null;
        }
        Nodo actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.siguiente;
        }
        return actual.dato;
    }

    // Este método devuelve la cantidad de elementos.
    public int getTamano() {
        return tamano;
    }

    // Este método indica si la lista no contiene elementos.
    public boolean estaVacia() {
        return tamano == 0;
    }
}