package librosdigitales;

/**
 * Lista enlazada simple generica, implementada a mano.
 * Se usa en todo el proyecto en lugar de ArrayList, LinkedList, etc.
 *
 * @param <T> tipo de dato que guarda la lista
 */
public class ListaEnlazada<T> {

    /** Nodo interno de la lista. */
    private class Nodo {
        private T dato;
        private Nodo siguiente;

        private Nodo(T dato) {
            this.dato = dato;
            this.siguiente = null;
        }
    }

    private Nodo cabeza;
    private Nodo cola;
    private int tamano;

    public ListaEnlazada() {
        this.cabeza = null;
        this.cola = null;
        this.tamano = 0;
    }

    /** Agrega un elemento al final de la lista. */
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

    /** Devuelve el elemento en la posicion indicada (empieza en 0), o null si no existe. */
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

    public int getTamano() {
        return tamano;
    }

    public boolean estaVacia() {
        return tamano == 0;
    }
}
