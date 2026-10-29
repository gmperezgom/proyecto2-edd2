package librosdigitales;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

// Esta clase implementa Huffman con árbol y montículo propios.
// Formato HUFFMAN2: marca, caracteres, bits útiles, frecuencias, Base64 y SHA-256 del texto.
// El hash detecta daños accidentales; no es una firma del autor del archivo.
public final class Huffman {
    public static final int MAX_CARACTERES=2_000_000;
    private static final int TOTAL=65536;
    private Huffman() { }

    // Comprime el texto y guarda los metadatos necesarios para una validación completa.
    public static String comprimir(String texto) {
        if(texto==null || texto.length()>MAX_CARACTERES) throw error("Texto demasiado grande o nulo.");
        int[] frecuencias=contar(texto);
        NodoHuffman raiz=construirArbol(frecuencias);
        String[] codigos=new String[TOTAL];
        if(raiz!=null) generarCodigos(raiz,"",codigos);
        long bits=calcularBits(frecuencias,codigos);
        byte[] datos=new byte[(int)((bits+7)/8)];
        long posicion=0;
        for(int i=0;i<texto.length();i++) {
            String codigo=codigos[texto.charAt(i)];
            for(int j=0;j<codigo.length();j++,posicion++)
                if(codigo.charAt(j)=='1') datos[(int)(posicion/8)]|=(byte)(1<<(7-(int)(posicion%8)));
        }
        StringBuilder tabla=new StringBuilder();
        for(int c=0;c<TOTAL;c++) if(frecuencias[c]>0) {
            if(tabla.length()>0) tabla.append(';');
            tabla.append(c).append(':').append(frecuencias[c]);
        }
        return "HUFFMAN2\n"+texto.length()+"\n"+bits+"\n"+tabla+"\n"
                +Base64.getEncoder().encodeToString(datos)+"\n"+HashUtil.sha256(texto);
    }

    // Rechaza metadatos incoherentes, datos truncados, relleno alterado y resúmenes diferentes.
    public static String descomprimir(String archivo) {
        try { return descomprimirValidado(archivo); }
        catch(IllegalArgumentException e) { throw error("Archivo Huffman dañado o no compatible: "+e.getMessage()); }
        catch(IndexOutOfBoundsException | NullPointerException e) { throw error("Estructura Huffman inválida."); }
    }

    private static String descomprimirValidado(String archivo) {
        if(archivo==null || archivo.length()>ArchivoUtil.MAX_BYTES) throw error("Tamaño de archivo inválido.");
        // Se admite un único salto de línea final, pero no líneas adicionales con datos.
        String[] lineas=archivo.split("\\r?\\n",-1);
        if(lineas.length==7 && lineas[6].isEmpty()) lineas=java.util.Arrays.copyOf(lineas,6);
        if(lineas.length!=6 || !lineas[0].equals("HUFFMAN2"))
            throw error("Se requiere el formato HUFFMAN2; vuelve a descargar los archivos antiguos.");
        int cantidad=numero(lineas[1],0,MAX_CARACTERES);
        long bits=numero(lineas[2],0,MAX_CARACTERES*32);
        if(!lineas[5].matches("[a-f0-9]{64}")) throw error("Resumen SHA-256 inválido.");
        int[] frecuencias=new int[TOTAL];
        long suma=0; int anterior=-1;
        if(!lineas[3].isEmpty()) {
            String[] entradas=lineas[3].split(";",-1);
            if(entradas.length>TOTAL) throw error("Demasiadas frecuencias.");
            for(String entrada:entradas) {
                String[] p=entrada.split(":",-1);
                if(p.length!=2) throw error("Entrada de frecuencias inválida.");
                int c=numero(p[0],0,TOTAL-1);
                int f=numero(p[1],1,MAX_CARACTERES);
                if(c<=anterior) throw error("Frecuencias duplicadas o desordenadas.");
                anterior=c; frecuencias[c]=f; suma+=f;
                if(suma>MAX_CARACTERES) throw error("Suma de frecuencias excesiva.");
            }
        }
        if(suma!=cantidad) throw error("Las frecuencias no coinciden con la cantidad de caracteres.");
        NodoHuffman raiz=construirArbol(frecuencias);
        String[] codigos=new String[TOTAL];
        if(raiz!=null) generarCodigos(raiz,"",codigos);
        if(bits!=calcularBits(frecuencias,codigos)) throw error("Cantidad de bits inconsistente.");
        int bytesEsperados=(int)((bits+7)/8);
        if(lineas[4].length()!=4*((bytesEsperados+2)/3)) throw error("Datos truncados o adicionales.");
        byte[] datos=Base64.getDecoder().decode(lineas[4]);
        if(datos.length!=bytesEsperados || !Base64.getEncoder().encodeToString(datos).equals(lineas[4]))
            throw error("Base64 no canónico.");
        if(bits%8!=0 && datos.length>0) {
            int mascara=(1<<(8-(int)(bits%8)))-1;
            if((datos[datos.length-1]&mascara)!=0) throw error("Bits de relleno alterados.");
        }
        StringBuilder salida=new StringBuilder(cantidad);
        if(cantidad>0 && raiz.esHoja()) {
            for(byte dato:datos) if(dato!=0) throw error("Bits inválidos para un solo carácter.");
            for(int i=0;i<cantidad;i++) salida.append(raiz.getCaracter());
        } else if(cantidad>0) {
            NodoHuffman actual=raiz;
            for(int b=0;b<bits;b++) {
                int bit=(datos[b/8]>>>(7-b%8))&1;
                actual=bit==0?actual.getIzquierdo():actual.getDerecho();
                if(actual==null) throw error("Recorrido fuera del árbol.");
                if(actual.esHoja()) {
                    if(salida.length()>=cantidad) throw error("Hay caracteres adicionales.");
                    salida.append(actual.getCaracter()); actual=raiz;
                }
            }
            if(actual!=raiz) throw error("Código binario incompleto al final.");
        }
        if(salida.length()!=cantidad) throw error("Texto incompleto.");
        String texto=salida.toString();
        if(!java.util.Arrays.equals(frecuencias,contar(texto))) throw error("Frecuencias recuperadas diferentes.");
        if(!HashUtil.sha256(texto).equals(lineas[5])) throw error("El contenido no coincide con su SHA-256.");
        return texto;
    }

    private static int numero(String s,int min,int max) {
        if(!s.matches("0|[1-9][0-9]{0,9}")) throw error("Número inválido.");
        long n=Long.parseLong(s);
        if(n<min || n>max) throw error("Número fuera de rango.");
        return (int)n;
    }
    private static int[] contar(String texto) {
        int[] f=new int[TOTAL];
        for(int i=0;i<texto.length();i++) f[texto.charAt(i)]++;
        return f;
    }
    private static long calcularBits(int[] f,String[] codigos) {
        long bits=0;
        for(int c=0;c<TOTAL;c++) if(f[c]>0) bits+=(long)f[c]*codigos[c].length();
        return bits;
    }
    private static NodoHuffman construirArbol(int[] f) {
        MonticuloMinimo m=new MonticuloMinimo();
        for(int c=0;c<TOTAL;c++) if(f[c]>0) m.insertar(new NodoHuffman((char)c,f[c]));
        while(m.getTamano()>1) m.insertar(new NodoHuffman(m.extraerMinimo(),m.extraerMinimo()));
        return m.extraerMinimo();
    }
    private static void generarCodigos(NodoHuffman n,String prefijo,String[] codigos) {
        if(n.esHoja()) { codigos[n.getCaracter()]=prefijo.isEmpty()?"0":prefijo; return; }
        generarCodigos(n.getIzquierdo(),prefijo+"0",codigos);
        generarCodigos(n.getDerecho(),prefijo+"1",codigos);
    }
    private static IllegalArgumentException error(String mensaje) { return new IllegalArgumentException(mensaje); }
}
