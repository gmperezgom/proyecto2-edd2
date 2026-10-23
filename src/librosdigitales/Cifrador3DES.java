package librosdigitales;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
//Esta clase es para realizar el cifrado simetrico 3DES

public class Cifrador3DES{
    private SecretKeySpec clave;
    private IvParameterSpec iv;

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

    //este metodo sirve para cifrar un texto plano y devuelve un resultado en Base64
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

    //este metodo sirve para descifrar un texto en Base64 y devuelve el texto plano
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