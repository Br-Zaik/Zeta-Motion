# Zeta Motion v23 — apertura rápida + timeline libre

Esta versión se centra en dos problemas de uso real del editor: esperar con el lienzo negro al abrir una imagen y no poder reorganizar los bloques verticalmente como en un editor de vídeo.

## Apertura rápida

- El editor abre primero con `thumb.jpg` (máx. 640 px) en vez de esperar a decodificar la vista previa grande.
- Mientras OpenGL crea el contexto, compila shaders y sube texturas, Compose mantiene el thumbnail visible encima: ya no debería aparecer un lienzo negro durante esos segundos.
- Al llegar el primer frame real de OpenGL, el placeholder desaparece automáticamente.
- La vista previa de trabajo sube después en segundo plano a un máximo de 1536 px. La exportación continúa leyendo `source.img`, por lo que esta reducción solo afecta a la vista previa y no a la calidad final.
- Personaje/importaciones auxiliares también se cargan después de entrar al editor para no bloquear la apertura.

## Timeline libre tipo CapCut

- Mantén pulsado el centro de un bloque y arrastra izquierda/derecha para cambiar el tiempo.
- Con el mismo gesto puedes subir o bajar el bloque entre pistas.
- Un bloque solo entra en una pista si ese tramo está libre; si hay choque, busca una pista libre cercana.
- Se guarda la pista (`track`) de cada bloque en `project.json`, por lo que la organización permanece al volver a abrir el proyecto.
- Proyectos v20-v22 siguen siendo compatibles: los clips sin pista guardada (`track=-1`) se distribuyen automáticamente.
- Siempre existe una pista vacía al final para poder bajar un bloque.
- La timeline muestra como máximo 4 pistas de alto y el resto se recorre verticalmente, evitando que muchos efectos reduzcan demasiado el lienzo.
- Los tiradores izquierdo/derecho siguen cambiando duración sin perder la pista del bloque.

## Compatibilidad

- Se conservan efectos repetibles, múltiples efectos en el mismo segundo, Movimiento Natural, Profundidad 2.5D, Deformar, Cámara, Bucle perfecto, importación desde Archivos y exportación existente.
