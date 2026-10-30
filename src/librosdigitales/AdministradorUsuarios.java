package librosdigitales;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.NoSuchElementException;

// Esta herramienta se ejecuta localmente en PC1. Premium no concede permisos administrativos.
// El acceso administrativo depende de los permisos Windows sobre la configuración privada y los datos.
public final class AdministradorUsuarios {
    static { System.setProperty("jdk.console", "java.base"); }
    private final Console consola = System.console();
    private RepositorioUsuarios repositorio;

    // Usa el archivo privado del servidor para localizar los datos sin imprimir sus claves.
    public static void main(String[] args) {
        try { new AdministradorUsuarios().iniciar(args.length > 0 ? args[0] : "servidor-config-v2.txt"); }
        catch (NoSuchElementException e) { System.out.println("Entrada cerrada. Administración finalizada."); }
    }

    public void iniciar(String archivoConfiguracion) {
        if (consola == null) {
            System.out.println("Abre una terminal interactiva en PC1 y ejecuta: java -cp bin librosdigitales.AdministradorUsuarios");
            System.out.println("Se necesita una consola para introducir contraseñas sin mostrarlas.");
            return;
        }
        try {
            Path config = Path.of(archivoConfiguracion).toRealPath();
            String contenido = ArchivoUtil.leerTexto(config.toString());
            Path datos = Path.of(Configuracion.valor(contenido, "rutaDatos", null)).toRealPath();
            if (config.startsWith(datos)) throw new IOException("La configuración privada debe estar fuera de la carpeta compartida.");
            Path administracion = config.getParent().resolve("administracion_usuarios");
            repositorio = new RepositorioUsuarios(datos.toString(), administracion.toString());
            repositorio.listar();
            System.out.println("Administración local de: " + datos);
            System.out.println("Respaldos, historial y reportes: " + administracion);
            menu();
        } catch (IOException e) { System.out.println("No se pudo abrir la administración: " + e.getMessage()); }
    }

    // Cada operación vuelve a leer los datos; se puede mantener abierto el servicio de libros.
    private void menu() {
        while (true) {
            System.out.println("\n===== ADMINISTRACIÓN DE USUARIOS =====");
            System.out.println("1. Listar usuarios\n2. Buscar usuario\n3. Crear usuario\n4. Cambiar categoría Premium/Estándar");
            System.out.println("5. Restablecer contraseña\n6. Suspender usuario\n7. Reactivar usuario\n8. Eliminar usuario");
            System.out.println("9. Exportar reporte\n10. Ver últimos movimientos\n0. Salir");
            String opcion = leer("Opción: ").trim();
            if (opcion.equals("0")) { System.out.println("Administración finalizada. El servidor continúa en su propia terminal."); return; }
            try {
                switch (opcion) {
                    case "1": listar(); break;
                    case "2": buscar(); break;
                    case "3": crear(); break;
                    case "4": cambiarCategoria(); break;
                    case "5": restablecer(); break;
                    case "6": cambiarSuspension(true); break;
                    case "7": cambiarSuspension(false); break;
                    case "8": eliminar(); break;
                    case "9": System.out.println("Reporte creado: " + repositorio.exportarReporte()); break;
                    case "10": System.out.println(repositorio.ultimosMovimientos()); break;
                    default: System.out.println("Opción no válida.");
                }
            } catch (IOException e) { System.out.println("No se completó la operación: " + e.getMessage()); }
        }
    }

    // Muestra categoría y estado, nunca contraseñas ni hashes.
    private void listar() throws IOException {
        ListaEnlazada<RepositorioUsuarios.FichaUsuario> usuarios = repositorio.listar();
        int premium = 0, suspendidos = 0;
        System.out.println("\nUsuario | Categoría | Estado");
        for (int i = 0; i < usuarios.getTamano(); i++) {
            RepositorioUsuarios.FichaUsuario ficha = usuarios.obtener(i);
            mostrar(ficha);
            if (ficha.isPremium()) premium++;
            if (ficha.isSuspendido()) suspendidos++;
        }
        System.out.println("Total: " + usuarios.getTamano() + "; Premium: " + premium
                + "; Estándar: " + (usuarios.getTamano() - premium) + "; Suspendidos: " + suspendidos);
    }
    private void buscar() throws IOException {
        RepositorioUsuarios.FichaUsuario ficha = repositorio.buscar(leer("Nombre exacto: ").trim());
        if (ficha == null) System.out.println("No existe ese usuario."); else mostrar(ficha);
    }
    private void mostrar(RepositorioUsuarios.FichaUsuario ficha) {
        System.out.println(ficha.getNombre() + " | " + (ficha.isPremium() ? "Premium" : "Estándar")
                + " | " + (ficha.isSuspendido() ? "Suspendido" : "Activo"));
    }

    private void crear() throws IOException {
        String nombre = leerNombre();
        if (repositorio.buscar(nombre) != null) { System.out.println("El usuario ya existe."); return; }
        Boolean premium = leerCategoria();
        if (premium == null) return;
        char[] clave = pedirContrasena();
        if (clave == null) return;
        try {
            if (!confirmar("Crear " + nombre + " como " + (premium ? "Premium" : "Estándar") + "?")) return;
            resultado(repositorio.crear(nombre, clave, premium));
        } finally { Arrays.fill(clave, '\0'); }
    }
    private void cambiarCategoria() throws IOException {
        String nombre = leerNombre();
        RepositorioUsuarios.FichaUsuario ficha = repositorio.buscar(nombre);
        if (ficha == null) { System.out.println("El usuario no existe."); return; }
        mostrar(ficha);
        Boolean premium = leerCategoria();
        if (premium == null || !confirmar("Cambiar la categoría de " + nombre + "?")) return;
        resultado(repositorio.cambiarCategoria(nombre, premium));
        System.out.println("El usuario debe cerrar e iniciar sesión para actualizar las opciones de su menú.");
    }
    private void restablecer() throws IOException {
        String nombre = leerNombre();
        if (repositorio.buscar(nombre) == null) { System.out.println("El usuario no existe."); return; }
        char[] clave = pedirContrasena();
        if (clave == null) return;
        try {
            if (confirmar("Restablecer la contraseña de " + nombre + "?")) resultado(repositorio.restablecerContrasena(nombre, clave));
        } finally { Arrays.fill(clave, '\0'); }
    }
    private void cambiarSuspension(boolean suspender) throws IOException {
        String nombre = leerNombre();
        RepositorioUsuarios.FichaUsuario ficha = repositorio.buscar(nombre);
        if (ficha == null) { System.out.println("El usuario no existe."); return; }
        mostrar(ficha);
        if (!confirmar((suspender ? "Suspender " : "Reactivar ") + nombre + "?")) return;
        resultado(suspender ? repositorio.suspender(nombre) : repositorio.reactivar(nombre));
    }
    private void eliminar() throws IOException {
        String nombre = leerNombre();
        RepositorioUsuarios.FichaUsuario ficha = repositorio.buscar(nombre);
        if (ficha == null) { System.out.println("El usuario no existe."); return; }
        mostrar(ficha);
        System.out.println("Se eliminará la cuenta del servidor. Sus archivos descargados no se borran remotamente.");
        if (!leer("Escribe ELIMINAR " + nombre + " para confirmar: ").equals("ELIMINAR " + nombre)) {
            System.out.println("Eliminación cancelada."); return;
        }
        resultado(repositorio.eliminar(nombre));
    }

    // Permite cancelar sin asignar una categoría por una entrada inválida.
    private Boolean leerCategoria() {
        while (true) {
            String opcion = leer("Categoría: 1. Premium | 2. Estándar | 0. Cancelar: ").trim();
            if (opcion.equals("1")) return true;
            if (opcion.equals("2")) return false;
            if (opcion.equals("0")) { System.out.println("Operación cancelada."); return null; }
            System.out.println("Selecciona 1, 2 o 0.");
        }
    }
    private String leerNombre() throws IOException {
        String nombre = leer("Nombre de usuario: ").trim();
        if (!RepositorioUsuarios.nombreValido(nombre) || nombre.equalsIgnoreCase("SALIR"))
            throw new IOException("Nombre inválido. No uses espacios, separadores de ruta ni SALIR.");
        return nombre;
    }

    // Solicita dos veces la contraseña invisible y limpia ambos arreglos cuando ya no se necesitan.
    private char[] pedirContrasena() {
        char[] clave = consola.readPassword("Nueva contraseña (6 a 128 caracteres): ");
        if (clave == null) throw new NoSuchElementException();
        char[] repetida = null;
        try {
            repetida = consola.readPassword("Confirma la contraseña: ");
            if (repetida == null) throw new NoSuchElementException();
            if (clave.length < 6 || clave.length > 128 || !Arrays.equals(clave, repetida)) {
                System.out.println("Las contraseñas no coinciden o su longitud es inválida. Operación cancelada.");
                Arrays.fill(clave, '\0'); return null;
            }
            return clave;
        } catch (RuntimeException e) { Arrays.fill(clave, '\0'); throw e; }
        finally { if (repetida != null) Arrays.fill(repetida, '\0'); }
    }
    private boolean confirmar(String mensaje) {
        boolean si = leer(mensaje + " Escribe SI para confirmar: ").trim().equalsIgnoreCase("SI");
        if (!si) System.out.println("Operación cancelada.");
        return si;
    }
    private String leer(String mensaje) {
        String texto = consola.readLine("%s", mensaje);
        if (texto == null) throw new NoSuchElementException();
        return texto;
    }
    private void resultado(boolean cambio) {
        System.out.println(cambio ? "Cambio guardado. Se creó un respaldo y se registró el movimiento." : "La cuenta ya tenía ese valor; no se realizaron cambios.");
    }
}