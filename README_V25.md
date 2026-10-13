# Zeta Motion v25 — fluidez, nitidez y edición más directa

Esta versión pule la experiencia de edición sin quitar funciones de v24.

## Vista previa más nítida y rápida

- El thumbnail sigue apareciendo primero para evitar pantalla negra.
- La imagen de trabajo se decodifica de forma adaptativa según la pantalla y memoria del dispositivo, normalmente entre 2048 y 3072 px de lado largo.
- Android 9+ usa `ImageDecoder` al tamaño objetivo para evitar decodificar una imagen enorme y reducirla después.
- La textura mantiene mipmaps y filtrado lineal para reducir serrucho.
- Hay protección automática si una GPU antigua admite una textura menor.
- Durante zoom, desplazamiento, pincel, sliders y timeline se activa temporalmente una cadencia de hasta 60 fps.
- Con escenas pesadas la cadencia se reduce de forma automática para no saturar el teléfono.

## Viento, Olas, Hojas, Lluvia y Nieve visibles

En **Efectos > Rápido** aparecen accesos directos a las cinco familias principales. Al tocarlas se muestra un selector compacto:

- Viento: Suave / Normal / Fuerte / Tormenta.
- Olas: Suaves / Medias / Fuertes / Tormenta.
- Hojas: Verdes / Secas / Otoño / Navidad.
- Lluvia: Suave / Normal / Fuerte / Tormenta.
- Nieve: Suave / Normal / Fuerte / Ventisca.

También se muestran en Anime para que no queden escondidas entre categorías. Elegir un estilo activa el efecto y crea un bloque editable sin obligar a entrar al panel de máscaras.

## Movimiento Natural más automático

Árbol, Bandera, Pasto, Cortina, Cabello y Ropa ahora cargan valores completos de física y anclaje al seleccionar el tipo:

- Árbol y pasto fijan la base.
- Bandera fija el lado del poste.
- Cortina, cabello y ropa fijan la parte superior/unión.
- Cada material usa fuerza, flexibilidad, variación y ráfagas distintas.
- La máscara pintada se conserva al cambiar de material.

## Timeline tipo CapCut

- Un efecto nuevo aparece centrado en el vídeo, no pegado al cabezal o al extremo derecho.
- Mantener pulsado el centro mueve el bloque.
- Los extremos izquierdo/derecho ajustan inicio y duración.
- Se puede mover entre pistas y superponer varios efectos en el mismo segundo.
- La vista previa del movimiento de bloques se actualiza hasta 60 fps durante el gesto.

## Exportación adaptable

- El cuadro usa un ancho adecuado a la pantalla.
- Las opciones son desplazables.
- El botón **Exportar** queda fijo abajo y no se corta aunque `Más ajustes` esté abierto.
- 1080p / 2K / 4K y FPS siguen visibles arriba.

## Compatibilidad

Se conserva todo lo anterior: importación desde Archivos, duración personalizada, efectos repetibles, múltiples efectos simultáneos, Profundidad 2.5D, Deformación, Cámara, Bucle perfecto, ambientes de v24, audio y exportación MP4/PNG.

`versionCode = 25`, `versionName = 25.0.0`, `applicationId = com.zeta.fondo`. Se mantiene la misma firma del proyecto.
