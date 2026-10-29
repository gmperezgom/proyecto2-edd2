# Clases y cambios

## Actualizar estas 9 clases completas

| Clase | Cambio |
|---|---|
| ArchivoUtil | UTF-8 estricto, límite de tamaño, temporales, escritura coordinada y reemplazo atómico cuando está disponible. |
| Cifrador3DES | IV aleatorio, claves separadas de cifrado/HMAC y verificación antes de descifrar. |
| Configuracion | Host, puerto, huella pública, tiempo de espera y validación de parámetros. |
| DiffieHellman | Parámetros públicos fijos de 2048 bits y privada independiente por conexión. |
| GeneradorServidor | Preparación V2 sin sobrescritura y migración V1 a otra carpeta. |
| HashUtil | SHA-256, HMAC, PBKDF2 y bytes aleatorios. |
| Huffman | Formato HUFFMAN2, validaciones y detección de daños. |
| LibrosDigitales | Menús en línea/local, comunicación real, caché Premium y errores controlados. |
| RepositorioServidor | Validación de registros, duplicados, ISBN y rutas. Lo utiliza el proceso servidor. |

## Agregar estas 5 clases

| Clase | Responsabilidad |
|---|---|
| ServidorLibros | Servicio TCP, identidad del servidor, autenticación y acceso concurrente a libros. |
| ConexionServidor | Cliente TCP, verificación de huella/firma y negociación real de la clave. |
| Protocolo | Formato de mensajes con longitud, límites, campos y firmas. Es de uso interno del paquete. |
| RepositorioLocal | Archivos locales separados por usuario y descargas Huffman. |
| AutenticacionLocal | Preparación, validación y revocación de acceso local Premium. |

## Conservar estas 5 clases

`Libro`, `Usuario`, `ListaEnlazada`, `MonticuloMinimo` y `NodoHuffman` se incluyen completas y conservan la implementación del proyecto revisado.

Copia o utiliza todas las clases del paquete juntas para evitar mezclar versiones. `Protocolo.java` es una clase adicional respecto del plan inicial: evita repetir código de red en cliente y servidor.

## Configuraciones distintas

- `servidor-config-v2.txt`: privado, solo en PC1 y fuera de la carpeta compartida. Contiene claves del almacén y de identidad. No subir a Git ni entregar a clientes.
- `config-cliente-v2.ejemplo.txt`: creado por el generador; sirve para preparar `config.txt` en cada equipo.
- `config.txt`: configuración del cliente. Incluye host, puerto, ruta local y huella pública de la instalación.

Cambiar la identidad o crear otro almacén produce una huella nueva; no mezclar configuraciones entre instalaciones. Conserva un respaldo de la configuración privada junto con los datos del almacén.
