package librosdigitales;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.UUID;

// Esta clase administra las cuentas conservando el formato usuario|hash|premium del enunciado.
// Las suspensiones se guardan aparte. Un bloqueo cubre la lectura y escritura de cada operación.
public final class RepositorioUsuarios {
    private static final Object MONITOR = new Object();
    private final Path datos;
    private final Path archivoUsuarios;
    private final Path archivoSuspendidos;
    private final Path administracion;

    // El servidor utiliza este constructor para consultar las cuentas sin habilitar cambios.
    public RepositorioUsuarios(String rutaDatos) throws IOException {
        this(rutaDatos, null);
    }

    // La herramienta administrativa guarda respaldos y auditoría fuera de la carpeta compartida.
    public RepositorioUsuarios(String rutaDatos, String rutaAdministracion) throws IOException {
        datos = Path.of(rutaDatos).toRealPath();
        archivoUsuarios = datos.resolve("usuarios.txt");
        archivoSuspendidos = datos.resolve("usuarios_suspendidos.txt");
        if (!Files.isRegularFile(archivoUsuarios)) throw new IOException("No existe usuarios.txt en " + datos);
        if (rutaAdministracion == null) administracion = null;
        else {
            Path candidata = Path.of(rutaAdministracion).toAbsolutePath().normalize();
            Files.createDirectories(candidata);
            administracion = candidata.toRealPath();
            if (administracion.startsWith(datos)) throw new IOException("Los respaldos deben estar fuera de la carpeta compartida.");
        }
    }

    // Esta ficha permite mostrar datos de administración sin exponer hashes ni contraseñas.
    public static final class FichaUsuario {
        private final String nombre;
        private final boolean premium;
        private final boolean suspendido;
        FichaUsuario(Usuario usuario, boolean suspendido) {
            nombre = usuario.getNombre(); premium = usuario.isPremium(); this.suspendido = suspendido;
        }
        public String getNombre() { return nombre; }
        public boolean isPremium() { return premium; }
        public boolean isSuspendido() { return suspendido; }
    }

    // Agrupa una lectura coherente de los archivos mientras se mantiene el bloqueo.
    private static final class Estado {
        ListaEnlazada<Usuario> usuarios;
        ListaEnlazada<String> suspendidos;
        String usuariosOriginal;
        String suspendidosOriginal;
        boolean existiaSuspendidos;
    }
    private interface Consulta<T> { T ejecutar(Estado estado) throws IOException; }
    private interface Cambio { boolean ejecutar(Estado estado) throws IOException; }

    // Todos los participantes actualizados usan el mismo cerrojo, también entre procesos Java.
    private <T> T consultar(Consulta<T> consulta) throws IOException {
        synchronized (MONITOR) {
            try (FileChannel canal = FileChannel.open(datos.resolve(".usuarios-admin.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock bloqueo = canal.lock()) {
                if (!bloqueo.isValid()) throw new IOException("No se pudo bloquear el archivo de usuarios.");
                return consulta.ejecutar(leerEstado());
            }
        }
    }

    // Lee y valida ambos archivos. No ignora registros inválidos ni elimina datos al encontrarlos.
    private Estado leerEstado() throws IOException {
        Estado e = new Estado();
        e.usuariosOriginal = ArchivoUtil.leerTexto(archivoUsuarios.toString());
        e.usuarios = analizarUsuarios(e.usuariosOriginal);
        e.existiaSuspendidos = Files.exists(archivoSuspendidos);
        e.suspendidosOriginal = e.existiaSuspendidos ? ArchivoUtil.leerTexto(archivoSuspendidos.toString()) : "";
        e.suspendidos = new ListaEnlazada<>();
        int linea = 0;
        for (String original : e.suspendidosOriginal.split("\\r?\\n", -1)) {
            linea++;
            String nombre = original.trim();
            if (nombre.isEmpty()) continue;
            if (!nombreValido(nombre) || contiene(e.suspendidos, nombre))
                throw new IOException("Registro de suspensión inválido en línea " + linea);
            e.suspendidos.agregar(nombre);
            if (e.suspendidos.getTamano() > 10000) throw new IOException("Demasiados registros de suspensión.");
        }
        return e;
    }

    // El análisis es compatible con las cuentas existentes del proyecto V2.
    public static ListaEnlazada<Usuario> analizarUsuarios(String contenido) throws IOException {
        ListaEnlazada<Usuario> lista = new ListaEnlazada<>();
        int numero = 0;
        for (String linea : contenido.split("\\r?\\n", -1)) {
            numero++;
            if (linea.trim().isEmpty()) continue;
            String[] p = linea.split("\\|", -1);
            if (p.length != 3 || !nombreValido(p[0].trim())
                    || !p[1].trim().matches("[a-fA-F0-9]{64}")
                    || !(p[2].trim().equals("true") || p[2].trim().equals("false")))
                throw new IOException("Registro de usuarios inválido en línea " + numero);
            if (encontrar(lista, p[0].trim()) != null) throw new IOException("Usuario duplicado en línea " + numero);
            lista.agregar(new Usuario(p[0].trim(), p[1].trim().toLowerCase(java.util.Locale.ROOT), Boolean.parseBoolean(p[2].trim())));
            if (lista.getTamano() > 10000) throw new IOException("Demasiados usuarios.");
        }
        return lista;
    }

    // Conserva la API de lectura utilizada por RepositorioServidor y el generador.
    public ListaEnlazada<Usuario> cargarUsuarios() throws IOException { return consultar(e -> e.usuarios); }

    // Comprueba contraseña y suspensión en la misma instantánea. Devuelve null si no autoriza.
    public Usuario autenticar(String nombre, String hash) throws IOException {
        return consultar(e -> {
            if (nombre == null || hash == null || !hash.matches("[a-fA-F0-9]{64}")) return null;
            Usuario u = encontrar(e.usuarios, nombre);
            if (u == null || contiene(e.suspendidos, nombre)) return null;
            return HashUtil.iguales(u.getHashClave().getBytes(StandardCharsets.US_ASCII),
                    hash.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII)) ? u : null;
        });
    }

    public ListaEnlazada<FichaUsuario> listar() throws IOException {
        return consultar(e -> {
            ListaEnlazada<FichaUsuario> fichas = new ListaEnlazada<>();
            for (int i = 0; i < e.usuarios.getTamano(); i++) {
                Usuario u = e.usuarios.obtener(i);
                fichas.agregar(new FichaUsuario(u, contiene(e.suspendidos, u.getNombre())));
            }
            return fichas;
        });
    }
    public FichaUsuario buscar(String nombre) throws IOException {
        return consultar(e -> {
            Usuario u = encontrar(e.usuarios, nombre);
            return u == null ? null : new FichaUsuario(u, contiene(e.suspendidos, nombre));
        });
    }

    // Crea una cuenta activa y no guarda la contraseña en claro.
    public boolean crear(String nombre, char[] contrasena, boolean premium) throws IOException {
        validarNuevoNombre(nombre); validarContrasena(contrasena);
        String hash = HashUtil.sha256(new String(contrasena));
        return cambiar("CREAR", nombre, e -> {
            if (encontrar(e.usuarios, nombre) != null) throw new IOException("El usuario ya existe.");
            if (e.usuarios.getTamano() >= 10000) throw new IOException("Se alcanzó el máximo de usuarios.");
            e.usuarios.agregar(new Usuario(nombre, hash, premium));
            e.suspendidos = quitar(e.suspendidos, nombre);
            return true;
        });
    }

    public boolean cambiarCategoria(String nombre, boolean premium) throws IOException {
        return cambiar(premium ? "CAMBIAR_A_PREMIUM" : "CAMBIAR_A_ESTANDAR", nombre, e -> {
            Usuario u = exigir(e, nombre);
            if (u.isPremium() == premium) return false;
            u.setPremium(premium); return true;
        });
    }

    // Restablecer una contraseña no reactiva una cuenta suspendida.
    public boolean restablecerContrasena(String nombre, char[] contrasena) throws IOException {
        validarContrasena(contrasena);
        String hash = HashUtil.sha256(new String(contrasena));
        return cambiar("RESTABLECER_CONTRASENA", nombre, e -> {
            Usuario u = exigir(e, nombre);
            if (u.getHashClave().equals(hash)) return false;
            u.setHashClave(hash); return true;
        });
    }

    public boolean suspender(String nombre) throws IOException {
        return cambiar("SUSPENDER", nombre, e -> {
            exigir(e, nombre);
            if (contiene(e.suspendidos, nombre)) return false;
            e.suspendidos.agregar(nombre); return true;
        });
    }
    public boolean reactivar(String nombre) throws IOException {
        return cambiar("REACTIVAR", nombre, e -> {
            exigir(e, nombre);
            if (!contiene(e.suspendidos, nombre)) return false;
            e.suspendidos = quitar(e.suspendidos, nombre); return true;
        });
    }
    public boolean eliminar(String nombre) throws IOException {
        return cambiar("ELIMINAR", nombre, e -> {
            exigir(e, nombre);
            ListaEnlazada<Usuario> nueva = new ListaEnlazada<>();
            for (int i = 0; i < e.usuarios.getTamano(); i++) {
                Usuario u = e.usuarios.obtener(i);
                if (!u.getNombre().equals(nombre)) nueva.agregar(u);
            }
            e.usuarios = nueva; e.suspendidos = quitar(e.suspendidos, nombre); return true;
        });
    }

    // El bloqueo abarca validar, respaldar, modificar y registrar; evita actualizaciones perdidas.
    private boolean cambiar(String operacion, String nombre, Cambio cambio) throws IOException {
        exigirAdministracion();
        return consultar(e -> {
            if (!cambio.ejecutar(e)) return false;
            String usuariosNuevos = serializar(e.usuarios);
            String suspendidosNuevos = serializarSuspendidos(e.suspendidos);
            Path respaldo = respaldar(e);
            String id = respaldo.getFileName().toString();
            auditar(id, "INICIADA", operacion, nombre);
            try {
                if (!usuariosNuevos.equals(e.usuariosOriginal)) ArchivoUtil.escribirTexto(archivoUsuarios.toString(), usuariosNuevos);
                if (!suspendidosNuevos.equals(e.suspendidosOriginal)) ArchivoUtil.escribirTexto(archivoSuspendidos.toString(), suspendidosNuevos);
                auditar(id, "COMPLETADA", operacion, nombre);
            } catch (IOException fallo) {
                // Si una escritura falla, se intenta restaurar ambos archivos antes de liberar el cerrojo.
                try {
                    ArchivoUtil.escribirTexto(archivoUsuarios.toString(), e.usuariosOriginal);
                    if (e.existiaSuspendidos) ArchivoUtil.escribirTexto(archivoSuspendidos.toString(), e.suspendidosOriginal);
                    else Files.deleteIfExists(archivoSuspendidos);
                    auditar(id, "REVERTIDA", operacion, nombre);
                } catch (IOException restauracion) {
                    fallo.addSuppressed(restauracion);
                    throw new IOException("No se pudo completar ni restaurar el cambio. Detén el servicio y revisa el respaldo: " + respaldo, fallo);
                }
                throw new IOException("El cambio falló y se restauraron los datos anteriores. Respaldo: " + respaldo, fallo);
            }
            return true;
        });
    }

    // Cada respaldo incluye ambos archivos y queda en una carpeta privada independiente.
    private Path respaldar(Estado e) throws IOException {
        Path carpeta = administracion.resolve("respaldos").resolve(Instant.now().toString().replace(':', '-') + "-" + UUID.randomUUID());
        ArchivoUtil.escribirTexto(carpeta.resolve("usuarios.txt").toString(), e.usuariosOriginal);
        ArchivoUtil.escribirTexto(carpeta.resolve("usuarios_suspendidos.txt").toString(), e.suspendidosOriginal);
        ArchivoUtil.escribirTexto(carpeta.resolve("LEEME.txt").toString(),
                "Instantánea anterior al cambio. Guarda estos archivos fuera de la carpeta compartida.\n"
                + "Para restaurar, detén el servicio y todas las herramientas administrativas.\n"
                + "Revisa ambos archivos antes de devolverlos a la carpeta de datos.\n");
        return carpeta;
    }

    // Registra operación, cuenta Windows y usuario afectado; no registra hashes ni contraseñas.
    private void auditar(String id, String estado, String operacion, String nombre) throws IOException {
        String operador = System.getProperty("user.name", "desconocido").replaceAll("[\\r\\n|]", "_");
        String linea = Instant.now() + "|" + id + "|" + estado + "|" + operador + "|" + operacion + "|" + nombre + "\n";
        Files.writeString(administracion.resolve("auditoria.log"), linea, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // Exporta un reporte de categoría y estado sin incluir información de autenticación.
    public Path exportarReporte() throws IOException {
        exigirAdministracion();
        return consultar(e -> {
            int premium = 0, suspendidos = 0;
            StringBuilder reporte = new StringBuilder("ADMINISTRACION DE USUARIOS\nFecha UTC: " + Instant.now() + "\n\nUsuario|Categoria|Estado\n");
            for (int i = 0; i < e.usuarios.getTamano(); i++) {
                Usuario u = e.usuarios.obtener(i);
                boolean suspendido = contiene(e.suspendidos, u.getNombre());
                if (u.isPremium()) premium++;
                if (suspendido) suspendidos++;
                reporte.append(u.getNombre()).append('|').append(u.isPremium() ? "Premium" : "Estandar")
                        .append('|').append(suspendido ? "Suspendido" : "Activo").append('\n');
            }
            reporte.append("\nTotal: ").append(e.usuarios.getTamano()).append("\nPremium: ").append(premium)
                    .append("\nEstandar: ").append(e.usuarios.getTamano() - premium)
                    .append("\nSuspendidos: ").append(suspendidos).append('\n');
            Path archivo = administracion.resolve("reportes").resolve("usuarios-" + UUID.randomUUID() + ".txt");
            ArchivoUtil.escribirTexto(archivo.toString(), reporte.toString()); return archivo;
        });
    }

    public String ultimosMovimientos() throws IOException {
        exigirAdministracion();
        return consultar(e -> {
            Path archivo = administracion.resolve("auditoria.log");
            if (!Files.exists(archivo)) return "Todavía no hay movimientos administrativos.";
            String[] lineas = ArchivoUtil.leerTexto(archivo.toString()).split("\\r?\\n");
            StringBuilder salida = new StringBuilder();
            for (int i = Math.max(0, lineas.length - 30); i < lineas.length; i++) salida.append(lineas[i]).append('\n');
            return salida.toString();
        });
    }

    private void exigirAdministracion() throws IOException {
        if (administracion == null) throw new IOException("Este repositorio se abrió solo para consultas.");
    }
    private static Usuario exigir(Estado e, String nombre) throws IOException {
        if (!nombreValido(nombre)) throw new IOException("Nombre de usuario inválido.");
        Usuario usuario = encontrar(e.usuarios, nombre);
        if (usuario == null) throw new IOException("El usuario no existe.");
        return usuario;
    }
    public static boolean nombreValido(String nombre) { return nombre != null && nombre.matches("[\\p{L}\\p{N}_.-]{1,64}"); }
    private static void validarNuevoNombre(String nombre) throws IOException {
        if (!nombreValido(nombre) || nombre.equalsIgnoreCase("SALIR"))
            throw new IOException("Usa de 1 a 64 letras, números, punto, guion o guion bajo. SALIR está reservado.");
    }
    private static void validarContrasena(char[] clave) throws IOException {
        if (clave == null || clave.length < 6 || clave.length > 128) throw new IOException("La contraseña debe tener entre 6 y 128 caracteres.");
    }
    private static Usuario encontrar(ListaEnlazada<Usuario> usuarios, String nombre) {
        for (int i = 0; i < usuarios.getTamano(); i++) if (usuarios.obtener(i).getNombre().equals(nombre)) return usuarios.obtener(i);
        return null;
    }
    private static boolean contiene(ListaEnlazada<String> lista, String nombre) {
        for (int i = 0; i < lista.getTamano(); i++) if (lista.obtener(i).equals(nombre)) return true;
        return false;
    }
    private static ListaEnlazada<String> quitar(ListaEnlazada<String> lista, String nombre) {
        ListaEnlazada<String> nueva = new ListaEnlazada<>();
        for (int i = 0; i < lista.getTamano(); i++) if (!lista.obtener(i).equals(nombre)) nueva.agregar(lista.obtener(i));
        return nueva;
    }
    private static String serializar(ListaEnlazada<Usuario> usuarios) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < usuarios.getTamano(); i++) {
            Usuario u = usuarios.obtener(i);
            texto.append(u.getNombre()).append('|').append(u.getHashClave()).append('|').append(u.isPremium()).append('\n');
        }
        return texto.toString();
    }
    private static String serializarSuspendidos(ListaEnlazada<String> lista) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < lista.getTamano(); i++) texto.append(lista.obtener(i)).append('\n');
        return texto.toString();
    }
}