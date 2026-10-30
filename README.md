# Libros Digitales: actualización V2

Esta entrega contiene las **19 clases completas**, comentarios `//` en español, pruebas sin bibliotecas externas y manuales. Requiere JDK 17 o superior. Se verificó con JDK 23.0.2, compilando con `--release 17`.

## Empieza aquí

1. Extrae el ZIP en una carpeta nueva y abre esa carpeta en VS Code. Conserva tu proyecto anterior funcionando como respaldo.
2. Lee [la lista de cambios](docs/CLASES_Y_CAMBIOS.md).
3. Sigue [el manual de usuario por fases](docs/MANUAL_USUARIO.md). Primero compila; después prepara los datos y ejecuta el servidor.
4. Consulta [la verificación ejecutada](docs/VERIFICACION.md) y [la matriz de la rúbrica](docs/RUBRICA_Y_PRUEBAS.md).

**La carpeta compartida por sí sola ya no completa el intercambio:** `ServidorLibros` debe permanecer ejecutándose en PC1, por defecto en TCP 5050. El cliente accede a los datos mediante ese servicio. `RepositorioServidor` lee los archivos desde la carpeta que compartes en Windows. Los libros viajan por la aplicación.

## Archivos incluidos

- `src/librosdigitales/`: las 19 clases de producción, listas para copiar juntas o abrir como proyecto.
- `pruebas/PruebasIntegracion.java`: suite reproducible, incluyendo TCP real en localhost.
- `compilar.ps1` y `probar.ps1`: ayudas para PowerShell.
- `docs/`: instrucciones, diagramas, límites y resultados.

No se incluyen claves privadas, cuentas locales preparadas, archivos de usuarios de tu equipo ni configuraciones activas. El generador crea configuraciones propias de tu instalación.

## Comandos breves

En PowerShell, desde esta carpeta, adapta la ruta de tu JDK:

```powershell
$jdkLibros = 'C:\Program Files\Java\jdk-23'
.\compilar.ps1 -Jdk $jdkLibros
& "$jdkLibros\bin\java.exe" -cp bin librosdigitales.GeneradorServidor
& "$jdkLibros\bin\java.exe" -cp bin librosdigitales.ServidorLibros
```

El generador crea datos **nuevos** en `servidor_v2`. Para conservar los libros anteriores, utiliza el modo de migración explicado en el manual **en lugar** de ejecutar el generador sin argumentos.

En otra terminal, después de adaptar `config-cliente-v2.ejemplo.txt` y guardarlo como `config.txt`:

```powershell
& "$jdkLibros\bin\java.exe" -cp bin librosdigitales.LibrosDigitales
```

Para probar:

```powershell
.\probar.ps1 -Jdk $jdkLibros
```

Las pruebas crean una carpeta nueva bajo `work/pruebas`. No utilizan ni modifican tus datos reales.

## Compatibilidad

Los libros cifrados V1 se importan mediante `--migrar` a una carpeta nueva. Las descargas antiguas `HUFFMAN` carecen del resumen de integridad V2: deben descargarse nuevamente como `HUFFMAN2`. Las carpetas locales ahora separan los archivos por usuario.

El formato SHA-256 del archivo `usuarios.txt` se conserva según el enunciado. El acceso local Premium utiliza PBKDF2, sal aleatoria y un archivo cifrado/autenticado, con la categoría verificada en la última conexión. No puede conocer una revocación hecha en el servidor mientras el cliente permanece desconectado.
