# Zeta Motion v35 — corregir el error «No se instaló la app»

## Causa demostrada por la captura

El archivo `app-release-unsigned.apk` **NO ESTÁ FIRMADO**. No puede instalarse tocándolo en Android.
La compilación de un APK no implica que se haya firmado; las variantes release se dejan sin firma por defecto.

## Qué cambia en este proyecto

- `.github/workflows/build.yml`: usa `:app:assembleDebug` y comprueba con `apksigner verify` que el APK esté firmado antes de publicarlo como artefacto.
- `app/build.gradle.kts`: el APK **debug de pruebas** añade `.pruebas` a su ID de instalación (`com.zeta.fondo.pruebas`). Esto permite instalarlo junto a una versión ya instalada, aunque aquella tenga otra firma.
- `app/src/debug/res/values/strings.xml`: etiqueta «Zeta Motion Pruebas» para distinguirlo del original.
- No se cambia el código del editor, sus efectos, ni sus proyectos.

## Instrucciones desde Android / GitHub

1. Sustituye todos los archivos del proyecto en GitHub, **también** `.github/workflows/build.yml`. La carpeta `.github` suele estar oculta en gestores de ZIP.
2. Inicia un **workflow nuevo** en Actions, no Re-run de uno anterior.
3. Busca el artefacto llamado **ZetaMotion-v35-INSTALABLE-firmado**.
4. Descarga el archivo ZIP del artefacto, **extrae el APK** y abre `ZetaMotion-v35-PRUEBAS-FIRMADO.apk` desde Archivos. No intentes instalarlo dentro del ZIP.
5. Deja instalada tu versión anterior si tiene trabajos: esta variante usa otra carpeta de proyectos por ser otra app.

## Importante para publicar y actualizar sin perder datos

Esta firma debug sirve **solo para pruebas**, no para publicar en Play Store.
Para instalar una actualización SOBRE la app original y mantener sus proyectos es obligatorio firmarla con la **misma clave de la versión anterior**. Guarda esa clave fuera del repositorio público y configura una firma de lanzamiento segura con secretos de GitHub. Nunca publiques tu archivo `.jks` ni las contraseñas.

## Estado de validación

Se ha revisado la configuración del workflow y los archivos XML/Gradle. No se ha ejecutado aquí una compilación Android completa por no disponer de Android SDK ni Gradle; GitHub debe confirmarla.
