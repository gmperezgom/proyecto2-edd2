package librosdigitales;

import java.util.Base64;

// Esta clase comprime y descomprime texto mediante Huffman. Guarda una marca, la cantidad de caracteres, la tabla de frecuencias y los bytes comprimidos en Base64.
public class Huffman {

    // Texto que identifica el formato del archivo Huffman.
    private static final String MARCA = "HUFFMAN";
    // Cantidad de valores posibles del tipo char de Java.
    private static final int TOTAL_CARACTERES = 65536;

    // El constructor privado evita crear objetos de esta clase de métodos estáticos.
    private Huffman() {
    }

    // Este método cuenta frecuencias, construye el árbol, genera códigos y empaqueta los bits para guardar el resultado como texto.
    public static String comprimir(String texto) {
        int[] frecuencias = new int[TOTAL_CARACTERES];
        for (int i = 0; i < texto.length(); i++) {
            frecuencias[texto.charAt(i)]++;
        }

        NodoHuffman raiz = construirArbol(frecuencias);
        String[] codigos = new String[TOTAL_CARACTERES];
        if (raiz != null) {
            generarCodigos(raiz, "", codigos);
        }

        // Cantidad total de bits que ocupa el texto comprimido
        long totalBits = 0;
        for (int c = 0; c < TOTAL_CARACTERES; c++) {
            if (frecuencias[c] > 0) {
                totalBits += (long) frecuencias[c] * codigos[c].length();
            }
        }

        // Empaquetar los bits en bytes
        byte[] bytes = new byte[(int) ((totalBits + 7) / 8)];
        long posicion = 0;
        for (int i = 0; i < texto.length(); i++) {
            String codigo = codigos[texto.charAt(i)];
            for (int j = 0; j < codigo.length(); j++) {
                if (codigo.charAt(j) == '1') {
                    bytes[(int) (posicion / 8)] |= (byte) (1 << (7 - (int) (posicion % 8)));
                }
                posicion++;
            }
        }

        // Tabla de frecuencias (en orden ascendente de caracter)
        StringBuilder tabla = new StringBuilder();
        for (int c = 0; c < TOTAL_CARACTERES; c++) {
            if (frecuencias[c] > 0) {
                if (tabla.length() > 0) {
                    tabla.append(';');
                }
                tabla.append(c).append(':').append(frecuencias[c]);
            }
        }

        return MARCA + "\n" + texto.length() + "\n" + tabla + "\n"
                + Base64.getEncoder().encodeToString(bytes);
    }

    // Este método lee el formato Huffman, reconstruye el árbol y recorre los bits para recuperar los caracteres.
    public static String descomprimir(String comprimido) {
        String[] lineas = comprimido.split("\\r?\\n", -1);
        if (lineas.length < 4 || !lineas[0].trim().equals(MARCA)) {
            throw new IllegalArgumentException("El archivo no tiene formato Huffman valido");
        }
        int cantidad = Integer.parseInt(lineas[1].trim());
        if (cantidad == 0) {
            return "";
        }

        // Reconstruir tabla de frecuencias
        int[] frecuencias = new int[TOTAL_CARACTERES];
        String[] entradas = lineas[2].trim().split(";");
        for (int i = 0; i < entradas.length; i++) {
            int dos = entradas[i].indexOf(':');
            int codigo = Integer.parseInt(entradas[i].substring(0, dos));
            int frecuencia = Integer.parseInt(entradas[i].substring(dos + 1));
            frecuencias[codigo] = frecuencia;
        }
        NodoHuffman raiz = construirArbol(frecuencias);

        StringBuilder resultado = new StringBuilder();
        if (raiz.esHoja()) { // texto con un solo caracter distinto
            for (int i = 0; i < cantidad; i++) {
                resultado.append(raiz.getCaracter());
            }
            return resultado.toString();
        }

        byte[] bytes = Base64.getDecoder().decode(lineas[3].trim());
        NodoHuffman actual = raiz;
        int decodificados = 0;
        for (int b = 0; b < bytes.length * 8 && decodificados < cantidad; b++) {
            int bit = (bytes[b / 8] >> (7 - (b % 8))) & 1;
            actual = (bit == 0) ? actual.getIzquierdo() : actual.getDerecho();
            if (actual.esHoja()) {
                resultado.append(actual.getCaracter());
                decodificados++;
                actual = raiz;
            }
        }
        return resultado.toString();
    }

    // Este método combina los dos nodos de menor frecuencia hasta obtener la raíz del árbol de Huffman.
    private static NodoHuffman construirArbol(int[] frecuencias) {
        MonticuloMinimo monticulo = new MonticuloMinimo();
        for (int c = 0; c < TOTAL_CARACTERES; c++) {
            if (frecuencias[c] > 0) {
                monticulo.insertar(new NodoHuffman((char) c, frecuencias[c]));
            }
        }
        if (monticulo.getTamano() == 0) {
            return null;
        }
        while (monticulo.getTamano() > 1) {
            NodoHuffman a = monticulo.extraerMinimo();
            NodoHuffman b = monticulo.extraerMinimo();
            monticulo.insertar(new NodoHuffman(a, b));
        }
        return monticulo.extraerMinimo();
    }

    // Este método recorre el árbol de forma recursiva: agrega 0 hacia la izquierda y 1 hacia la derecha.
    private static void generarCodigos(NodoHuffman nodo, String prefijo, String[] codigos) {
        if (nodo.esHoja()) {
            codigos[nodo.getCaracter()] = prefijo.isEmpty() ? "0" : prefijo;
            return;
        }
        generarCodigos(nodo.getIzquierdo(), prefijo + "0", codigos);
        generarCodigos(nodo.getDerecho(), prefijo + "1", codigos);
    }
}