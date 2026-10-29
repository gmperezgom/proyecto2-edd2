# Manual de usuario e instalación por fases

## Fase 1. Preparar el código

Extrae el ZIP en una carpeta nueva. Abre **esa carpeta completa** en VS Code y utiliza el soporte de Java de Microsoft. Todas las clases están en `src/librosdigitales`.

En PowerShell, desde la raíz:

```powershell
$jdkLibros = 'C:\Program Files\Java\jdk-23'
.\compilar.ps1 -Jdk $jdkLibros
```

Adapta la ruta al JDK de tu computadora. Si PowerShell bloquea el script, puedes compilar directamente sin cambiar sus políticas:

```powershell
New-Item -ItemType Directory -Force -Path bin | Out-Null
& "$jdkLibros\bin\javac.exe" -encoding UTF-8 --release 17 -d bin .\src\librosdigitales\*.java
```

**Comprobación:** no deben aparecer errores de compilación. No modifiques todavía la carpeta compartida que utilizabas antes.

## Fase 2. Preparar el almacén y sus claves en PC1

Elige **una** de estas opciones. No ejecutes las dos en el mismo destino.

### Opción A: conservar los libros y usuarios anteriores

Si tu carpeta anterior sigue en esta ruta:

```powershell
& "$jdkLibros\bin\java.exe" -cp bin librosdigitales.GeneradorServidor --migrar 'C:\PROYECTO2_EDD2\proyecto2-edd2\servidor' 'servidor_v2'
```

La migración lee la versión anterior, recupera sus textos y crea el nuevo cifrado en `servidor_v2`. No modifica el origen. Es compatible con el formato y las claves predeterminadas del proyecto original revisado.

### Opción B: crear nuevos ejemplos

```powershell
& "$jdkLibros\bin\java.exe" -cp bin librosdigitales.GeneradorServidor
```

Crea tres libros de ejemplo y las cuentas `ana / ana123` (Premium) y `luis / luis123` (estándar).

### Archivos que se crean

```text
carpeta del proyecto V2/
├── servidor-config-v2.txt          PRIVADO, fuera de lo compartido
├── config-cliente-v2.ejemplo.txt   plantilla pública de cliente
└── servidor_v2/                   carpeta que se compartirá
    ├── usuarios.txt
    ├── catalogo.txt
    └── libros/
```

El generador rechaza destinos/configuraciones existentes. Si ves ese rechazo, no borres nada: conserva la instalación preparada y continúa, o utiliza una carpeta de trabajo nueva si necesitas otra instalación.

**Comprobación:** el mensaje indica el almacén, el archivo privado y la huella pública. Guarda una copia de respaldo de los datos y del archivo privado; perder la clave del almacén impide descifrar sus libros.

## Fase 3. Compartir la carpeta y arrancar el servicio

Comparte **solo `servidor_v2`** como `LibrosCompartidosV2`, con permisos de lectura para la cuenta Windows de los clientes. Mantén `servidor-config-v2.txt` fuera de esa carpeta. La carpeta compartida original puede seguir existiendo para tu respaldo V1.

Desde otra computadora puedes comprobar `\\PC1\LibrosCompartidosV2` con Windows + R, utilizando la cuenta Windows que ya configuraste. Esta comprobación verifica la carpeta; el intercambio de claves de Java utiliza el servicio TCP.

En PC1, ejecuta:

```powershell
& "$jdkLibros\bin\java.exe" -cp bin librosdigitales.ServidorLibros
```

Debe aparecer **Servidor listo. Puerto TCP: 5050**. Deja esa terminal abierta. En VS Code también puedes abrir `ServidorLibros.java` y ejecutar su `main` con Run.

Para permitir conexiones desde la red local, si el firewall bloquea el servicio, abre PowerShell como administrador **en PC1** y crea una regla para la red privada:

```powershell
New-NetFirewallRule -Name 'LibrosDigitalesV2-TCP5050' -DisplayName 'Libros Digitales V2 TCP 5050' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 5050 -Profile Private -RemoteAddress LocalSubnet
```

Si Windows ya permitió Java en la red privada y el puerto responde, no necesitas otra regla. Mantén ambas computadoras en la misma red de confianza. No se requiere redirigir puertos del router.

**Comprobación desde el cliente, en PowerShell:**

```powershell
Test-NetConnection PC1 -Port 5050
```

`TcpTestSucceeded` debe ser `True`. Si el nombre PC1 no resuelve, utiliza la IPv4 del servidor obtenida con `ipconfig`.

## Fase 4. Configurar cada cliente

El cliente necesita las clases y Java, pero no la configuración privada del servidor ni el generador ejecutado localmente.

En PC1, toma `config-cliente-v2.ejemplo.txt`, adapta una copia y guárdala como `config.txt` en la raíz del proyecto de **cada cliente**:

```properties
rutaServidor=\\PC1\LibrosCompartidosV2
rutaLocal=cliente_v2
hostServidor=PC1
puertoServidor=5050
tiempoEsperaMs=5000
huellaServidor=PEGAR_AQUI_LA_HUELLA_REAL_DE_64_CARACTERES
```

La huella se copia completa desde el archivo de ejemplo de **esa misma instalación**. No escribas literalmente el marcador ni uses la huella de un ejemplo ajeno. Es pública; la privada permanece en PC1.

`rutaServidor` conserva la referencia documental de la carpeta compartida. En V2 el cliente recibe catálogo y libros por el servicio autenticado; no abre directamente esa ruta SMB para cada operación. El servidor lee `rutaDatos` desde su configuración privada. Esto permite un tiempo de espera controlado y un arranque local sin esperar a una unidad de red desconectada.

Si utilizaste una IP para conectarte, colócala en `hostServidor` y en la referencia `rutaServidor`.

En PC1, si también ejecutas un cliente, puedes usar `hostServidor=localhost` con la misma huella. En los equipos remotos, `localhost` se refiere al propio cliente: allí utiliza `PC1` o su IP.

Arranca el cliente:

```powershell
& "$jdkLibros\bin\java.exe" -cp bin librosdigitales.LibrosDigitales
```

O ejecuta `LibrosDigitales.java` con Run. `config.txt` se busca en el directorio de ejecución. Si falta, el mensaje indica ese directorio.

**Comprobación:** aparece servicio conectado, identidad verificada y después el inicio de sesión. No se muestran privadas ni secretos.

## Fase 5. Lectura y descarga

Con `ana / ana123`:

1. Inicia sesión en línea. Debe indicar que el acceso local Premium quedó preparado en esa computadora.
2. Explora el catálogo y lee un libro.
3. Vuelve a seleccionar el libro y elige descargar.
4. Regresa al menú principal y selecciona Lectura local.
5. Verifica que se muestre el mismo texto.

Con `luis / luis123`, la lectura se inicia directamente al seleccionar un libro y la opción local se rechaza. El servidor vuelve a comprobar las credenciales y Premium en cada solicitud; modificar el menú del cliente no autoriza una descarga estándar.

Los archivos quedan separados por usuario:

```text
cliente_v2/
├── cuentas/
│   └── HASH_DEL_USUARIO.cuenta
└── usuarios/
    └── HASH_DEL_USUARIO/
        ├── copias_cifradas/ISBN.txt    igual al original del servidor
        ├── entregas_cifradas/ISBN.txt  cifrado con la clave negociada
        └── descargas/ISBN.huf.txt      formato HUFFMAN2
```

El hash del nombre sirve para identificar una carpeta, no para ocultar la existencia de la cuenta. Los archivos `.lock` son auxiliares de coordinación de escritura. Ningún archivo guarda el secreto negociado. El texto de una lectura se muestra en memoria y consola; la descarga guarda la representación comprimida, como exige el enunciado.

## Fase 6. Arranque sin conexión

La primera autenticación Premium y la descarga deben haberse realizado **en ese cliente** con conexión. Después:

1. Sal completamente de `LibrosDigitales`.
2. Desconecta el cliente de la red o detén `ServidorLibros`.
3. Abre nuevamente `LibrosDigitales`.
4. Espera el tiempo configurado (por defecto, 5 segundos por conexión fallida).
5. Inicia sesión con el mismo usuario y contraseña.
6. Selecciona Lectura local.

También puedes entrar directamente al modo local sin intentar la red:

```powershell
& "$jdkLibros\bin\java.exe" -cp bin librosdigitales.LibrosDigitales --sin-conexion
```

Una contraseña errónea o un archivo de cuenta modificado se rechazan. No hay acceso local para cuentas estándar ni usuarios nunca preparados. Si cambias la contraseña o la categoría en el servidor, la información local se actualiza con una autenticación en línea correcta. Sin red, se utiliza la última categoría verificada. Para volver al modo en línea, reconecta y reinicia la aplicación.

## Fase 7. Archivos dañados y prueba simultánea

Haz una copia de un `.huf.txt` de prueba. Guarda su contenido original y, en la copia utilizada para la prueba, elimina parte de los datos o modifica el resumen final. Al abrirlo debe aparecer un mensaje de archivo Huffman dañado y volver al menú. Restaura después el archivo original o descárgalo de nuevo.

En dos computadoras, abre el mismo libro simultáneamente, con Ana en una y Luis en otra. También prueba una descarga Premium mientras el otro cliente lee.

Las descargas V1 no se importan automáticamente: vuelve a descargarlas para obtener HUFFMAN2 con resumen. No elimines el respaldo anterior hasta verificar tu nueva instalación.

## Solución de errores comunes

| Mensaje | Revisión |
|---|---|
| Falta hostServidor o huellaServidor | El `config.txt` es V1; adapta el archivo de ejemplo V2. |
| Huella no coincide | Usa la huella del servidor que realmente estás ejecutando; no se cambia automáticamente a acceso local ante este error. |
| Servicio no disponible | Revisa PC1, IP, puerto 5050, firewall y que `ServidorLibros` siga abierto. |
| No hay acceso local preparado | Autentica primero un Premium en línea en esa computadora. |
| Clave o almacén inválidos | No mezcles el almacén V2 con otra configuración privada. |
| Formato HUFFMAN2 requerido | Vuelve a descargar la copia antigua con el cliente actualizado. |
| Usuarios o catálogo inválidos | Corrige la línea indicada; se exige el formato del enunciado sin encabezados. |

## Cómo detenerlo

En cada cliente utiliza Salir. Para detener el servicio en PC1, presiona Ctrl + C en su terminal. Los clientes con acceso local preparado podrán utilizar sus descargas después de reiniciar en modo sin conexión.
