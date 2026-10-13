# Zeta Motion 16 — Sonidos de ambiente, encuadre libre, exportación compacta y escenas anime

Se trabaja sobre el código de v15. Mismo applicationId `com.zeta.fondo` y misma firma: se instala encima sin perder proyectos. versionCode = 16.

## Audio
- Sonidos de ambiente generados por código (sin grabaciones ni derechos): lluvia suave, lluvia fuerte, trueno, relámpago, viento suave, ventarrón, arena y brisa, olas, río, cascada, pájaros, grillos, fuego, hojas y ciudad lejana.
- Se mezclan varios a la vez, cada uno con su volumen, junto con la música. Cada sonido se genera para un bucle exacto con fundido interno: no salta al repetirse.
- Trueno y relámpago suenan en el mismo instante en que cae el rayo del efecto Relámpago.
- Música: onda con zoom (pellizco y botones), arrastre fuera del cuadro para recorrer canciones largas, arrastre del cuadro morado con avance automático en los bordes, toque para llevar el cuadro, mapa de la canción completa debajo, botón para ir a la selección y fundido suave al empalmar.
- Vista previa y MP4 usan la misma mezcla.

## Formato
- Botones con el rectángulo dibujado a la proporción real (9:16, 16:9, 1:1, 3:4, 4:3, completo).
- Cambiar de formato ya no recrea la vista OpenGL: el cuadro se anima suave.
- Encuadre libre: al elegir un formato aparece un marco sobre la imagen completa; se arrastra para elegir la parte y se pellizca para agrandar o achicar. "Centrar" y "Listo".

## Exportación
- Configuración corta: resolución, fps, una línea de resumen (tamaño, duración, formato, MB, sonido) y "Más ajustes" en filas compactas. Se quitaron encuadre y duración repetidos.
- Resultado centrado con el vídeo reproduciéndose en bucle dentro de la app y botones Alight Motion, Compartir, Galería y Otra.
- 1080×1920 exacto: si el codificador de hardware exige otra alineación (salía 1072×1920), se usa otro codificador que acepte el tamaño exacto.

## Efectos
- Catálogo por categorías (Clima, Agua y cielo, Luz, Anime, Manga, Cámara) con iconos animados dibujados en código.
- Toque = activar/desactivar. Pulsación larga o el botón de ajustes = abrir sus controles.
- Nuevos: Relámpago (rayo ramificado + destello), Ráfaga de viento (líneas curvas con hojas y polvo), Remolino / tornado, Aparición de personaje (PNG con humo, ráfaga, destello o fundido), Nubes en capas (paralaje), Olas con espuma, Reflejo del sol en el mar e Impacto (zoom de golpe con temblor).
- Línea de tiempo bajo el vídeo: arrastra las marcas de Viento, Personaje e Impacto para elegir cuándo ocurren. Las marcas amarillas son los rayos.

## Comprobaciones
- No se ejecutó una compilación de Android aquí: este entorno no tiene Android SDK ni Gradle. El workflow de GitHub ejecuta las pruebas unitarias (incluidas las nuevas de V16Test) y compila el APK.
