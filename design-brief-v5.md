# Design Brief — Anime List App

**Versión:** v5.1 (especificación definitiva de evolución post-MVP)  
**Fecha:** 2026-08-10  
**Estado:** Autocontenido y listo para implementación por fases  
**Stack objetivo:** Kotlin · Jetpack Compose · Material 3 · Room · DataStore  
**Modo de construcción:** Código escrito esencialmente a mano. La IA actúa como asistente de arquitectura y verificación, respetando el desarrollo especificado.

---

## 1. Resumen & principio rector

La aplicación **AniNote** ya existe y se encuentra en estado funcional MVP (v4). **v5 no es una reescritura desde cero**: es una especificación de **evolución e ingeniería de calidad**. El objetivo deja de ser "llegar a un scope mínimo" para enfocarse en **lograr que la aplicación viva se sienta sólida, clara, pulida y accesible, resolviendo inconsistencias de UX y rendimiento sin distorsionar su filosofía minimalista**.

**Principio rector de v5: Funcionalidad clara + pulido visual + robustez reactiva + arquitectura limpia, sin características estrafalarias.**

* **No se agregan funcionalidades complejas**: Sin imágenes ni posters remotos, sin categorías/etiquetas complejas, sin almacenamiento en la nube, sin cuentas de usuario.
* **Sí se agregan mejoras de alta calidad**: Numeración canónica real, orientación clara en listas largas, feedback visual sutil, canalizaciones reactivas aisladas (`combine` optimizado), importación/exportación transaccional resiliente y localización completa en `strings.xml`.

**Promesa de fondo (inmutable):** Todo dato vive de forma 100% local en el dispositivo. No hay backend, no hay autenticación ni consumo de APIs de red para datos de anime. Las únicas salidas externas son abrir el navegador para búsquedas en Google y leer/escribir archivos `.txt` / `.json` para respaldo e importación.

---

## 2. Matriz delta: Cambios de v4 a v5

| Área | Estado en v4 (MVP actual) | Especificación v5 (Evolución) | Razón técnica / UX |
|---|---|---|---|
| **Estrategia Docs** | Documentos divididos con referencias cruzadas | Especificación v5 100% autocontenida | Elimina la desincronización de especificaciones (*Split-Brain Spec*) |
| **Default de Orden** | Ascendente (`createdAt` ASC) | Descendente ("Recientes" primero) | El caso de uso principal con ~200 items es revisar lo último agregado |
| **Numeración Visual** | Posición en pantalla (`index + 1`) | Posición canónica de la lista ascendente | En vista descendente se busca ver `N…1`, y las búsquedas deben mostrar el número real |
| **Query de DB** | Múltiples queries (`getAll` / `getAllDesc`) | Query canónica única (`createdAt ASC, id ASC`) | Garantiza la fuente única de verdad histórica; elimina empates por milisegundos idénticos |
| **Pipeline Reactivo** | Meclaba estados de UI y datos en 1 solo `combine` | Pipeline de datos aislado de estados efímeros de UI | Evita filtrar/ordenar la lista cuando se abre un diálogo o se escribe en un `TextField` |
| **Persistencia Orden** | No se persistía | Persistido en DataStore (`SORT_ORDER_KEY`) | Mantiene el modo de lectura elegido por el usuario entre sesiones |
| **Importación Batch** | `insertAll` con timestamps idénticos | Timestamps secuenciales (`base + index`) + `@Transaction` | Preserva el orden exacto del archivo importado y garantiza atomicidad en replace |
| **Deduplicación** | Sensible a mayúsculas/minúsculas | Case-insensitive con `trim().lowercase()` | Evita duplicar entradas como "Naruto" y "naruto" al importar |
| **Exportación `.txt`** | Strip forzado del sufijo `xN` | No destructivo (`formatLine` condicional) | No muta texto del usuario; reserva el respaldo exacto para `.json` |
| **Orientación UI** | Sin indicadores de cantidad | Subtítulo con contador (Total / Resultados) | Proporciona contexto inmediato en listas largas o filtradas |
| **Localización** | Textos en español hardcodeados en UI | 100% extraído a `strings.xml` | Buenas prácticas de Android, facilita mantenibilidad y testing |

---

## 3. Stack técnico & restricciones de entorno

Se mantiene la pila tecnológica base con refinamientos de arquitectura:

* **Lenguaje:** Kotlin 1.9+ / 2.0+
* **UI:** Jetpack Compose (Toolkit declarativo actual)
* **Sistema de Diseño:** Material 3 (Tokens, capas de color, tipografía M3, animaciones)
* **Persistencia Principal:** Room Database (SQLite tipado con queries reactivas vía `Flow`)
* **Preferencias:** DataStore Preferences (Modo de iluminación, acento de color y orden persistido)
* **Asincronía & Reactividad:** Kotlin Coroutines + Flow (`StateFlow`, `SharedFlow`, `combine`)
* **Serialización:** `kotlinx.serialization` (Serializador Kotlin-first para backups JSON)

**Restricciones explícitas de entorno:**
* **Sin DI Frameworks (Hilt/Koin):** El grafo de dependencias se mantiene liviano mediante instanciación en la clase `Application` (`AniNoteApp`) y ViewModel Factories simples.
* **Sin Navigation Compose:** La aplicación se mantiene en una sola pantalla con estados bien delimitados (`AnimeListScreen`).
* **Ejecución de Pruebas:** Las pruebas unitarias e instrumentadas se ejecutan manualmente desde Android Studio (JUnit 4 + AndroidX Test).

---

## 4. Identidad visual & Motion system

### 4.1 Sistema de temas (Dos capas independientes)

1. **Capa 1 — Modo de iluminación (Claro / Oscuro / Sistema):**
   * Controla superficies, fondos y jerarquía de contraste de texto.
   * `background`: `#FAFAF9` (Claro) / `#121212` (Oscuro).
   * `surface`: `#FFFFFF` (Claro) / `#1E1E1E` (Oscuro).
   * `surfaceVariant`: `#F0F0EE` (Claro) / `#262626` (Oscuro).

2. **Capa 2 — Color de acento de marca:**
   * El usuario elige explícitamente entre 4 acentos: Verde (`#4CAF50`), Naranja (`#FF7A45`), Azul (`#4B7BE5`), Morado (`#8B6FE0`), Rojo (`#E53935`).
   * **Decisión cerrada:** No usar `dynamicColor` (Material You basado en wallpaper). El acento es elección explícita del usuario.
   * El acento aplica a: FAB, número de lista, chips activos e indicador de foco en búsqueda.

### 4.2 Sistema de animación (Motion System)

**Principio:** Las animaciones explican los cambios de estado, jamás decoran sin propósito.

* **Animaciones permitidas:**
  * Transición suave en `EmptyState` y botón de limpiar búsqueda: `AnimatedVisibility` (fade + scale).
  * Cambio de texto en contador de TopBar: `Crossfade`.
  * Cambio de orden ("Recientes" / "Antiguos"): `listState.animateScrollToItem(0)`.
  * Resaltado de item recién agregado: Animación de color de fondo `primaryContainer.copy(alpha = 0.35f)` desvaneciéndose a `surface` en ~1000ms con `animateColorAsState`.
* **Animaciones prohibidas:**
  * Transiciones complejas de Lottie, librerías externas de animación, shared elements o efectos de sonido.

---

## 5. Layout por pantalla & Modularización Compose (State Hoisting)

La interfaz se estructura en una sola pantalla modularizada en 4 archivos principales dentro de `ui/screens/` para mantener responsabilidades delimitadas sin inflar el árbol de recomposición:

```
ui/screens/
├── AnimeListScreen.kt        → Orquestador principal (Scaffold, SnackbarHost, gestión de diálogos y sheets)
├── AnimeListTopBar.kt        → TopBar dedicada: Título, subtítulo contador, SortToggle, Theme Icon, Overflow Menu
├── AnimeListContent.kt       → Fila de FilterChips, SearchBar, LazyColumn con keys estables y EmptyStates
└── AnimeListFileActions.kt   → Contrato e integración con launchers SAF (Storage Access Framework)
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

#### Regla de Claves Estables en `LazyColumn`
En `AnimeListContent`, los ítems de la lista **deben usar explícitamente la clave de ID de Room** para evitar recomposiciones masivas al borrar o filtrar elementos:
```kotlin
LazyColumn(state = listState) {
    items(
        items = animes,
        key = { anime -> anime.id }
    ) { anime ->
        AnimeCard(anime = anime, ...)
    }
}
```

---

## 6. Modelo de datos, pipeline reactivo y comportamiento

### 6.1 Entity y Query Canónica de Room

La entidad Room representa cada registro de anime:

```kotlin
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

    @Delete
    suspend fun delete(anime: AnimeEntity)

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
val ACCENT_KEY = stringPreferencesKey("accent")      // "green" | "orange" | "blue" | "purple"
val SORT_ORDER_KEY = stringPreferencesKey("sort")    // "desc" | "asc" (default: "desc")
```

Cualquier valor corrupto o ausente debe ser manejado mediante fallback automático (`"desc"`, `"system"`, `"green"`) sin emitir valores nulos al flujo de estado.

### 6.3 Pipeline Reactivo Optimizado (ViewModel)

Para evitar re-ejecutar el filtrado y ordenamiento de datos cuando cambian estados efímeros de la interfaz (como mostrar un diálogo), la canalización en `AnimeViewModel` se descompone en un flujo puro de datos:

```kotlin
// Pipeline de datos inmutable
val uiState: StateFlow<AnimeListUiState> = combine(
    repository.getAllCanonical(),
    searchQueryFlow,
    sortOrderFlow,
    filterFlow,
    pendingDeleteIdsFlow
) { entities, query, sortOrder, filter, pendingDeletes ->
    
    // 1. Excluir pending deletes (borrado con Undo)
    val visibleEntities = entities.filterNot { it.id in pendingDeletes }

    // 2. Asignar numeración canónica (1..N sobre el orden ascendente)
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
}.stateIn(
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
Usa `kotlinx.serialization` con el siguiente esquema versionado:
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
* **Cálculo dinámico:** La numeración y los contadores se calculan excluyendo las IDs presentes en `pendingDeleteIdsFlow`.

---

## 7. Localización & Accesibilidad

### 7.1 Catálogo Único de Recursos (`strings.xml`)

Queda estrictamente prohibido hardcodear cadenas de texto en los componentes Composable. Todos los textos deben ser referenciados mediante `stringResource(R.string.id)`:

```xml
<!-- strings.xml (Extracto de catálogo) -->
<resources>
    <string name="app_name">AniNote</string>
    <string name="topbar_title">Mi lista</string>
    <string name="counter_all">%1$d animes</string>
    <string name="counter_filtered">%1$d de %2$d</string>
    
    <string name="sort_recent">Recientes</string>
    <string name="sort_oldest">Antiguos</string>
    <string name="sort_desc_cd">Más recientes primero</string>
    <string name="sort_asc_cd">Más antiguos primero</string>
    
    <string name="filter_all">Todos</string>
    <string name="filter_rewatched">x2+</string>
    
    <string name="import_result_success">Se importaron %1$d animes</string>
    <string name="import_result_with_skips">Se importaron %1$d animes (%2$d duplicados omitidos)</string>
</resources>
```

### 7.2 Accesibilidad (Semantics)

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

---

## 9. Fuera de alcance (v5)

Permanecen fuera del alcance de la aplicación para preservar la simplicidad:
* Imágenes, posters remotos o consumo de APIs externas de anime (Jikan, Kitsu, AniList, etc.).
* Categorizaciones avanzadas, géneros, o etiquetas personalizadas.
* Sincronización en la nube o cuentas de usuario.
* Ordenamiento alfabético (A-Z / Z-A).
* Animaciones complejas con Lottie o Shared Element Transitions.
* Soporte dedicado para tablets o widgets de pantalla de inicio.

---

## 10. Matriz de decisiones cerradas (v5.1)

| # | Decisión | Razón técnica |
|---|---|---|
| 1 | Stack: Kotlin + Compose + M3 + Room + DataStore | Estándar moderno de Android nativo |
| 2 | Theming de dos capas (Modo + Acento) | Flexibilidad de personalización sin acoplar superficies |
| 3 | Sin `dynamicColor` (Wallpaper) | El acento es elección explícita de marca/usuario |
| 4 | Sin DI Frameworks (Hilt/Koin) | Grafo simple instanciado en `Application` / ViewModel Factories |
| 5 | Una sola pantalla (`AnimeListScreen`) | Evita la complejidad innecesaria de Navigation Compose |
| 6 | Query Canónica (`createdAt ASC, id ASC`) | Fuente única de verdad histórica; elimina empates de milisegundos |
| 7 | Pipeline de datos en `combine` separado de UI | Previene re-filtrados costosos al interactuar con diálogos |
| 8 | Numeración canónica (`1..N`) | Refleja la posición real de alta en la lista |
| 9 | Default de vista Descendente ("Recientes") | Permite ver inmediatamente lo último agregado al abrir la app |
| 10 | Persistencia de orden en DataStore | Conserva la preferencia de lectura del usuario entre sesiones |
| 11 | Exportación `.txt` no destructiva (`formatLine`) | Evita mutar nombres que contengan 'xN' legítimamente |
| 12 | Importación batch con timestamps secuenciales | Preserva el orden del archivo al insertar en Room |
| 13 | Deduplicación case-insensitive (`trim().lowercase()`) | Evita duplicados por diferencias de mayúsculas/espacios |
| 14 | Resumen de importación (`ImportResult`) | Transparencia honesta sobre duplicados y líneas ignoradas |
| 15 | Localización 100% en `strings.xml` | Calidad de código, mantenibilidad y facilidades de testing |

---

## 11. Estructura de carpetas del proyecto

```
app/src/main/java/com/laumar/aninote/
│
├── AniNoteApp.kt                → Application: Inicialización de Room y DataStore
│
├── data/
│   ├── AnimeEntity.kt            → Entidad Room (@Entity)
│   ├── AnimeDao.kt               → DAO con query canónica y @Transaction replaceAll
│   ├── AppDatabase.kt            → Base de datos Room (Singleton, Versión 1)
│   └── AppPreferences.kt         → DataStore Preferences (modo, acento, orden)
│
├── repository/
│   └── AnimeRepository.kt        → Intermediario: Operaciones de datos, parsers y dedup
│
├── viewmodel/
│   ├── AnimeViewModel.kt         → Pipeline combine de datos, numeración canónica, eventos UI
│   └── ThemeViewModel.kt         → Estado y escritura de preferencias de tema
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
│   │   ├── AnimeListContent.kt   → Fila de filtros, LazyColumn con keys estables
│   │   └── AnimeListFileActions.kt → Integración SAF para import/export
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

---

## 12. Roadmap de implementación por fases

Las fases son incrementales y no rompedoras; la app se mantiene funcional tras completar cada fase:

* **Fase 1: Query Canónica y Pipeline de Datos (Núcleo)**
  * Actualizar `AnimeDao` con `getAllCanonical()`.
  * Configurar la numeración canónica y el sort en el `combine` de `AnimeViewModel`.
  * Añadir persistencia del orden elegido en `AppPreferences`.
* **Fase 2: Contadores y Orientación UI**
  * Actualizar `AnimeListTopBar` con subtítulo dinámico y animación `Crossfade`.
  * Implementar etiquetas "Recientes" / "Antiguos" en `SortToggle`.
* **Fase 3: Robustez de Importación / Exportación**
  * Actualizar `ImportExportUtils` con timestamps secuenciales y deduplicación `trim().lowercase()`.
  * Implementar `@Transaction replaceAll` en `AnimeDao`.
  * Crear el diálogo de confirmación con `ImportResult`.
* **Fase 4: Motion & Feedback Visual**
  * Implementar el highlight temporal de color al agregar un anime.
  * Añadir `animateScrollToItem(0)` al cambiar el orden.
  * Integrar `AnimatedVisibility` en estados vacíos y botón de limpiar búsqueda.
* **Fase 5: Filtros de Sesión y Componentes UI**
  * Implementar `ListFilterChips` (Todos / x2+).
  * Habilitar click en chip `xN` para abrir edición directa.
* **Fase 6: Localización y Calidad**
  * Extraer todas las cadenas de texto a `strings.xml`.
  * Verificar accesibilidad con TalkBack mediante `Modifier.semantics`.

---

## 13. Criterios de aceptación & plan de pruebas (Android Studio JUnit)

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

### Pruebas de Integración In-Memory (`app/src/test/` con Room in-memory)
1. **Query Canónica (`AnimeDaoTest`):**
   * Verificar que `getAllCanonical()` devuelva siempre los elementos en orden `createdAt ASC, id ASC`.
   * Verificar que `replaceAll` elimine los datos existentes e inserte la nueva lista dentro de una sola transacción.

---

## 14. Glosario técnico

* **State Hoisting:** Patrón de diseño en Compose donde el estado se mueve hacia arriba en el árbol para hacer los composables puros, testeables y reutilizables.
* **Numeración Canónica:** Asignación de un índice persistente secuencial (`1..N`) basado en el orden cronológico original de inserción de cada registro.
* **Combine Flow:** Operador de Coroutines que agrupa múltiples `Flow` y emite un nuevo valor procesado cada vez que cualquiera de las fuentes cambia.
* **Atomicidad Transactional (`@Transaction`):** Propiedad de base de datos que garantiza que un conjunto de operaciones (borrar e insertar) se completen totalmente o se reviertan en caso de fallo, impidiendo estados corruptos.