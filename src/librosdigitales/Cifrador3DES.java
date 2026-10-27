package librosdigitales;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// Esta clase cifra y descifra el contenido de los libros mediante 3DES. El resultado cifrado se representa en Base64 para guardarlo como texto.
public class Cifrador3DES {

    // Clave simétrica utilizada por 3DES.
    private SecretKeySpec clave;
    // Vector de inicialización utilizado por el modo CBC.
    private IvParameterSpec iv;

    // Este constructor aplica SHA-256 al secreto compartido y obtiene 24 bytes para la clave 3DES y 8 bytes para el vector de inicialización.
    public Cifrador3DES(BigInteger secretoCompartido) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] resumen = md.digest(secretoCompartido.toByteArray());
            byte[] bytesClave = new byte[24];
            byte[] bytesIv = new byte[8];
            System.arraycopy(resumen, 0, bytesClave, 0, 24);
            System.arraycopy(resumen, 24, bytesIv, 0, 8);
            this.clave = new SecretKeySpec(bytesClave, "DESede");
            this.iv = new IvParameterSpec(bytesIv);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo preparar la clave 3DES", e);
        }
    }

    // Este método convierte el texto original en contenido cifrado con 3DES y lo devuelve en Base64.
    public String cifrar(String textoPlano) {
        try {
            Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, clave, iv);
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cifrado);
        } catch (Exception e) {
            throw new IllegalStateException("Error al cifrar", e);
        }
    }

    // Este método decodifica el contenido Base64 y utiliza 3DES para recuperar el texto original.
    public String descifrar(String textoCifradoBase64) {
        try {
            Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, clave, iv);
            byte[] cifrado = Base64.getDecoder().decode(textoCifradoBase64.trim());
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Error al descifrar (clave incorrecta o archivo danado)", e);
        }
    }
}