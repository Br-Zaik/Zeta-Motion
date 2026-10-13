# Zeta Motion 11 — editor para tus imágenes

## Cambios

- Inicio con Importar y proyectos propios. Se retiraron los fondos ilustrados incluidos; no se borran proyectos existentes.
- Lienzo con tamaño independiente de la herramienta abierta. Panel inferior fijo y panel lateral en pantallas horizontales.
- Exportar únicamente arriba. Sin tira de miniaturas ni controles duplicados de exportación.
- Reproducir, pausa, búsqueda temporal, duración, deshacer, rehacer y pantalla completa.
- Efectos en cuadrícula directa, con iconos vectoriales y nombres breves. Al seleccionar uno aparecen sus parámetros en el mismo espacio. Tocar Efectos vuelve al catálogo. Sin categorías ni botón Todos.
- Intensidad, velocidad y parámetros propios del efecto; dirección, origen y selección de máscara donde corresponden.
- Animar con rutas, anclas, congelación y borrador. Movimiento y velocidad juntos. Máscaras con tamaño y suavizado juntos.
- Formatos completo, 9:16, 16:9, 1:1, 3:4 y 4:3; el recorte de la vista previa coincide con la exportación.
- Se retiró el generador de sonidos sintéticos. Audio permite importar grabaciones del teléfono, cambiar volumen, silenciar, repetir o quitar la pista.
- Importación mediante los decodificadores del dispositivo, mono o estéreo. Límite de 10 minutos y 128 MiB de PCM. La grabación queda dentro del proyecto; no depende del archivo externo después de importarla.
- La vista previa y el MP4 usan la misma fuente PCM y el mismo comportamiento de repetición. El audio se reinicia con cada bucle de animación. PNG no incluye audio.

## Rendimiento

- Superficie OpenGL única al cambiar de herramienta y contexto conservado al pausar.
- La barra de reproducción actualiza su propio estado, sin reconstruir todo el editor cada tick.
- Renderizado de vista previa limitado a aproximadamente 30 FPS. Pausado, solo pide cuadros cuando cambian los parámetros, las máscaras o la vista.
- Las miniaturas de proyectos se decodifican fuera del hilo de interfaz.
- Audio en un hilo dedicado y exportación AAC fuera del hilo de interfaz; respeta la pausa, el segundo plano y el foco de audio de Android.

## Compilar

Java 17, Android SDK 34, Gradle 8.9:

```sh
gradle testReleaseUnitTest assembleRelease
```

El workflow incluido ejecuta esas dos tareas y publica el APK como artefacto. La firma y el identificador de paquete se conservan para actualizar la app existente.

## Alcance de la verificación

Verificado: `testReleaseUnitTest assembleRelease` terminó correctamente; cinco pruebas pasaron y la firma del APK fue verificada.

Pruebas unitarias de lectura PCM: canales estéreo, silencio al terminar una grabación, volumen, reinicio con el bucle de animación y metadatos de longitud desactualizados.

La compilación y las pruebas unitarias no sustituyen una prueba en teléfono. Queda pendiente verificar visualmente las transiciones de pantalla completa, escuchar la importación y comprobar la sincronía AAC/MP4 y la fluidez con el codificador y la GPU del dispositivo real.
