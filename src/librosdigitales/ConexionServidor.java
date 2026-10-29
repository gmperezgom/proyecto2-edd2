package librosdigitales;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.*;

// Esta clase abre una conexión independiente y negocia un secreto nuevo por operación.
public final class ConexionServidor {
    private final Configuracion config;
    public ConexionServidor(Configuracion config) { this.config=config; }

    // Diferencia un servidor inaccesible de credenciales incorrectas o una identidad no válida.
    public static final class NoDisponibleException extends IOException {
        private static final long serialVersionUID=1L;
        NoDisponibleException(Throwable causa) { super("El servicio del servidor no está disponible.",causa); }
    }
    public static final class CredencialesException extends IOException {
        private static final long serialVersionUID=1L;
        CredencialesException() { super("Usuario o contraseña incorrectos."); }
    }

    // Consulta el catálogo antes del inicio de sesión sin crear datos locales de autenticación.
    public ListaEnlazada<Libro> consultarCatalogo() throws IOException {
        String[] r=operar("CATALOGO");
        if(r.length!=2) throw new IOException("Respuesta de catálogo inválida.");
        return RepositorioServidor.analizarCatalogo(Protocolo.decodificar(r[1]));
    }

    // El hash de la contraseña viaja dentro del canal cifrado autenticado.
    public Usuario autenticar(String nombre,String hash) throws IOException {
        String[] r=operar("LOGIN\n"+Protocolo.codificar(nombre)+"\n"+hash);
        if(r.length!=3 || !(r[2].equals("true") || r[2].equals("false"))) throw new IOException("Respuesta de autenticación inválida.");
        return new Usuario(Protocolo.decodificar(r[1]),hash,Boolean.parseBoolean(r[2]));
    }

    // Obtiene el original cifrado del almacén y una entrega cifrada con la clave de esta conexión.
    // Primero se guardan ambos cifrados localmente; después se realiza el descifrado del cliente.
    public String obtenerLibro(Usuario usuario,Libro libro,boolean descarga,RepositorioLocal local) throws IOException {
        String peticion="LIBRO\n"+Protocolo.codificar(usuario.getNombre())+"\n"+usuario.getHashClave()
                +"\n"+libro.getIsbn()+"\n"+(descarga?"DESCARGAR":"LEER");
        String[] r=operar(peticion);
        if(r.length!=4) throw new IOException("Respuesta de libro inválida.");
        String original=Protocolo.decodificar(r[1]);
        String entrega=Protocolo.decodificar(r[2]);
        String material= r[3];
        // operar añade el material de sesión únicamente dentro de la memoria del cliente.
        local.guardarCifrados(usuario.getNombre(),libro.getIsbn(),original,entrega);
        try { return new Cifrador3DES(java.util.Base64.getDecoder().decode(material)).descifrar(entrega); }
        catch(IllegalArgumentException e) { throw new IOException("La entrega no superó la verificación de integridad.",e); }
    }

    // Completa un intercambio real de públicas y valida la identidad del servidor con una huella fijada.
    private String[] operar(String peticion) throws IOException {
        try(Socket socket=new Socket()) {
            socket.connect(new InetSocketAddress(config.getHost(),config.getPuerto()),config.getTiempoEspera());
            socket.setSoTimeout(config.getTiempoEspera());
            DataInputStream entrada=new DataInputStream(socket.getInputStream());
            DataOutputStream salida=new DataOutputStream(socket.getOutputStream());
            BigInteger privada=DiffieHellman.generarPrivada();
            String publica=DiffieHellman.calcularPublica(privada).toString(16);
            Protocolo.enviar(salida,Protocolo.VERSION); Protocolo.enviar(salida,publica);
            String remota=Protocolo.recibir(entrada,1024);
            BigInteger publicaServidor=Protocolo.publicaDh(remota);
            PublicKey identidad=Protocolo.publicaRsa(Protocolo.recibir(entrada,4096));
            String huella=HashUtil.hexadecimal(HashUtil.resumen(identidad.getEncoded()));
            if(!huella.equals(config.getHuellaServidor())) throw new IOException("La huella del servidor no coincide; verifica config.txt.");
            Protocolo.verificar(Protocolo.transcripcion(publica,remota),Protocolo.recibir(entrada,4096),identidad);
            byte[] secreto=DiffieHellman.calcularSecreto(publicaServidor,privada).toByteArray();
            Cifrador3DES canal=new Cifrador3DES(secreto);
            Protocolo.enviar(salida,canal.cifrar("PETICION\n"+peticion));
            String respuesta=canal.descifrar(Protocolo.recibir(entrada,ArchivoUtil.MAX_BYTES));
            if(!respuesta.startsWith("RESPUESTA\n")) throw new IOException("Respuesta fuera del protocolo.");
            String[] campos=respuesta.substring("RESPUESTA\n".length()).split("\n",-1);
            if(campos.length==0) throw new IOException("Respuesta vacía.");
            if(campos[0].equals("AUTH")) throw new CredencialesException();
            if(!campos[0].equals("OK")) throw new IOException(campos.length>1?Protocolo.decodificar(campos[1]):"Solicitud rechazada.");
            if(peticion.startsWith("LIBRO\n")) {
                if(campos.length!=3) throw new IOException("Respuesta de libro incompleta.");
                return new String[]{campos[0],campos[1],campos[2],Protocolo.b64(secreto)};
            }
            return campos;
        } catch(ConnectException | UnknownHostException | NoRouteToHostException | SocketTimeoutException e) {
            throw new NoDisponibleException(e);
        } catch(GeneralSecurityException | IllegalArgumentException e) {
            throw new IOException("Falló la validación criptográfica del intercambio.",e);
        }
    }
}
