package librosdigitales;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;

// Esta clase controla los menús del cliente y permite iniciar sin conexión con una cuenta local preparada.
public final class LibrosDigitales {
    private final Scanner teclado=new Scanner(System.in);
    private Configuracion config;
    private ConexionServidor conexion;
    private RepositorioLocal local;
    private AutenticacionLocal autenticacionLocal;
    private ListaEnlazada<Libro> catalogo=new ListaEnlazada<>();
    private Usuario usuarioActual;
    private boolean sinConexion;

    public LibrosDigitales() { }
    public static void main(String[] args) {
        String archivo="config.txt"; boolean offline=false;
        for(String argumento:args) { if(argumento.equals("--sin-conexion")) offline=true; else archivo=argumento; }
        try { new LibrosDigitales().iniciar(archivo,offline); }
        catch(NoSuchElementException e) { System.out.println("Entrada cerrada. Hasta luego."); }
    }
    public void iniciar() { iniciar("config.txt",false); }

    // Primero carga archivos locales; una conexión fallida no impide mostrar el inicio de sesión local.
    public void iniciar(String archivo,boolean forzarLocal) {
        try {
            config=Configuracion.cargar(archivo);
            conexion=new ConexionServidor(config);
            local=new RepositorioLocal(config.getRutaLocal());
            autenticacionLocal=new AutenticacionLocal(local);
            sinConexion=forzarLocal;
            if(!sinConexion) {
                try {
                    catalogo=conexion.consultarCatalogo();
                    System.out.println("Servicio conectado: "+config.getHost()+":"+config.getPuerto()+". Identidad verificada y DH real completado.");
                } catch(ConexionServidor.NoDisponibleException e) { sinConexion=true; }
            }
        } catch(IOException e) {
            System.out.println("No se pudo iniciar: "+e.getMessage());
            System.out.println("Directorio de ejecución: "+Path.of("").toAbsolutePath()); return;
        }
        if(sinConexion) System.out.println("MODO SIN CONEXIÓN: solo cuentas Premium preparadas y libros ya descargados.");
        boolean seguir=true;
        while(seguir && pantallaLogin()) seguir=menuPrincipal();
        System.out.println("Hasta luego.");
    }

    // En línea, el servidor decide la categoría. Sin red se verifica la cuenta local protegida.
    private boolean pantallaLogin() {
        usuarioActual=null;
        while(true) {
            System.out.println("\n===== INICIO DE SESIÓN "+(sinConexion?"LOCAL":"EN LÍNEA")+" =====");
            System.out.print("Usuario (SALIR para terminar): "); String nombre=teclado.nextLine().trim();
            if(nombre.equalsIgnoreCase("SALIR")) return false;
            if(!nombre.matches("[\\p{L}\\p{N}_.-]{1,64}")) { System.out.println("Nombre de usuario inválido."); continue; }
            System.out.print("Contraseña: "); char[] contrasena=teclado.nextLine().toCharArray();
            try {
                if(sinConexion) entrarLocal(nombre,contrasena);
                else {
                    try {
                        Usuario usuario=conexion.autenticar(nombre,HashUtil.sha256(new String(contrasena)));
                        ListaEnlazada<Libro> actual=conexion.consultarCatalogo();
                        usuarioActual=usuario; catalogo=actual;
                        try {
                            autenticacionLocal.preparar(usuario,contrasena,catalogo);
                            if(usuario.isPremium()) System.out.println("Acceso local Premium preparado en esta computadora.");
                        } catch(IOException e) { System.out.println("Sesión válida; no se pudo actualizar el acceso local: "+e.getMessage()); }
                    } catch(ConexionServidor.NoDisponibleException e) {
                        System.out.println("Servidor inaccesible. Se comprobará el acceso local previamente preparado.");
                        sinConexion=true; entrarLocal(nombre,contrasena);
                    }
                }
                System.out.println("Bienvenido, "+usuarioActual.getNombre()+(usuarioActual.isPremium()?" (Premium)":" (Estándar)"));
                return true;
            } catch(IOException e) { usuarioActual=null; System.out.println(e.getMessage()); }
            finally { Arrays.fill(contrasena,'\0'); }
        }
    }
    private void entrarLocal(String nombre,char[] contrasena) throws IOException {
        AutenticacionLocal.CuentaLocal cuenta=autenticacionLocal.autenticar(nombre,contrasena);
        usuarioActual=cuenta.getUsuario(); catalogo=cuenta.getCatalogo();
    }

    // Conserva las cuatro opciones del enunciado y restringe las operaciones según el modo.
    private boolean menuPrincipal() {
        while(true) {
            System.out.println("\n===== MENÚ PRINCIPAL "+(sinConexion?"[SIN CONEXIÓN]":"[EN LÍNEA]")+" =====");
            System.out.println("1. Explorar catálogo\n2. Lectura local\n3. Cerrar sesión\n4. Salir");
            System.out.print("Opción: ");
            switch(teclado.nextLine().trim()) {
                case "1":
                    if(sinConexion) System.out.println("Sin conexión, utiliza Lectura local para abrir las descargas.");
                    else explorarCatalogo();
                    break;
                case "2": lecturaLocal(); break;
                case "3": usuarioActual=null; System.out.println("Sesión cerrada."); return true;
                case "4": usuarioActual=null; return false;
                default: System.out.println("Opción no válida.");
            }
        }
    }
    private void explorarCatalogo() {
        while(true) {
            System.out.println("\n===== CATÁLOGO =====");
            for(int i=0;i<catalogo.getTamano();i++) {
                Libro l=catalogo.obtener(i); System.out.println((i+1)+". "+l.getTitulo()+" (ISBN "+l.getIsbn()+")");
            }
            System.out.print("0. Volver\nSeleccione un libro: "); int n=leerEntero();
            if(n==0) return;
            Libro libro=catalogo.obtener(n-1);
            if(libro==null) { System.out.println("Selección inválida."); continue; }
            boolean descarga=false;
            if(usuarioActual.isPremium()) {
                System.out.print("1. Leer en línea\n2. Descargar libro\n0. Volver\nOpción: "); int opcion=leerEntero();
                if(opcion==0) continue;
                if(opcion!=1 && opcion!=2) { System.out.println("Opción inválida."); continue; }
                descarga=opcion==2;
            }
            try {
                String texto=conexion.obtenerLibro(usuarioActual,libro,descarga,local);
                System.out.println("[DH real] Transferencia verificada. Se conservaron el original y la entrega cifrados.");
                if(descarga) {
                    Path archivo=local.guardarDescarga(usuarioActual.getNombre(),libro.getIsbn(),texto);
                    System.out.println("Descarga Huffman verificada: "+archivo);
                    long original=texto.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                    System.out.println("Tamaño original UTF-8: "+original+" bytes; archivo Huffman: "+java.nio.file.Files.size(archivo)+" bytes.");
                } else mostrarLibro(libro.getTitulo(),texto);
            } catch(IOException | IllegalArgumentException e) {
                System.out.println("No se completó la operación: "+e.getMessage());
                if(e instanceof ConexionServidor.NoDisponibleException) {
                    System.out.println("Puedes continuar con Lectura local; el modo de acceso se revisará al reiniciar."); return;
                }
            }
        }
    }

    // Un archivo dañado se informa y no interrumpe la sesión ni el menú.
    private void lecturaLocal() {
        if(!usuarioActual.isPremium()) { System.out.println("La lectura local es exclusiva de Premium."); return; }
        try {
            ListaEnlazada<String> isbns=local.listarDescargas(usuarioActual.getNombre());
            if(isbns.estaVacia()) { System.out.println("No hay descargas para este usuario en esta computadora."); return; }
            for(int i=0;i<isbns.getTamano();i++) System.out.println((i+1)+". "+tituloDe(isbns.obtener(i))+" ("+isbns.obtener(i)+")");
            System.out.print("0. Volver\nSeleccione: "); int n=leerEntero();
            if(n==0) return;
            String isbn=isbns.obtener(n-1);
            if(isbn==null) { System.out.println("Selección inválida."); return; }
            mostrarLibro(tituloDe(isbn),local.leerDescarga(usuarioActual.getNombre(),isbn));
        } catch(IOException | IllegalArgumentException e) { System.out.println("No se pudo leer la descarga: "+e.getMessage()); }
    }
    private String tituloDe(String isbn) {
        for(int i=0;i<catalogo.getTamano();i++) if(catalogo.obtener(i).getIsbn().equals(isbn)) return catalogo.obtener(i).getTitulo();
        return "Libro fuera del catálogo actual";
    }
    private void mostrarLibro(String titulo,String texto) {
        System.out.println("\n----- "+titulo+" -----\n"+texto+"\n----- Fin del libro -----");
    }
    private int leerEntero() {
        try { return Integer.parseInt(teclado.nextLine().trim()); }
        catch(NumberFormatException e) { return -1; }
    }
}
