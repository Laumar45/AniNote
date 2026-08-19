# Coding Audit — AniNote

> Auditoría de calidad de código enfocada en malas prácticas y patrones no sostenibles.
> Fecha: 2026-08-19

---

## Resumen Ejecutivo

El proyecto es funcional y bien estructurado para un ejercicio de aprendizaje, pero contiene varios patrones que van a escalar mal. Los problemas más críticos son: un ViewModel gigante con múltiples responsabilidades, inyección de dependencias manual con fábricas boilerplate, uso de `runBlocking` en el main thread, y modelos de datos de base de datos filtrados hasta la UI.

---

## 🔴 CRÍTICOS

### 1. God ViewModel — 288 líneas, 7 responsabilidades

**Archivo:** `viewmodel/AnimeViewModel.kt`

El ViewModel maneja:
- Búsqueda y orden
- Estado del diálogo (add/edit)
- Borrado con undo
- Coordinación de import/export
- Emisión de eventos UI
- Highlight temporal
- Gestión de pending deletes

**Por qué es malo:** Cada nueva feature agrega más líneas. En 6 meses este archivo va a tener 500+ líneas y va a dar miedo tocarlo. Testing unitario de componentes individuales se vuelve imposible.

**Patrón sostenible:** Separar en ViewModels más pequeños o extraer lógica a UseCases.

---

### 2. `runBlocking` en `onCleared()` — Potencial ANR

**Archivo:** `viewmodel/AnimeViewModel.kt`, línea 258

```kotlin
override fun onCleared() {
    super.onCleared()
    val remaining = _pendingDeleteIds.value
    if (remaining.isNotEmpty()) {
        runBlocking(Dispatchers.IO) {
            remaining.forEach { repository.deleteById(it) }
        }
    }
}
```

`runBlocking` en el main thread durante la destrucción del ViewModel puede causar ANR si la base de datos tarda más de lo esperado.

**Patrón sostenible:** No bloquear el main thread nunca. Usar un scope separado o dejar que las operaciones pendientes se completen de forma asíncrona.

---

### 3. Inyección de dependencias manual — Fábricas boilerplate

**Archivos:** `viewmodel/AnimeViewModelFactory.kt`, `viewmodel/ThemeViewModel.kt` (línea 35-43), `MainActivity.kt` (líneas 33-41)

```kotlin
// MainActivity.kt - 15 líneas solo para crear ViewModels
val animeViewModel = ViewModelProvider(
    this,
    AnimeViewModelFactory(repository, preferences)
)[AnimeViewModel::class.java]
```

Cada vez que cambies el constructor de un ViewModel, tenés que actualizar:
1. El ViewModel
2. Su Factory
3. Donde se instancia (MainActivity)

**Patrón sostenible:** Hilt/Koin para DI. O al menos un patrón de Service Locator.

---

### 4. Entity de base de datos filtrada hasta la UI

**Archivos:** `viewmodel/AnimeUiState.kt`, `viewmodel/AnimeViewModel.kt`

```kotlin
// AnimeUiState.kt
data class DialogState(
    val showDialog: Boolean = false,
    val editingAnime: AnimeEntity? = null,  // Entity en estado de UI
    ...
)

// AnimeViewModel.kt - Acepta ambos tipos
fun requestDelete(anime: AnimeUi) { ... }
fun requestDelete(anime: AnimeEntity) { ... }
fun openEditDialog(anime: AnimeUi) { ... }
fun openEditDialog(anime: AnimeEntity) { ... }
```

Mezclás modelos de datos de capas diferentes. Si cambiás la estructura de `AnimeEntity`, se rompe la UI sin querer.

**Patrón sostenible:** Modelos separados por capa (Entity → Domain Model → UI Model) con mappers explícitos.

---

## 🟠 ALTOS

### 5. Violación de dependencia inversa — Data → ViewModel

**Archivos:** `data/AppPreferences.kt`, `viewmodel/AnimeUiState.kt`

```kotlin
// AppPreferences.kt (capa data)
import com.laumar.aninote.viewmodel.SortOrder  // Importa de ViewModel

// SortOrder está en viewmodel/AnimeUiState.kt
enum class SortOrder { DESC, ASC }
```

La capa de datos **nunca** debería depender de la capa de presentación. Esto crea acoplamiento circular que dificulta testing y reutilización.

**Patrón sostenible:** `SortOrder` debería estar en un paquete `domain` o `model` compartido.

---

### 6. Sin capa de UseCases — ViewModel llama directamente al Repository

**Archivo:** `viewmodel/AnimeViewModel.kt`

```kotlin
viewModelScope.launch {
    val newId = repository.insert(AnimeEntity(...))
    preferences.setSortOrder(SortOrder.DESC)
    _highlightedAnimeId.value = newId
    _events.send(UiEvent.ScrollToTop)
    _events.send(UiEvent.ShowSnackbar("Anime agregado"))
}
```

Toda la lógica de negocio está en el ViewModel. No podés reutilizar la lógica de "agregar anime" en otro lugar (widget, notificación, etc).

**Patrón sostenible:** Extraer a UseCases: `AddAnimeUseCase`, `DeleteAnimeUseCase`, `ImportAnimesUseCase`.

---

### 7. Sin estrategia de migración de base de datos

**Archivo:** `data/AppDatabase.kt`

```kotlin
@Database(entities = [AnimeEntity::class], version = 1, exportSchema = false)
```

Cuando cambies el schema (y lo vas a hacer), vas a tener que hacer fallback a destrucción y los usuarios van a perder datos.

**Patrón sostenible:** `exportSchema = true` + migraciones versionadas.

---

### 8. CoroutineScope inyectado en el Controller

**Archivo:** `viewmodel/ImportExportController.kt`

```kotlin
class ImportExportController(
    private val repository: AnimeRepository,
    private val scope: CoroutineScope,  // Leaks ViewModel scope
    private val emitEvent: suspend (UiEvent) -> Unit
)
```

El controller se acopla al lifecycle del ViewModel. No podés testearlo independientemente.

**Patrón sostenible:** Que el controller maneje su propio scope o que sea un objeto de estado puro con funciones suspend.

---

## 🟡 MEDIOS

### 9. Números mágicos sin constantes

**Archivos:** `viewmodel/AnimeViewModel.kt`, `ui/screens/AnimeListContent.kt`

```kotlin
delay(1200)                          // Highlight duration
delay(4000)                          // Undo window
SharingStarted.WhileSubscribed(5000) // Subscription timeout
```

Deberían ser constantes nombradas para mantener el significado explícito.

---

### 10. Strings hardcodeados en ViewModel

**Archivo:** `viewmodel/AnimeViewModel.kt`

```kotlin
_events.send(UiEvent.ShowSnackbar("Anime agregado"))
_events.send(UiEvent.ShowSnackbar("Anime actualizado"))
```

Deberían usar string resources para soporte multilingual.

---

### 11. Mutaciones de estado no atómicas

**Archivo:** `viewmodel/AnimeViewModel.kt`

```kotlin
_pendingDeleteIds.value += anime.id   // Read-modify-write
_pendingDeleteIds.value -= anime.id   // No es atómico
```

Aunque `StateFlow` es thread-safe, la operación read-modify-write no lo es. Si dos coroutines modifican el Set concurrentemente, se puede perder una actualización.

**Patrón sostenible:** Usar `update {}` de `MutableStateFlow`:

```kotlin
_pendingDeleteIds.update { it + anime.id }
```

---

### 12. Función con 11 parámetros

**Archivo:** `ui/screens/AnimeListScreen.kt`

```kotlin
@Composable
private fun AnimeListOverlays(
    viewModel: AnimeViewModel,
    themeViewModel: ThemeViewModel,
    dialogState: DialogState,
    pendingDeleteAnime: AnimeEntity?,
    showThemeSheet: Boolean,
    showImportDialog: Boolean,
    pendingImportContent: String?,
    pendingImportIsJson: Boolean,
    onDismissThemeSheet: () -> Unit,
    onDismissImportDialog: () -> Unit
)
```

**Patrón sostenible:** Un data class que agrupe los parámetros relacionados.

---

### 13. Dual `requestDelete` / `openEditDialog` sin mappers

**Archivo:** `viewmodel/AnimeViewModel.kt`

```kotlin
fun requestDelete(anime: AnimeUi) {
    _pendingDeleteAnime.value = AnimeEntity(
        id = anime.id,
        nombre = anime.nombre,
        vecesVisto = anime.vecesVisto,
        createdAt = anime.createdAt
    )
}

fun requestDelete(anime: AnimeEntity) {
    _pendingDeleteAnime.value = anime
}
```

Conversión manual e inline de `AnimeUi` a `AnimeEntity`. Esto se repite en `openEditDialog`.

**Patrón sostenible:** Un mapper `AnimeUi.toEntity()` o un `AnimeMapper` dedicado.

---

## 🟢 MENORES

### 14. Typealias confusos

**Archivo:** `utils/JsonImportExport.kt`

```kotlin
typealias AnimeJson = AnimeJsonDto
typealias AnimeListJson = AnimeBackupDto
```

Dos nombres para lo mismo — crea confusión sobre cuál usar.

---

### 15. Enum `ListFilter` sin uso

**Archivo:** `viewmodel/AnimeUiState.kt`

```kotlin
enum class ListFilter { ALL, REWATCHED }  // Definido pero nunca usado
```

Código muerto que增加了 mantenimiento.

---

### 16. `@Immutable` en Room Entity

**Archivo:** `data/AnimeEntity.kt`

```kotlin
@Immutable
@Entity(tableName = "animes")
data class AnimeEntity(...)
```

`@Immutable` es un hint para Compose, pero Room entities no lo necesitan — el modelo de UI (`AnimeUi`) sí lo tiene correctamente.

---

## 📊 Tabla de Severidad

| # | Problema | Severidad | Archivo principal | Impacto |
|---|----------|-----------|-------------------|---------|
| 1 | God ViewModel (288 líneas) | 🔴 CRÍTICO | `AnimeViewModel.kt` | Testing imposible, miedo a modificar |
| 2 | `runBlocking` en main thread | 🔴 CRÍTICO | `AnimeViewModel.kt` | ANR en producción |
| 3 | Sin DI framework | 🔴 CRÍTICO | `MainActivity.kt`, factories | Cada cambio = 3 archivos |
| 4 | Entity en UI state | 🔴 CRÍTICO | `AnimeUiState.kt` | Bug silencioso al cambiar schema |
| 5 | Dependencia inversa Data→VM | 🟠 ALTO | `AppPreferences.kt` | Acoplamiento circular |
| 6 | Sin UseCases | 🟠 ALTO | `AnimeViewModel.kt` | Lógica no reutilizable |
| 7 | Sin migraciones DB | 🟠 ALTO | `AppDatabase.kt` | Pérdida de datos |
| 8 | Scope inyectado | 🟠 ALTO | `ImportExportController.kt` | Testing difícil |
| 9 | Números mágicos | 🟡 MEDIO | `AnimeViewModel.kt` | Mantenimiento difícil |
| 10 | Strings hardcodeados | 🟡 MEDIO | `AnimeViewModel.kt` | Sin multilingual |
| 11 | Mutaciones no atómicas | 🟡 MEDIO | `AnimeViewModel.kt` | Race conditions |
| 12 | 11 parámetros en función | 🟡 MEDIO | `AnimeListScreen.kt` | ilegible |
| 13 | Dual overloads sin mapper | 🟡 MEDIO | `AnimeViewModel.kt` | Conversión manual |
| 14 | Typealias confusos | 🟢 BAJO | `JsonImportExport.kt` | Confusión |
| 15 | Enum sin uso | 🟢 BAJO | `AnimeUiState.kt` | Código muerto |
| 16 | @Immutable en Entity | 🟢 BAJO | `AnimeEntity.kt` | Confusión semántica |

---

## 🎯 Plan de Mejora Recomendado

### Fase 1 — Quick Wins (1-2 horas)

1. **Extraer `SortOrder`** a un paquete `model` o `domain`
   - Elimina la violación de dependencia inversa
   - 5 minutos de trabajo

2. **Eliminar `runBlocking`** de `onCleared()`
   - Reemplazar con `viewModelScope.launch` o simplemente omitir
   - Previene ANR

3. **Reemplazar mutaciones no atómicas** por `update {}`
   - `_pendingDeleteIds.update { it + anime.id }`
   - Previene race conditions

4. **Eliminar `ListFilter`** no usado
   - Limpieza de código muerto

### Fase 2 — Arquitectura (4-6 horas)

5. **Introducir Hilt/Koin** para DI
   - Elimina fábricas boilerplate
   - Simplifica testing

6. **Separar modelos** (Entity vs UI) con mappers
   - `AnimeMapper.kt` con funciones de extensión
   - Elimina la dependencia circular

7. **Agregar migraciones Room**
   - `exportSchema = true`
   - `Migration(1, 2) { ... }`

### Fase 3 — Refactor profundo (8-12 horas)

8. **Extraer UseCases** del ViewModel
   - `AddAnimeUseCase`, `DeleteAnimeUseCase`, `ImportAnimesUseCase`
   - Cada UseCase es testeable independientemente

9. **Dividir God ViewModel**
   - `AnimeListViewModel` (lista y búsqueda)
   - `AnimeDialogViewModel` (add/edit)
   - `AnimeDeleteViewModel` (borrado con undo)

---

## Archivos del Proyecto

```
app/src/main/java/com/laumar/aninote/
├── AniNoteApp.kt                    ─ Application
├── MainActivity.kt                  ─ Entry point (DI manual aquí)
├── data/
│   ├── AnimeDao.kt                  ─ Room DAO
│   ├── AnimeEntity.kt               ─ Room Entity (@Immutable innecesario)
│   ├── AppDatabase.kt               ─ Room Database (sin migraciones)
│   └── AppPreferences.kt            ─ DataStore (depende de viewmodel/)
├── repository/
│   └── AnimeRepository.kt           ─ Repository (thin wrapper)
├── viewmodel/
│   ├── AnimeUiEvent.kt              ─ UI Events
│   ├── AnimeUiState.kt              ─ UI State + SortOrder + ListFilter
│   ├── AnimeViewModel.kt            ─ God ViewModel (288 líneas)
│   ├── AnimeViewModelFactory.kt     ─ Factory boilerplate
│   ├── ImportExportController.kt    ─ Scope inyectado
│   ├── ThemeViewModel.kt            ─ Theme VM + Factory
│   └── ThemeViewModelFactory.kt     ─ Factory boilerplate
├── ui/
│   ├── components/
│   │   ├── AddEditDialog.kt
│   │   ├── AnimeCard.kt
│   │   ├── DeleteConfirmDialog.kt
│   │   ├── EmptyState.kt
│   │   ├── ImportConfirmDialog.kt
│   │   ├── SortToggle.kt
│   │   ├── ThemeBottomSheet.kt
│   │   └── VecesVistoStepper.kt
│   ├── screens/
│   │   ├── AnimeListContent.kt
│   │   ├── AnimeListFileActions.kt
│   │   ├── AnimeListScreen.kt       ─ 11 parámetros en AnimeListOverlays
│   │   └── AnimeListTopBar.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── utils/
    ├── ImportExportUtils.kt         ─ TXT parsing
    └── JsonImportExport.kt          ─ JSON parsing + typealiases
```
