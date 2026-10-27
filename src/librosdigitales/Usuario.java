package librosdigitales;

// Esta clase representa un usuario con su nombre, hash de contraseña y condición de cuenta Premium.
public class Usuario {

    // Nombre que identifica al usuario.
    private String nombre;
    // Hash de la contraseña utilizado para verificar el inicio de sesión.
    private String hashClave;
    // Indica si la cuenta tiene acceso a las funciones Premium.
    private boolean premium;

    // Este constructor asigna el nombre, el hash de la contraseña y la condición Premium del usuario.
    public Usuario(String nombre, String hashClave, boolean premium) {
        this.nombre = nombre;
        this.hashClave = hashClave;
        this.premium = premium;
    }

    // Este método devuelve el nombre del usuario.
    public String getNombre() {
        return nombre;
    }

    // Este método actualiza el nombre del usuario.
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    // Este método devuelve el hash de la contraseña.
    public String getHashClave() {
        return hashClave;
    }

    // Este método actualiza el hash de la contraseña.
    public void setHashClave(String hashClave) {
        this.hashClave = hashClave;
    }

    // Este método devuelve la condición de cuenta Premium.
    public boolean isPremium() {
        return premium;
    }

    // Este método actualiza la condición de cuenta Premium.
    public void setPremium(boolean premium) {
        this.premium = premium;
    }
}