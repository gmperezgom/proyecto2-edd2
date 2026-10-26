package librosdigitales;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Herramienta para preparar la carpeta del servidor con datos de prueba:
 * usuarios.txt, catalogo.txt y libros/ISBN.txt (cifrados con 3DES).
 * Tambien crea el config.txt del cliente.
 *
 * Uso: ejecutar este archivo (Run File) y, si quiere, pasar la ruta del
 * servidor como argumento. Por defecto usa la carpeta "servidor".
 */
public class GeneradorServidor {

    public static void main(String[] args) throws IOException {
        String ruta = args.length > 0 ? args[0] : "servidor";

        // Usuarios de prueba: la clave se guarda como hash, nunca en claro
        String usuarios = "ana|" + HashUtil.sha256("ana123") + "|true\n"
                + "luis|" + HashUtil.sha256("luis123") + "|false\n";
        ArchivoUtil.escribirTexto(ArchivoUtil.unir(ruta, "usuarios.txt"), usuarios);

        // Catalogo
        String catalogo = "9780000000001|El viaje de la ceiba\n"
                + "9780000000002|Cronicas del lago\n"
                + "9780000000003|Manual del buen programador\n";
        ArchivoUtil.escribirTexto(ArchivoUtil.unir(ruta, "catalogo.txt"), catalogo);

        // Libros de ejemplo, cifrados con la clave que sale de Diffie-Hellman
        BigInteger pub = DiffieHellman.calcularPublica(DiffieHellman.PRIVADA_CLIENTE);
        BigInteger secreto = DiffieHellman.calcularSecreto(pub, DiffieHellman.PRIVADA_SERVIDOR);
        Cifrador3DES cifrador = new Cifrador3DES(secreto);

        String carpetaLibros = ArchivoUtil.unir(ruta, "libros");
        ArchivoUtil.escribirTexto(ArchivoUtil.unir(carpetaLibros, "9780000000001.txt"),
                cifrador.cifrar("Capitulo 1\nHabia una vez una ceiba enorme en medio de la plaza.\n"
                        + "Todos los ninos del pueblo jugaban bajo su sombra.\n"));
        ArchivoUtil.escribirTexto(ArchivoUtil.unir(carpetaLibros, "9780000000002.txt"),
                cifrador.cifrar("Capitulo 1\nEl lago amanecio cubierto de niebla y nadie se atrevio a salir.\n"
                        + "Las lanchas esperaron hasta el mediodia.\n"));
        ArchivoUtil.escribirTexto(ArchivoUtil.unir(carpetaLibros, "9780000000003.txt"),
                cifrador.cifrar("Capitulo 1\nPrimero compile, despues pregunte.\n"
                        + "Un programa que no compila no tiene derecho a calificacion.\n"));

        // Configuracion del cliente (se puede editar a mano)
        ArchivoUtil.escribirTexto("config.txt",
                "# Configuracion del cliente\n"
                + "rutaServidor=" + ruta + "\n"
                + "rutaLocal=cliente\n");

        System.out.println("Servidor de prueba creado en: " + ruta);
        System.out.println("Usuarios: ana / ana123 (Premium), luis / luis123 (Estandar)");
        System.out.println("Se genero config.txt en la carpeta del proyecto.");
    }
}
