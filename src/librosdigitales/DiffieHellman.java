package librosdigitales;

import java.math.BigInteger;

/**
 * Intercambio de claves Diffie-Hellman con valores predeterminados.
 *
 * Cliente y servidor comparten p y g (publicos). Cada uno tiene un numero
 * privado, calcula su clave publica (g^privada mod p), la envia al otro y
 * ambos terminan con el mismo secreto: (publicaDelOtro^privada) mod p.
 */
public class DiffieHellman {

    /** Primo publico p = 2^521 - 1 (primo de Mersenne). */
    public static final BigInteger P = BigInteger.ONE.shiftLeft(521).subtract(BigInteger.ONE);

    /** Generador publico g. */
    public static final BigInteger G = BigInteger.valueOf(5);

    /** Numero privado del cliente (predeterminado). */
    public static final BigInteger PRIVADA_CLIENTE = new BigInteger("918273645564738291827364556473829101");

    /** Numero privado del servidor (predeterminado). */
    public static final BigInteger PRIVADA_SERVIDOR = new BigInteger("102938475610293847561029384756102938");

    private DiffieHellman() {
    }

    /** Calcula la clave publica: g^privada mod p. */
    public static BigInteger calcularPublica(BigInteger privada) {
        return G.modPow(privada, P);
    }

    /** Calcula el secreto compartido: publicaDelOtro^privada mod p. */
    public static BigInteger calcularSecreto(BigInteger publicaDelOtro, BigInteger privada) {
        return publicaDelOtro.modPow(privada, P);
    }
}
