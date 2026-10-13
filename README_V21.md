# Zeta Motion v21 — Fondos anime más fáciles de animar

Esta versión amplía v20 sin cambiar el flujo de timeline tipo CapCut.

## Novedades principales

- **Profundidad 2.5D / parallax**:
  - Zona azul = fondo.
  - Zona verde = primer plano.
  - Cada plano se mueve a distinta profundidad con movimiento periódico y protección de áreas congeladas.
  - Botones directos `Pintar fondo` y `Pintar frente`.
- **Deformación suave** para cabello, ropa, agua, humo y detalles dibujados:
  - Ondulación.
  - Respiración.
  - Tela.
  - Se limita a una zona pintada para no deformar toda la imagen.
- **Cámara más útil**:
  - Acercar, alejar, desplazar, girar y temblar.
  - El paneo ahora puede elegir dirección de 0° a 360°.
  - El movimiento es periódico para cerrar el loop.
- **Bucle perfecto** dentro de Animar > Bucle:
  - Fuerza velocidades enteras por ciclo.
  - Usa trayectoria periódica de ida y vuelta para el movimiento base.
  - Cámara, profundidad y deformaciones nuevas vuelven al punto inicial al terminar.
- **Presets rápidos para anime/manhwa** dentro de Efectos > Rápido:
  - Fondo con profundidad.
  - Cabello al viento.
  - Ropa suave.
  - Cielo vivo.
  - Lluvia nocturna anime.
  - Manga respirando.
- **Accesos directos desde Animar**: Bucle, Profundidad, Deformar, Cámara y Movimiento por zona.

## Se conserva de v20

- Un toque crea/selecciona un solo bloque.
- `+` añade otra copia solo cuando el usuario lo pide.
- Arrastrar el centro mueve el bloque.
- Arrastrar los extremos cambia inicio/duración.
- Varios efectos pueden coincidir en el mismo segundo.
- Duración personalizada del vídeo.
- Importación desde Galería o Archivos.
- Exportación MP4/PNG, 1080p/2K/4K según soporte del dispositivo.

## Validación realizada

- Shader OpenGL ES 3.0 compila y enlaza.
- Pruebas de render existentes siguen pasando.
- Parallax 2.5D comprobado con zonas separadas y cierre sin salto entre t=0 y t=1.
- Deformación suave comprobada dentro de máscara, sin fuga fuera de ella y con cierre sin salto.
- Los modelos puros Kotlin (`Effects`, `EffectClip`, `Presets`, `RenderParams`) compilan con `kotlinc`.

VersionCode: 21
VersionName: 21.0.0
