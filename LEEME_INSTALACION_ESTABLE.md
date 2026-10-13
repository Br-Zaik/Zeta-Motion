# Zeta Motion v35.1 — reparación del APK no instalable

## ¿Qué sucedió?

Las primeras versiones estaban firmadas con una clave y el paquete `com.zeta.fondo`.
Después se generó `app-release-unsigned.apk`: Android **no instala APKs sin firma**. Y un APK de
`com.zeta.fondo` firmado con una clave diferente **no puede actualizar** la aplicación existente.
Tampoco conviene instalar directamente desde el explorador interno de un archivo ZIP.

## Qué se ha corregido

- `app/build.gradle.kts`: **assembleRelease** siempre firma el APK. Sin firma privada, usa un
  paquete independiente (`com.zeta.fondo.beta`) y una firma estable de pruebas. Nunca produce
  automáticamente un APK release sin firma.
- `app/zeta-beta-public-only.jks`: certificado de pruebas que NO sirve para Play Store.
  Es **intencionadamente público**, únicamente para el paquete beta. No contiene la clave privada
  de la aplicación original.
- `.github/workflows/build.yml`: ejecuta la compilación, comprueba la firma con `apksigner`,
  comprueba que el ID de instalación es el esperado y sube únicamente el APK firmado.
- La misma firma de beta se conserva entre nuevas compilaciones, evitando tener que reinstalarla
  siempre. Subida de `versionCode` a 36.

## Pasos en GitHub

1. Reemplaza el código de GitHub con los archivos de este ZIP, incluida la carpeta `.github`.
   Verifica que está el archivo `app/zeta-beta-public-only.jks` (sí, este archivo de pruebas se publica).
2. Inicia un nuevo workflow desde GitHub Actions.
3. Descarga el **artefacto** `ZetaMotion-v35_1-APK-FIRMADO-INSTALABLE`, no otros ZIP anteriores.
4. Extrae completamente el ZIP descargado. Abre el archivo
   `ZetaMotion-v35_1-BETA-INSTALABLE-FIRMADO.apk` desde la aplicación Archivos de Android.
5. Zeta Motion Beta se instala sin tocar la aplicación original ni sus proyectos.

Si ya existe instalada **Zeta Motion Beta** con una firma distinta y Android informa «No se instaló
la app», desinstala **solo la Beta anterior** y vuelve a instalarla. **No desinstales Zeta Motion
original si tienes proyectos dentro.**

## ¿Y para actualizar Zeta Motion ORIGINAL sin perder proyectos?

Por seguridad, **no publiques** la clave de firma de las primeras versiones.
Para actualizar sobre `com.zeta.fondo`, necesitas el archivo privado de firma correspondiente
al APK anterior (y su contraseña). En *Settings > Secrets and variables > Actions* de GitHub
se pueden configurar:

- `ZETA_RELEASE_KEYSTORE_BASE64`: archivo de firma privada convertido a Base64.
- `ZETA_KEYSTORE_PASSWORD`: contraseña del almacén.
- `ZETA_KEY_ALIAS`: alias de la clave.
- `ZETA_KEY_PASSWORD`: contraseña de la clave.

Cuando estén los cuatro secretos configurados, este mismo workflow generará la versión de
`com.zeta.fondo` firmada con esa clave y NO la Beta. La clave nunca se incluye en el repositorio.
Si la clave no coincide con la original, Android seguirá rechazando la actualización.

## Advertencia de calidad

No se dispone de Gradle Android/Android SDK para ejecutar `assembleRelease` en esta sesión;
la firma del APK será validada automáticamente en GitHub Actions al ejecutar la compilación.
No se han modificado los efectos, las máscaras ni el motor de animación.
