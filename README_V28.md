# Zeta Motion v28 — Duplicar bloques + exportación clara + zoom fluido

Cambios de esta versión:

- **Duplicar bloque**: un viento/efecto ya ajustado puede copiarse conservando fuerza, velocidad, dirección, estilo, máscara y duración.
- El botón **+** de un efecto activo ahora duplica el bloque seleccionado (o el más cercano) en vez de crear una repetición genérica.
- La copia se coloca después del original cuando cabe; si no, usa el hueco/pista libre más cercana. Un bloque de todo el vídeo se duplica en otra pista para que siga siendo editable.
- En el panel del efecto aparece un botón **Duplicar** junto a **Eliminar**.
- **Resolución de exportación unificada**: 720p, 1080p, 2K, QHD, 4K, Original y Otro viven en un solo selector. Se eliminó el segundo selector “Tamaño”.
- Si vuelves a tocar **Original** u otra resolución especial ya seleccionada, vuelve de forma segura a **1080p**; nunca queda la exportación sin tamaño válido.
- **2K y QHD ya no son el mismo tamaño**: 2K usa lado largo 2048 y QHD 2560.
- **Primer zoom más fluido**: la imagen de edición ya no se reemplaza segundos después de abrir el proyecto. Esto evita decodificar/subir otra textura justo cuando haces pinch/zoom.
- La caché de edición de proyectos nuevos sube a **2560 px** de lado largo para mantener líneas de anime nítidas sin cargar la imagen original 4K durante la edición.
- Zoom/pan actualizan OpenGL en **una sola invalidación por gesto** en lugar de varias por cada movimiento del dedo.
- La imagen original completa sigue usándose al exportar; la optimización solo afecta a la vista previa del editor.
- `versionCode 28` / `versionName 28.0.0`.

Se conservan timeline libre tipo CapCut, múltiples efectos en el mismo segundo, Movimiento Natural, máscaras, 2.5D, deformación, cámara, presets y exportación H.264/HEVC.
