package librosdigitales;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.*;
import java.security.*;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.*;

// Esta clase prepara un almacén V2 nuevo o migra una copia de los libros de la versión original.
// Nunca reemplaza el almacén anterior ni un archivo de configuración existente.
public final class GeneradorServidor {
    private GeneradorServidor() { }

    public static void main(String[] args) {
        try {
            String origen=null;
            String destino="servidor_v2";
            if(args.length>0 && args[0].equals("--migrar")) {
                if(args.length!=3) throw new IOException("Uso: GeneradorServidor --migrar carpetaAnterior carpetaNueva");
                origen=args[1]; destino=args[2];
            } else if(args.length==1) destino=args[0];
            else if(args.length>1) throw new IOException("Uso: GeneradorServidor [carpetaNueva]");
            generar(destino,"servidor-config-v2.txt",origen,5050);
            System.out.println("Preparación terminada. No se modificó config.txt ni el almacén anterior.");
            System.out.println("Servidor: ejecutar ServidorLibros con servidor-config-v2.txt.");
            System.out.println("Cliente: adaptar config-cliente-v2.ejemplo.txt y guardarlo como config.txt.");
        } catch(Exception e) { System.out.println("No se generaron o no se completaron los datos: "+e.getMessage()); }
    }

    // Se puede invocar con rutas temporales desde las pruebas automáticas.
    public static void generar(String rutaDatos,String rutaConfig,String origen,int puerto) throws IOException, GeneralSecurityException {
        Path datos=Path.of(rutaDatos).toAbsolutePath().normalize();
        Path config=Path.of(rutaConfig).toAbsolutePath().normalize();
        Path ejemplo=config.resolveSibling("config-cliente-v2.ejemplo.txt");
        if(config.startsWith(datos) || ejemplo.startsWith(datos)) throw new IOException("Las configuraciones deben estar fuera de la carpeta compartida.");
        if(Files.exists(datos) || Files.exists(config) || Files.exists(ejemplo))
            throw new IOException("El destino o la configuración ya existen. Elige rutas nuevas; no se sobrescriben datos.");
        if(puerto<1 || puerto>65535) throw new IOException("Puerto inválido.");
        ListaEnlazada<Libro> libros;
        String usuarios;
        String[] textos;
        if(origen==null) {
            libros=RepositorioServidor.analizarCatalogo("9780000000001|El viaje de la ceiba\n9780000000002|Cronicas del lago\n9780000000003|Manual del buen programador\n");
            usuarios =
            "gerber|" + HashUtil.sha256("gerber123") + "|true\n" + "mogo|" + HashUtil.sha256("mogo123") + "|false\n";
            textos=new String[]{"Capitulo 1\nHabia una vez una ceiba enorme en medio de la plaza.\nTodos los ninos del pueblo jugaban bajo su sombra.\n",
                    "Capitulo 1\nEl lago amanecio cubierto de niebla y nadie se atrevio a salir.\nLas lanchas esperaron hasta el mediodia.\n",
                    "Capitulo 1\nPrimero compile, despues pregunte.\nLa documentacion permite comprender el programa.\n"};
        } else {
            RepositorioServidor anterior=new RepositorioServidor(origen);
            anterior.cargarUsuarios();
            usuarios=ArchivoUtil.leerTexto(ArchivoUtil.unir(origen,"usuarios.txt"));
            libros=anterior.cargarCatalogo();
            textos=new String[libros.getTamano()];
            // Estos valores solo permiten importar el cifrado histórico que ya utilizaba el proyecto.
            for(int i=0;i<textos.length;i++) textos[i]=descifrarVersionAnterior(anterior.leerLibroCifrado(libros.obtener(i).getIsbn()));
        }
        for(String texto:textos) if(texto.length()>Huffman.MAX_CARACTERES) throw new IOException("Un libro excede el límite de caracteres.");
        byte[] claveAlmacen=HashUtil.aleatorios(32);
        Cifrador3DES cifrador=new Cifrador3DES(claveAlmacen);
        KeyPairGenerator generador=KeyPairGenerator.getInstance("RSA"); generador.initialize(2048);
        KeyPair identidad=generador.generateKeyPair();
        ArchivoUtil.escribirTexto(datos.resolve("usuarios.txt").toString(),usuarios);
        ArchivoUtil.escribirTexto(datos.resolve("catalogo.txt").toString(),RepositorioServidor.serializarCatalogo(libros));
        for(int i=0;i<textos.length;i++) ArchivoUtil.escribirTexto(datos.resolve("libros").resolve(libros.obtener(i).getIsbn()+".txt").toString(),cifrador.cifrar(textos[i]));
        String huella=HashUtil.hexadecimal(HashUtil.resumen(identidad.getPublic().getEncoded()));
        ArchivoUtil.escribirTexto(config.toString(),"# PRIVADO: mantener fuera de la carpeta compartida y no subir a Git\n"
                +"rutaDatos="+datos+"\ninterfaz=0.0.0.0\npuerto="+puerto+"\ntiempoEsperaMs=10000\n"
                +"claveAlmacen="+Protocolo.b64(claveAlmacen)+"\nidentidadPrivada="+Protocolo.b64(identidad.getPrivate().getEncoded())
                +"\nidentidadPublica="+Protocolo.b64(identidad.getPublic().getEncoded())+"\n");
        ArchivoUtil.escribirTexto(ejemplo.toString(),"# Adaptar hostServidor y rutaServidor en cada computadora cliente\n"
                +"rutaServidor="+datos+"\nrutaLocal=cliente_v2\nhostServidor=localhost\npuertoServidor="+puerto
                +"\ntiempoEsperaMs=5000\nhuellaServidor="+huella+"\n");
        System.out.println("Almacén V2 creado: "+datos);
        System.out.println("Configuración privada: "+config);
        System.out.println("Huella pública que se copia a los clientes: "+huella);
    }

    // Compatibilidad de importación exclusiva para los archivos Base64 del programa original.
    private static String descifrarVersionAnterior(String archivo) throws GeneralSecurityException, IOException {
        if(archivo.startsWith("TDES2|")) throw new IOException("El origen ya es V2; conserva su configuración privada en vez de migrarlo como V1.");
        BigInteger p=BigInteger.ONE.shiftLeft(521).subtract(BigInteger.ONE);
        BigInteger a=new BigInteger("918273645564738291827364556473829101");
        BigInteger b=new BigInteger("102938475610293847561029384756102938");
        BigInteger secreto=BigInteger.valueOf(5).modPow(a,p).modPow(b,p);
        byte[] resumen=HashUtil.resumen(secreto.toByteArray());
        Cipher motor=Cipher.getInstance("DESede/CBC/PKCS5Padding");
        motor.init(Cipher.DECRYPT_MODE,new SecretKeySpec(java.util.Arrays.copyOf(resumen,24),"DESede"),
                new IvParameterSpec(java.util.Arrays.copyOfRange(resumen,24,32)));
        try { return ArchivoUtil.decodificar(motor.doFinal(Base64.getDecoder().decode(archivo.trim()))); }
        catch(IllegalArgumentException e) { throw new IOException("Un libro anterior no contiene Base64 válido.",e); }
    }
}
