# Zeta Motion 15 — Selección de audio, velocidad y guardado compacto

Se trabaja sobre el código de v14. Proyecto fuente ZIP; no contiene APK. Se conserva applicationId `com.zeta.fondo` y `app/fondozeta.jks`; versionCode=15 para instalar encima de v14.

## Audio

- Ajustes → Audio: onda del audio importado, ventana de duración igual a un bucle del vídeo y selección arrastrando o tocando la forma de onda.
- Se muestra el inicio y el final del fragmento (ejemplo `00:32 → 00:37`) junto a «Escuchar con animación / Pausar».
- La selección `audioStartFrame` se conserva por proyecto en el JSON de exportación. Cambiar de canción reinicia la selección al inicio.
- Vista previa y codificación AAC del MP4 comparten el mismo lector PCM: reproducen el fragmento elegido, no el principio de la canción. Si «Repetir» está activo y el archivo es más corto que el bucle, se repite; si no, completa con silencio. Si se repite el movimiento del MP4, se repite ese fragmento en cada bucle.
- La onda se muestrea con pocas lecturas fuera del hilo de interfaz para evitar retrasos en móviles modestos.

## Bucle

- Animar → Bucle de movimiento: control «Velocidad» fijo, incluso al desplazarse, con el mismo parámetro `flow.cycles` que Animar, permite probar cambios durante la reproducción. Duración/ciclos indican tiempo por ciclo.
- «Repeticiones del MP4» mantiene duración adicional sin modificar la velocidad del movimiento.

## Exportación

- Mientras el MP4/PNG se genera, el diálogo se vuelve compacto y centrado: «Exportando…», porcentaje, barra y «Cancelar». El resto de controles «Calidad y formato» permanecen solo en la configuración previa.

## Comprobaciones efectuadas

- Se compiló el modelo JSON y el lector PCM con `kotlinc` y se ejecutaron 10 comprobaciones sobre selección, repetición, silencio, cambio de duración, volumen y serialización.
- **No se ejecutó una compilación completa de Android ni se probó en un teléfono aquí**: este entorno no tiene Android SDK/Gradle instalados. Al subir el ZIP a GitHub, el workflow existente ejecuta `gradle testReleaseUnitTest assembleRelease` y compila el APK.
