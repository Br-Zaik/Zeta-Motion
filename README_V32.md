# Zeta Motion v32 — Interfaz compacta y animación por zonas

Basada en **Zeta Motion v31**. La aplicación sigue destinada **solo** a dar movimiento a fondos e ilustraciones de anime, manga y manhwa. Conserva IDs de efectos, proyectos, máscaras, anclajes, flechas, herramientas de Animar, exportación y `applicationId`.

## Mejoras implementadas

1. **Más espacio para la imagen.** Panel de herramientas compacto por defecto, ampliable/contraíble tocando la flecha del tirador superior. El tamaño queda acotado por el alto disponible para proteger el lienzo en pantallas pequeñas. Se mantiene la edición desplazable verticalmente.
2. **Acciones con iconos.** Pincel/borrador, pintura de zonas, vista previa, activación/desactivación, proteger/fijar/borrar anclajes, guías, duplicar/eliminar un clip y confirmar edición tienen controles compactos con `contentDescription`. Se conservan títulos de parámetros (Velocidad, Fuerza, Tamaño, etc.). La explicación del borde fijo ahora aparece a petición mediante `?`.
3. **Borde fijo sin scroll horizontal.** En Movimiento natural: ninguno, izquierda, derecha, abajo y arriba quedan en una fila de cinco iconos de ancho flexible.
4. **Efectos sin duplicados.** La nueva pestaña `Útiles` concentra los efectos de animación ambiental/escena más habituales. `Todos` y las categorías siguen mostrando **todos** los efectos originales; un efecto adicional que esté activo también se muestra en Útiles. NO se crearon segundas versiones de lluvia, viento, nieve, agua ni partículas, ni se eliminaron efectos o proyectos existentes.
5. **Efectos más fáciles de explorar en teléfonos modestos.** Las miniaturas animadas de Efectos y Audio leen su animación dentro de `Canvas`, en la fase de dibujo. La animación continua no fuerza la recomposición de todo el catálogo cada fotograma.
6. **Apertura de imágenes adaptada a memoria.** Se reutiliza la caché del proyecto y se lee con `ImageDecoder` dimensionado antes de decodificar en Android 9+ (el código para Android 8 permanece). La textura del editor se dimensiona según `ActivityManager` (1600/2048/2560 px), sin reducir la imagen original que utiliza exportación.
7. **Zoom/pan.** Se descartan transformaciones no finitas y gestos microscópicos repetidos, sin sacrificar movimientos normales; esto evita renders redundantes. Se conserva el bitmap respaldo durante recreaciones de superficie de OpenGL para reducir flashes negros.
8. **Movimiento natural perfeccionado.** Cabello y ropa existentes incorporan retraso de fase creciente hacia las zonas libres y rebote armónico, sin shaders nuevos ni cambios en los seis materiales. Sigue respetando máscara, congelación, intensidad y borde fijo y mantiene el mismo frame al inicio y final del bucle.
9. **Versión:** `versionCode=32`, `versionName=32.0.0`.

## Verificación efectuada aquí

- Compilación y enlace de shaders de OpenGL ES 3 con EGL por software; pruebas de máscaras, congelación, anclajes, 6 materiales naturales, 3 modos de movimiento, 3 zonas, color, efectos localizados y bucles: **correctas** (`tools/check_render.py`).
- Análisis sintáctico de los archivos Kotlin mediante el parser del compilador: **sin errores sintácticos**.
- Comparación de `Effects.kt` con v31: se mantienen las 62 definiciones de efectos sin duplicar ni quitar IDs. La lista recomendada hace referencia a efectos existentes.

## Límites y pendientes importantes

Esta entrega contiene **código fuente ZIP**, no un APK. Este entorno no dispone de **Android SDK/Gradle**, por lo que no se ha realizado una compilación Android completa ni una prueba de la interfaz, fluidez táctil o exportación en un teléfono. No es posible garantizar cero caídas, cero tirones o FPS constantes en todos los dispositivos. Los cambios en cabello y tela son mejoras de deformación periódica, **no** una simulación física completa al nivel de Live2D. No se ha implementado cambio de resolución durante un pellizco; se prioriza mantener una sola textura estable mientras se edita para evitar saltos.

Tampoco se han reescrito individualmente todos los efectos: esto requeriría ejemplos de salida y pruebas visuales de cada uno. R8 continúa sin habilitarse automáticamente hasta que la versión release pueda compilarse y probarse.

### Compilar

Con Java 17, Android SDK 34 y Gradle 8.9, ejecutar `gradle testReleaseUnitTest assembleRelease`. Como alternativa, GitHub Actions incluye `.github/workflows/build.yml`.

**Seguridad:** `app/fondozeta.jks` y su configuración de firma ya estaban en v31; se conservaron para no romper la compatibilidad de actualización. No publiques estas claves en un repositorio abierto. Sustituye las credenciales antes de distribución pública cuando puedas planificar una transición de firma.
