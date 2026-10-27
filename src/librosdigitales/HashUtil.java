package librosdigitales;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Esta clase calcula el hash SHA-256 de una contraseña para compararlo con el hash almacenado en el archivo de usuarios.
public class HashUtil {

    // El constructor privado evita crear objetos de esta clase de métodos estáticos.
    private HashUtil() {
    }

    // Este método calcula SHA-256 con el texto en UTF-8 y devuelve el hash en hexadecimal de 64 caracteres.
    public static String sha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] resumen = md.digest(texto.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < resumen.length; i++) {
                String hex = Integer.toHexString(resumen[i] & 0xff);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("No se pudo calcular el hash", e);
        }
    }
}