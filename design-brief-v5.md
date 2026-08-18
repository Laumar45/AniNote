# Design Brief — Anime List App

**Versión:** v5.2 (especificación definitiva de evolución post-MVP + optimizaciones de rendimiento y calidad Mihon-grade)  
**Fecha:** 2026-08-17  
**Estado:** Autocontenido y listo para implementación por fases  
**Stack objetivo:** Kotlin · Jetpack Compose · Material 3 · Room · DataStore · ProGuard/R8  
**Modo de construcción:** Código escrito esencialmente a mano. La IA actúa como asistente de arquitectura y verificación, respetando el desarrollo especificado.

---

## 1. Resumen & principio rector

La aplicación **AniNote** ya existe y se encuentra en estado funcional MVP (v4). **v5 no es una reescritura desde cero**: es una especificación de **evolución e ingeniería de calidad**. El objetivo deja de ser "llegar a un scope mínimo" para enfocarse en **lograr que la aplicación viva se sienta sólida, clara, pulida, accesible y de alto rendimiento (60fps constantes sin caídas de frames ni dependencias infladas), resolviendo inconsistencias de UX y cuellos de botella técnicos**.

**Principio rector de v5: Funcionalidad clara + pulido visual + robustez reactiva + rendimiento Mihon-grade + arquitectura limpia, sin sobre-ingeniería innecesaria.**

* **No se agregan complejidades innecesarias**: Sin arquitectura multi-módulo prematura (se mantiene mono-módulo `:app` bien delimitado), sin frameworks pesados de DI (sin Hilt/Metro), sin posters remotos, sin almacenamiento en la nube, sin cuentas de usuario.
* **Sí se agregan optimizaciones de alta calidad y rendimiento**:
  * **Zero Main-Thread Blocking**: Serialización de respaldos (JSON/TXT) completamente delegada a `Dispatchers.Default` e I/O en `Dispatchers.IO`.
  * **Pipeline Reactivo de Emisión Única**: Flujo de búsqueda y filtrado desacoplado y optimizado con `debounce(250)` y `distinctUntilChanged()`, eliminando dobles emisiones por tecla.
  * **Poda de Dependencias**: Eliminación de `material-icons-extended` (~6MB de APK) a favor de `material-icons-core` e íconos vectoriales específicos.
  * **Compilación Release Optimizada**: Minificación R8 habilitada (`isMinifyEnabled = true`) con reglas ProGuard específicas para Room y `kotlinx.serialization`.
  * **Consolidación de Flujos**: Estado de tema unificado en un solo `ThemeUiState`, eliminando suscripciones redundantes a DataStore.
  * **Experiencia de Usuario Pulida**: Numeración canónica real (`1..N`), orientación en listas largas con contadores, microinteracciones suaves y localización total en `strings.xml`.

**Promesa de fondo (inmutable):** Todo dato vive de forma 100% local en el dispositivo. No hay backend, no hay autenticación ni consumo de APIs de red para datos de anime. Las únicas salidas externas son abrir el navegador para búsquedas en Google y leer/escribir archivos `.txt` / `.json` para respaldo e importación.

---

## 2. Matriz delta: Cambios de v4 a v5.2

| Área | Estado en v4 (MVP actual) | Especificación v5.2 (Evolución & Calidad) | Razón técnica / UX (Benchmark Mihon) |
|---|---|---|---|
| **Estrategia Docs** | Documentos divididos con referencias cruzadas | Especificación v5.2 100% autocontenida | Elimina la desincronización de especificaciones (*Split-Brain Spec*) |
| **Serialización SAF** | `getExportTxt()` / `Json()` ejecutados en Main Thread | Serialización en `Dispatchers.Default` + I/O en `Dispatchers.IO` | **Crítico (P1)**: Previene congelamientos de UI y frame drops con colecciones grandes |
| **Pipeline Reactivo** | `_query` duplicado en `combine` externo (doble emisión) | Pipeline unificado de emisión única con `debounce(250)` | **Crítico (P2)**: Elimina doble recomposición por cada tecla pulsada en búsqueda |
| **Dependencia de Íconos** | `material-icons-extended` (~6MB APK) | `material-icons-core` + vectores/drawables puntuales | **Medio (P3)**: Reduce drásticamente el tamaño del APK y clases en el DEX |
| **Minificación Release** | `isMinifyEnabled = false` | `isMinifyEnabled = true` + ProGuard rules | **Medio (P4)**: Poda de código muerto, optimización R8 y menor tiempo de inicio |
| **Suscripción DataStore** | 3 StateFlows independientes en `ThemeViewModel` | 1 solo flujo unificado `ThemeUiState` | **Bajo (P6)**: Evita lecturas paralelas redundantes a DataStore |
| **Default de Orden** | Ascendente (`createdAt` ASC) | Descendente ("Recientes" primero) | El caso de uso principal con ~200 items es revisar lo último agregado |
| **Numeración Visual** | Posición en pantalla (`index + 1`) | Posición canónica de la lista ascendente | En vista descendente se busca ver `N…1`, y las búsquedas deben mostrar el número real |
| **Query de DB** | Múltiples queries (`getAll` / `getAllDesc`) | Query canónica única (`createdAt ASC, id ASC`) | Garantiza la fuente única de verdad histórica; elimina empates por milisegundos idénticos |
| **Persistencia Orden** | No se persistía | Persistido en DataStore (`SORT_ORDER_KEY`) | Mantiene el modo de lectura elegido por el usuario entre sesiones |
| **Importación Batch** | `insertAll` con timestamps idénticos | Timestamps secuenciales (`base + index`) + `@Transaction` | Preserva el orden exacto del archivo importado y garantiza atomicidad en replace |
| **Deduplicación** | Sensible a mayúsculas/minúsculas | Case-insensitive con `trim().lowercase()` | Evita duplicar entradas como "Naruto" y "naruto" al importar |
| **Exportación `.txt`** | Strip forzado del sufijo `xN` | No destructivo (`formatLine` condicional) | No muta texto del usuario; reserva el respaldo exacto para `.json` |
| **Orientación UI** | Sin indicadores de cantidad | Subtítulo con contador (Total / Resultados) | Proporciona contexto inmediato en listas largas o filtradas |
| **Localización** | Textos en español hardcodeados en UI | 100% extraído a `strings.xml` | Buenas prácticas de Android, facilita mantenibilidad y accesibilidad |

---

## 3. Stack técnico & restricciones de entorno

Se mantiene la pila tecnológica base con refinamientos de arquitectura y optimizaciones de build:

* **Lenguaje:** Kotlin 2.1+
* **UI:** Jetpack Compose (BOM 2024.12.01 o superior)
* **Sistema de Diseño:** Material 3 (Tokens, capas de color, tipografía M3, animaciones estándar)
* **Íconos:** `androidx.compose.material:material-icons-core` (Prohibido `material-icons-extended`)
* **Persistencia Principal:** Room Database 2.6+ (SQLite tipado con queries reactivas vía `Flow`)
* **Preferencias:** DataStore Preferences 1.1+ (Modo de iluminación, acento de color y orden persistido)
* **Asincronía & Reactividad:** Kotlin Coroutines + Flow (`StateFlow`, `combine`, `debounce`, `flowOn`)
* **Serialización:** `kotlinx.serialization` (JSON tipado Kotlin-first)
* **Optimizador de Build:** R8 / ProGuard (`isMinifyEnabled = true` en release)

**Restricciones de Despacho de Hilos (Threading Contracts):**
* **Main Thread (UI):** Exclusivo para renderizado y eventos de usuario. Prohibido ejecutar serializaciones, parsing de texto o I/O en este hilo.
* **`Dispatchers.Default`:** Para operaciones CPU-bound (serialización/deserialización JSON/TXT, ordenamiento y filtrado de listas en memoria).
* **`Dispatchers.IO`:** Para operaciones de disco y base de datos (lectura/escritura de streams de Storage Access Framework y transacciones Room).

**Restricciones de Arquitectura y Complejidad:**
* **Sin DI Frameworks (Hilt/Metro/Koin):** El grafo de dependencias se mantiene liviano mediante instanciación en la clase `Application` (`AniNoteApp`) y ViewModel Factories simples.
* **Estructura Mono-módulo (`:app`):** Para el tamaño del proyecto (<1000 registros y 1 pantalla principal), un multi-módulo introduce fricción innecesaria. La separación se aplica a nivel de paquetes (`data`, `repository`, `viewmodel`, `ui`, `utils`).
* **Sin Navigation Compose:** La aplicación se mantiene en una sola pantalla con estados bien delimitados (`AnimeListScreen`).
* **Ejecución de Pruebas:** Las pruebas unitarias e instrumentadas se ejecutan manualmente desde Android Studio (JUnit 4 + AndroidX Test).

**Agent Constraints (implementation rules):**
* If it's not in this brief, it does not exist — do not add features, files, or dependencies "because it makes sense."
* If something is ambiguous, do not guess — ask, or mark it as an open question / assumption per this brief's policies.
* Do not add dependencies outside the Stack table without flagging it first.
* Do not create files outside Project Structure without flagging it first.
* Do not rename established identifiers (see Naming Dictionary) without flagging it first.
* Do not compile or run build commands (`./gradlew`, `build`, etc.) directly; the developer compiles, installs, and validates in Android Studio per `AGENTS.md`.

---

## 4. Identidad visual & Motion system

### 4.1 Sistema de temas (Dos capas independientes)

1. **Capa 1 — Modo de iluminación (Claro / Oscuro / Sistema):**
   * Controla superficies, fondos y jerarquía de contraste de texto.
   * `background`: `#FAFAF9` (Claro) / `#121212` (Oscuro).
   * `surface`: `#FFFFFF` (Claro) / `#1E1E1E` (Oscuro).
   * `surfaceVariant`: `#F0F0EE` (Claro) / `#262626` (Oscuro).

2. **Capa 2 — Color de acento de marca:**
   * El usuario elige explícitamente entre 5 acentos: Verde (`#4CAF50`), Naranja (`#FF7A45`), Azul (`#4B7BE5`), Morado (`#8B6FE0`), Rojo (`#E53935`).
   * **Decisión cerrada:** No usar `dynamicColor` (Material You basado en wallpaper). El acento es elección explícita del usuario.
   * El acento aplica a: FAB, número de lista, chips activos e indicador de foco en búsqueda.

### 4.2 Sistema de animación y rendimiento de renderizado

**Principio:** Las animaciones explican los cambios de estado con transiciones fluidas de 60fps, evitando recomposiciones innecesarias.

* **Animaciones permitidas:**
  * Transición suave en `EmptyState` y botón de limpiar búsqueda: `AnimatedVisibility` (fade + scale).
  * Cambio de texto en contador de TopBar: `Crossfade`.
  * Cambio de orden ("Recientes" / "Antiguos"): `listState.animateScrollToItem(0)`.
  * Resaltado de item recién agregado: Animación de color de fondo `primaryContainer.copy(alpha = 0.35f)` desvaneciéndose a `surface` en ~1000ms con `animateColorAsState`.
* **Reglas de Rendimiento en Listas (`LazyColumn`):**
  * Uso obligatorio de `key = { anime.id }` para reciclaje eficiente de nodos.
  * Uso de `contentType = { "anime_card" }` en `items()` para optimizar el pool de composición.
  * Modelos de UI marcados como `@Immutable` para permitir el *skipping* de recomposiciones en Compose Compiler.

---

## 5. Layout por pantalla & Modularización Compose (State Hoisting)

La interfaz se estructura en una sola pantalla modularizada en 4 archivos principales dentro de `ui/screens/`:

```
ui/screens/
├── AnimeListScreen.kt        → Orquestador principal (Scaffold, SnackbarHost, gestión de diálogos y sheets)
├── AnimeListTopBar.kt        → TopBar dedicada: Título, subtítulo contador, SortToggle, Theme Icon, Overflow Menu
├── AnimeListContent.kt       → Fila de FilterChips, SearchBar, LazyColumn con keys estables y EmptyStates
└── AnimeListFileActions.kt   → Contrato e integración SAF con despacho asíncrono off-main-thread
```

### 5.1 Firmas de State Hoisting y desacoplamiento UI

Para evitar que los subcomposables dependan directamente del ViewModel, se exige el siguiente contrato inmutable:

#### `AnimeListTopBar`
```kotlin
@Composable
fun AnimeListTopBar(
    totalCount: Int,
    visibleCount: Int,
    isFilteredOrSearched: Boolean,
    sortOrder: SortOrder,
    onSortOrderChanged: (SortOrder) -> Unit,
    onOpenThemeSheet: () -> Unit,
    onImportRequested: () -> Unit,
    onExportTxtRequested: () -> Unit,
    onExportJsonRequested: () -> Unit,
    modifier: Modifier = Modifier
)
```

#### `AnimeListContent`
```kotlin
@Composable
fun AnimeListContent(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    activeFilter: ListFilter,
    onFilterChanged: (ListFilter) -> Unit,
    animes: List<AnimeUi>,
    isInitialLoading: Boolean,
    highlightedAnimeId: Long?,
    onAnimeClick: (AnimeUi) -> Unit,
    onChipRewatchedClick: (AnimeUi) -> Unit,
    onDeleteClick: (AnimeUi) -> Unit,
    onCopyClick: (AnimeUi) -> Unit,
    onGoogleSearchClick: (AnimeUi) -> Unit,
    modifier: Modifier = Modifier
)
```

### 5.2 Despacho Asíncrono en `AnimeListFileActions` (Storage Access Framework)

Para garantizar 0 congelamientos de UI durante exportaciones o importaciones de listas grandes:

```kotlin
@Composable
fun rememberAnimeListFileActions(
    viewModel: AnimeViewModel,
    onImportLoaded: (content: String, isJson: Boolean) -> Unit
): AnimeListFileActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportTxtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                // 1. Serialización CPU-bound en Dispatchers.Default
                val exportContent = withContext(Dispatchers.Default) {
                    viewModel.getExportTxt()
                }
                // 2. Escritura I/O-bound en Dispatchers.IO
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                        writer.write(exportContent)
                    }
                }
            }
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val exportContent = withContext(Dispatchers.Default) {
                    viewModel.getExportJson()
                }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer ->
                        writer.write(exportContent)
                    }
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            scope.launch {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(fileUri)?.bufferedReader()?.use { reader ->
                        reader.readText()
                    }
                }
                if (content != null) {
                    val mimeType = context.contentResolver.getType(fileUri)
                    val isJson = mimeType == "application/json" ||
                            fileUri.toString().endsWith(".json", ignoreCase = true) ||
                            content.trimStart().startsWith("{")
                    onImportLoaded(content, isJson)
                }
            }
        }
    }

    return AnimeListFileActions(
        launchImport = { importLauncher.launch(arrayOf("text/plain", "application/json")) },
        launchExportTxt = { exportTxtLauncher.launch("anime_list.txt") },
        launchExportJson = { exportJsonLauncher.launch("anime_list.json") }
    )
}
```

---

## 6. Modelo de datos, pipeline reactivo y comportamiento

### 6.1 Entity y Query Canónica de Room

La entidad Room representa cada registro de anime con garantía de inmutabilidad:

```kotlin
@Immutable
@Entity(tableName = "animes")
data class AnimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val vecesVisto: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
```

**DAO (`AnimeDao`) — Fuente única de verdad:**
Se elimina la duplicación de queries SQL (`getAll` / `getAllDesc`). Se utiliza una única query canónica que garantiza la cronología histórica y desempata inserciones continuas:

```kotlin
@Dao
interface AnimeDao {
    @Query("SELECT * FROM animes ORDER BY createdAt ASC, id ASC")
    fun getAllCanonical(): Flow<List<AnimeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(anime: AnimeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(animes: List<AnimeEntity>)

    @Update
    suspend fun update(anime: AnimeEntity)

    @Query("UPDATE animes SET nombre = :nombre, vecesVisto = :vecesVisto WHERE id = :id")
    suspend fun updateNameAndCount(id: Long, nombre: String, vecesVisto: Int)

    @Delete
    suspend fun delete(anime: AnimeEntity)

    @Query("DELETE FROM animes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM animes")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(animes: List<AnimeEntity>) {
        deleteAll()
        insertAll(animes)
    }
}
```

### 6.2 DataStore Preferences

`AppPreferences.kt` administra las claves de configuración local:

```kotlin
val MODE_KEY = stringPreferencesKey("mode")          // "light" | "dark" | "system"
val ACCENT_KEY = stringPreferencesKey("accent")      // "green" | "orange" | "blue" | "purple" | "red"
val SORT_ORDER_KEY = stringPreferencesKey("sort")    // "desc" | "asc" (default: "desc")
```

Cualquier valor corrupto o ausente debe ser manejado mediante fallback automático (`"desc"`, `"system"`, `"green"`) sin emitir valores nulos al flujo de estado.

### 6.3 Modelos de UI y Pipeline Reactivo de Emisión Única (ViewModel)

**Modelos inmutables de UI y Estado (`AnimeUiState.kt`):**

```kotlin
enum class SortOrder { DESC, ASC }

enum class ListFilter { ALL, REWATCHED }

@Immutable
data class AnimeUi(
    val id: Long,
    val numero: Int,
    val nombre: String,
    val vecesVisto: Int,
    val createdAt: Long = 0L
)

sealed interface AnimeListUiState {
    data object Loading : AnimeListUiState
    data class Success(
        val animes: List<AnimeUi>,
        val totalCount: Int,
        val visibleCount: Int
    ) : AnimeListUiState
}
```

Para corregir la doble emisión por pulsación de tecla y evitar recomposiciones masivas:
1. `_query` se somete a `debounce(250)` y `distinctUntilChanged()`.
2. Se utiliza un único `combine` plano que consolida datos sin anidar `dataState` y `_query` por separado:

```kotlin
@OptIn(FlowPreview::class)
private val debouncedQueryFlow = _query
    .debounce(250)
    .distinctUntilChanged()

val uiState: StateFlow<AnimeListUiState> = combine(
    repository.getAllCanonical(),
    debouncedQueryFlow,
    _sortOrder,
    _activeFilter,
    _pendingDeleteIds
) { entities, query, sortOrder, filter, pendingDeletes ->

    // 1. Excluir pending deletes (borrado con Undo)
    val visibleEntities = entities.filterNot { it.id in pendingDeletes }

    // 2. Asignar numeración canónica (1..N sobre el orden ascendente histórico)
    val canonicalList = visibleEntities.mapIndexed { index, entity ->
        AnimeUi(
            id = entity.id,
            numero = index + 1,
            nombre = entity.nombre,
            vecesVisto = entity.vecesVisto
        )
    }

    // 3. Aplicar búsqueda por nombre (case-insensitive)
    val searchedList = if (query.isBlank()) canonicalList
    else canonicalList.filter { it.nombre.contains(query, ignoreCase = true) }

    // 4. Aplicar filtro secundario (Todos / vistos > 1)
    val filteredList = when (filter) {
        ListFilter.ALL -> searchedList
        ListFilter.REWATCHED -> searchedList.filter { it.vecesVisto > 1 }
    }

    // 5. Aplicar orden de vista (Recientes = DESC, Antiguos = ASC)
    val finalList = if (sortOrder == SortOrder.DESC) filteredList.asReversed() else filteredList

    AnimeListUiState.Success(
        animes = finalList,
        totalCount = canonicalList.size,
        visibleCount = finalList.size
    )
}.flowOn(Dispatchers.Default)
 .stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = AnimeListUiState.Loading
 )
```

### 6.4 Algoritmo de Importación & Exportación

#### Respaldo e Importación Formato `.txt`
* **Parser de importación:** Lee línea por línea, trimea espacios y descarta líneas vacías. Remueve prefijos numéricos iniciales (ej. `"1. "`). **No parsea el sufijo `xN`**; las entradas importadas desde `.txt` se asignan por defecto con `vecesVisto = 1`.
* **Exportación no destructiva (`formatLine`):**
```kotlin
fun formatLine(position: Int, name: String, vecesVisto: Int): String {
    val hasSuffix = Regex("\\s+x\\d+$").containsMatchIn(name)
    return if (vecesVisto > 1 && !hasSuffix) "$position. $name x$vecesVisto"
           else "$position. $name"
}
```

#### Respaldo e Importación Formato `.json`
Usa `kotlinx.serialization` con el siguiente esquema versionado y DTOs tipados:

```kotlin
@Serializable
data class AnimeJsonDto(
    val nombre: String,
    val vecesVisto: Int = 1
)

@Serializable
data class AnimeBackupDto(
    val version: Int = 1,
    val animes: List<AnimeJsonDto>
)

data class ImportResult(
    val importedCount: Int,
    val skippedDuplicates: Int,
    val invalidLines: Int
)
```

Esquema JSON representativo (`anime_list.json`):
```json
{
  "version": 1,
  "animes": [
    { "nombre": "One Punch Man", "vecesVisto": 1 },
    { "nombre": "Konosuba", "vecesVisto": 2 }
  ]
}
```
`Json { prettyPrint = true; ignoreUnknownKeys = true }` garantiza compatibilidad con versiones futuras y archivos legibles. **El formato JSON preserva el valor exacto de `vecesVisto`**.

#### Reglas Críticas de Importación (Batch & Deduplicación)
1. **Timestamp Secuencial:** Para evitar colisiones en Room al insertar listas masivas, se asigna `createdAt = baseTimestamp + index`. Esto preserva el orden exacto del archivo al hacer query por `createdAt ASC`.
2. **Deduplicación en modo "Combinar":** Se compara usando `existing.nombre.trim().lowercase() == imported.nombre.trim().lowercase()`. Si ya existe, la entrada del archivo se descarta.
3. **Resumen de Importación (`ImportResult`):** Transmite al usuario cuántas entradas se importaron, cuántos duplicados se omitieron y cuántas líneas tenían formato inválido.

### 6.5 Política de Pending Deletes (Undo)

* **Un solo borrado pendiente a la vez:** Confirmar un segundo borrado commitea inmediatamente el anterior en la base de datos Room.
* **Confirmación automática en IO:** Iniciar una importación o exportación commitea de inmediato cualquier borrado pendiente.
* **Cálculo dinámico:** La numeración y los contadores se calculan excluyendo las IDs presentes en `pendingDeleteIds`.

### 6.6 Consolidación de `ThemeViewModel`

Para evitar la triple suscripción paralela a DataStore, `ThemeViewModel` consume directamente `AppPreferences`:

```kotlin
sealed interface ThemeUiState {
    data object Loading : ThemeUiState
    data class Success(val mode: String, val accent: String) : ThemeUiState
}

class ThemeViewModel(private val preferences: AppPreferences) : ViewModel() {
    val uiState: StateFlow<ThemeUiState> = combine(
        preferences.modeFlow,
        preferences.accentFlow
    ) { mode, accent ->
        ThemeUiState.Success(mode = mode, accent = accent)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeUiState.Loading
    )

    fun setMode(mode: String) = viewModelScope.launch { preferences.setMode(mode) }
    fun setAccent(accent: String) = viewModelScope.launch { preferences.setAccent(accent) }
}
```

---

## 7. Optimización de Recursos & Localización

### 7.1 Poda de Dependencias de Íconos

Se prohíbe el uso de `androidx.compose.material:material-icons-extended` (ahorro de ~6MB en APK). La aplicación solo utiliza los siguientes íconos esenciales desde `material-icons-core` o `ImageVector` locales:
* `Icons.Default.Add`
* `Icons.Default.Delete`
* `Icons.Default.Search`
* `Icons.Default.Close`
* `Icons.Default.MoreVert`
* `Icons.Default.Settings`
* `Icons.Default.ContentCopy` / Vector drawable local
* `Icons.Default.SearchOff` / Vector drawable local

### 7.2 Catálogo Único de Recursos (`strings.xml`)

Queda estrictamente prohibido hardcodear cadenas de texto en los componentes Composable. Todos los textos deben ser referenciados mediante `stringResource(R.string.id)`.

### 7.3 Accesibilidad (Semantics)

Cada tarjeta de anime debe incluir un `Modifier.semantics` descriptivo para lectores de pantalla (TalkBack):
```kotlin
Modifier.semantics {
    contentDescription = "$numero. $nombre" + if (vecesVisto > 1) ", visto $vecesVisto veces" else ""
}
```

---

## 8. Microinteracciones

| Acción | Comportamiento en pantalla |
|---|---|
| **Tap en Tarjeta** | Abre el diálogo de edición con datos pre-cargados. |
| **Tap en Chip `xN`** | Abre el diálogo de edición enfocando la modificación de conteo. |
| **Borrar** | Muestra diálogo de confirmación. Al confirmar, remueve de vista y muestra Snackbar con opción "Deshacer" por 4 segundos. |
| **Búsqueda en Google** | Lanza `Intent.ACTION_VIEW` codificando la URL en UTF-8 (`Uri.encode`) para prevenir fallos con caracteres especiales o japoneses. |
| **Cambio de Orden** | Invierte la lista y ejecuta `listState.animateScrollToItem(0)`. |
| **Agregar Anime** | Inserta registro, scrollea al item nuevo y aplica un highlight de color temporal (~1000ms). |

### 8.1 Taxonomía de errores y manejo de fallos (Error Taxonomy)

| Tipo de Error | Detonante / Causa | Respuesta de Usuario (UI) | Manejo Técnico | ¿Reintentable? |
|---|---|---|---|---|
| **`JsonParseException`** | Archivo JSON malformado, sintaxis rota o versión incompatible | Snackbar: *"Error al procesar el archivo JSON de respaldo"* | Log en Logcat, abortar operación sin mutar Room | No |
| **`TxtEmptyOrInvalid`** | Archivo TXT vacío o sin ninguna línea parseable válida | Snackbar: *"El archivo seleccionado no contiene registros válidos"* | Log warning, abortar importación | No |
| **`StorageIoException`** | Fallo de permisos en SAF, stream interrumpido o disco lleno | Snackbar / Toast: *"Error de almacenamiento al leer/escribir archivo"* | `catch (e: IOException)`, cerrar streams de forma segura | Sí (Reintentar desde SAF) |
| **`DatabaseException`** | Error SQLite en inserción/actualización en Room | Snackbar: *"Error en base de datos al guardar los registros"* | Revertir `@Transaction` atómica, emitir log | No |

---

## 9. Fuera de alcance (v5.2)

Permanecen fuera del alcance de la aplicación para preservar la simplicidad y evitar sobre-ingeniería:
* Arquitectura Multi-módulo de 14 módulos (estilo Mihon): innecesaria para un proyecto mono-pantalla de <1000 registros.
* Frameworks de Inyección de Dependencias en tiempo de compilación (Metro/Hilt): se preserva DI manual por factories para propósitos pedagógicos y cero overhead.
* Paging 3: `Flow<List<AnimeEntity>>` es suficiente para <500-1000 items en memoria. Paging 3 se evaluará únicamente si la colección supera 1000 items.
* Baseline Profiles: Se documenta como optimización futura; en v5 el cold-start se optimiza mediante inicialización temprana de Room en `Application`.
* Imágenes, posters remotos o consumo de APIs externas de anime (Jikan, Kitsu, AniList).
* Sincronización en la nube o cuentas de usuario.

---

## 10. Matriz de decisiones cerradas (v5.2)

| # | Decisión | Razón técnica (Benchmark Mihon) |
|---|---|---|
| 1 | Stack: Kotlin + Compose + M3 + Room + DataStore + R8 | Estándar moderno de Android nativo optimizado |
| 2 | Theming de dos capas (Modo + Acento) | Flexibilidad de personalización sin acoplar superficies |
| 3 | Sin `dynamicColor` (Wallpaper) | El acento es elección explícita de marca/usuario |
| 4 | Sin DI Frameworks (Hilt/Metro) | Grafo simple instanciado en `Application` / ViewModel Factories |
| 5 | Una sola pantalla (`AnimeListScreen`) | Evita la complejidad innecesaria de Navigation Compose |
| 6 | Query Canónica (`createdAt ASC, id ASC`) | Fuente única de verdad histórica; elimina empates de milisegundos |
| 7 | Pipeline reactivo de emisión única con `debounce(250)` | Elimina doble recomposición por pulsación de tecla |
| 8 | Serialización SAF en `Dispatchers.Default` + I/O en `IO` | Previene bloqueo del hilo principal al exportar cientos de items |
| 9 | Exclusión de `material-icons-extended` | Ahorro de ~6MB en tamaño de APK final |
| 10 | Minificación R8 habilitada en build Release | Poda de código muerto y optimización de bytecode DEX |
| 11 | Unificación en `ThemeUiState` único | Elimina suscripciones paralelas redundantes a DataStore |
| 12 | Numeración canónica (`1..N`) | Refleja la posición real de alta en la lista |
| 13 | Default de vista Descendente ("Recientes") | Permite ver inmediatamente lo último agregado al abrir la app |
| 14 | Persistencia de orden en DataStore | Conserva la preferencia de lectura del usuario entre sesiones |
| 15 | Exportación `.txt` no destructiva (`formatLine`) | Evita mutar nombres que contengan 'xN' legítimamente |
| 16 | Importación batch con timestamps secuenciales | Preserva el orden del archivo al insertar en Room |
| 17 | Deduplicación case-insensitive (`trim().lowercase()`) | Evita duplicados por diferencias de mayúsculas/espacios |
| 18 | Resumen de importación (`ImportResult`) | Transparencia honesta sobre duplicados y líneas ignoradas |
| 19 | Localización 100% en `strings.xml` | Calidad de código, mantenibilidad y facilidades de testing |
| 20 | Política de Idioma de Identificadores (Mixed Policy) | Entidades Room y DTOs de backup preservan campos de dominio en español (`nombre`, `vecesVisto`) por compatibilidad histórica con la DB v4. Toda la arquitectura, ViewModels, UI State, funciones y componentes se escriben en inglés estándar. |

### 10.1 Supuestos a confirmar

Todos los parámetros clave han sido verificados y alineados con la experiencia MVP:
* **Debounce en búsqueda (250ms):** Confirmado para evitar recomposiciones innecesarias sin generar latencia perceptible al teclear.
* **Highlight temporal en inserción (~1000ms):** Confirmado para brindar feedback visual suave al usuario al agregar un item.
* **Snackbar de borrado con Deshacer (4 segundos):** Confirmado conforme a directrices de Material 3 para operaciones destructivas reversibles.
* **Acentos de tema (5 opciones fijas):** Confirmados como valores fijos de marca para evitar inconsistencias de contraste con wallpaper dinámico.

---

## 11. Configuración de Build & ProGuard Rules

### `app/build.gradle.kts`
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### `app/proguard-rules.pro`
```proguard
# Room Database
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationExtension
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    <init>(...);
}
```

---

## 12. Estructura de carpetas del proyecto

```
app/
├── proguard-rules.pro
└── src/main/java/com/laumar/aninote/
    │
    ├── AniNoteApp.kt                → Application: Inicialización temprana de Room y DataStore
    │
    ├── data/
    │   ├── AnimeEntity.kt            → Entidad Room (@Immutable @Entity)
    │   ├── AnimeDao.kt               → DAO con query canónica y @Transaction replaceAll
    │   ├── AppDatabase.kt            → Base de datos Room (Singleton)
    │   └── AppPreferences.kt         → DataStore Preferences (modo, acento, orden)
    │
    ├── repository/
    │   └── AnimeRepository.kt        → Intermediario: Operaciones de datos, parsers y dedup
    │
    ├── viewmodel/
    │   ├── AnimeViewModel.kt         → Pipeline combine de datos de emisión única, eventos UI
    │   └── ThemeViewModel.kt         → Estado ThemeUiState consolidado
    │
    ├── ui/
    │   ├── theme/
    │   │   ├── Color.kt              → Paletas por acento y modos
    │   │   ├── Theme.kt              → Proveedor MaterialTheme
    │   │   └── Type.kt               → Jerarquía de tipografía M3
    │   │
    │   ├── screens/
    │   │   ├── AnimeListScreen.kt    → Orquestador de UI y estados de Scaffold
    │   │   ├── AnimeListTopBar.kt    → TopBar con contadores y controles
    │   │   ├── AnimeListContent.kt   → Fila de filtros, LazyColumn con keys estables y contentType
    │   │   └── AnimeListFileActions.kt → Integración SAF asíncrona (Default + IO dispatchers)
    │   │
    │   └── components/
    │       ├── AnimeCard.kt          → Tarjeta de anime con semantics y highlight
    │       ├── AddEditDialog.kt      → Diálogo modal agregar/editar
    │       ├── DeleteConfirmDialog.kt→ Diálogo modal de borrado
    │       ├── ImportConfirmDialog.kt→ Diálogo modal de resumen de importación
    │       ├── ThemeBottomSheet.kt   → Selector de tema y acento
    │       ├── SortToggle.kt         → Botón segmentado Recientes/Antiguos
    │       ├── ListFilterChips.kt    → Chips de filtrado Todos / x2+
    │       └── EmptyState.kt         → Iluminación visual de estados vacíos
    │
    └── utils/
        ├── ImportExportUtils.kt      → Parsers .txt, formatLine, ImportResult
        └── JsonImportExport.kt       → Parsers .json kotlinx.serialization
```

### 12.1 Diccionario de nombres canónicos (Naming Dictionary)

| Concepto | Término Canónico | Capa / Ubicación | No confundir con |
|---|---|---|---|
| Registro persistido en base de datos Room | `AnimeEntity` | Room (`data/`) | `AnimeUi` (UI), `AnimeJsonDto` (backup) |
| Modelo inmutable proyectado para renderizado | `AnimeUi` | UI State (`viewmodel/`, `ui/`) | `AnimeEntity` (Room) |
| Tarjeta composable de lista | `AnimeCard` | Compose Component (`ui/components/`) | `AnimeUi` (modelo de datos) |
| DTOs de serialización JSON | `AnimeBackupDto`, `AnimeJsonDto` | Serialización (`utils/`) | `AnimeEntity`, `AnimeUi` |
| Resumen de operación de importación | `ImportResult` | Utils / Diálogo (`utils/`, `ui/`) | `AnimeListUiState` |
| Estado unificado de la pantalla principal | `AnimeListUiState` | StateFlow / UI (`viewmodel/`) | `ThemeUiState` |
| Gestor de preferencias DataStore | `AppPreferences` | DataStore (`data/`) | `AnimeRepository` |

---

## 13. Roadmap de implementación por fases

Las fases son estrictamente incrementales y no rompedoras; la app se mantiene funcional tras completar cada fase:

### Fase 1: Núcleo Reactivo, Despacho y DataStore Consolidado (Alto Impacto) — ✅ IMPLEMENTADA
**Deliverables:** `AnimeDao.kt`, `AnimeViewModel.kt`, `AnimeUiState.kt`, `AppPreferences.kt`, `ThemeViewModel.kt`  
**Done when ALL of:**
- [x] `AnimeDao.getAllCanonical()` emite `Flow<List<AnimeEntity>>` con orden garantizado `(createdAt ASC, id ASC)`.
- [x] `AnimeViewModel.uiState` emite `AnimeListUiState.Success` consolidado tras `debounce(250)` en búsqueda sin recomposiciones duplicadas.
- [x] `ThemeViewModel.uiState` se alimenta de un único flujo combinado desde `AppPreferences` y el orden de lista se persiste en `SORT_ORDER_KEY`.
- [x] No se crearon archivos fuera de la estructura de paquetes definida en §12.

### Fase 2: Contadores y Orientación UI
**Deliverables:** `AnimeListTopBar.kt`, `SortToggle.kt`, `AnimeListScreen.kt`  
**Done when ALL of:**
- [ ] `AnimeListTopBar` muestra el subtítulo dinámico con conteo total vs filtrado animado mediante `Crossfade`.
- [ ] `SortToggle` presenta estados y etiquetas claras ("Recientes" / "Antiguos") sincronizadas con `SortOrder`.
- [ ] La selección de orden actualiza inmediatamente la persistencia en DataStore y la vista.

### Fase 3: Robustez Asíncrona de Importación / Exportación (Alto Impacto)
**Deliverables:** `AnimeListFileActions.kt`, `ImportExportUtils.kt`, `JsonImportExport.kt`, `ImportConfirmDialog.kt`  
**Done when ALL of:**
- [ ] La serialización/deserialización TXT y JSON se ejecuta en `Dispatchers.Default` y el I/O en `Dispatchers.IO` sin bloquear el hilo principal.
- [ ] La detección de archivo en SAF distingue confiablemente JSON de TXT mediante MIME type o inspección de contenido.
- [ ] La deduplicación al importar en modo combinar es case-insensitive (`trim().lowercase()`) y genera un `ImportResult` válido mostrado en `ImportConfirmDialog`.

### Fase 4: Motion & Rendimiento de Renderizado
**Deliverables:** `AnimeListContent.kt`, `AnimeCard.kt`, `EmptyState.kt`  
**Done when ALL of:**
- [ ] `LazyColumn` implementa `key = { it.id }` y `contentType = { "anime_card" }` en todos los items.
- [ ] Al cambiar el orden (`SortOrder`), la lista ejecuta `animateScrollToItem(0)` sin saltos de scroll.
- [ ] Al agregar un nuevo anime, la tarjeta correspondiente recibe un highlight temporal animado con `animateColorAsState` (~1000ms).
- [ ] `AnimatedVisibility` anima suavemente la transición de `EmptyState` y el botón de limpiar búsqueda.

### Fase 5: Filtros de Sesión y Componentes UI
**Deliverables:** `ListFilterChips.kt`, `AnimeListContent.kt`, `AnimeCard.kt`, `AddEditDialog.kt`  
**Done when ALL of:**
- [ ] `ListFilterChips` permite conmutar entre `ListFilter.ALL` y `ListFilter.REWATCHED` actualizando el `visibleCount`.
- [ ] Hacer tap en el chip `xN` de una tarjeta abre el diálogo de edición enfocando directamente el stepper/campo de conteo.
- [ ] Toda la interacción respeta el contrato de State Hoisting sin acoplar composables al ViewModel.

### Fase 6: Poda de Dependencias, Build Release y Localización (Medio Impacto)
**Deliverables:** `build.gradle.kts`, `proguard-rules.pro`, `res/values/strings.xml`, `AnimeCard.kt`  
**Done when ALL of:**
- [ ] Se remueve la dependencia `material-icons-extended` de `app/build.gradle.kts` utilizando solo `material-icons-core` o drawables locales.
- [ ] `isMinifyEnabled = true` está configurado para builds release con las reglas ProGuard especificadas en §11.
- [ ] El 100% de las cadenas visibles en Compose provienen de `strings.xml` mediante `stringResource`.
- [ ] Las tarjetas de anime incluyen `Modifier.semantics` con descripción completa para TalkBack.

---

## 14. Criterios de aceptación & plan de pruebas (Android Studio JUnit)

Las pruebas se diseñan para ejecutarse localmente desde el entorno de Android Studio:

### Pruebas Unitarias (`app/src/test/`)
1. **Parser TXT (`ImportExportUtilsTest`):**
   * Verificar que líneas vacías o con prefijos `"1. "` se limpien correctamente.
   * Verificar que `formatLine` no duplique el sufijo `xN` si este ya existe en el nombre.
2. **Parser JSON (`JsonImportExportTest`):**
   * Validar deserialización correcta preservando `vecesVisto`.
   * Verificar que JSONs con `version != 1` o campos inválidos lancen excepciones controladas.
3. **Deduplicación (`AnimeRepositoryTest`):**
   * Confirmar que `"Konosuba "` y `"konosuba"` se identifiquen como duplicados al combinar listas.
4. **Pipeline ViewModel (`AnimeViewModelTest`):**
   * Verificar que emitir caracteres en el search emita el estado filtrado tras el debounce sin duplicaciones.

### Pruebas de Integración In-Memory (`app/src/test/` con Room in-memory)
1. **Query Canónica (`AnimeDaoTest`):**
   * Verificar que `getAllCanonical()` devuelva siempre los elementos en orden `createdAt ASC, id ASC`.
   * Verificar que `replaceAll` elimine los datos existentes e inserte la nueva lista dentro de una sola transacción.

### Verificación de Build & Performance (Manual en Android Studio)
1. **Compilación Release:** Generar APK release con R8 minification activa y comprobar que Room y Kotlinx Serialization funcionen sin `ClassNotFoundException`.
2. **Inspección de Tamaño de APK:** Verificar reducción de tamaño tras remover `material-icons-extended`.
3. **Export Test:** Exportar una lista con >200 registros y comprobar que no haya frame drops en el UI thread.

---

## 15. Glosario técnico

* **State Hoisting:** Patrón de diseño en Compose donde el estado se mueve hacia arriba en el árbol para hacer los composables puros, testeables y reutilizables.
* **Numeración Canónica:** Asignación de un índice persistente secuencial (`1..N`) basado en el orden cronológico original de inserción de cada registro.
* **Combine Flow:** Operador de Coroutines que agrupa múltiples `Flow` y emite un nuevo valor procesado cada vez que cualquiera de las fuentes cambia.
* **Atomicidad Transactional (`@Transaction`):** Propiedad de base de datos que garantiza que un conjunto de operaciones (borrar e insertar) se completen totalmente o se reviertan en caso de fallo.
* **R8 / Minification:** Herramienta de compilación de Android que optimiza el bytecode, elimina código no utilizado (*tree shaking*) y ofusca clases para reducir el tamaño del APK.