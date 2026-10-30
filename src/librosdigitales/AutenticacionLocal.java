package librosdigitales;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Base64;

// Esta clase habilita el acceso sin conexión después de una autenticación Premium en línea.
// Protege nombre, categoría y catálogo con una clave derivada de la contraseña y una sal aleatoria.
// Sin red, la categoría representa la última validación realizada; se actualiza al reconectar.
public final class AutenticacionLocal {
    private static final int ITERACIONES=210000;
    private final RepositorioLocal local;
    public AutenticacionLocal(RepositorioLocal local) { this.local=local; }

    public static final class CuentaLocal {
        private final Usuario usuario;
        private final ListaEnlazada<Libro> catalogo;
        CuentaLocal(Usuario usuario,ListaEnlazada<Libro> catalogo) { this.usuario=usuario; this.catalogo=catalogo; }
        public Usuario getUsuario() { return usuario; }
        public ListaEnlazada<Libro> getCatalogo() { return catalogo; }
    }

    // No conserva la contraseña ni su hash SHA-256 en un archivo local de autenticación.
    public void preparar(Usuario usuario,char[] contrasena,ListaEnlazada<Libro> catalogo) throws IOException {
        if(!usuario.isPremium()) { revocar(usuario.getNombre()); return; }
        byte[] sal=HashUtil.aleatorios(16);
        byte[] clave=HashUtil.claveLocal(contrasena,sal,ITERACIONES);
        try {
            String contenido="PREMIUM_LOCAL_2\n"+Protocolo.codificar(usuario.getNombre())+"\ntrue\n"
                    +Protocolo.codificar(RepositorioServidor.serializarCatalogo(catalogo));
            String cifrado=new Cifrador3DES(clave).cifrar(contenido);
            String archivo="CUENTA_LOCAL_2\n"+ITERACIONES+"\n"+Protocolo.b64(sal)+"\n"+cifrado;
            ArchivoUtil.escribirTexto(local.rutaCuenta(usuario.getNombre()).toString(),archivo);
        } finally { Arrays.fill(clave,(byte)0); }
    }

    // Una contraseña equivocada o metadatos alterados no pueden producir una cuenta Premium válida.
    public CuentaLocal autenticar(String nombre,char[] contrasena) throws IOException {
        Path archivo=local.rutaCuenta(nombre);
        if(!Files.isRegularFile(archivo)) throw new IOException("No hay acceso local preparado para este usuario. Primero inicia sesión Premium con conexión.");
        String texto=ArchivoUtil.leerTexto(archivo.toString());
        if(texto.length()>8*1024*1024) throw new IOException("Cuenta local demasiado grande.");
        byte[] clave=null;
        try {
            String[] p=texto.split("\n",-1);
            if(p.length!=4 || !p[0].equals("CUENTA_LOCAL_2") || !p[1].equals(Integer.toString(ITERACIONES))) throw new IllegalArgumentException();
            byte[] sal=Base64.getDecoder().decode(p[2]);
            if(sal.length!=16) throw new IllegalArgumentException();
            clave=HashUtil.claveLocal(contrasena,sal,ITERACIONES);
            String[] datos=new Cifrador3DES(clave).descifrar(p[3]).split("\n",-1);
            if(datos.length!=4 || !datos[0].equals("PREMIUM_LOCAL_2") || !datos[2].equals("true") || !Protocolo.decodificar(datos[1]).equals(nombre)) throw new IllegalArgumentException();
            return new CuentaLocal(new Usuario(nombre,"",true),RepositorioServidor.analizarCatalogo(Protocolo.decodificar(datos[3])));
        } catch(IllegalArgumentException e) { throw new IOException("Contraseña local incorrecta o cuenta local dañada.",e); }
        finally { if(clave!=null) Arrays.fill(clave,(byte)0); }
    }
    public void revocar(String nombre) throws IOException { Files.deleteIfExists(local.rutaCuenta(nombre)); }
}
