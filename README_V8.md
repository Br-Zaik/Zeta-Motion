# Zeta Motion v8 — editor de franja inferior

El punto de partida es la versión 7. El ZIP `ZetaMotion-GitHub.zip` enviado por el usuario se usó para comprobar los símbolos y títulos originales; ese ZIP es la versión 1 y no reemplaza el motor más reciente.

- Barra inferior con nombres legibles (13 sp).
- Sin las categorías Anime/Batalla/Ambiente/Cielo: todos los efectos están en un carrusel horizontal.
- Iconografía original del catálogo de efectos (emojis y símbolos), sin JPG ni fotografías integradas.
- Carrusel compacto de 106 dp; al seleccionar un efecto se abre activación y velocidad, y con «Más» aparecen otros parámetros.
- Herramientas, máscaras y exportación siguen en pestañas separadas; el botón activo abre o cierra su panel.
- La barra de reproducción queda por debajo de los controles, accesible sin taparse.
- La vista OpenGL no se redimensiona ni se desmonta al abrir controles.
- No se modificaron shaders, exportador, package ni keystore.
- `versionCode 8`, `versionName 8.0.0`.

La compilación debe ejecutarse mediante el workflow de GitHub Actions incluido; este entorno no cuenta con Gradle/Android SDK.
