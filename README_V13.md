# Zeta Motion 13 — bucle, color e importación directa

## Cambios solicitados

**Importar** abre directamente el selector de imágenes del teléfono, como en v11. Se eliminó la galería intermedia y sus permisos de lectura general. La app sigue enfocada en animar imágenes propias; no incorpora importación/edición de vídeos.

**Animar → Bucle**:
- Continuo: desplazamiento con enlace entre dos fases; suavizado del enlace ajustable.
- Ida y vuelta: oscilación en ambos sentidos, con una sola muestra de la imagen para evitar la doble imagen del enlace.
- Pulso: avanza desde la posición original siguiendo las flechas y regresa suavemente.
- Duración de la animación: 2–60 s; ciclos de movimiento: 1–8; se indica la duración de cada ciclo.
- Punto de inicio: 0–100 % del ciclo; modifica la fase del movimiento de rutas/deriva.
- Repeticiones del MP4: 1–10; muestra la duración total exportada. Repetir no acelera el movimiento. PNG es una imagen y la secuencia de imágenes conserva un ciclo de animación.

Estos tres modos son implementaciones propias. Las capturas de referencia no permiten certificar que coincidan exactamente con los tres comportamientos de la otra app. Los controles de bucle se aplican a rutas y deriva; los efectos de lluvia, cámara, elementos, etc. conservan sus velocidades propias, sincronizadas con la duración de la animación.

**Ajustes → Color**: brillo, contraste, saturación, tono, exposición, nitidez, viñeta, resplandor, prisma, grano, desvanecer, sombras, luces y temperatura. Carrusel compacto de herramientas y un deslizador para la seleccionada. Restablecer afecta al ajuste seleccionado; Restablecer todo afecta únicamente a color. Los cambios se guardan en el proyecto y se aplican en vista previa y exportación.

Formato y Audio permanecen junto a Color. Tocar los segundos de la barra abre Formato. Se conservan la firma, paquete, proyectos y herramientas de v12. El lienzo no cambia de tamaño al abrir estos controles.

## Comprobaciones

Compilación: Java 17, SDK 34, Gradle 8.9, `gradle testReleaseUnitTest assembleRelease`.

Diez pruebas unitarias de parámetros y audio. Shader real compilado y enlazado en OpenGL ES 3/Mesa. Pruebas de las zonas y herramientas de v12, de los catorce ajustes de color (cada uno modifica píxeles y vuelve al resultado neutral al restablecer), y de los tres modos de movimiento (diferentes entre sí, animados y con inicio/fin coincidentes). Script reproducible: `python3 tools/check_render.py`, requiere EGL/Mesa, numpy y Pillow.

No se probó físicamente en teléfono: quedan por comprobar los gestos, selector específico del fabricante, reproducción y exportación con la GPU/codificador del dispositivo. Compilar y probar el shader en software no garantiza ausencia absoluta de fallos ni una velocidad fija en todos los teléfonos.
