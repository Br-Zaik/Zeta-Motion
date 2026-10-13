# Zeta Motion v30

Versión centrada en fondos anime, manga y manhwa con efectos localizados.

## Cambios principales
- Nueva categoría **Manhwa**.
- Olas corregidas: la primera vez obligan a pintar agua/orilla en vez de cubrir toda la pantalla.
- Olas con espuma más natural: usa el color de la imagen y permite Natural, Turquesa, Azul, Atardecer y Noche.
- Las máscaras de efectos se pueden **reutilizar**: una zona de agua puede compartirse entre Olas, Reflejo, Cascada, etc.
- Nuevos efectos: Bokeh, Destellos suaves, Luz ambiental, Cascada, Reflejo de agua, Aura oscura, Tinta en movimiento y Sombra dramática.
- Los nuevos efectos admiten máscara por zona y bloques en la línea de tiempo.
- Anime/Manhwa/Manga reorganizados para encontrar más rápido efectos adecuados al escenario.
- Los efectos nuevos son procedurales y se calculan solo cuando están activos para no añadir carga permanente.
- Mantiene v29: duplicado alineado, guías magnéticas, cabezal único de timeline, importación desde Archivos, Movimiento Natural, 2.5D, exportación 1080p/2K/4K y demás funciones existentes.

## Validación
- Shader GLSL ES 3.0 compila y enlaza.
- 8 efectos v30 probados con máscara: no alteran píxeles fuera de la zona.
- Olas probadas con máscara localizada.
- Nuevos efectos y olas cierran el bucle entre t=0 y t=1.
- Pruebas de render anteriores siguen pasando.
