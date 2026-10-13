# Zeta Motion v33 — Herramientas coherentes, variantes reales y movimiento 2D/2.5D/3D simulado

Actualización incremental de Zeta Motion v32 para animar **fondos e ilustraciones anime, manga y manhwa**. Se conservan los **62 IDs de efectos**, el formato de proyectos, todas las máscaras, flechas y anclajes, los controles de línea de tiempo, las mejoras de memoria/zoom de v32 y el mismo `applicationId`. No se crean efectos duplicados de lluvia, nieve, viento, agua, etc.

## Cambios incorporados

1. **Borrador reconocible:** vector con silueta de goma, sustituyendo el icono anterior similar a una varita. Accesibilidad: `Borrador`.
2. **Duplicar con dos hojas:** sustituye `+ Copia` por un botón con `ContentCopy`; mismo icono en catálogo y controles del bloque, conservando su comportamiento. Los ajustes y deslizadores mantienen texto.
3. **Semántica clara:** el interruptor visual de cada efecto muestra ojo abierto/cerrado para activar u ocultar (no una papelera que sugería eliminar). Las ayudas 3D se muestran a petición con `?`.
4. **Rayos de luz, 5 variantes **reales**:** suaves (original), contraluz, atardecer, ventana y haces volumétricos. Cambian color, geometría y atenuación en shader, no solo la etiqueta. Sigue funcionando la máscara y la línea de tiempo.
5. **Reflejo del sol, 4 variantes reales:** dorado, atardecer, luna y destellos finos. Sigue limitado a la misma zona pintada; accesible también desde la categoría Luz y desde Útiles.
6. **Aura anime, 4 variantes:** azul, violeta, dorado y rojo, dentro de Energía existente.
7. **Llamas, 4 variantes:** fuego normal, azul, verde y violeta; conserva sus controles de máscara, tamaño, velocidad e intensidad.
8. **Movimiento 2D:** se mantienen `Natural`, `Por zona`, `Deformar`, cámara plana y flechas. `Natural` incorpora **Peso y Elasticidad** con retraso e influencia sobre rebote para cabello/ropa; los seis presets ahora asignan estos valores. No es simulación de cuerpos rígidos o física de mallas Live2D.
9. **2.5D:** los planos fondo/frente ya existentes tienen tres movimientos distintos: paralaje horizontal/original, órbita elíptica y acercamiento por profundidad. Conserva congelación y máscaras pintadas.
10. **3D simulado:** nuevo modo **Órbita 3D sim.** dentro de Cámara, con cizallamiento/perspectiva 2D suave, zoom de seguridad y recorrido periódico que regresa al inicio. **No convierte la imagen plana en un modelo 3D real, ni descubre zonas ocultas**; esa función exigiría capas separadas o reconstrucción de profundidad.
11. **Editor adaptable:** se mantienen de v32 el panel compacto, el mayor espacio de lienzo, los íconos para pintar/proteger, ajuste de tamaño/feather, caché para abrir imágenes, ajustes de vista sin valores no finitos y resto de mejoras anteriores. No se ha rediseñado de cero.
12. **Versión:** `versionCode = 33`, `versionName = 33.0.0`.

## Pruebas realizadas aquí

- Shader OpenGL ES 3.0 compilado y enlazado en Mesa EGL por software.
- Test de máscara aislada, congelación, anclajes, deformación, color, varios tipos de movimiento, continuidad temporal.
- Tests **nuevos v33** de cinco estilos de rayos, cuatro de iluminación/reflejo, cuatro paletas de aura, cuatro de llamas, tres estilos de parallax, seis modos de cámara y cambios observables de Peso/Elasticidad para cabello/ropa. Comprueban que los efectos se ven, tienen diferencias, respetan máscaras y cierran el bucle (ver `tools/check_render.py`).
- Comprobación sintáctica de todos los `.kt`: cero errores sintácticos.
- Comparación de definiciones de efectos con v32: 62 IDs conservados, ninguno añadido/eliminado.

## Pendientes de validación

- **No se ha compilado el APK completo ni se ha probado en teléfono Android real**, debido a la ausencia de Android SDK/Gradle en este entorno. El parser Kotlin no verifica tipos ni dependencias Android. No se garantiza ausencia de cierres, tirones ni FPS constantes en todos los móviles; hacen falta pruebas de integración, pantalla y exportación en el dispositivo real.
- Las variantes de movimiento son efectos 2D/2.5D/simulación visual 3D, no animación 3D de geometría. La variedad añadida cubre familias seleccionadas, no se afirma haber reescrito todos los efectos existentes.
- El flujo de exportación no se modifica; todavía necesita comparación de fotogramas entre vista previa y vídeo exportado.
- Se conserva la firma anterior para compatibilidad de actualización. `app/fondozeta.jks` y su configuración están incluidos como en las versiones anteriores. No compartir públicamente el ZIP sin revisar sus credenciales.

## Compilación

Abrir el proyecto con Android Studio usando JDK 17 y SDK 34, o cargar los fuentes a un repositorio privado y ejecutar el workflow `.github/workflows/build.yml`. Probar en un móvil antes de distribuir.
