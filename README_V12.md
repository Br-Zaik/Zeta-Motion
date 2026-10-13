# Zeta Motion 12

Editor para animar imágenes propias. Paquete y firma conservados respecto a v11.

## Uso

- Importar: Recientes, Álbumes con miniatura/cantidad y búsqueda por nombre de carpeta. Recuerda el último álbum y posición. También Fotos del sistema y Archivos. La galería interna pide permiso; los selectores del sistema siguen disponibles sin él.
- Animar: rutas, anclas, borrar y mover; cantidad de movimiento y velocidad.
- Proteger: pincel para inmovilizar partes frente al movimiento de las rutas. Pincel, borrador, tamaño y Listo. Cancelar restaura los cambios del panel.
- Efectos: catálogo único con iconos vectoriales, sin categorías ni pestaña duplicada de presets. Al elegir un efecto aparecen sus controles y Toda la imagen/Pintar zona. Listo confirma y X cancela. Tocar Efectos vuelve al catálogo confirmando el ajuste actual.
- Cada efecto tiene su zona propia. Se conservan las zonas antiguas internamente para abrir proyectos previos, sin mostrar botones azul/verde/nueva. Máximo nueve zonas nuevas independientes.
- Cielo, Agua y Dispersión comienzan pidiendo pintar la zona. Al terminar se pueden ajustar sus parámetros o escoger Toda la imagen.
- Al pintar se muestra el original y la zona seleccionada, para ubicar el pincel aunque haya efectos o movimientos activos. Lupa en Android 9 o posterior.
- Ajustes reúne audio importado, duración de 2 a 60 segundos y formato original, 1:1, 3:4, 4:3, 9:16 y 16:9.
- Exportar permanece arriba. Lienzo y panel inferior conservan su espacio al cambiar de herramienta. En horizontal el panel va al costado. La pantalla completa oculta los paneles y mantiene la misma superficie de renderizado.

## Nuevas herramientas

- Cielo: día, atardecer, noche y tormenta, generados por código; mezcla e intensidad dentro de la zona pintada. No segmenta automáticamente el cielo.
- Agua: ondas, corriente y reflejos; intensidad, velocidad y dirección.
- Cámara: acercar, alejar, desplazar, girar y temblar. Movimiento periódico para fondos en bucle.
- Dispersión: fragmentos de la imagen, pétalos o polvo; intensidad, tamaño, dirección, distancia y velocidad. El hueco se rellena mediante aproximación de colores cercanos, no IA; imágenes complejas pueden mostrar un relleno imperfecto.
- Elementos: nubes, humo, pétalos, brillos, energía y niebla, hasta cuatro instancias independientes. Arrastrar, pellizcar y girar con dos dedos; tamaño, giro, opacidad y velocidad. Son elementos procedurales, no una biblioteca de fotos o stickers externos.
- Se conservan los efectos anteriores y el audio importado de la v11. Sin catálogo de sonidos sintéticos ni fondos ilustrados incluidos.

## Rendimiento y guardado

Miniaturas y lectura de álbumes fuera del hilo de interfaz; cuadrícula carga solo miniaturas visibles. Vista previa hasta 30 FPS; pausada, solo vuelve a dibujar con cambios. No se recompila el shader al abrir una herramienta. El reloj actualiza únicamente la barra de transporte. Al pintar se omite el trabajo de efectos del shader. Un solo motor se utiliza en vista previa y exportación.

Los ajustes en edición se aíslan del guardado automático hasta confirmarlos. Cancelar restaura parámetros, zonas y guías. Guardados serializados; las versiones de máscara son crecientes para evitar texturas antiguas tras cancelar y reutilizar una zona.

## Compilar y comprobar

Java 17, Android SDK 34, Gradle 8.9:

```
gradle testReleaseUnitTest assembleRelease
```

Se incluyen diez pruebas unitarias: cinco de audio PCM y cinco de parámetros, aislamiento de instancias, activación y selección de zonas. `tools/check_render.py` verifica el shader real con EGL/OpenGL ES 3 y Mesa (Python, numpy y Pillow), los límites de las zonas pintadas, cierre de bucles, cinco modos de cámara y seis tipos de elementos. Se ejecuta desde la raíz del código y genera imágenes de diagnóstico en `checks/`.

Verificación en equipo de desarrollo; no equivale a una prueba en teléfono. Pendiente comprobar interacción táctil, permisos del fabricante, pantalla completa, audio y exportación con la GPU/codificador del dispositivo real. No se garantiza una tasa de cuadros idéntica en todos los teléfonos ni al combinar muchos efectos o exportar en 4K.
