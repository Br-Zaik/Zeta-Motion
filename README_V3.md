# Zeta Motion Studio v3 · anime y manga

Proyecto Android (Kotlin, Jetpack Compose, OpenGL ES 3). Se conserva `applicationId=com.zeta.fondo`, el keystore de la base y los proyectos JSON existentes. Compilar con el workflow `.github/workflows/build.yml` para obtener el APK desde GitHub Actions.

## Implementado sobre la base v2

- Editor nuevo tipo canvas-first: panel contraído por defecto, inspector contextual, panel inferior arrastrable en vertical, rail lateral e inspector a la derecha en horizontal y modo inmersivo con controles mínimos.
- Tema visual Studio nocturno violeta/cian; inicio con galería adaptable, sin recursos fotográficos de muestra.
- Navegación renovada: Movimiento, Máscaras, Presets, Efectos, Manga, Cámara y Exportar. Biblioteca categorizada dentro de Efectos; propiedades avanzadas bajo demanda.
- 12 efectos nuevos implementados en el shader y compartidos por preview/exportación: cenizas, hojas, brasas, polvo, burbujas, electricidad, líneas de enfoque, onda de choque, flash, screentone, grano y tinta.
- Control de origen (ocho bordes/esquinas + punto), ángulo 360° para movimientos direccionales y herramienta de origen manipulable directamente sobre el lienzo para efectos puntuales. Los parámetros se guardan en el mapa JSON heredado.
- Presets anime/manga adicionales incluyendo aura, choque, hojas, brasas y tramas.
- Hasta 12 máscaras con nombre y color de visualización para personalizadas; pincel, goma, suavizado, rellenar y vaciar. Se pueden asignar individualmente a los efectos; los antiguos proyectos mantienen sus tres primeras máscaras.
- Exportador con Original, 4K UHD, 2K, 1080p, 720p, QHD/1440p y tamaño personalizado. El tamaño personalizado ajusta el encuadre sin deformar proporciones; MP4 AVC/HEVC/AV1 disponible según encoder de Android, y secuencias PNG. Las capacidades de resolución y fps del codificador se siguen evaluando antes de exportar.

## Limitaciones pendientes, no confundir con funciones listas

- **Máscaras**: se admiten 12 por proyecto (límite conservador por memoria gráfica), no una cantidad infinita. No se eliminan automáticamente máscaras de proyectos previos.
- **Pila de instancias**: varios tipos de efectos se pueden combinar, pero el mismo tipo aún no puede duplicarse con parámetros independientes. El renderer continúa siendo un solo programa de fragmentos; no es un compositor multipaso.
- No se ha integrado un modelo de IA para superresolución ni se afirma que reescalar a 4K reconstruya detalle inexistente.
- No se ha verificado un APK en dispositivo en este entorno: compilar y probar con Actions en Android real antes de distribuir. El perfil AV1 solamente se ofrece cuando Android 14+ declara encoder.

## Seguridad de firma

La base recibida trae `app/fondozeta.jks` y contraseñas en `app/build.gradle.kts`. Se **han conservado** para que tu APK nuevo se instale encima del anterior. Antes de publicar el repositorio, mueve las contraseñas a secrets sin perder el keystore.
