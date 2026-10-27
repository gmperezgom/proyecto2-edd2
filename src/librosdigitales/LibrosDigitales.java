package librosdigitales;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Scanner;

// Esta clase controla la aplicación cliente: carga la configuración, autentica usuarios y presenta las opciones de lectura, descarga y cierre de sesión.
public class LibrosDigitales {

    // Nombre del archivo que se busca en el directorio de ejecución.
    private static final String ARCHIVO_CONFIG = "config.txt";

    // Lector de las entradas del usuario en consola.
    private Scanner teclado;
    // Configuración de las rutas utilizadas por el cliente.
    private Configuracion config;
    // Objeto que permite consultar los archivos del servidor.
    private RepositorioServidor servidor;
    // Lista de usuarios cargada al iniciar la aplicación.
    private ListaEnlazada<Usuario> usuarios;
    // Lista de libros disponibles cargada al iniciar la aplicación.
    private ListaEnlazada<Libro> catalogo;
    // Usuario autenticado; al cerrar sesión se establece en null.
    private Usuario usuarioActual;

    // Este constructor prepara el lector de las entradas que se escriben en consola.
    public LibrosDigitales() {
        this.teclado = new Scanner(System.in);
    }

    // Este método es el punto de entrada para ejecutar esta clase.
    public static void main(String[] args) {
        new LibrosDigitales().iniciar();
    }

    // Este método carga la configuración y los datos del servidor. Después controla el ciclo de inicio y cierre de sesión.
    public void iniciar() {
        try {
            config = Configuracion.cargar(ARCHIVO_CONFIG);
            servidor = new RepositorioServidor(config.getRutaServidor());
            usuarios = servidor.cargarUsuarios();
            catalogo = servidor.cargarCatalogo();
        } catch (IOException e) {
            System.out.println("No se pudo conectar con el servidor: " + e.getMessage());
            return;
        }
        System.out.println("Conectado al servidor: " + config.getRutaServidor());

        boolean seguir = true;
        while (seguir) {
            if (!pantallaLogin()) {
                break; // el usuario eligio salir desde el login
            }
            seguir = menuPrincipal();
        }
        System.out.println("Hasta luego.");
    }

    // Este método solicita las credenciales y compara el hash de la contraseña. Devuelve false si se escribe SALIR como usuario.
    private boolean pantallaLogin() {
        System.out.println("\n===== INICIO DE SESION (escriba SALIR para terminar) =====");
        while (true) {
            System.out.print("Usuario: ");
            String nombre = teclado.nextLine().trim();
            if (nombre.equalsIgnoreCase("SALIR")) {
                return false;
            }
            System.out.print("Contrasena: ");
            String clave = teclado.nextLine();
            String hash = HashUtil.sha256(clave);

            Usuario encontrado = buscarUsuario(nombre);
            if (encontrado != null && encontrado.getHashClave().equalsIgnoreCase(hash)) {
                usuarioActual = encontrado;
                System.out.println("Bienvenido, " + usuarioActual.getNombre()
                        + (usuarioActual.isPremium() ? " (Premium)" : " (Estandar)"));
                return true;
            }
            System.out.println("Usuario o contrasena incorrectos.");
        }
    }

    // Este método busca un usuario por su nombre y devuelve null si no está registrado.
    private Usuario buscarUsuario(String nombre) {
        for (int i = 0; i < usuarios.getTamano(); i++) {
            Usuario u = usuarios.obtener(i);
            if (u.getNombre().equals(nombre)) {
                return u;
            }
        }
        return null;
    }

    // Este método procesa el menú. Devuelve true para volver al inicio de sesión y false para terminar la aplicación.
    private boolean menuPrincipal() {
        while (true) {
            System.out.println("\n===== MENU PRINCIPAL =====");
            System.out.println("1. Explorar catalogo");
            System.out.println("2. Lectura local");
            System.out.println("3. Cerrar sesion");
            System.out.println("4. Salir");
            System.out.print("Opcion: ");
            String opcion = teclado.nextLine().trim();

            switch (opcion) {
                case "1":
                    explorarCatalogo();
                    break;
                case "2":
                    lecturaLocal();
                    break;
                case "3":
                    usuarioActual = null;
                    System.out.println("Sesion cerrada.");
                    return true;
                case "4":
                    return false;
                default:
                    System.out.println("Opcion no valida.");
            }
        }
    }

    // Este método muestra los libros y permite seleccionar uno o regresar al menú anterior.
    private void explorarCatalogo() {
        while (true) {
            System.out.println("\n===== CATALOGO =====");
            for (int i = 0; i < catalogo.getTamano(); i++) {
                Libro l = catalogo.obtener(i);
                System.out.println((i + 1) + ". " + l.getTitulo() + " (ISBN " + l.getIsbn() + ")");
            }
            System.out.println("0. Volver");
            System.out.print("Seleccione un libro: ");
            int eleccion = leerEntero();
            if (eleccion == 0) {
                return;
            }
            Libro libro = catalogo.obtener(eleccion - 1);
            if (libro == null) {
                System.out.println("Opcion no valida.");
                continue;
            }
            procesarLibro(libro);
        }
    }

    // Este método permite al usuario Premium elegir lectura o descarga; al usuario estándar le inicia la lectura.
    private void procesarLibro(Libro libro) {
        if (!usuarioActual.isPremium()) {
            leerEnLinea(libro);
            return;
        }
        System.out.println("\n" + libro.getTitulo());
        System.out.println("1. Leer en linea");
        System.out.println("2. Descargar libro");
        System.out.println("0. Volver");
        System.out.print("Opcion: ");
        int op = leerEntero();
        if (op == 1) {
            leerEnLinea(libro);
        } else if (op == 2) {
            descargar(libro);
        }
    }

    // Este método guarda la copia cifrada local, prepara la clave, descifra el contenido y lo muestra en consola.
    private void leerEnLinea(Libro libro) {
        try {
            String rutaCopia = copiarLibroCifrado(libro);
            Cifrador3DES cifrador = negociarClave();
            String texto = cifrador.descifrar(ArchivoUtil.leerTexto(rutaCopia));
            System.out.println("\n----- " + libro.getTitulo() + " -----");
            System.out.println(texto);
            System.out.println("----- Fin del libro -----");
        } catch (IOException e) {
            System.out.println("No se pudo leer el libro: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    // Este método guarda la copia cifrada, descifra el libro y almacena una versión comprimida con Huffman.
    private void descargar(Libro libro) {
        try {
            String rutaCopia = copiarLibroCifrado(libro);
            Cifrador3DES cifrador = negociarClave();
            String texto = cifrador.descifrar(ArchivoUtil.leerTexto(rutaCopia));
            String comprimido = Huffman.comprimir(texto);
            String rutaDescarga = rutaDescarga(libro.getIsbn());
            ArchivoUtil.escribirTexto(rutaDescarga, comprimido);

            System.out.println("Libro descargado y comprimido en: " + rutaDescarga);
            System.out.println("Tamano original: " + texto.length() + " caracteres, comprimido: "
                    + comprimido.length() + " caracteres.");
            System.out.println("La copia cifrada se conserva en: " + rutaCopia);
        } catch (IOException e) {
            System.out.println("No se pudo descargar el libro: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println(e.getMessage());
        }
    }

    // Este método comprueba que el usuario sea Premium y permite descomprimir y mostrar una descarga local.
    private void lecturaLocal() {
        if (!usuarioActual.isPremium()) {
            System.out.println("La lectura local es solo para usuarios Premium.");
            return;
        }
        File carpeta = new File(ArchivoUtil.unir(config.getRutaLocal(), "descargas"));
        String[] archivos = carpeta.list();
        ListaEnlazada<String> isbns = new ListaEnlazada<>();
        if (archivos != null) {
            for (int i = 0; i < archivos.length; i++) {
                if (archivos[i].endsWith(".huf.txt")) {
                    isbns.agregar(archivos[i].substring(0, archivos[i].length() - ".huf.txt".length()));
                }
            }
        }
        if (isbns.estaVacia()) {
            System.out.println("No hay libros descargados todavia.");
            return;
        }
        System.out.println("\n===== LIBROS DESCARGADOS =====");
        for (int i = 0; i < isbns.getTamano(); i++) {
            String isbn = isbns.obtener(i);
            System.out.println((i + 1) + ". " + tituloDe(isbn) + " (ISBN " + isbn + ")");
        }
        System.out.println("0. Volver");
        System.out.print("Seleccione un libro: ");
        int eleccion = leerEntero();
        String isbn = isbns.obtener(eleccion - 1);
        if (isbn == null) {
            return;
        }
        try {
            String comprimido = ArchivoUtil.leerTexto(rutaDescarga(isbn));
            String texto = Huffman.descomprimir(comprimido);
            System.out.println("\n----- " + tituloDe(isbn) + " -----");
            System.out.println(texto);
            System.out.println("----- Fin del libro -----");
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("No se pudo leer la copia local: " + e.getMessage());
        }
    }

    // Este método lee el libro cifrado del servidor y lo escribe en copias_cifradas del cliente. La copia permanece disponible.
    private String copiarLibroCifrado(Libro libro) throws IOException {
        String cifrado = servidor.leerLibroCifrado(libro.getIsbn());
        String destino = ArchivoUtil.unir(ArchivoUtil.unir(config.getRutaLocal(), "copias_cifradas"),
                libro.getIsbn() + ".txt");
        ArchivoUtil.escribirTexto(destino, cifrado);
        return destino;
    }

    // Este método simula ambos lados del cálculo Diffie-Hellman dentro del cliente y prepara el cifrador con el secreto obtenido.
    private Cifrador3DES negociarClave() {
        BigInteger publicaCliente = DiffieHellman.calcularPublica(DiffieHellman.PRIVADA_CLIENTE);
        BigInteger publicaServidor = DiffieHellman.calcularPublica(DiffieHellman.PRIVADA_SERVIDOR);
        BigInteger secreto = DiffieHellman.calcularSecreto(publicaServidor, DiffieHellman.PRIVADA_CLIENTE);
        System.out.println("[Diffie-Hellman] Clave publica del cliente:  " + resumen(publicaCliente));
        System.out.println("[Diffie-Hellman] Clave publica del servidor: " + resumen(publicaServidor));
        System.out.println("[Diffie-Hellman] Secreto compartido:         " + resumen(secreto));
        return new Cifrador3DES(secreto);
    }

    // Este método abrevia un número en hexadecimal para mostrarlo en consola.
    private String resumen(BigInteger n) {
        String s = n.toString(16);
        return s.substring(0, 16) + "..." + s.substring(s.length() - 8);
    }

    // Este método construye la ruta local del archivo del libro comprimido con extensión .huf.txt.
    private String rutaDescarga(String isbn) {
        return ArchivoUtil.unir(ArchivoUtil.unir(config.getRutaLocal(), "descargas"), isbn + ".huf.txt");
    }

    // Este método busca el título asociado al ISBN; si no existe, devuelve un texto indicando que está fuera del catálogo.
    private String tituloDe(String isbn) {
        for (int i = 0; i < catalogo.getTamano(); i++) {
            if (catalogo.obtener(i).getIsbn().equals(isbn)) {
                return catalogo.obtener(i).getTitulo();
            }
        }
        return "(libro fuera del catalogo)";
    }

    // Este método convierte la entrada de consola a un entero y devuelve -1 si el formato no es válido.
    private int leerEntero() {
        try {
            return Integer.parseInt(teclado.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}