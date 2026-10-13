# Corrección de compilación en GitHub (Gradle 8.9)

## Error confirmado en la captura de GitHub

`Minimum supported Gradle version is 8.11.1. Current version is 8.9`.

El workflow activo del repositorio sigue usando Gradle 8.9 y el proyecto anterior requería AGP 8.10.1, cuyo mínimo es Gradle 8.11.1.

## Cambio aplicado en esta revisión

- Android Gradle Plugin: **8.7.3** (mínimo Gradle 8.9).
- Kotlin: **1.9.24** con Compose Compiler **1.5.14**, combinación ya usada en v33.
- Se conserva Android API **36** para compileSdk y targetSdk. AGP 8.7 no garantiza soporte probado para API 36; se suprime únicamente la advertencia de versión sin afirmar que es una combinación oficialmente soportada.
- Las herramientas de animación, efectos y proyectos de v34 siguen iguales.
- El workflow adjunto usa Gradle **8.9**, instala SDK 36 y muestra el error real al fallar.

**Límite:** esta corrección soluciona la incompatibilidad del registro recibido. Aquí no se dispone de Gradle ni de Android SDK para ejecutar `assembleRelease`; pueden existir errores posteriores, y la combinación AGP 8.7 / compileSdk 36 se debe validar en CI. Si se puede actualizar .github/workflows/build.yml de tu repositorio, la solución oficial preferible es usar AGP 8.10.1 con Gradle 8.11.1; el ZIP anterior contenía ese workflow, pero no llegó a utilizarse en la ejecución mostrada.

## Cómo aplicar

Extrae el ZIP, sube el **contenido** (incluidos archivos ocultos `.github`) a la raíz del repositorio y sustituye los archivos existentes. No subas solo el ZIP si el repositorio ya contiene archivos de Gradle: el workflow antiguo no lo descomprime.

No contiene claves de firma privadas; no desinstales una instalación anterior sin copia de seguridad.
