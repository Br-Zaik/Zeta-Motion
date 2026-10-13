# Zeta Motion v24 — biblioteca de ambientes y variantes naturales

Esta versión amplía los efectos para fondos de anime, manga, manhwa e ilustraciones sin convertir la app en un editor complicado.

## Biblioteca rápida

En **Efectos > Rápido** ahora hay filtros compactos: **Todos, Viento, Hojas, Agua, Clima, Ambiente y Anime**. Los presets son editables después de aplicarlos.

Se añadieron combinaciones listas para usar:

- Brisa suave, Viento fuerte y Viento de tormenta.
- Árbol con brisa y Árbol viento fuerte.
- Hojas verdes, secas, de otoño y de Navidad.
- Navidad con nieve.
- Olas suaves, Olas fuertes y Mar de tormenta.
- Lluvia suave y Lluvia fuerte.
- Nieve suave, Nieve fuerte y Ventisca.
- Nubes suaves, Niebla baja, Polvo de luz, Fuego y brasas y Noche mágica.

## Variantes dentro del propio efecto

No se duplicaron botones innecesariamente. Al abrir el ajuste del efecto aparecen estilos de un toque:

- **Viento:** Brisa / Viento / Fuerte / Tormenta.
- **Hojas:** Verdes / Secas / Otoño / Navidad.
- **Olas:** Suaves / Medias / Fuertes / Tormenta.
- **Lluvia:** Suave / Normal / Fuerte / Tormenta.
- **Nieve:** Suave / Normal / Fuerte / Ventisca.

Cada estilo configura intensidad, velocidad y parámetros relacionados, y luego se puede ajustar manualmente.

## Hojas estacionales reales en el render

Las hojas ya no son siempre verdes. El shader usa cuatro paletas procedurales distintas y las mismas se usan en vista previa y exportación:

- verde natural;
- seca marrón/amarilla;
- otoño naranja/rojo;
- Navidad verde oscuro con pequeños acentos rojos.

No se añadieron texturas externas ni assets pesados, por lo que el cambio mantiene bajo el uso de memoria.

## Compatibilidad

Se conserva todo lo de v23: carga rápida, timeline libre tipo CapCut, bloques repetibles, varios efectos en el mismo segundo, Movimiento Natural, Profundidad 2.5D, Deformación, Cámara, Bucle perfecto e importación desde Archivos.

`versionCode = 24`, `versionName = 24.0.0`. Se mantiene `applicationId = com.zeta.fondo` y la misma firma del proyecto para poder instalar encima de versiones anteriores.
