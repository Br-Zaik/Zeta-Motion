# Zeta Motion v35 — composición manga/manhwa (código fuente)

Partiendo de v34 compatible con Gradle 8.9 y AGP 8.7.3. Conserva las herramientas, 62 efectos previos y los IDs de los proyectos.

## Nuevas herramientas
- **Humo** existente: cinco variantes (gris, negro, blanco, violeta, cálido), densidad y deriva. Posición delante/detrás de un **PNG de Aparición**; en una ilustración plana debe pintarse la máscara para no tapar al personaje.
- **Explosión anime**: núcleo luminoso, anillo expansivo, chispas y humo con parámetros separados; cuatro estilos y origen posicionable.
- **Salpicaduras manga**: gotas estilizadas, tres paletas y origen; no es un simulador físico de líquidos.
- **Corte de energía**: trazos animados en cuatro colores, ángulo y longitud.
- **Revelación entre humo**: oscurece y descubre progresivamente la región de una imagen, de forma cíclica. **No elimina al personaje ni reconstruye el fondo detrás**; si necesitas un personaje que aparezca realmente, importa PNG transparente en Aparición.
- **8 presets guiados**: dos humos, explosión, corte, tinta, revelación, cielo vivo y ojo con brasas.

Todos usan el mismo shader de vista previa y exportación. Soportan máscaras por efecto, parámetros y líneas de tiempo existentes; los nuevos efectos se dibujan solamente cuando están activos. Las animaciones cierran en bucle.

## Pruebas
Ejecuta `python tools/check_render.py` y `python tools/check_v35_fx.py` desde el directorio del proyecto (EGL/GLES3 por software). Para comprobar Android: sube TODO el contenido (incluida `.github`), ejecuta el workflow y prueba en un teléfono. **No se garantiza APK Android compilado sin CI real.**

## Límites
2D y 2.5D simulada, no reconstrucción automática 3D. Efectos procedurales ilustrados, no reproducción exacta de escenas originales. No incluye animación esquelética ni segmentación IA automática. Para humo detrás de personas de la misma imagen, debes pintar una máscara que excluya la figura o usar el PNG adicional de Aparición.
