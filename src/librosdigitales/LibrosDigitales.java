package librosdigitales;

import java.io.Console;
import java.io.IOError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.NoSuchElementException;

// Esta clase presenta una interfaz de consola para autenticación, catálogo y lectura.
// Mantiene los servicios y formatos del proyecto V2 y lee las contraseñas sin eco.
public final class LibrosDigitales {
    // La consola nativa evita diferencias del proveedor JLine en Windows con JDK recientes.
    static { System.setProperty("jdk.console", "java.base"); }

    private static final String BORDE = "======================================================================";
    private static final int LINEAS_POR_PAGINA = 20;
    private static final int ANCHO_LECTURA = 78;
    private final Console consola = System.console();
    private Configuracion config;
    private ConexionServidor conexion;
    private RepositorioLocal local;
    private AutenticacionLocal autenticacionLocal;
    private ListaEnlazada<Libro> catalogo = new ListaEnlazada<>();
    private Usuario usuarioActual;
    private boolean sinConexion;

    public LibrosDigitales() { }

    // Se conservan config.txt por defecto y el argumento opcional --sin-conexion.
    public static void main(String[] args) {
        String archivo = "config.txt";
        boolean modoLocal = false;
        for (String argumento : args) {
            if (argumento.equals("--sin-conexion")) modoLocal = true;
            else archivo = argumento;
        }
        try { new LibrosDigitales().iniciar(archivo, modoLocal); }
        catch (NoSuchElementException e) { System.out.println("\nEntrada cerrada. Gracias por visitar tu biblioteca."); }
        catch (IOError e) { System.out.println("\nNo se pudo leer la terminal. Abre una terminal interactiva y ejecuta el cliente nuevamente."); }
    }
    public void iniciar() { iniciar("config.txt", false); }

    // La falta de red permite utilizar las descargas de una cuenta local previamente preparada.
    public void iniciar(String archivo, boolean forzarLocal) {
        if (consola == null) {
            System.out.println("Para proteger tu contraseña, abre una terminal interactiva.");
            System.out.println("Desde la raíz del proyecto ejecuta:");
            System.out.println("java -cp bin librosdigitales.LibrosDigitales");
            System.out.println("Usa Terminal > Nueva terminal; evita la consola de depuración y la entrada redirigida.");
            return;
        }
        titulo("LIBROS DIGITALES", "Tu biblioteca, donde estés.");
        System.out.println("Preparando tu biblioteca...");
        try {
            config = Configuracion.cargar(archivo);
            conexion = new ConexionServidor(config);
            local = new RepositorioLocal(config.getRutaLocal());
            autenticacionLocal = new AutenticacionLocal(local);
            sinConexion = forzarLocal;
            if (!sinConexion) {
                try {
                    catalogo = conexion.consultarCatalogo();
                    mensaje("LISTO", "Conexión segura establecida. Tu catálogo está disponible.");
                } catch (ConexionServidor.NoDisponibleException e) {
                    sinConexion = true;
                    mensaje("SIN CONEXIÓN", "No pudimos contactar la biblioteca. Puedes usar tus descargas preparadas.");
                }
            }
        } catch (IOException e) {
            mensaje("NO SE PUDO ABRIR", e.getMessage());
            System.out.println("Revisa la configuración: " + Path.of(archivo).toAbsolutePath());
            return;
        }
        if (sinConexion) System.out.println("El acceso local requiere una cuenta Premium autenticada anteriormente en este equipo.");
        boolean continuar = true;
        while (continuar && pantallaLogin()) continuar = menuPrincipal();
        titulo("HASTA PRONTO", "Gracias por visitar tu biblioteca.");
    }

    // Console lee usuario y contraseña para evitar mezclar lectores de entrada.
    private boolean pantallaLogin() {
        usuarioActual = null;
        while (true) {
            titulo("INICIAR SESIÓN", sinConexion ? "Lectura sin conexión" : "Bienvenido a tu biblioteca");
            System.out.println("Escribe SALIR en Usuario para cerrar la aplicación.");
            String nombre = leer("Usuario: ").trim();
            if (nombre.equalsIgnoreCase("SALIR")) return false;
            if (!nombre.matches("[\\p{L}\\p{N}_.-]{1,64}")) {
                mensaje("REVISA EL USUARIO", "Usa letras, números, punto, guion o guion bajo, sin espacios.");
                continue;
            }
            System.out.println("La contraseña permanece invisible. Pulsa Enter al terminar.");
            char[] contrasena = consola.readPassword("Contraseña: ");
            if (contrasena == null) return false;
            try {
                if (contrasena.length == 0) {
                    mensaje("FALTA LA CONTRASEÑA", "Introduce tu contraseña para continuar.");
                    continue;
                }
                System.out.println("Comprobando tus datos...");
                if (sinConexion) entrarLocal(nombre, contrasena);
                else {
                    try {
                        Usuario usuario = conexion.autenticar(nombre, HashUtil.sha256(new String(contrasena)));
                        ListaEnlazada<Libro> actual = conexion.consultarCatalogo();
                        usuarioActual = usuario;
                        catalogo = actual;
                        try {
                            autenticacionLocal.preparar(usuario, contrasena, catalogo);
                            if (usuario.isPremium()) mensaje("ACCESO LOCAL LISTO", "Puedes entrar sin conexión y leer los libros que descargues en este equipo.");
                        } catch (IOException e) {
                            mensaje("AVISO", "Tu sesión está abierta, pero no pudimos actualizar el acceso sin conexión: " + e.getMessage());
                        }
                    } catch (ConexionServidor.NoDisponibleException e) {
                        sinConexion = true;
                        mensaje("SIN CONEXIÓN", "Intentaremos acceder con la cuenta local previamente preparada.");
                        entrarLocal(nombre, contrasena);
                    }
                }
                mensaje("BIENVENIDO", usuarioActual.getNombre() + ". Tu cuenta es " + (usuarioActual.isPremium() ? "Premium." : "Estándar."));
                return true;
            } catch (IOException e) {
                usuarioActual = null;
                mensaje("NO PUDIMOS INICIAR SESIÓN", e.getMessage());
                System.out.println("Revisa tus datos e inténtalo nuevamente.");
            } finally {
                // Limpia el arreglo incluso cuando falla la autenticación.
                Arrays.fill(contrasena, '\0');
            }
        }
    }

    private void entrarLocal(String nombre, char[] contrasena) throws IOException {
        AutenticacionLocal.CuentaLocal cuenta = autenticacionLocal.autenticar(nombre, contrasena);
        usuarioActual = cuenta.getUsuario();
        catalogo = cuenta.getCatalogo();
    }

    // Conserva las cuatro opciones del enunciado y explica su disponibilidad.
    private boolean menuPrincipal() {
        while (true) {
            titulo("TU BIBLIOTECA", "¿Qué te gustaría hacer?");
            mostrarSesion();
            System.out.println();
            System.out.println("  1. Explorar catálogo" + (sinConexion ? "  [requiere conexión]" : ""));
            System.out.println("     Consulta los títulos disponibles y elige tu próxima lectura.");
            System.out.println("  2. Mis libros descargados" + (usuarioActual.isPremium() ? "" : "  [solo Premium]"));
            System.out.println("     Abre una descarga guardada en esta computadora.");
            System.out.println("  3. Cerrar sesión");
            System.out.println("     Permite que otra persona acceda con su cuenta.");
            System.out.println("  4. Salir de la aplicación\n");
            switch (leerOpcion("Elige una opción [1-4]: ", 1, 4)) {
                case 1:
                    if (sinConexion) {
                        mensaje("CATÁLOGO NO DISPONIBLE", "Utiliza la opción 2 para leer tus descargas. Para volver en línea, reconecta y reinicia la aplicación.");
                        pausa();
                    } else explorarCatalogo();
                    break;
                case 2: lecturaLocal(); break;
                case 3:
                    usuarioActual = null;
                    mensaje("SESIÓN CERRADA", "Puedes iniciar sesión con otra cuenta.");
                    return true;
                case 4: usuarioActual = null; return false;
                default: break;
            }
        }
    }

    // Una selección inválida se corrige en el mismo paso.
    private void explorarCatalogo() {
        while (true) {
            titulo("EXPLORAR CATÁLOGO", "Elige un título para comenzar.");
            mostrarSesion();
            if (catalogo.estaVacia()) {
                mensaje("CATÁLOGO VACÍO", "Todavía no hay libros disponibles.");
                pausa(); return;
            }
            System.out.println();
            for (int i = 0; i < catalogo.getTamano(); i++) mostrarFicha(i + 1, catalogo.obtener(i));
            System.out.println("  0. Volver a tu biblioteca\n");
            int seleccion = leerOpcion("Número del libro [0-" + catalogo.getTamano() + "]: ", 0, catalogo.getTamano());
            if (seleccion == 0) return;
            Libro libro = catalogo.obtener(seleccion - 1);
            boolean descarga = false;
            if (usuarioActual.isPremium()) {
                titulo("TU PRÓXIMA LECTURA", libro.getTitulo());
                System.out.println("ISBN: " + libro.getIsbn());
                System.out.println("\n  1. Leer ahora\n  2. Descargar para leer sin conexión\n  0. Volver al catálogo\n");
                int accion = leerOpcion("¿Cómo quieres disfrutar este libro? [0-2]: ", 0, 2);
                if (accion == 0) continue;
                descarga = accion == 2;
            }
            System.out.println(descarga ? "Preparando tu descarga..." : "Abriendo tu libro...");
            try {
                String texto = conexion.obtenerLibro(usuarioActual, libro, descarga, local);
                if (descarga) {
                    Path archivo = local.guardarDescarga(usuarioActual.getNombre(), libro.getIsbn(), texto);
                    mensaje("DESCARGA COMPLETA", libro.getTitulo() + " ya está disponible en Mis libros descargados.");
                    System.out.println("Guardado en: " + archivo);
                    long original = texto.getBytes(StandardCharsets.UTF_8).length;
                    System.out.println("Texto original: " + original + " bytes | Descarga: " + Files.size(archivo) + " bytes");
                    pausa();
                } else mostrarLibro(libro.getTitulo(), texto);
            } catch (IOException | IllegalArgumentException e) {
                mensaje("NO SE PUDO ABRIR EL LIBRO", e.getMessage());
                if (e instanceof ConexionServidor.NoDisponibleException) {
                    System.out.println("Puedes regresar a tu biblioteca y utilizar las descargas que ya tengas.");
                    pausa(); return;
                }
                System.out.println("Si cambió tu contraseña o categoría, cierra sesión y vuelve a ingresar.");
                pausa();
            }
        }
    }

    // Solo enumera las descargas del usuario autenticado y controla los archivos dañados.
    private void lecturaLocal() {
        if (!usuarioActual.isPremium()) {
            mensaje("FUNCIÓN PREMIUM", "Tu cuenta Estándar permite leer en línea. Las descargas y su lectura local requieren Premium.");
            pausa(); return;
        }
        while (true) {
            titulo("MIS LIBROS DESCARGADOS", "Tus lecturas disponibles en esta computadora.");
            try {
                ListaEnlazada<String> isbns = local.listarDescargas(usuarioActual.getNombre());
                if (isbns.estaVacia()) {
                    mensaje("TU ESTANTE ESTÁ VACÍO", "Con conexión, entra al catálogo y elige Descargar en un libro.");
                    pausa(); return;
                }
                for (int i = 0; i < isbns.getTamano(); i++) {
                    String isbn = isbns.obtener(i);
                    mostrarFicha(i + 1, new Libro(isbn, tituloDe(isbn)));
                }
                System.out.println("  0. Volver a tu biblioteca\n");
                int opcion = leerOpcion("Número del libro [0-" + isbns.getTamano() + "]: ", 0, isbns.getTamano());
                if (opcion == 0) return;
                String isbn = isbns.obtener(opcion - 1);
                System.out.println("Abriendo tu descarga...");
                try { mostrarLibro(tituloDe(isbn), local.leerDescarga(usuarioActual.getNombre(), isbn)); }
                catch (IOException | IllegalArgumentException e) {
                    mensaje("NO SE PUDO LEER LA DESCARGA", e.getMessage());
                    System.out.println("Si el archivo está dañado, vuelve a descargarlo cuando tengas conexión.");
                    pausa();
                }
            } catch (IOException e) {
                mensaje("NO SE PUDIERON CONSULTAR TUS DESCARGAS", e.getMessage());
                pausa(); return;
            }
        }
    }

    private String tituloDe(String isbn) {
        for (int i = 0; i < catalogo.getTamano(); i++)
            if (catalogo.obtener(i).getIsbn().equals(isbn)) return catalogo.obtener(i).getTitulo();
        return "Libro descargado";
    }

    // Presenta páginas de 20 líneas, sin modificar el contenido guardado.
    private void mostrarLibro(String nombre, String texto) {
        titulo("LECTURA", nombre);
        if (texto.isEmpty()) {
            mensaje("LIBRO SIN CONTENIDO", "Este archivo no contiene texto.");
            pausa(); return;
        }
        int impresas = 0, pagina = 1;
        String[] lineas = texto.split("\\R", -1);
        for (int i = 0; i < lineas.length; i++) {
            String restante = lineas[i];
            do {
                int cantidad = restante.codePointCount(0, restante.length());
                int corte = restante.offsetByCodePoints(0, Math.min(ANCHO_LECTURA, cantidad));
                if (corte < restante.length()) {
                    int espacio = restante.lastIndexOf(' ', corte - 1);
                    if (espacio > corte / 2) corte = espacio + 1;
                }
                System.out.println(restante.substring(0, corte));
                restante = restante.substring(corte);
                impresas++;
                if (impresas == LINEAS_POR_PAGINA && (!restante.isEmpty() || i < lineas.length - 1)) {
                    System.out.println("\n--- Página " + pagina + " ---");
                    while (true) {
                        String opcion = leer("Enter: continuar | 0: volver: ").trim();
                        if (opcion.equals("0")) return;
                        if (opcion.isEmpty()) break;
                        System.out.println("Pulsa Enter para continuar o escribe 0 para volver.");
                    }
                    impresas = 0; pagina++;
                    titulo("LECTURA - PÁGINA " + pagina, nombre);
                }
            } while (!restante.isEmpty());
        }
        System.out.println("\n--- Fin del libro. Gracias por leer. ---");
        pausa();
    }

    private void titulo(String principal, String detalle) {
        System.out.println("\n" + BORDE);
        System.out.println("  " + principal);
        if (detalle != null && !detalle.isEmpty()) System.out.println("  " + detalle);
        System.out.println(BORDE);
    }
    private void mostrarSesion() {
        System.out.println("Usuario: " + usuarioActual.getNombre() + " | Cuenta: "
                + (usuarioActual.isPremium() ? "Premium" : "Estándar") + " | "
                + (sinConexion ? "Sin conexión" : "En línea"));
    }
    private void mostrarFicha(int numero, Libro libro) {
        System.out.println("  " + numero + ". " + libro.getTitulo());
        System.out.println("     ISBN: " + libro.getIsbn() + "\n");
    }
    private void mensaje(String encabezado, String texto) {
        System.out.println("\n[" + encabezado + "] " + texto);
    }
    private void pausa() { leer("\nPulsa Enter para continuar..."); }

    // Las entradas inválidas repiten la pregunta y no expulsan al usuario del menú.
    private int leerOpcion(String pregunta, int minimo, int maximo) {
        while (true) {
            String entrada = leer(pregunta).trim();
            try {
                int numero = Integer.parseInt(entrada);
                if (numero >= minimo && numero <= maximo) return numero;
            } catch (NumberFormatException e) { /* Se solicita otra selección. */ }
            System.out.println("Escribe un número entre " + minimo + " y " + maximo + ".");
        }
    }
    private String leer(String pregunta) {
        String texto = consola.readLine("%s", pregunta);
        if (texto == null) throw new NoSuchElementException();
        return texto;
    }
}