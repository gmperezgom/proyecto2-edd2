package librosdigitales;

import java.math.BigInteger;
import java.security.SecureRandom;

// Esta clase implementa las potencias modulares de Diffie-Hellman.
// p y g son predeterminados: grupo MODP 14, de 2048 bits, definido en RFC 3526 sección 3.
// Cada conexión genera una privada nueva en cada equipo; ninguna privada viaja por la red.
public final class DiffieHellman {
    public static final BigInteger P = new BigInteger(
            "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
            "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
            "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
            "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
            "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
            "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
            "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
            "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
            "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9" +
            "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" +
            "15728E5A8AACAA68FFFFFFFFFFFFFFFF", 16);
    public static final BigInteger G = BigInteger.TWO;
    private static final BigInteger Q = P.subtract(BigInteger.ONE).divide(BigInteger.TWO);
    private DiffieHellman() { }

    // Genera 320 bits aleatorios para el exponente privado.
    public static BigInteger generarPrivada() {
        return new BigInteger(320, new SecureRandom()).setBit(319);
    }

    // Calcula g^privada mod p, conservando el cálculo individual del algoritmo.
    public static BigInteger calcularPublica(BigInteger privada) {
        if (privada == null || privada.signum() <= 0) throw new IllegalArgumentException("Clave privada inválida.");
        return G.modPow(privada, P);
    }

    // Valida rango y pertenencia al subgrupo antes de usar una pública recibida.
    public static void validarPublica(BigInteger publica) {
        if (publica == null || publica.compareTo(BigInteger.TWO) < 0
                || publica.compareTo(P.subtract(BigInteger.TWO)) > 0
                || !publica.modPow(Q, P).equals(BigInteger.ONE))
            throw new IllegalArgumentException("Clave pública Diffie-Hellman inválida.");
    }

    // Cada participante usa la pública recibida y exclusivamente su propia privada.
    public static BigInteger calcularSecreto(BigInteger publicaDelOtro, BigInteger privada) {
        validarPublica(publicaDelOtro);
        return publicaDelOtro.modPow(privada, P);
    }
}
