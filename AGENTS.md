# AGENTS.md — AniLista

Instrucciones para agentes de IA que trabajen en este repositorio.

## Regla de oro

**NO COMPILES NI EJECUTES EL PROYECTO.** No corras `gradlew`, `gradle`, `assemble`, `build`, `installDebug` ni ningún comando de build o test. El usuario compila, instala y prueba manualmente desde Android Studio.

## Cuando escribas código

1. Escribí el código que se pide, siguiendo el `design-brief.md` y `design-brief-v5.md` y las convenciones del proyecto.
2. Al terminar, indicá en tu respuesta final:
   - **Resumen**: qué archivos creaste/modificaste y qué hiciste en cada uno.
   - **Indicaciones**: qué debe verificar el usuario al compilar/probar (dependencias nuevas, imports, cambios de config, pasos manuales, edge cases).
3. No propongas compilar "para verificar" ni asumas que los cambios compilan. El usuario valida.
4. Si detectás un problema que bloquea el build (ej: propiedad faltante en `gradle.properties`), reportalo en **Indicaciones**, no lo "arregles" compilando ni asumas el resultado.
5. Mantené el código en inglés (nombres de variables, funciones, UI) salvo que el brief o el usuario pida lo contrario.

## Contexto del proyecto

- App Android nativa: Kotlin + Jetpack Compose + Material 3 + Room + DataStore.
- Ejercicio de aprendizaje: el usuario escribe/entiende el código. La IA asiste, no reemplaza.
- **`design-brief.md` y `design-brief-v5.md` es el documento más importante de contexto a la hora de implementar cualquier cosa nueva.** Es obligatorio leerlo antes de implementar y seguí su roadmap, estructura de carpetas y decisiones cerradas. Es la única fuente de verdad del proyecto.
- No agregues dependencias ni stacks que el brief no justifique.
