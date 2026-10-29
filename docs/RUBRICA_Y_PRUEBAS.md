# Rúbrica y pruebas para la demostración

La tabla relaciona el código con los criterios visibles de la rúbrica. Es una guía de evidencias; no garantiza una calificación.

| Criterio | Puntos | Evidencia |
|---|---:|---|
| Hash, DH, 3DES y Huffman | 50 | Login correcto/incorrecto, claves públicas entre procesos, identidad comprobada, lectura y descarga con integridad. |
| Gestión de archivos | 15 | Directorios correctos, mensajes de archivos faltantes, rechazo de registros inválidos y copias cifradas persistentes. |
| Claridad y modularidad | 10 | Clases separadas, comentarios `//`, lista y montículo propios; funciones estándar para IO y criptografía. |
| Manual técnico y de usuario | 5 | Manuales incluidos con diagramas de clases y flujo, formatos e instrucciones. |
| Trabajo colaborativo | 5 | Historial y aportes reales de los integrantes. Completar la tabla de responsabilidades al final de este documento. |
| Conceptos teóricos y reportes | 10 | Explicación de hash, intercambio, cifrado, árbol, heap, listas y análisis/validación de registros. Reporte de pruebas. |
| Prototipo funcional y reportes | 5 | Demostración con dos equipos, modo sin red y errores controlados. |

La rúbrica menciona análisis léxico, aunque el enunciado no define un lenguaje o analizador específico. Aquí se documenta el análisis de registros, delimitadores y formatos. Confirma con el docente si requiere un componente adicional. No se afirma implementar un compilador o analizador léxico de propósito general.

## Pruebas reproducibles

Desde PowerShell, ajustando tu JDK:

```powershell
.\probar.ps1 -Jdk 'C:\Program Files\Java\jdk-23'
```

Opcionalmente, para probar también la migración, proporciona una carpeta V1 que contenga los ejemplos originales sin modificar:

```powershell
.\probar.ps1 -Jdk 'C:\Program Files\Java\jdk-23' -OrigenV1 'C:\ruta\al\servidor-anterior'
```

Las expectativas de la prueba opcional de migración corresponden al primer libro de ejemplo original. Si el contenido real fue personalizado, adapta esa expectativa antes de evaluar la migración. Los datos del origen se leen, no se modifican.

Las pruebas sin sockets pueden ejecutarse con:

```powershell
.\probar.ps1 -Jdk 'C:\Program Files\Java\jdk-23' -SoloLocal
```

Cada ejecución crea su propia carpeta `work/pruebas/libros-v2-*`. No reutiliza el puerto 5050 de producción: elige un puerto libre para su servidor de prueba. La suite completa sin migración tiene 670 comprobaciones; con la migración de los ejemplos originales, 672. El modo local tiene 639.

## Guion entre dos computadoras

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Ejecutar ServidorLibros en PC1. | Puerto disponible y huella pública visible. |
| 2 | Cliente remoto con huella correcta. | Catálogo y login disponibles. |
| 3 | Contraseña errónea. | Acceso rechazado. |
| 4 | Ana y Luis leen simultáneamente el mismo libro. | Contenido correcto en ambos equipos. |
| 5 | Ana descarga. | Original y entrega cifrados, y archivo HUFFMAN2 local. |
| 6 | Comparar original servidor y copia cliente. | Mismo contenido y resumen de archivo. |
| 7 | Luis intenta lectura local o descarga. | Acceso Premium rechazado; descarga también protegida en el servidor. |
| 8 | Cerrar cliente Premium y desconectarlo de red. | Al reiniciar aparece modo local y permite autenticar la cuenta preparada. |
| 9 | Leer descarga sin red. | Mismo texto que en línea. |
| 10 | Contraseña local incorrecta. | Acceso rechazado. |
| 11 | Modificar una copia de prueba Huffman. | Mensaje de daño y retorno al menú. |
| 12 | Alterar cuenta local de prueba. | Acceso rechazado. |
| 13 | Configurar una huella equivocada en línea. | Rechazo de identidad; no se acepta el servidor. |
| 14 | Cerrar una sesión y salir de un cliente. | El otro cliente y el servicio siguen funcionando. |

Para comparar archivos entre equipos puedes usar `Get-FileHash -Algorithm SHA256` sobre cada archivo. No confundas este resumen de evidencia con el hash de contraseña ni con el secreto DH.

## Evidencia de colaboración (completar con trabajo real)

| Integrante | Responsabilidad real | Archivo/commit/evidencia |
|---|---|---|
| Por completar | Por completar | Por completar |
| Por completar | Por completar | Por completar |

No se incluyen aportes ficticios. Cada integrante debe poder explicar las clases y pruebas que presenta.
