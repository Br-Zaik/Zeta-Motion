# Zeta Motion v35 — corrección concreta del fallo de Kotlin

**Error confirmado en GitHub Actions:** `AppViewModel.kt:109:22` y `:111:5`: `Platform declaration clash`, firma `setShowOriginal(Z)V`.

## Cambios
- `AppViewModel.kt`: la función `setShowOriginal(show: Boolean)` ahora se llama `updateOriginalPreview(show: Boolean)`. La propiedad Compose `showOriginal` mantiene su nombre y el setter privado.
- `EditorScreen.kt`: actualizadas las dos llamadas al nuevo método.
- `.github/workflows/build.yml`: conserva el ensamblado del APK de diagnóstico y muestra una verificación para confirmar que se compiló la fuente corregida.

El cambio evita que el setter JVM generado automáticamente para la propiedad `showOriginal` choque con el método escrito manualmente.
**Se mantienen los 66 efectos y los 8 presets v35**; no se cambió su lógica.

## Para utilizar este ZIP
**No basta con subir el ZIP si el workflow del repositorio no lo extrae.** Extrae este archivo en la raíz de tu repositorio y reemplaza también el archivo oculto `.github/workflows/build.yml`, luego crea un commit nuevo. Si usas el workflow incluido, admite seleccionar el ZIP más reciente que se encuentre en el repositorio.

## Pruebas
- Corrección verificada mediante revisión de ambas llamadas y búsqueda global: no queda ninguna llamada a `setShowOriginal(` en Kotlin.
- Analizador sintáctico Kotlin: 0 errores.
- Prueba autónoma de modelos y presets: 66 efectos, 8 presets comprobados.
- Prueba de shader OpenGL y máscaras de render: correcta.

**Limitación:** aquí no hay Android SDK ni Gradle configurados para ejecutar el empaquetado completo del APK. La próxima ejecución de GitHub Actions confirmará la compilación real y podría revelar errores adicionales no visibles en las capturas.
