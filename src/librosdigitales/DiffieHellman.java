package librosdigitales;

import java.math.BigInteger;

// Esta clase realiza los cálculos de Diffie-Hellman con valores predeterminados. En esta versión las claves privadas de ambos participantes están definidas en el código.
public class DiffieHellman {

    // Módulo público: primo de Mersenne calculado como 2 elevado a 521 menos 1.
    public static final BigInteger P = BigInteger.ONE.shiftLeft(521).subtract(BigInteger.ONE);

    // Base pública para las potencias modulares de Diffie-Hellman.
    public static final BigInteger G = BigInteger.valueOf(5);

    // Valor privado predeterminado del cliente para la simulación.
    public static final BigInteger PRIVADA_CLIENTE = new BigInteger("918273645564738291827364556473829101");

    // Valor privado predeterminado del servidor para la simulación.
    public static final BigInteger PRIVADA_SERVIDOR = new BigInteger("102938475610293847561029384756102938");

    // El constructor privado evita crear objetos de esta clase de métodos estáticos.
    private DiffieHellman() {
    }

    // Este método calcula la clave pública mediante la operación g elevada a la clave privada, módulo p.
    public static BigInteger calcularPublica(BigInteger privada) {
        return G.modPow(privada, P);
    }

    // Este método calcula el secreto con la clave pública del otro participante y la clave privada propia, módulo p.
    public static BigInteger calcularSecreto(BigInteger publicaDelOtro, BigInteger privada) {
        return publicaDelOtro.modPow(privada, P);
    }
}