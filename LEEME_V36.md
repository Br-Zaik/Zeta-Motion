# Zeta Motion v36 (versionCode 37 · 36.0.0)

Gratis, sin anuncios ni compras. Paquete com.zeta.fondo (beta: com.zeta.fondo.beta).

## Cómo compilar
- Sube TODO el contenido de este ZIP a la raíz del repositorio (incluida la carpeta oculta `.github`).
- GitHub Actions ejecuta: pruebas unitarias → APK + AAB firmados → verificación de firma y paquete.
- Artefacto: **ZetaMotion-v36-APK-AAB** (extrae el APK antes de instalarlo).
- Sin secretos: se genera Zeta Motion Beta (instalable junto a la versión anterior).
- Para Google Play: crea los 4 secretos del repositorio (Settings › Secrets › Actions):
  `ZETA_RELEASE_KEYSTORE_BASE64`, `ZETA_KEYSTORE_PASSWORD`, `ZETA_KEY_ALIAS`, `ZETA_KEY_PASSWORD`.
  La clave privada NUNCA va en el repositorio (`.gitignore` bloquea `*.jks` salvo la clave pública de pruebas).
  Sube el `.aab` a Play Console con Play App Signing.

## Cambios principales

### 1. Estabilidad y rendimiento
- **Parámetros en Uniform Buffer (std140)**: antes había ~250 uniforms sueltos, al límite de GPUs Adreno/Mali económicas (224-256 vectores); en algunos teléfonos el shader podía no enlazar. Ahora ocupan ~3,4 KB de un bloque (mínimo garantizado 16 KB) y se suben en una sola llamada.
- **Máscaras**: las 3 capas base se leen del mismo atlas que las demás (menos memoria). Al pintar solo se suben las filas tocadas por el pincel (antes, la capa completa dos veces por trazo).
- **Render sin sondeo**: la vista previa dibuja al instante cuando algo cambia (zoom, pincel) y, al reproducir, al ritmo del vídeo exportado (30 fps = 30 cuadros, no 60 repetidos).
- **Teléfonos con poca RAM**: la superficie se dibuja al 75 % y el sistema la escala (casi la mitad de trabajo de GPU). La exportación sigue a resolución completa.
- **Lienzo de tamaño fijo**: abrir herramientas, la línea de tiempo o ampliar el panel ya no reduce la imagen ni redimensiona la superficie OpenGL (causa de saltos).

### 2. Interfaz
- Panel inferior deslizable: arrastra el asa para ampliarlo POR ENCIMA de la imagen; un toque lo alterna.
- Deslizadores en una sola fila (nombre · control · valor): ~40 % menos altura.
- **Modo Fácil / Avanzado** (botón arriba): Fácil muestra Intensidad, Velocidad, Tamaño, Dirección, formas y color; Avanzado añade turbulencia, suavizado, densidad, peso, elasticidad, etc.
- Línea de tiempo contraíble (botón en la barra de reproducción).
- Catálogo sin duplicados: Viento en árboles, Pelo y ropa (→ Movimiento natural), Anillos (→ Energía), Deriva de nubes, Ondas y Brillos se ocultan; los proyectos antiguos que los usan siguen funcionando y se muestran si están activos.

### 3. Importación
- Importar abre directamente **Archivos de Android** (JPG, PNG, WebP, BMP, GIF, HEIC). Se eliminó el menú Galería/Archivos. Sin permisos de almacenamiento.

### 4. Movimiento natural (cabello, ropa, árboles, pasto, banderas, cortinas)
- La raíz se fija en el borde REAL de la zona pintada (antes se medía contra el borde de toda la imagen).
- Amplitud tipo viga sujeta (s^γ): el peso endurece la raíz; la flexibilidad suaviza la curva.
- Onda que viaja de la raíz a la punta, distinta por material; fase propia por mechón/rama (sin efecto gelatina); temblor de hojas en la copa; briznas de pasto independientes.
- Ráfagas con armónicos enteros: el bucle sigue cerrando exacto.

### 5. Profundidad 2.5D
- Búsqueda iterativa de profundidad (3 pasos), primer plano dilatado (el estiramiento cae en el fondo, no en la silueta) y sobreencuadre automático (sin bordes negros).
- Planos, Órbita y Acercamiento empiezan en la imagen original. Nuevos: Plano de foco y Protección de bordes (Avanzado).
- Sin modelos de IA: un modelo de profundidad pesa 27-100 MB y en dibujos planos de anime da resultados poco fiables; se mantiene el pintado de planos.

### 6. Efectos: formas reales (no solo color) + color libre HEX
- Humo: Ondulante, Hilos, Tinta manga (con contorno), Rastrero; luz y sombra de anime.
- Fuego (antes "Llamas dibujadas"): Hoguera, Vela, Antorcha al viento, Mágico, Fantasma. La zona pintada es la base y las llamas suben por encima.
- Explosión por fases: destello → bola de fuego → onda → chispas → humo. Clásica, Hongo, Estallido manga (animación en dos), Energía.
- Salpicaduras: Spray (dirección y apertura), Goteo, Mancha de tinta.
- Cortes: Trazo, Media luna, Cruz, Múltiple; grosor ajustable.
- Energía y auras: Rayos, Aura alrededor del personaje (capa Proteger), Anillos de poder, Aura de batalla con contorno.
- Ojos brillantes: Brillo, Pulso, Destello, con halo.
- Niebla: Bancos, Niebla baja, Bruma. Revelación entre humo: desde el centro, desde abajo o lateral, con humo en el borde.
- **Color propio** (muestras + HEX + tono/saturación/brillo) en humo, fuego, explosión, salpicaduras, cortes, energía, ojos, niebla, aura oscura y revelación.
- **Detrás del personaje**: interruptor en ~30 efectos; el personaje pintado en Proteger queda delante.
- **Elegir origen tocando la imagen** en más efectos (humo, energía, rayos, niebla, líneas…).

### 7. Escenas rápidas (Efectos › Escenas)
Aparición entre humo · Explosión anime · Cielo animado · Aura oscura · Ojo brillante · Impacto manga · Viento en cabello · Fuego y humo · Nubes detrás del personaje. Un toque las aplica y abre el efecto principal para personalizarlo.

### 8. Bucles y exportación
- Tiempo unificado (LoopClock): la vista previa usa la misma rejilla de cuadros que el MP4 (t = i/N, el cuadro N no se escribe).
- El panel de exportación consulta el codificador del teléfono: atenúa las resoluciones no admitidas e indica el tamaño exacto que se exportará.
- Si el codificador rechaza el perfil o la tasa de bits, se reintenta con valores menores antes de fallar.

## Pruebas ejecutadas realmente
- **Compilación Android en GitHub Actions** (AGP 8.7.3, Kotlin 1.9.24, API 36): correcta, APK y AAB firmados y verificados (apksigner + aapt).
- **Pruebas unitarias JVM** (todas las anteriores + V36Test): bucle, rejilla de cuadros, rectángulo de máscara, HEX/HSV, detrás del personaje, formas, escenas, compatibilidad de proyectos antiguos y contrato Kotlin↔shader (todo uniform que escribe Kotlin existe en el shader).
- **Shader en WebGL2** (GLSL ES 3.00, mismo lenguaje que OpenGL ES 3; Chromium + SwiftShader): compila y enlaza; bloque de 3,4 KB; renders de cada forma de cada efecto revisados visualmente.
- **Continuidad del bucle** (último cuadro → primero vs. paso normal): humo, hilos, niebla, lluvia, nubes, fuego, aura, movimiento natural, 2.5D y brasas sin salto. (Explosión, salpicaduras y cortes reinician a propósito en cada impacto.)
- 16 KB: la app no contiene librerías nativas (.so), por lo que ya es compatible.

## Pendiente de probar en un teléfono Android real
- Compilación del shader en drivers reales (Adreno, Mali, PowerVR) y fluidez en un móvil económico.
- Zoom con dos dedos, pincel y panel deslizable al tacto.
- Superficie al 75 % en teléfonos con poca RAM (nitidez aceptable).
- Exportación MP4 720p/1080p/2K/4K a 30 y 60 fps y su coincidencia con la vista previa.
- Guardado automático y apertura de proyectos v35 (deben verse igual salvo los efectos mejorados).
- Antes de Google Play: probar con firma privada, reactivar R8 (minify) y probarlo, y completar la ficha de privacidad (PRIVACIDAD_BORRADOR.md).
