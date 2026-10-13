# Zeta Motion v26 — Apertura nítida + efectos limpios + exportación directa

Cambios sobre v25:

- El editor ya no abre con `thumb.jpg` de 480/640 px. Cada proyecto crea `edit_preview.jpg` de hasta 2304 px y calidad 97 para mostrarse nítido desde el primer cuadro.
- Los proyectos viejos crean esa caché una sola vez al abrirse. La imagen original sigue intacta y se usa para exportación.
- Se eliminó la pestaña y panel **Rápido**. Los efectos y sus variantes siguen en Todos / Clima / Agua y cielo / Luz / Anime / Manga / Cámara.
- Exportar ya no tiene el desplegable **Más ajustes**: formato, tamaño, códec, calidad, repetir y solo efectos aparecen directamente.
- El botón Exportar sigue fijo abajo; la zona central solo hace scroll si una pantalla realmente no alcanza.
- Diálogo de exportación más alto y compacto para aprovechar la pantalla sin cortar controles.

La caché de edición es solo para el editor. No reduce la resolución ni la calidad del archivo exportado.
