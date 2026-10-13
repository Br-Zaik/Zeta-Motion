# Zeta Motion 17 — duración al importar y efectos repetibles en la línea de tiempo

Se trabaja sobre Zeta Motion v16. Se conserva `applicationId = com.zeta.fondo`, namespace y la misma configuración de firma para que el proyecto siga siendo compatible con la instalación anterior. `versionCode = 17` y `versionName = 17.0.0`.

## Duración del vídeo
- Al importar una imagen desde Inicio se pregunta primero la duración del vídeo.
- Presets rápidos: 4, 6, 10, 15 y 30 segundos.
- Control personalizado de 2 a 60 segundos.
- El valor `6s` de la barra del editor ahora abre directamente el selector de duración.
- Si se cambia la duración después, los bloques de vídeo completo se adaptan al nuevo final y los demás se mantienen dentro del rango válido.

## Efectos repetibles
- Cada aparición de un efecto se guarda como un `EffectClip` independiente con inicio y duración en segundos.
- El mismo efecto puede añadirse varias veces sin reemplazar la copia anterior.
- Puede coexistir una copia que cubra todo el vídeo con otras copias cortas del mismo efecto.
- No existe un límite pequeño de cantidad de bloques. Las copias que coinciden en el tiempo se acumulan con protección de intensidad en el render para evitar valores extremos.
- En los ajustes de cada efecto aparecen `Todo el vídeo` y `+ Otra vez`, una lista de las copias creadas y controles de Inicio y Duración para la copia seleccionada.
- Cada copia puede eliminarse individualmente y `Quitar todo` elimina todos los efectos.

## Línea de tiempo
- Los clips se muestran como bloques con ancho proporcional a su duración.
- Cada bloque puede arrastrarse horizontalmente al segundo deseado.
- El cabezal de reproducción y marcas de segundos permanecen visibles.
- Viento, impacto y relámpago ya no dependen de un único momento global cuando usan los nuevos bloques; impacto y relámpago reciben tiempo local por bloque.
- Los proyectos v16 sin bloques guardados mantienen compatibilidad con la línea de tiempo heredada hasta que el efecto se edite o se añada otra copia.

## Guardado y exportación
- Los bloques se guardan en `project.json` dentro de `effectClips` y se restauran al abrir el proyecto.
- `RenderParams` incluye la línea de tiempo tanto en vista previa como al exportar, por lo que los bloques afectan también al vídeo final.
- Deshacer/rehacer activación global conserva también la lista de clips para no perderlos al quitar y restaurar efectos.

## Comprobaciones realizadas aquí
- Shader OpenGL ES: compilación, enlace y batería `tools/check_render.py`: correcta.
- Prueba directa con `kotlinc` sobre `EffectTimeline`: correcta, incluida superposición de un efecto continuo con otra copia y repeticiones en distintos segundos.
- Se añadió `EffectTimelineTest.kt` para GitHub Actions.
- No se generó APK en este entorno; el workflow incluido sigue ejecutando `gradle testReleaseUnitTest assembleRelease` en GitHub Actions.
