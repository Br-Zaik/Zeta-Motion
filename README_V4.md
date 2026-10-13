# Zeta Motion Studio v4 — Anime/Manga (sin IA)

Proyecto fuente Android 8+, Kotlin/Jetpack Compose y OpenGL ES 3.0. Se conserva el identificador `com.zeta.fondo`, los datos guardados y la firma del proyecto proporcionado. No hay API de IA, pagos, créditos ni descargas de modelos.

## Cambios de esta versión respecto a v3
- Seis efectos procedurales nuevos, reales en el shader: cometas de luz, círculo mágico, rayado manga, anillos de poder, humo estilizado y fragmentos de cristal. Preview y exportación ejecutan el mismo shader.
- Segunda instancia independiente para 12 efectos de la biblioteca v3: cenizas, hojas, brasas, polvo, burbujas, electricidad, líneas de enfoque, onda de choque, flash, screentone, grano y tinta. Cada instancia tiene su intensidad, tamaño/densidad, velocidad y máscara; ocho de ellas también tienen origen y dirección propios. No es un compositor multipaso: es una segunda evaluación en el mismo shader (para limitar copias y coste gráfico).
- Editor de efectos: panel de segunda capa, ajustes independientes, origen y máscara. Se guardan en los ajustes JSON del proyecto existentes, sin migración destructiva.
- Categorías Anime, Ambiente, Batalla y Manga ampliadas con los efectos nuevos.
- Se mantienen el lienzo y los paneles compactos, las máscaras personalizadas, la configuración de origen y dirección, los presets y la exportación personalizada 2K/4K incorporados en v3.

## Compilar
Subir el contenido del ZIP a tu repositorio y ejecutar `.github/workflows/build.yml`, que genera un artefacto de APK release. **No se ha compilado ni probado una APK real en este entorno**: probar en un Android real antes de sustituir una versión de trabajo. 2K/4K depende de los límites del codificador, memoria y GPU del teléfono; no se afirma que agrandar un original cree detalles nuevos.

## Importante: firma
El ZIP de origen incluye `app/fondozeta.jks` y contraseñas en `app/build.gradle.kts`. Se conservan por compatibilidad con la APK anterior. NO publicar en un repositorio público: trasladar la clave de firma y las contraseñas a secretos privados antes de publicar.

## Qué NO está implementado
No hay IA. No hay renderizado multipaso con capas ilimitadas, ni duplicación ilimitada de todos los efectos (hay una instancia B en los doce listados). Evitamos presentar botones de funciones inexistentes.
