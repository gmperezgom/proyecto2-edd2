package librosdigitales;

/** Representa a un usuario registrado en el archivo de usuarios del servidor. */
public class Usuario {

    private String nombre;
    private String hashClave;
    private boolean premium;

    public Usuario(String nombre, String hashClave, boolean premium) {
        this.nombre = nombre;
        this.hashClave = hashClave;
        this.premium = premium;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getHashClave() {
        return hashClave;
    }

    public void setHashClave(String hashClave) {
        this.hashClave = hashClave;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }
}
