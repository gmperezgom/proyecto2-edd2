package librosdigitales;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

// Esta clase define mensajes con longitud, límites y campos Base64 para evitar separadores ambiguos.
final class Protocolo {
    static final String VERSION="LIBROS-DH-2";
    private Protocolo() { }

    // Envía UTF-8 precedido de un entero; no se utiliza writeUTF por su límite de 65535 bytes.
    static void enviar(DataOutputStream salida,String texto) throws IOException {
        byte[] datos=texto.getBytes(StandardCharsets.UTF_8);
        if(datos.length>ArchivoUtil.MAX_BYTES) throw new IOException("Mensaje demasiado grande.");
        salida.writeInt(datos.length); salida.write(datos); salida.flush();
    }
    static String recibir(DataInputStream entrada,int limite) throws IOException {
        int cantidad=entrada.readInt();
        if(cantidad<0 || cantidad>limite) throw new IOException("Longitud de mensaje inválida.");
        byte[] datos=new byte[cantidad]; entrada.readFully(datos); return ArchivoUtil.decodificar(datos);
    }
    static String codificar(String s) { return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)); }
    static String decodificar(String s) throws IOException {
        try { return ArchivoUtil.decodificar(Base64.getDecoder().decode(s)); }
        catch(IllegalArgumentException e) { throw new IOException("Campo Base64 inválido.",e); }
    }
    static String b64(byte[] b) { return Base64.getEncoder().encodeToString(b); }

    // El cliente y el servidor firman/verifican exactamente ambas públicas y la versión.
    static byte[] transcripcion(String cliente,String servidor) {
        return (VERSION+"\n"+cliente+"\n"+servidor).getBytes(StandardCharsets.UTF_8);
    }
    static String firmar(byte[] datos,PrivateKey privada) throws GeneralSecurityException {
        Signature firma=Signature.getInstance("SHA256withRSA"); firma.initSign(privada); firma.update(datos);
        return b64(firma.sign());
    }
    static PublicKey publicaRsa(String texto) throws GeneralSecurityException {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(texto)));
    }
    static PrivateKey privadaRsa(String texto) throws GeneralSecurityException {
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(texto)));
    }
    static void verificar(byte[] datos, String firmaTexto, PublicKey publica) throws GeneralSecurityException {
        Signature firma=Signature.getInstance("SHA256withRSA"); firma.initVerify(publica); firma.update(datos);
        if(!firma.verify(Base64.getDecoder().decode(firmaTexto))) throw new SignatureException("Firma del intercambio inválida.");
    }
    static BigInteger publicaDh(String texto) throws IOException {
        if(!texto.matches("[0-9a-f]{1,512}")) throw new IOException("Clave pública fuera del formato esperado.");
        BigInteger valor=new BigInteger(texto,16);
        try { DiffieHellman.validarPublica(valor); }
        catch(IllegalArgumentException e) { throw new IOException(e.getMessage(),e); }
        return valor;
    }
}
