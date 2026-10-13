# Zeta Motion 14 — bordes, protección y movimiento por zona

Entrega de código fuente; no incluye una APK. Se conservan paquete y firma.

## Cambios

- Bordes: el movimiento de rutas/deriva y las deformaciones se limitan suavemente según el espacio disponible dentro de la imagen. Los centros de los píxeles exteriores quedan fijos en el eje del desplazamiento. Esto evita prolongar la última columna/fila de píxeles al salir de la imagen en Continuo, Ida y vuelta o Pulso. Cerca del límite se reduce el movimiento; no inventa contenido fuera del dibujo.
- Efectos: botón «Quitar todos» fijo encima del catálogo, con cantidad de instancias activas. Desactiva efectos, copias y elementos; conserva parámetros, zonas, color y animación de rutas/por zona. Deshacer restaura sus estados y Rehacer vuelve a desactivarlos.
- Proteger: reúne Pintar área, Fijar punto (la antigua Ancla) y Borrar punto. El borrado de puntos en esta pestaña no borra rutas. Ancla ya no aparece en Animar. Pintar protege un área; los puntos de apoyo siguen actuando sobre el campo de movimiento de las rutas.
- Inicio: únicamente Importar arriba. Se eliminó la tarjeta duplicada que aparecía cuando no había proyectos.
- Animar → Por zona: pintar una zona y elegir Balanceo, Ondulación o Respiración. Fuerza, velocidad y dirección (en Balanceo/Ondulación), zona editable, activar/quitar, confirmar y cancelar. Comienza por pintar, sin requerir flechas. Proteger también detiene estos movimientos.

## Uso de Por zona

1. Animar → Por zona.
2. Pintar cabello, ropa, follaje o la parte deseada; Listo.
3. Elegir movimiento y ajustar Fuerza y Velocidad. Reproducir permite revisar el resultado.
4. Listo confirma; X revierte los cambios del panel.

Es deformación local de una imagen, no articulación anatómica ni generación de fotogramas con IA. No reconstruye partes ocultas. Mantiene una zona de movimiento de este tipo por proyecto y el límite de nueve zonas independientes existente; se pueden pintar varias regiones dentro de ella.

## Validación

`gradle clean testReleaseUnitTest` compila el código release y ejecuta las pruebas, sin empaquetar APK.

Pruebas unitarias de audio, parámetros y «Quitar todos»: conservación de animación/máscaras/color, inclusión de copias y elementos, restauración de estados y proyecto sin efectos activos.

`python3 tools/check_render.py` ejecuta el shader real en EGL/OpenGL ES 3 (Mesa, numpy y Pillow). Incluye las pruebas anteriores de máscaras/color/bucles; 60 combinaciones de modos, direcciones y fases con desplazamientos extremos para comprobar bordes; los tres movimientos por zona; y bloqueo mediante la máscara Proteger.

No se probó físicamente en teléfono. Las pruebas de código y renderizado en software no sustituyen la comprobación de gestos, fluidez y exportación con el dispositivo real. El script genera imágenes de diagnóstico en `checks/`, fuera del ZIP entregado.
