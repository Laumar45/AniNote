# Architectural Code Audit — AniNote
**Auditoría de Calidad, Diseño y Sostenibilidad**
*Nivel: Senior Software Architect / Android Specialist*

---

## 1. Resumen Ejecutivo y Diagnóstico Comparativo

El proyecto **AniNote** tiene una base funcional limpia, pero sufre de vicios típicos de desarrollo rápido ("hacer que funcione") que comprometen la sostenibilidad, la integridad de los datos y la testabilidad.

La auditoría previa identificó síntomas superficiales válidos (como la sobrecarga del ViewModel y `runBlocking`), pero **falló en el análisis de causa raíz y propuso soluciones técnicamente peligrosas** (como sugerir `viewModelScope.launch` en `onCleared()`, lo cual provoca pérdida silenciosa de datos).

A continuación se presenta la evaluación técnica real, diferenciando malas prácticas genuinas de recomendaciones "cargo-cult" (como forzar frameworks de DI innecesarios).

---

## 2. Hallazgos por Nivel de Criticidad

### 🔴 CRÍTICOS (Integridad de datos, ciclos de vida y estabilidad)

#### 1. Antipatrón en Undo Delete: Datos Zombie y Riesgo de Pérdida
- **Archivo:** `AnimeViewModel.kt` (L219-L263)
- **Problema real:** El ViewModel implementa el "Undo" reteniendo el borrado en memoria con un temporizador `delay(4000)` y filtrando `_pendingDeleteIds`. Si el usuario borra un elemento y la app es cerrada por el sistema operativo (Process Death / Crash) durante esos 4 segundos, la corrutina muere, el registro **nunca se borra de Room SQLite** y reaparece al abrir la app ("registro zombie").
- **El error de la auditoría anterior:** La auditoría previa propuso arreglar `runBlocking` en `onCleared()` usando `viewModelScope.launch`. **Esto es un error conceptual grave**: en `onCleared()`, el `viewModelScope` ya está cancelado, por lo que la corrutina jamás se ejecutaría.
- **Solución Sostenible:**
  1. **Enfoque Inmediato con Snapshot:** Borrar inmediatamente en la base de datos y guardar el objeto en memoria. Si el usuario presiona "Deshacer", se ejecuta un `insert()` del snapshot. Esto es atómico, no requiere jobs pendientes, elimina `runBlocking` y no genera inconsistencias tras process death.
  2. **Enfoque Soft-Delete:** Agregar una columna `is_deleted: Boolean` o `deleted_at: Long?` en SQLite y un worker de purga periódica.

#### 2. Fuga de Modelos de Base de Datos hacia la Capa de Presentación
- **Archivos:** `AnimeUiState.kt` (L28-L33), `AnimeViewModel.kt` (L123-L144, L196-L207)
- **Problema real:** `DialogState` almacena directamente `editingAnime: AnimeEntity?`. Además, `AnimeViewModel` expone sobrecargas duplicadas como `requestDelete(anime: AnimeUi)` y `requestDelete(anime: AnimeEntity)`.
- **Impacto:** Si la entidad de base de datos cambia (nuevas columnas, relaciones Room o migraciones), la UI y los diálogos se rompen directamente. Rompe el principio de abstracción por capas.
- **Solución Sostenible:** La capa de presentación solo debe conocer modelos de UI (`AnimeUi`) o un modelo de dominio puro. El mapeo debe ocurrir en extensiones explícitas (`AnimeEntity.toUi()`, `AnimeUi.toDomain()`).

#### 3. Inversión de Dependencias Rota (Data dependiente de ViewModel)
- **Archivos:** `data/AppPreferences.kt` (L8), `viewmodel/AnimeUiState.kt` (L6)
- **Problema real:** `AppPreferences.kt` (capa de datos) importa `SortOrder` desde el paquete `com.laumar.aninote.viewmodel`.
- **Impacto:** La capa inferior (Data) conoce a la capa superior (Presentation). Esto impide modularizar la app o testear la capa de persistencia de forma aislada.
- **Solución Sostenible:** Mover `SortOrder` a un paquete común `domain/model/SortOrder.kt`.

---

### 🟠 ALTOS (Arquitectura, Testabilidad y Acoplamiento)

#### 4. Ausencia de Composition Root / AppContainer Unificado
- **Archivos:** `MainActivity.kt` (L29-L41), `AniNoteApp.kt` (L10-L21)
- **Problema real:** La instanciación de Base de Datos, DAO, Repositorio, DataStore y ViewModel Factories está dispersa e instanciada directamente en el `onCreate` de `MainActivity`.
- **Aclaración Arquitectónica:** No es estrictamente obligatorio añadir Hilt/Koin para una app pequeña; el "Manual Dependency Injection" mediante un `AppContainer` en la clase `Application` es un patrón oficial de Android, liviano y 100% testeable sin sobrecarga de librerías.
- **Solución Sostenible:** Crear un contenedor de dependencias centralizado en `AniNoteApp`:
  ```kotlin
  class AniNoteApp : Application() {
      lateinit var container: AppContainer
      override fun onCreate() {
          super.onCreate()
          container = DefaultAppContainer(this)
      }
  }
  ```

#### 5. Deduplicación Ineficiente en Memoria y DAO Desaprovechado
- **Archivos:** `ImportExportController.kt` (L69-L72), `data/AnimeDao.kt` (L18-L19)
- **Problema real:** `AnimeDao` declara `findByNameCaseInsensitive()`, pero en la importación combinada (`replace = false`), `ImportExportController` carga **todos** los registros de la base de datos a memoria con `repository.getAllCanonical().first()` y crea un `Set` en RAM para buscar duplicados.
- **Impacto:** Aunque para pocos elementos no es perceptible, este patrón carga la memoria inútilmente e ignora las capacidades de indexación de SQLite.
- **Solución Sostenible:** Realizar la verificación mediante consultas SQL en lote o delegar la resolución de conflictos a SQLite (`OnConflictStrategy.IGNORE` con constraint de unicidad).

#### 6. Strings Hardcodeados en Lógica de Control (Falta de Internacionalización)
- **Archivo:** `viewmodel/ImportExportController.kt` (L104-L128)
- **Problema real:** Los mensajes de resultado de importación y errores de parsing ("Importaste X animes", "líneas ignoradas", "duplicados omitidos") están concatenados en español plano dentro de la clase lógica.
- **Impacto:** Rompe el soporte multiidioma (`strings.xml`) y mezcla responsabilidades de formateo de texto UI con lógica de negocio.
- **Solución Sostenible:** Emitir objetos de evento fuertemente tipados con datos crudos (ej: `ImportSummary(imported = 5, duplicates = 2)`) y dejar que la UI o composables formateen el mensaje usando `stringResource()`.

---

### 🟡 MEDIOS (Limpieza de Código, Concurrencia y Compose)

#### 7. God ViewModel con Múltiples Responsabilidades
- **Archivo:** `AnimeViewModel.kt`
- **Problema:** El ViewModel coordina: (1) Búsqueda debounced, (2) Ordenamiento y persistencia en DataStore, (3) Estado de formulario de diálogo, (4) Highlight temporal de nuevo elemento, (5) Orquestación de borrado/undo, y (6) Delegación de import/export.
- **Solución Sostenible:** Extraer el estado del diálogo a un componente/estado específico o separar la lógica de import/export en una función de dominio independiente.

#### 8. Acoplamiento de Efectos Colaterales en Componentes UI (`AnimeCard`)
- **Archivo:** `ui/components/AnimeCard.kt` (L144-L154)
- **Problema:** `AnimeCard` ejecuta directamente `copyToClipboard()` y lanza un intent a Google usando `LocalContext.current` en funciones privadas del archivo UI.
- **Solución Sostenible:** Elevar los eventos como lambdas (`onCopy: (String) -> Unit`, `onSearchWeb: (String) -> Unit`) para que el composable sea puramente presentacional y 100% previsualizable en Compose Previews y testeable en Unit Tests UI.

#### 9. Mutaciones de Estado No Atómicas en `StateFlow`
- **Archivo:** `AnimeViewModel.kt` (L230, L239, L249)
- **Problema:** Se usa `_pendingDeleteIds.value += anime.id` (lectura-modificación-escritura). En entornos concurrentes con múltiples corrutinas, esto genera race conditions.
- **Solución Sostenible:** Utilizar `_pendingDeleteIds.update { it + anime.id }`.

---

### 🟢 BAJOS (Deuda Técnica Menor)

#### 10. Código Muerto y Redundancias
- `viewmodel/AnimeUiState.kt` (L8): `enum class ListFilter { ALL, REWATCHED }` está declarado pero no se utiliza en ningún lugar.
- `utils/JsonImportExport.kt` (L23-L24): `typealias AnimeJson = AnimeJsonDto` y `typealias AnimeListJson = AnimeBackupDto` son alias redundantes que aumentan el ruido cognitivo.
- `data/AnimeEntity.kt` (L7): `@Immutable` en una `@Entity` de Room no tiene efecto útil ya que Room no interactúa con el runtime de Compose.

---

## 3. Matriz Comparativa: Auditoría Previa vs. Auditoría Senior Architect

| Aspecto / Problema | Diagnóstico Auditoría Previa | Diagnóstico & Solución Real (Senior Architect) |
| :--- | :--- | :--- |
| **Inyección de Dependencias** | Marcado como 🔴 CRÍTICO. Exigía meter Hilt/Koin. | **Exageración.** Para esta app, Hilt agrega overhead innecesario. La solución limpia es un **`AppContainer` (Manual DI)** centralizado en `AniNoteApp`. |
| **Capa de Use Cases** | Exigía crear UseCases para todo (`AddAnimeUseCase`, etc.). | **Sobreingeniería.** Crear UseCases que solo son un pasamanos de una línea hacia el Repositorio agrega código basura. Los UseCases solo se justifican cuando hay lógica de negocio real. |
| **Fix para `runBlocking`** | Sugirió cambiar `runBlocking` por `viewModelScope.launch` en `onCleared()`. | **PELIGROSO / INCORRECTO.** `viewModelScope` ya está cancelado en `onCleared()`. Provoca pérdida de datos. La solución real es rediseñar el Undo para borrar de inmediato en SQLite y reinsertar si se deshace. |
| **Undo Delete Architecture** | No detectó la falla fundamental de datos zombie en process death. | **Detectado como 🔴 CRÍTICO.** El temporizador en memoria con delay no sobrevive a la muerte del proceso. Se requiere snapshot in-memory o soft-delete en BD. |
| **Strings en Controller** | Solo mencionó 2 strings en `AnimeViewModel`. | **Identificado.** `ImportExportController` tiene lógica completa de strings concatenados en español que rompen `strings.xml`. |

---

## 4. Plan de Acción Recomendado (Por Fases)

### Fase 1: Integridad y Estabilidad Inmediata (Prioridad Alta)
1. **Rediseñar Undo Delete:**
   - Eliminar `_pendingDeleteIds`, `pendingDeleteJob` y el `runBlocking` de `onCleared()`.
   - Al borrar: ejecutar `repository.delete(anime)` de inmediato y retener `deletedAnimeSnapshot`.
   - En `undoDelete`: ejecutar `repository.insert(deletedAnimeSnapshot)`.
2. **Desacoplar Inversión de Dependencias:**
   - Mover `SortOrder` a `com.laumar.aninote.model.SortOrder`.
3. **Saneamiento de StateFlow:**
   - Reemplazar todas las mutaciones `.value = ...` concurrentes por `.update { }`.

### Fase 2: Arquitectura y Capas (Prioridad Media)
4. **Desacoplar Entity de UI:**
   - Cambiar `DialogState` para que almacene solo tipos primitivos o `AnimeUi`, nunca `AnimeEntity`.
   - Crear mappers puros `AnimeEntity.toUi(): AnimeUi`.
5. **Implementar `AppContainer`:**
   - Centralizar la creación de Room, DataStore y Repositorios en `AniNoteApp`.
6. **Limpieza de Strings:**
   - Extraer todos los mensajes de `ImportExportController` hacia `strings.xml` pasando estados o IDs de recursos.

### Fase 3: Refactor de UI y Componentes (Prioridad Baja)
7. **Hacer `AnimeCard` puramente presentacional:**
   - Elevar los callbacks de copiar y buscar en Google.
8. **Eliminar código muerto:**
   - Remover `ListFilter` y typealiases innecesarios.
