# Zeta Motion v22 — Movimiento natural para fondos anime/manhwa

Esta versión parte de v21 y añade una herramienta específica para que partes dibujadas del fondo se muevan de forma localizada sin deformar toda la imagen.

## Movimiento natural

Nuevo acceso `Animar > Natural` y nueva tarjeta `Movimiento natural` en Efectos.

Flujo:
1. Elige el material: **Árbol, Bandera, Pasto, Cortina, Cabello o Ropa**.
2. Pulsa **Pintar / retocar** y marca únicamente la parte flexible.
3. Elige el **borde fijo** (ninguno, izquierda, derecha, abajo o arriba).
4. Ajusta **fuerza, dirección, velocidad, flexibilidad, variación y ráfagas**.
5. Usa Vista previa. El efecto puede ocupar todo el vídeo o un bloque de la timeline.

El borde fijo evita movimientos falsos: una bandera puede quedar sujeta al poste, el pasto y el árbol a la base, y una cortina/cabello a la parte superior.

## Motor de movimiento

- Variación espacial estable para que no todas las hojas/mechones se muevan igual.
- Movimiento secundario con varias ondas armónicas.
- Ráfagas periódicas sin ruido temporal aleatorio, por lo que el bucle cierra sin salto.
- Máscara independiente para que troncos, postes, cuerpo, paredes y objetos rígidos permanezcan quietos.
- La misma ruta de shader se usa en vista previa y exportación.
- Compatible con la timeline tipo CapCut de v20/v21.

## Presets rápidos nuevos

- Árbol natural
- Bandera realista
- Pasto al viento
- Cortina suave
- Cabello natural
- Ropa al viento

Los presets usan valores suaves de inicio; después se pueden ajustar sin cambiar de herramienta.

## Conservado de v21

Profundidad 2.5D, deformación suave, cámara, bucle perfecto, máscaras, protección, importación desde Archivos/Galería, duración personalizada, efectos repetibles y exportación 1080p/2K/4K según soporte.

## Validación realizada

- Shader OpenGL ES 3.0: compila y enlaza.
- Pruebas de render anteriores: pasan.
- Movimiento natural: 6 materiales comprobados.
- Máscara: el efecto no fuga fuera de la zona pintada en la prueba automática.
- Borde fijo de bandera: comprobado que el borde sujeto se desplaza menos que el extremo libre.
- Bucle: `t=0` y `t=1` coinciden en las pruebas de los 6 materiales.
- Modelos puros Kotlin (`Effects`, `EffectClip`, `Presets`, `RenderParams`) compilan con `kotlinc`.

VersionCode: 22  
VersionName: 22.0.0
