# Zeta Motion v31 — Mejora de herramientas y estabilidad de edición

Parte de Zeta Motion v30. Se mantiene el `applicationId` y la firma del proyecto para permitir actualizar instalaciones compatibles, sin eliminar las herramientas existentes.

## Cambios implementados
- **Animar → Natural, Profundidad, Deformar, Cámara y Por zona** abre todos sus controles en **Animar**. No salta a **Efectos**. Se reutilizan los controles existentes.
- Barra **Animar** distribuida en dos filas de herramientas y una fila de flechas/guías, sin obligar a deslizar hacia la derecha para descubrir botones.
- **Efectos** ya no muestra duplicadas las herramientas de Animar; conserva el catálogo de efectos visuales (clima, agua, luz, anime, manhwa y manga).
- **Máscaras:** el panel de pintura ahora expone el control de borde suave, también para retocar zonas y protección.
- **Línea de tiempo:** cada bloque tiene su propia **intensidad (0 a 2)**. Duplicar copia ese valor y puedes editarlo independientemente. Se combina con bloques superpuestos y se aplica en vista previa y exportación a través de `EffectTimeline.sample`.
- Los proyectos guardan la intensidad en `effectClips[].strength`. Proyectos previos abren con intensidad **1**, conservando su apariencia anterior.
- Etiquetas de la vista de efectos acordes con el botón de ajuste visible.
- `versionCode=31`, `versionName=31.0.0`.

## Alcance real y siguientes verificaciones
- La intensidad sí es independiente por bloque; **los otros parámetros del mismo efecto (dirección, máscara, velocidad, variante) siguen siendo comunes**. Para independizarlos también hará falta rediseñar el renderizado de los efectos simultáneos.
- No se han sustituido los shaders ni cambiado los anclajes, flechas o sistema existente de máscaras; se priorizó no dañar animaciones que ya funcionan.
- Conserva `EffectTimelineTest.kt` y añade `EffectTimelineStrengthTest.kt` para compatibilidad de proyectos anteriores, intensidad individual, suma de bloques y límites.
- Es **código fuente** Android. No es un APK compilado; se debe probar en un dispositivo real antes de considerarlo validado en producción.

## Compilación
- Gradle 8.9, Java 17 y Android SDK 34: `gradle testReleaseUnitTest assembleRelease` (ver `.github/workflows/build.yml`).
- Aviso de seguridad: la configuración de firma ya venía incorporada en v30. No distribuyas las claves/contraseñas del proyecto fuente de forma pública.
