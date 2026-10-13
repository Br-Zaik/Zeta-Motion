# Zeta Motion v19 — Línea de tiempo más fluida

Optimización sobre la base v18, sin cambiar el sistema de importación, exportación ni los efectos añadidos en v17/v18.

## Cambios
- El bloque de efecto de la línea de tiempo sigue el dedo de forma local e inmediata.
- Mientras se arrastra ya no se reconstruye todo el estado Compose en cada píxel.
- El preview OpenGL del efecto movido se actualiza con límite aproximado de 30 fps durante el arrastre para evitar saturación.
- La posición definitiva solo se confirma y guarda al soltar el bloque.
- Mover un bloque ya no reinicia innecesariamente el reloj del bucle en cada actualización.
- Los bloques mantienen un orden/carril estable al cruzarse para evitar saltos visuales.
- El cabezal de reproducción de la línea de tiempo se sincroniza con los frames de Compose, en lugar de avanzar a saltos de 60 ms.
- Se usa traslación de GPU para desplazar visualmente los bloques, reduciendo trabajo de layout.

## Se mantiene
- Importación por Galería y Archivos.
- Duración personalizada del vídeo.
- Múltiples copias del mismo efecto.
- Varios efectos en el mismo segundo.
- Efectos de todo el vídeo y bloques independientes.
