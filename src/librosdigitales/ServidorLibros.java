package librosdigitales;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.Base64;
import java.util.concurrent.*;

// Este programa permanece activo en PC1 y atiende varios clientes con secretos independientes.
// Su configuración privada debe estar fuera de la carpeta compartida.
public final class ServidorLibros implements AutoCloseable {
    private final RepositorioServidor repositorio;
    private final Cifrador3DES almacen;
    private final PrivateKey identidadPrivada;
    private final PublicKey identidadPublica;
    private final ServerSocket escucha;
    private final ThreadPoolExecutor trabajadores;
    private final int espera;
    private volatile boolean cerrado;

    // Valida configuración, datos e identidad antes de aceptar conexiones.
    public ServidorLibros(String archivoConfig) throws IOException {
        try {
            String c=ArchivoUtil.leerTexto(archivoConfig);
            Path carpeta=Path.of(Configuracion.valor(c,"rutaDatos",null)).toRealPath();
            Path archivo=Path.of(archivoConfig).toRealPath();
            if(archivo.startsWith(carpeta)) throw new IOException("La configuración privada debe estar fuera de la carpeta compartida.");
            int puerto=Configuracion.entero(Configuracion.valor(c,"puerto","5050"),1,65535,"puerto");
            espera=Configuracion.entero(Configuracion.valor(c,"tiempoEsperaMs","10000"),500,30000,"tiempoEsperaMs");
            byte[] clave=Base64.getDecoder().decode(Configuracion.valor(c,"claveAlmacen",null));
            if(clave.length!=32) throw new IOException("La clave de almacenamiento debe tener 32 bytes.");
            almacen=new Cifrador3DES(clave);
            identidadPrivada=Protocolo.privadaRsa(Configuracion.valor(c,"identidadPrivada",null));
            identidadPublica=Protocolo.publicaRsa(Configuracion.valor(c,"identidadPublica",null));
            byte[] comprobacion=HashUtil.aleatorios(32);
            Protocolo.verificar(comprobacion,Protocolo.firmar(comprobacion,identidadPrivada),identidadPublica);
            repositorio=new RepositorioServidor(carpeta.toString());
            repositorio.cargarUsuarios();
            ListaEnlazada<Libro> libros=repositorio.cargarCatalogo();
            for(int i=0;i<libros.getTamano();i++) {
                String texto=almacen.descifrar(repositorio.leerLibroCifrado(libros.obtener(i).getIsbn()));
                if(texto.length()>Huffman.MAX_CARACTERES) throw new IOException("Libro demasiado grande: "+libros.obtener(i).getIsbn());
            }
            escucha=new ServerSocket();
            escucha.bind(new InetSocketAddress(Configuracion.valor(c,"interfaz","0.0.0.0"),puerto));
            trabajadores=new ThreadPoolExecutor(8,8,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(16));
            System.out.println("Servidor listo. Puerto TCP: "+escucha.getLocalPort());
            System.out.println("Carpeta de datos: "+carpeta);
            System.out.println("Huella pública: "+HashUtil.hexadecimal(HashUtil.resumen(identidadPublica.getEncoded())));
        } catch(GeneralSecurityException | IllegalArgumentException e) { throw new IOException("Configuración privada o almacén inválidos.",e); }
    }

    // Ejecuta el ciclo de escucha; cada conexión se atiende en un trabajador separado.
    public void servir() throws IOException {
        while(!cerrado) {
            Socket cliente;
            try { cliente=escucha.accept(); }
            catch(SocketException e) { if(cerrado) return; throw e; }
            try { trabajadores.execute(()->atender(cliente)); }
            catch(RejectedExecutionException e) { cliente.close(); System.out.println("Capacidad ocupada; se rechazó una conexión."); }
        }
    }

    // Realiza el cálculo del lado servidor y firma las dos claves públicas.
    private void atender(Socket socket) {
        try(socket) {
            socket.setSoTimeout(espera);
            DataInputStream entrada=new DataInputStream(socket.getInputStream());
            DataOutputStream salida=new DataOutputStream(socket.getOutputStream());
            if(!Protocolo.recibir(entrada,64).equals(Protocolo.VERSION)) throw new IOException("Versión de protocolo no compatible.");
            String recibida=Protocolo.recibir(entrada,1024);
            BigInteger publicaCliente=Protocolo.publicaDh(recibida);
            BigInteger privada=DiffieHellman.generarPrivada();
            String publica=DiffieHellman.calcularPublica(privada).toString(16);
            Cifrador3DES canal=new Cifrador3DES(DiffieHellman.calcularSecreto(publicaCliente,privada));
            Protocolo.enviar(salida,publica);
            Protocolo.enviar(salida,Protocolo.b64(identidadPublica.getEncoded()));
            Protocolo.enviar(salida,Protocolo.firmar(Protocolo.transcripcion(recibida,publica),identidadPrivada));
            String mensaje=canal.descifrar(Protocolo.recibir(entrada,8192));
            if(!mensaje.startsWith("PETICION\n")) throw new IOException("Petición fuera del protocolo.");
            String respuesta;
            try { respuesta=procesar(mensaje.substring("PETICION\n".length()),canal); }
            catch(IOException | IllegalArgumentException e) { respuesta="ERROR\n"+Protocolo.codificar(e.getMessage()==null?"Solicitud inválida.":e.getMessage()); }
            Protocolo.enviar(salida,canal.cifrar("RESPUESTA\n"+respuesta));
            System.out.println("[DH real] Solicitud atendida desde "+socket.getInetAddress().getHostAddress());
        } catch(IOException | GeneralSecurityException | IllegalArgumentException e) {
            System.out.println("Conexión cerrada: "+e.getMessage());
        }
    }

    // Autentica también en el servidor; un cliente estándar no puede pedir una descarga Premium.
    private String procesar(String peticion,Cifrador3DES canal) throws IOException {
        String[] p=peticion.split("\n",-1);
        if(p.length==1 && p[0].equals("CATALOGO"))
            return "OK\n"+Protocolo.codificar(RepositorioServidor.serializarCatalogo(repositorio.cargarCatalogo()));
        if(!((p.length==3 && p[0].equals("LOGIN")) || (p.length==5 && p[0].equals("LIBRO"))))
            throw new IOException("Operación desconocida.");
        String nombre=Protocolo.decodificar(p[1]);
        if(!p[2].matches("[a-f0-9]{64}")) return "AUTH";
        Usuario usuario=null;
        ListaEnlazada<Usuario> usuarios=repositorio.cargarUsuarios();
        for(int i=0;i<usuarios.getTamano();i++) {
            Usuario candidato=usuarios.obtener(i);
            if(candidato.getNombre().equals(nombre) && HashUtil.iguales(candidato.getHashClave().getBytes(StandardCharsets.US_ASCII),p[2].getBytes(StandardCharsets.US_ASCII)))
                usuario=candidato;
        }
        if(usuario==null) return "AUTH";
        if(p[0].equals("LOGIN")) return "OK\n"+Protocolo.codificar(usuario.getNombre())+"\n"+usuario.isPremium();
        if(!p[4].equals("LEER") && !p[4].equals("DESCARGAR")) throw new IOException("Acción de libro inválida.");
        if(p[4].equals("DESCARGAR") && !usuario.isPremium()) throw new IOException("La descarga es exclusiva de Premium.");
        boolean encontrado=false;
        ListaEnlazada<Libro> libros=repositorio.cargarCatalogo();
        for(int i=0;i<libros.getTamano();i++) if(libros.obtener(i).getIsbn().equals(p[3])) encontrado=true;
        if(!encontrado) throw new IOException("El libro no pertenece al catálogo.");
        String original=repositorio.leerLibroCifrado(p[3]);
        String texto=almacen.descifrar(original);
        if(texto.length()>Huffman.MAX_CARACTERES) throw new IOException("Libro demasiado grande.");
        // El original permanece cifrado en disco; la entrega usa el secreto nuevo de esta conexión.
        String entrega=canal.cifrar(texto);
        return "OK\n"+Protocolo.codificar(original)+"\n"+Protocolo.codificar(entrega);
    }

    // Cierra la escucha y deja que los trabajadores terminen, con un tiempo máximo definido.
    @Override public void close() throws IOException {
        cerrado=true;
        escucha.close(); trabajadores.shutdown();
        try { if(!trabajadores.awaitTermination(espera+1000L,TimeUnit.MILLISECONDS)) trabajadores.shutdownNow(); }
        catch(InterruptedException e) { trabajadores.shutdownNow(); Thread.currentThread().interrupt(); }
    }
    public int getPuerto() { return escucha.getLocalPort(); }

    // Ejecutar esta clase únicamente en el equipo que atiende las conexiones.
    @SuppressWarnings("try")
    public static void main(String[] args) {
        try(ServidorLibros servidor=new ServidorLibros(args.length>0?args[0]:"servidor-config-v2.txt")) {
            Runtime.getRuntime().addShutdownHook(new Thread(()->{try {servidor.close();} catch(IOException ignored) { }}));
            servidor.servir();
        } catch(IOException e) { System.out.println("No se pudo iniciar el servidor: "+e.getMessage()); }
    }
}
