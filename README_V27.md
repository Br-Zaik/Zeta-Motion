# Zeta Motion v27 — Exportación compacta + recorte de timeline en bordes

Cambios de esta versión:

- El cuadro **Exportar vídeo** ya no fuerza una altura grande ni deja un bloque negro vacío debajo de las opciones.
- La ventana se adapta al contenido y solo usa scroll interno cuando la pantalla realmente no alcanza.
- El botón **Exportar** permanece visible abajo.
- Se redujeron espacios verticales para aprovechar mejor la pantalla.
- Los tiradores izquierdo y derecho de los bloques de la timeline ahora tienen una zona táctil mayor (28 dp).
- El centro del bloque reserva espacio a los tiradores para que el gesto de mover no robe el gesto de recortar.
- Si un bloque empieza exactamente en **0 s**, el borde izquierdo se puede arrastrar hacia la derecha para acortarlo.
- Si un bloque termina exactamente al final del vídeo, el borde derecho se puede arrastrar hacia la izquierda para acortarlo.
- El cálculo de recorte usa un ancla fija tomada al comenzar el gesto, evitando saltos o acumulación de error mientras se arrastra.
- Se mantiene la vista previa ligera durante el recorte y solo se guarda al soltar.
- `versionCode 27` / `versionName 27.0.0`.

Se conservan las funciones de v26: caché nítida de edición, efectos por categorías, Movimiento Natural, múltiples clips de efectos, timeline libre, 1080p/2K/4K, 24/30/60 fps, H.264/HEVC y exportación directa.
