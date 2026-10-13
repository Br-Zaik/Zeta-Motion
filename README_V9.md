# Zeta Motion 9.0.0

## Controles

- Dos ajustes principales visibles al seleccionar un efecto: fuerza o intensidad junto a velocidad.
- Un efecto apagado muestra los controles deshabilitados hasta pulsar Activar.
- Calor se identifica correctamente como distorsión, no como llamas visibles.
- Ayuda contextual para pintar Zona 1 (azul) o Zona 2 (verde) antes de usar el efecto.
- Más permite acceder a detalle, dirección y otros ajustes secundarios sin ampliar siempre el panel.

## Nuevos efectos del motor (vista previa y exportación)

- Llamas dibujadas: brillo naranja animado limitado a Zona 2.
- Brillo de ojos: pulso azul limitado a Zona 1.
- Parpadeo de sombra: oscurecimiento temporal de Zona 1; no genera párpados anatómicos.
- Pelo y ropa: ondulación suave limitada a Zona 2.

## Renderizado

Los valores constantes del shader se envían solo cuando cambia un parámetro o una máscara asignada; el tiempo, la vista y el tamaño de textura se siguen actualizando en cada cuadro. Esto reduce llamadas por cuadro sin alterar el motor de exportación. No se ha medido el rendimiento en un teléfono.

Se conservan el paquete `com.zeta.fondo`, la firma y el flujo de GitHub Actions. Esta versión tiene `versionCode 9`. El APK debe compilarse y probarse en Android para validar el shader y el rendimiento real.
