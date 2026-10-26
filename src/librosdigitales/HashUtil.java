package librosdigitales;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Funcion hash (SHA-256) para guardar y comparar contrasenas sin almacenarlas en claro. */
public class HashUtil {

    private HashUtil() {
    }

    /** Devuelve el SHA-256 del texto, en hexadecimal minuscula. */
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