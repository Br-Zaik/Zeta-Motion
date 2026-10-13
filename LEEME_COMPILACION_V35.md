# v35: diagnóstico y reparación de compilación en GitHub

Se ha mantenido Gradle 8.9 + AGP 8.7.3 + Kotlin 1.9.24.

Esta revisión desactiva provisionalmente R8/reducción de recursos en `release` para aislar los fallos reales de compilación (revisar y reactivar antes de publicar). No modifica motores, efectos, presets ni proyectos.

**El log visible de la ejecución 38185868035 no incluye el error original:** muestra tareas exitosas y, más tarde, la traza final. No es posible demostrar todavía que el APK compila sin el log completo.

**IMPORTANTE:** El registro de GitHub dice `Run gradle assembleRelease --stacktrace`, pero el workflow de la versión v35 original empieza compilando `assembleDebug`. Eso significa que GitHub está ejecutando un workflow distinto del que venía en el ZIP.

### Para aplicar
- Extraer el ZIP en la raíz del repositorio y reemplazar todo, especialmente `.github/workflows/build.yml` (archivo oculto).
- Después de hacer commit, ejecutar un workflow NUEVO (no pulsar 'Re-run' del anterior).
- El nuevo workflow genera primero un APK debug instalable y registra errores concretos arriba en el resumen.
- Si decides subir únicamente el ZIP en el futuro, el nuevo workflow sabe extraerlo automáticamente; **la primera vez sí debes reemplazar el workflow viejo**.
- Para instalar sobre una versión anterior se necesita la MISMA firma; el APK debug usa otra, no desinstales nada sin copia.
- Publicación Google Play: requerirá firma release y verificación de todas las políticas vigentes.
