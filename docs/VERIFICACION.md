# Verificación de la actualización

Ejecutada el 27 de octubre de 2026 con JDK 23.0.2 en Windows.

- Compilación con `--release 17 -Xlint:all`: correcta, sin advertencias.
- Suite Java: **672 verificaciones correctas**, incluidas conexiones TCP reales en localhost, 8 solicitudes concurrentes, rechazo de descarga estándar, validación de identidad, persistencia cifrada, caché local, archivos dañados y migración V1 a V2 sin alterar el origen.
- Suite de consola: **8 escenarios correctos**.

1. Consola en línea: login Premium, descarga, lectura local, cierre de sesión y lectura estándar.
2. Dos procesos cliente simultáneos con cuentas Premium y estándar.
3. Huella incorrecta: se rechaza el servidor sin entrar automáticamente al modo local.
4. Arranque automático sin servidor, contraseña incorrecta rechazada y lectura local correcta.
5. Usuario nunca autenticado: acceso local rechazado.
6. Usuario estándar: arranque local no autorizado.
7. Descarga dañada: mensaje controlado y retorno al menú.
8. Cuenta local alterada: autenticación rechazada.

Las pruebas de red de esta entrega se ejecutaron en localhost. La prueba entre tus dos computadoras, el puerto del firewall y el recurso compartido deben verificarse siguiendo el manual. No se modificaron los archivos de tu proyecto original ni su configuración.
