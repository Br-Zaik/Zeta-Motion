# Zeta Motion v34 — Estabilidad y preparación técnica para Play

**Actualización desde v33 del código fuente**, enfocada en editores principiantes que animan anime, manga, manhwa y fondos. Conserva los IDs de los efectos y archivos del proyecto; no añade otra lluvia/nieve/viento redundante. **No es un APK ya validado ni una publicación en Google Play.**

## Cambios efectuados y comprobables en el código

1. **Autoguardado diferido** tras pintar trazos, mover anclajes/flechas, cambiar parámetros y deshacer/rehacer. Se espera 900 ms sin cambios antes de programar el guardado para evitar escribir por cada movimiento de dedo. Las operaciones de edición abiertas ahora pueden conservarse si la aplicación pasa al fondo; cancelar también persiste el estado restaurado. `AppViewModel.kt`, `MainActivity.kt`.
2. **Guardar proyectos de forma atómica**, con recuperación desde la copia de respaldo `.bak` cuando el sistema corta el guardado. Mejora las garantías frente a un JSON parcial. `ProjectStore.kt`.
3. **Comparación de original/animado** con icono de ojo en el lienzo. Desactiva temporalmente efectos y máscaras visuales en la vista previa, sin tocar parámetros ni afectar la exportación. `RenderHub.kt`, `PreviewRenderer.kt`, `EditorScreen.kt`.
4. **Línea de tiempo plegable** desde la barra de reproducción. Cuando se oculta aparece un control de posición normal para no perder la capacidad de avanzar al instante deseado. `EditorScreen.kt`.
5. **Panel más amplio al entrar a controles de efectos**, sin añadir instrucciones extensas por defecto. La pantalla mantiene límite mínimo de 152 dp de lienzo en modo vertical. Botón `?` de Animar para ayuda breve opcional.
6. **Vista previa adaptable al móvil**: reducción de carga de GPU (30 fps en móviles de baja RAM fuera de gestos; prioridad a la interacción), decodificación con `ImageDecoder` en Android 9+ y caché con tamaño adecuado, menos recomposiciones forzadas de OpenGL. `ProjectStore.kt`, `GlPreview`, `AppViewModel.kt`.
7. **Sección de efectos principales más breve** (Útiles), con el catálogo completo disponible en Todos/categorías. Los efectos originales, máscaras, presets, anclajes, flechas, variantes de sol y simulación de profundidad de v33 se mantienen.
8. **Exportación simplificada**: 720p/1080p/2K/4K, FPS y duración en primera vista. Formatos especiales, códec, calidad y repetición quedan en Opciones avanzadas, sin eliminar ninguno. `Panels.kt`.
9. **Mejoras de efectos existentes**: gotas de lluvia con grosor y longitud menos uniformes; copos de nieve con tamaños/transparencias que simulan varios planos. Mantienen tres capas, máscaras, variaciones de estilo y bucle. No se agregan efectos duplicados. `fondo.frag`.
10. **Preparación de compilación Google Play**: `compileSdk=36`, `targetSdk=36`, AGP 8.10.1 / Gradle 8.11.1, `versionCode=34`; R8 activado para builds `release` (requiere pruebas). El workflow de GitHub Actions compila APK de pruebas y un AAB de preparación. **Se retiró la clave privada de firma del ZIP compartible**: release solo se firma si se proporciona mediante variables de entorno, nunca con contraseñas incrustadas. `app/build.gradle.kts`, `build.gradle.kts`, `.github/workflows/build.yml`.
11. **Tests de regresión de timeline**, añadidos bajo `app/src/test/...`, para superposición, fundidos, duración e intensidades por bloque.

## Estado de comprobación

- ✅ Sintaxis de archivos `.kt` revisada con analizador Kotlin.
- ✅ Pruebas de timeline por Kotlin CLI: 9 aserciones correctas.
- ✅ Shader de OpenGL ES 3.0 compilado/enlazado en renderizador Mesa software; tests existentes de estilos, máscaras, movimiento y continuidad temporal pasan.
- ⏳ Sin Android SDK/Gradle disponibles en el entorno de esta sesión: **no se compiló APK/AAB**, ni se ejecutaron las pruebas JUnit/Android UI en un dispositivo real. No se garantiza fluidez en todos los modelos; hace falta validar rendimiento y calidad de exportación.

## Firma al actualizar desde v33

**Importante antes de desinstalar:** los proyectos están en el almacenamiento interno de la app. Si quieres actualizar la v33 instalada sin perderlos, el APK v34 debe estar firmado con **la misma clave privada** que se usó para tu APK v33. Conserva el archivo original de firma en un lugar privado (en el ZIP v33 anterior) y configúralo sin publicarlo:

- `ZETA_KEYSTORE_PATH`: ruta completa a ese almacén JKS en tu equipo o CI.
- `ZETA_KEYSTORE_PASSWORD`: contraseña, a través de variables protegidas.
- `ZETA_KEY_ALIAS`: alias de clave existente.
- `ZETA_KEY_PASSWORD`: contraseña de la clave.

Un APK `debug` generado con el nuevo entorno de compilación se firma con la clave debug local, **no puede reemplazar una instalación v33 firmada de otra forma**. No desinstales sin preservar los proyectos. Para subir a Play, configurar Play App Signing y custodiar la clave de subida. Un `.aab` sin firma no es listo para publicación.

## Lista obligatoria antes del lanzamiento público

1. Compilar `testDebugUnitTest assembleDebug bundleRelease` con JDK 17, Gradle 8.11.1, Android SDK 36 y Android Studio compatible.
2. Probar importación, pincel, goma, anclajes, gestos zoom/pan, comparación, 2D, 2.5D, órbita simulada, preview y exportación en Android 11, 13 y 16, incluyendo un móvil de baja RAM.
3. Medir con Android Studio Profiler fotogramas que se pierden, RAM y cierres. Corregir ANR/fallos, verificar reproducción de MP4 y calidad/relación de aspecto exportada.
4. Preparar icono propio, capturas reales, ficha de Play, política de privacidad pública con contacto verificable, formulario de Seguridad de datos y clasificación de contenido.
5. Firmar AAB desde CI privado; configurar pruebas internas o cerradas antes de solicitar publicación.

La app **sigue siendo gratuita y sin anuncios ni compras**. No se añade rastreo publicitario ni cuentas.
