package librosdigitales;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.*;

// Esta clase utiliza 3DES/CBC con IV aleatorio y HMAC-SHA256 antes de descifrar.
// Formato: TDES2|IV en Base64|contenido cifrado en Base64|HMAC en Base64.
public final class Cifrador3DES {
    private final byte[] clave;
    private final byte[] claveMac;

    // Deriva claves independientes desde el secreto compartido de Diffie-Hellman.
    public Cifrador3DES(BigInteger secreto) { this(secreto.toByteArray()); }
    public Cifrador3DES(byte[] material) {
        if (material == null || material.length < 16) throw new IllegalArgumentException("Material de clave insuficiente.");
        clave = Arrays.copyOf(HashUtil.derivar(material, "LIBROS-V2-3DES"), 24);
        claveMac = HashUtil.derivar(material, "LIBROS-V2-HMAC");
    }

    // Cifra con un IV nuevo y autentica la versión, el IV y los bytes cifrados.
    public String cifrar(String texto) {
        try {
            byte[] iv = HashUtil.aleatorios(8);
            Cipher motor = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            motor.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(clave, "DESede"), new IvParameterSpec(iv));
            String cuerpo = "TDES2|" + b64(iv) + "|" + b64(motor.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
            return cuerpo + "|" + b64(HashUtil.hmac(claveMac, cuerpo.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) { throw new IllegalStateException("No se pudo cifrar el contenido.", e); }
    }

    // Verifica HMAC antes de descifrar. Una clave errónea o un archivo alterado se rechazan.
    public String descifrar(String paquete) {
        try {
            String[] campos = paquete.split("\\|", -1);
            if (campos.length != 4 || !campos[0].equals("TDES2")) throw new IllegalArgumentException();
            byte[] iv = Base64.getDecoder().decode(campos[1]);
            byte[] cifrado = Base64.getDecoder().decode(campos[2]);
            byte[] recibido = Base64.getDecoder().decode(campos[3]);
            if (iv.length != 8 || cifrado.length == 0 || cifrado.length % 8 != 0 || recibido.length != 32) throw new IllegalArgumentException();
            String cuerpo = campos[0] + "|" + campos[1] + "|" + campos[2];
            if (!HashUtil.iguales(recibido, HashUtil.hmac(claveMac, cuerpo.getBytes(StandardCharsets.UTF_8)))) throw new IllegalArgumentException();
            Cipher motor = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            motor.init(Cipher.DECRYPT_MODE, new SecretKeySpec(clave, "DESede"), new IvParameterSpec(iv));
            return ArchivoUtil.decodificar(motor.doFinal(cifrado));
        } catch (Exception e) { throw new IllegalArgumentException("Clave incorrecta o contenido cifrado dañado.", e); }
    }
    private static String b64(byte[] datos) { return Base64.getEncoder().encodeToString(datos); }
}
