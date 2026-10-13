# Zeta Motion v5 — rediseño táctil completo (anime/manga, sin IA)

Este ZIP contiene el **proyecto fuente Android** actualizado a partir de v4. Se conservaron el paquete `com.zeta.fondo`, la base local de proyectos, los shaders y el motor original de vista previa/exportación. `versionCode = 5`, `versionName = 5.0.0`.

## Interfaz nueva, conectada a funciones reales

- **Inicio** negro, banner ilustrado anime integrado y botón grande «Importar ilustración». Galería de proyectos del usuario con miniaturas reales, nombre y dimensiones. Atajos Animar/Cielo/Efectos/Exportar abren el proyecto reciente (o piden imagen si no hay ninguno). Confirmación antes de borrar. Barra Inicio/Proyectos/Crear con acciones.
- **Editor**: vista previa GL grande, deshacer/rehacer para trazos, controles superiores compactos, un único panel inferior plegable y reajustable arrastrando, pestañas Animar/Máscaras/Presets/Efectos/Exportar. Botones del panel ≥48 dp; al reproducir se ocultan las guías para ver el dibujo sin flechas sobre él. Modo pantalla completa; paisaje con lienzo a un lado e inspector al otro.
- **Animar**: ruta, ancla, congelar, borrador, mover y origen, con controles visibles de movimiento/velocidad o pincel. Dibujo de guías en el lienzo y animación del motor v4 conservados. Cuando se elige una herramienta se pausa para editar.
- **Máscaras**: capas, pintado/borrado, grosor/suavizado, relleno/vaciado, visualización; conserva hasta 12 máscaras guardadas y asignación a efectos.
- **Presets**: tarjetas visuales que aplican presets reales de `Presets.all`.
- **Efectos**: tarjetas visuales grandes, categorías Anime/Batalla/Ambiente/Cielo/Manga/Cámara/Todos y ajustes por efecto. Al tocar una tarjeta se activa el efecto (si estaba apagado) y se abre su configuración. Origen, ángulo y máscaras; segunda capa independiente para los 12 efectos compatibles de v4 en «Más ajustes». Las miniaturas de las tarjetas son **referencias visuales**; el resultado real se ve en el lienzo, renderizado por el shader.
- **Exportar**: resolución 1080p/2K/4K, formato MP4 o **secuencia PNG**, FPS 24/30/60, controles avanzados de recorte, tamaños, bucle, códec disponible en ese dispositivo y calidad. Exportar, cancelar, progreso, compartir y reenviar a Alight cuando hay vídeo.

## Recursos y límites

- No hay IA, API externa, suscripción ni créditos. El arte del banner y las 8 miniaturas ilustrativas están incluido localmente en `app/src/main/assets/ui/` y no requieren conexión.
- 2K/4K depende de las capacidades reales del codificador, GPU y RAM del Android; reescalar no inventa detalles de la ilustración fuente. No se han añadido botones para herramientas de renderizado multipaso o duplicación ilimitada que el motor v4 no tenga.
- Este entorno **no dispone de Android SDK/Gradle instalados ni acceso de red**, de modo que no se ha compilado ni probado una APK en un teléfono. El ZIP es código fuente y contiene `.github/workflows/build.yml` para compilar APK `release` mediante GitHub Actions. Comprueba en tu Android antes de borrar la versión estable.

## Firma y seguridad

Se conservó `app/fondozeta.jks` con las credenciales originales de `app/build.gradle.kts` para que la APK nueva pueda actualizar la existente. **No subas esa clave ni contraseñas a un repositorio público**. Mueve la clave y las contraseñas a almacenamiento/secrets privados sin regenerar la firma.
