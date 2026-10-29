package librosdigitales;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.*;

// Esta clase centraliza SHA-256, HMAC y la derivación de una clave local desde la contraseña.
public final class HashUtil {
    private HashUtil() { }

    // Calcula el hash utilizado por el archivo usuario|hash|premium.
    public static String sha256(String texto) { return hexadecimal(resumen(texto.getBytes(StandardCharsets.UTF_8))); }

    // Devuelve los 32 bytes del resumen SHA-256.
    public static byte[] resumen(byte[] datos) {
        try { return MessageDigest.getInstance("SHA-256").digest(datos); }
        catch (GeneralSecurityException e) { throw new IllegalStateException("SHA-256 no disponible.", e); }
    }

    // Genera bytes aleatorios para sal, claves y vectores de inicialización.
    public static byte[] aleatorios(int cantidad) {
        byte[] datos = new byte[cantidad]; new SecureRandom().nextBytes(datos); return datos;
    }

    // Convierte bytes a hexadecimal con longitud fija.
    public static String hexadecimal(byte[] datos) {
        StringBuilder salida = new StringBuilder(datos.length * 2);
        for (byte dato : datos) salida.append(String.format("%02x", dato & 255));
        return salida.toString();
    }

    // Compara resúmenes sin detenerse en el primer byte diferente.
    public static boolean iguales(byte[] a, byte[] b) { return MessageDigest.isEqual(a, b); }

    // Calcula un código de autenticación para detectar cambios en el contenido cifrado.
    public static byte[] hmac(byte[] clave, byte[] datos) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256")); return mac.doFinal(datos);
        } catch (GeneralSecurityException e) { throw new IllegalStateException("No se pudo calcular HMAC.", e); }
    }

    // Usa etiquetas distintas para separar la clave de cifrado de la clave de autenticación.
    public static byte[] derivar(byte[] material, String etiqueta) {
        return hmac(material, etiqueta.getBytes(StandardCharsets.UTF_8));
    }

    // Deriva una clave local con sal y un costo de cálculo para verificar la contraseña sin guardarla.
    public static byte[] claveLocal(char[] contrasena, byte[] sal, int iteraciones) {
        PBEKeySpec especificacion = new PBEKeySpec(contrasena, sal, iteraciones, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(especificacion).getEncoded(); }
        catch (GeneralSecurityException e) { throw new IllegalStateException("No se pudo derivar la clave local.", e); }
        finally { especificacion.clearPassword(); }
    }
}
