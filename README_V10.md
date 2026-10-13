# Zeta Motion 10.0.0 — editor de ilustraciones

## Flujo de edición

- Lienzo con espacio propio y vista previa estable. Exportar solo en la barra superior.
- Controles de reproducción, miniaturas de la duración, pista sonora y avance por toque/arrastre.
- Duración de 2 a 60 segundos; formatos Completo, 9:16, 16:9, 1:1, 3:4 y 4:3.
- Catálogo único de efectos, con selección que activa el efecto y muestra Intensidad, Velocidad y parámetros adicionales.
- Ocho fondos de ilustración vectorial creados por código: atardecer, noche, sakura, tormenta, ciudad neón, lago, bosque y nieve. Conserva la importación de ilustraciones propias.

## Sonido

Seis ambientes propios sintetizados en la app: lluvia, truenos, viento, fuego, magia y río. Incluyen selección, volumen, reproducción preliminar y una pista AAC en la salida MP4. Las secuencias PNG no tienen audio. No se incluyen grabaciones externas ni canciones comerciales.

## Compatibilidad y límites

Se mantiene `com.zeta.fondo`, la misma firma y los proyectos antiguos. La pista de sonido se renderiza una sola vez y se une al mismo MP4; escenas y efectos se renderizan con el shader actual. Los fondos incluidos son ilustraciones básicas de partida y la app no genera un personaje ni convierte cada foto en un anime. El recorte de aspecto se muestra con guía al abrir Lienzo. Es preciso compilar con GitHub Actions y verificar el MP4 y la fluidez en un dispositivo Android real.
