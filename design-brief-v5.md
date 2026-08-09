# Design Brief — Anime List App

**Versión:** v5 (evolución post-MVP)
**Fecha:** 2026-08-09
**Base:** v4 (2026-08-08)
**Stack objetivo:** Kotlin · Jetpack Compose · Material 3 · Room · DataStore
**Modo de construcción:** código esencialmente escrito a mano. La IA se usa solo como apoyo y para consultas, no para generar implementación.

---

## 1. Resumen & principio rector

La app ya existe y funciona. v5 no es un rewrite: es la etapa de evolución. El objetivo ya no es "llegar a un MVP con scope mínimo", sino **lograr que lo que ya funciona se sienta cómodo, claro y pulido, sin convertir la app en algo extravagante**.

**Nuevo principio rector: funcionalidad clara + pulido visual + robustez de comportamiento, sin features extravagantes.**

- No se agrega nada que abra la puerta a un gestor de colecciones (sin posters, sin categorías, sin cloud, sin cuentas).
- Sí se agrega: orientación en listas largas, feedback de acciones, animaciones sutiles y features simples que usan datos que ya existen.

**Promesa de fondo (no cambia):** todo el dato vive local. No hay backend, no hay cuentas, no hay llamadas API de red para datos de animes. La única "salida" hacia afuera es abrir el navegador para buscar en Google, o leer/escribir archivos para importar/exportar.

**Aclaración de orden (central en v5):**

- No existe ni existirá orden alfabético (A-Z / Z-A no representan el orden de alta y no aportan valor).
- El orden relevante es por alta (cronológico / de inserción).
- Con una lista de ~200 animes, el caso de uso principal es **ver rápido lo último agregado**.
- En vista descendente deben verse los números reales de la lista (`194, 193, 192…`), no `1, 2, 3`.

---

## 2. Qué cambia respecto a v4

| Área | v4 | v5 | Razón |
|---|---|---|---|
| Principio | Minimalismo estricto | Minimalismo con pulido | La app ya está hecha; ahora el objetivo es que se sienta bien |
| Default de orden | Ascendente | Descendente ("Recientes") | El caso principal es ver lo último sin scrollear |
| Numeración | Posición visual de la vista actual (`index + 1`) | Posición canónica de la lista ascendente | En descendente querés ver `N…1`, y en búsqueda el número real |
| Sort | Dos queries SQL + `flatMapLatest` | Query canónica única + derivación en memoria | La numeración canónica exige una sola fuente; a ~200 items el costo es nulo |
| Persistencia de orden | No se persistía | Se persiste en DataStore | Comodidad sin costo |
| Contador | No existe | Visible (total y resultados) | Orientación en listas largas |
| Feedback | Básico | Scroll inteligente + highlight + animaciones sutiles | La app debe explicar el cambio |
| Import | Snackbar simple | `ImportResult` rico + diálogo con resumen | Transparencia en operaciones sensibles |
| Export `.txt` | Strip de sufijo `xN` siempre | No reescribe nombres legítimos | No mutar texto del usuario |
| Pending delete | Sin política explícita | Reglas claras | Coherencia entre estado visual y DB |
| Animaciones | Fuera de alcance | Sutiles y funcionales (lista cerrada) | Pulido sin extravagancia |

---

## 3. Stack técnico

Se hereda v4 sin cambios: Kotlin, Compose, M3, Room, DataStore, Coroutines/Flow, single screen con estados. Sin Hilt/Koin, sin Navigation Compose, sin Retrofit/OkHttp, sin Coil/Glide. Si en el futuro se suma algo, se justifica acá, no "porque sí".

| Elemento | Decisión v5 | Razón |
|---|---|---|
| `material-color-utilities` | Recomendado (optativo en esta etapa) | Genera tokens armónicos con contraste correcto para 4 acentos × 2 modos, con semilla explícita. **No** es dynamic color del wallpaper |
| Lottie / libs de animación | No | Con Compose + M3 alcanza para la lista cerrada de animaciones. Sumar una librería para 4 animaciones es extravagante |

---

## 4. Identidad visual

### 4.1 Sistema de temas — dos capas independientes

Se hereda v4: capa de modo (claro / oscuro / sistema) y capa de acento (verde, naranja, azul, morado). Decisión cerrada heredada: **no usar `dynamicColor`**; el acento es elección explícita del usuario.

**Agregado v5:** validar contraste de los 8 `ColorScheme` (chips activos, texto sobre `primaryContainer`, tinte de card en luz solar). Si mantenerlos a mano se vuelve molesto, migrar a `material-color-utilities` con semilla explícita por acento. Empezar manual ya no es obligatorio: la app existe, el objetivo ahora es calidad visual.

### 4.2 Tipografía

Se hereda v4 (fuente del sistema, jerarquía M3, número antes del nombre como firma visual).

**Agregado v5:** el contador del TopBar (§5.1) usa `bodySmall` / `labelMedium` en `onSurfaceVariant`. No compite con el título.

### 4.3 Forma y espaciado

Se hereda v4 sin cambios (12dp en tarjetas, elevación mínima, 8dp entre cards, 16dp de padding, 56dp de altura mínima).

### 4.4 Motion (nuevo)

**Principio: las animaciones explican el cambio, no lo decoran.**

Duraciones sugeridas:

| Tipo | Duración |
|---|---|
| Micro (ícono clear, chips) | 120–200 ms |
| Cambio de estado (empty states, contador) | 200–300 ms |
| Highlight de item nuevo | 1000–1200 ms |
| Scroll programático | default de `animateScrollToItem` |
| Segmented buttons, sheets, snackbars | lo que trae M3 |

**Lista cerrada de lo que SÍ se anima:**

| Elemento | Animación | Implementación |
|---|---|---|
| Empty states | fade + scale suave | `AnimatedVisibility` |
| Botón limpiar búsqueda | fade/scale in-out | `AnimatedVisibility` |
| Contador del TopBar | crossfade | `Crossfade` |
| Cambio de orden | scroll al inicio | `listState.animateScrollToItem(0)` |
| Item recién agregado | fondo que se desvanece | `animateColorAsState` + `highlightedAnimeId` |
| Aparición de chips de filtro | fade + scale | `AnimatedVisibility` |
| Movimientos dentro de LazyColumn | solo reposicionamiento | `Modifier.animateItem()` si la versión de Compose lo soporta |

**Lista cerrada de lo que NO se anima:**

- Paleta de colores al cambiar tema (sigue siendo instantáneo, como v4).
- Texto mientras se escribe en búsqueda.
- Entrada/salida elaborada de items individuales en LazyColumn.
- Lottie, parallax, shared element transitions, sonido.
- Haptics (siguen fuera de alcance, como en v4).

---

## 5. Layout por pantalla

### 5.1 Pantalla principal

Dentro del `Scaffold`, de arriba hacia abajo:

**Top bar (`TopAppBar`)**

- Título: "Mi lista".
- **Subtítulo contador (nuevo):**
  - Sin búsqueda ni filtro: `194 animes`
  - Con búsqueda y/o filtro: `12 de 194`
  - Cambia con `Crossfade`.
- Acción 1: `SortToggle` con 2 estados y **nuevos labels**:
  - **"Recientes"** — descendente (`N → 1`). **Default.**
  - **"Antiguos"** — ascendente (`1 → N`).
  - `contentDescription`: "Más recientes primero" / "Más antiguos primero".
  - El orden elegido **se persiste** en DataStore (§6.1).
  - Al cambiar: `animateScrollToItem(0)`.
  - Sin orden alfabético. Decisión cerrada.
- Acción 2: ícono de tema (hereda v4).
- Acción 3: menú overflow (hereda v4, con nombres de archivo fechados, §5.4).

**Barra de búsqueda**

- Hereda v4 (persistente, placeholder "Buscar anime", foco con `primary`).
- Botón limpiar (X) con `AnimatedVisibility`.
- El contador de resultados no vive acá: vive en el subtítulo del TopBar.

**Fila de filtro (nuevo)**

- Dos `FilterChip` debajo de la búsqueda: `Todos` y `x2+`.
- `x2+` filtra por `vecesVisto > 1`.
- No se persiste: es exploración temporal de la sesión. El orden sí se persiste porque es un modo de lectura global.
- Aparece solo si la lista tiene al menos un item.

**Lista (`LazyColumn`)**

- Cada item es una `AnimeCard` con **número canónico** (§6.2). La UI **no** calcula `index + 1`: lee `ui.numero`.
- `items(items, key = { it.id })` — nunca índice como key.
- Padding inferior 88dp (hereda).
- Highlight de item recién agregado (§6.6).

**FAB** — hereda v4 (estándar con items, `ExtendedFloatingActionButton` con lista vacía).

### 5.2 Diálogo agregar / editar

Hereda v4 sin cambios estructurales (nombre + veces visto, validaciones, strip de newlines).

**Agregado v5:** si el diálogo se abre desde el chip `xN` (§6.3), el foco inicial va al campo "Veces visto". Si resulta costoso, abrir el diálogo sin foco especial es aceptable; el foco es un nice-to-have documentado.

### 5.3 Bottom sheet de tema

Hereda v4 sin cambios (modo + acento, previsualización en vivo, sin botón guardar).

### 5.4 Menú overflow (Import / Export)

Hereda v4 (Importar, Exportar .txt, Exportar .json; cierre del sheet antes de lanzar SAF).

**Agregado v5:** nombres sugeridos fechados:

- `anime_list_2026-08-09.txt`
- `anime_list_2026-08-09.json`

Mejora identificación de backups sin agregar complejidad.

### 5.5 Estados vacíos

Se heredan los dos de v4 (lista vacía / búsqueda sin resultados) y se agregan dos:

- **Filtro `x2+` sin items:** "Ningún anime visto más de una vez".
- **Búsqueda + filtro sin resultados:** `Ningún resultado para "{query}" entre los vistos más de una vez` + botón "Limpiar búsqueda".

Todos con `AnimatedVisibility` (fade + scale) en entrada/salida.

### 5.6 Estado de carga inicial

Hereda v4 (`isInitialLoading` + skeleton shimmer de 12 cards).

**Cláusula v5:** si en device real el skeleton se siente excesivo para una DB local que responde en milisegundos, es válido simplificarlo a un `CircularProgressIndicator` breve o a nada. El flag `isInitialLoading` se mantiene igual para evitar el parpadeo del empty state.

---

## 6. Modelo de datos y comportamiento

### 6.1 Persistencia local

**Room** — entity heredada:

```kotlin
@Entity(tableName = "animes")
data class AnimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val vecesVisto: Int = 1,
    val createdAt: Long = System.currentTimeMillis()  // metadata; ya no es la fuente de orden
)
```

**DAO — cambio v5:** una sola query canónica. Se eliminan `getAll()` / `getAllDesc()`:

```kotlin
@Query("SELECT * FROM animes ORDER BY id ASC")
fun getAllCanonical(): Flow<List<AnimeEntity>>
```

`id ASC` representa el orden de alta real, evita empates de timestamp y no depende del reloj del dispositivo. El orden descendente y la numeración se derivan en el ViewModel (§6.2). Se mantienen `insert`, `update`, `delete`, `deleteAll`, `insertAll`. La dedup de import se hace en Kotlin sobre la lista canónica ya emitida (se elimina `findByNameCaseInsensitive` como query separada).

**DataStore** — claves de tema heredadas + nueva clave de orden:

```kotlin
val SORT_ORDER_KEY = stringPreferencesKey("sort_order")  // "desc" | "asc", default "desc"
```

Valores inválidos o ausentes → fallback a `"desc"`. Sin crash.

### 6.2 Número de lista — posición canónica (reemplaza §5.2 de v4)

**Decisión cerrada:** el número visible es la **posición canónica del item dentro de la lista ascendente visible** (excluyendo pending deletes).

Implicaciones:

- Vista "Antiguos": `1, 2, 3… N`.
- Vista "Recientes": se invierte el render, **no la numeración**: `N, N-1, N-2… 1`.
- Búsqueda y filtro `x2+`: los resultados **conservan su número canónico**. Buscar no renumera a `1, 2, 3`. En una lista larga el número es referencia de ubicación, no decoración.
- Borrar renumera (el número refleja la lista actual). Undo renumera de vuelta.

**Pipeline de derivación (ViewModel):**

```kotlin
combine(roomFlow, queryFlow, sortOrderFlow, filterFlow, pendingDeleteFlow) { entities, query, sort, filter, pending ->
    val visible = entities.filterNot { it.id in pending }

    val numbered = visible.mapIndexed { index, entity ->
        AnimeUi(id = entity.id, numero = index + 1, nombre = entity.nombre, vecesVisto = entity.vecesVisto)
    }

    val searched = if (query.isBlank()) numbered
        else numbered.filter { it.nombre.contains(query, ignoreCase = true) }

    val filtered = when (filter) {
        ListFilter.ALL -> searched
        ListFilter.REWATCHED -> searched.filter { it.vecesVisto > 1 }
    }

    val ordered = if (sort == SortOrder.DESC) filtered.asReversed() else filtered
    // ...arma UiState con ordered, totalCount = numbered.size, visibleCount = ordered.size
}
```

**Justificación de la derivación en memoria:** revoca la decisión v4 de "sort 100% SQL" porque la numeración canónica exige una única fuente ascendente. Para el tamaño real de esta lista (cientos de items) el costo es irrelevante; la claridad de UX gana sobre la optimización prematura.

**Alternativa documentada (si la lista creciera a miles):** campo explícito `posicion: Int` con mantenimiento en insert/delete/import y `ORDER BY posicion ASC/DESC`. No se implementa ahora.

### 6.3 vecesVisto — independiente del nombre

Hereda v4 (no se infiere del nombre, chip `xN` + tinte `primaryContainer.copy(alpha = 0.18f)` cuando `vecesVisto > 1`).

**Agregado v5:** cuando `vecesVisto > 1`, el chip es **clickable** y abre el diálogo de edición. Es el punto obvio de interacción para corregir el conteo. No se agrega stepper en la card.

### 6.4 Import / Export

Hereda v4: dual `.txt` (humano, pierde `vecesVisto`) / `.json` (backup completo), detección por contenido (`startsWith("{")` tras trim), default "Combinar", dedup contra existentes con `trim().lowercase()` (comparación en Kotlin, sin normalizar acentos).

**Cambios v5:**

**1) Resultado de import rico:**

```kotlin
data class ImportResult(
    val detectedFormat: DetectedFormat,  // TXT | JSON
    val validEntries: Int,
    val imported: Int,
    val duplicatesSkipped: Int,
    val invalidSkipped: Int,
    val ignoredLines: Int
)
```

**2) Dedup interno del archivo:** si el mismo archivo trae duplicados, **el primero gana** y el resto suma a `duplicatesSkipped`. Aplica a `.txt` y `.json`.

**3) Diálogo de import con resumen:** tras elegir el archivo, se parsea en background (`Dispatchers.IO`) y el diálogo de confirmación muestra formato detectado, entradas válidas, duplicados e inválidas. En "Reemplazar" advierte además cuántos animes actuales se eliminarán. El parseo vive en el ViewModel (estado efímero); si el proceso muere con el diálogo abierto, se acepta re-parsear al reabrir (edge case documentado, no bloqueante).

**4) Replace transaccional:**

```kotlin
@Transaction
suspend fun replaceAll(animes: List<AnimeEntity>) {
    deleteAll()
    insertAll(animes)
}
```

**5) `createdAt` secuencial en import** (`base + index`) para que la metadata refleje el orden del archivo, aunque el orden canónico ya lo garantiza `id ASC` por el orden de inserción.

**6) Export `.txt` no destructivo (reemplaza la regla de strip de v4):**

```kotlin
fun formatLine(position: Int, name: String, vecesVisto: Int): String {
    val hasSuffix = Regex("\\s+x\\d+$").containsMatchIn(name)
    return if (vecesVisto > 1 && !hasSuffix) "$position. $name x$vecesVisto"
           else "$position. $name"
}
```

- `vecesVisto == 1` → nunca se toca el nombre.
- `vecesVisto > 1` y el nombre ya termina en `xN` → se deja igual (no se reescribe texto del usuario).
- `vecesVisto > 1` sin sufijo → se agrega `xN`.

**Trade-off documentado:** en casos ambiguos el `.txt` puede no reflejar `vecesVisto`. El backup exacto es `.json`. Prioridad: no mutar nombres literales.

**7) Snackbars post-import actualizados:**

| Resultado | Mensaje |
|---|---|
| 0 válidas | "No se encontraron entradas válidas en el archivo" |
| N importadas, sin omisiones | "Importaste N animes" |
| Con duplicados | "Importaste N animes (M duplicados omitidos)" |
| Con duplicados e inválidas | "Importaste N animes (M duplicados, K inválidos)" |

### 6.5 Pending delete — política explícita (nuevo)

- **Regla 1 — Un pending delete a la vez.** Si se confirma un segundo borrado, el primero se commitea inmediatamente y arranca el undo del segundo.
- **Regla 2 — Import/export resuelven pending deletes.** Al iniciar una importación o exportación, los pending deletes se commitean antes de operar.
- **Regla 3 — Numeración y contador se calculan sobre la lista visible** (excluyendo pending deletes). Durante los 4 segundos puede haber una renumeración transitoria; al hacer undo, vuelve. Es el comportamiento más consistente con "la lista que estoy viendo".

### 6.6 Feedback de cambios (nuevo)

- **Al cambiar de orden:** `animateScrollToItem(0)`.
- **Al agregar un anime:** scroll hasta el item nuevo (arriba en DESC, abajo en ASC) y **highlight** temporal: fondo `primaryContainer.copy(alpha = 0.35f)` que se desvanece a `surface` en ~1000–1200 ms vía `animateColorAsState`, limpiando `highlightedAnimeId` con `LaunchedEffect`.
- El highlight no compite con el tinte de `vecesVisto > 1`: es transitorio y más intenso; el tinte es permanente y sutil.

---

## 7. Microinteracciones

| Acción | Comportamiento |
|---|---|
| Tap en tarjeta | Abre edición (hereda) |
| Tap en chip `xN` | Abre edición (nuevo) |
| Tap en FAB | Abre diálogo de agregar (hereda) |
| Borrar | Diálogo de confirmación + snackbar "Deshacer" 4s (hereda) + política §6.5 |
| Copiar | Snackbar breve "Copiado" (hereda) |
| Buscar en Google | `ACTION_VIEW` con query **codificada** (`appendQueryParameter` o `Uri.encode`) y manejo de `ActivityNotFoundException` (actualizado) |
| Cambiar tema / acento | Instantáneo (hereda) |
| Cambiar orden | Scroll al inicio (nuevo) |
| Agregar item | Scroll al item + highlight (nuevo) |

Snacks: hereda v4 (único `SnackbarHostState`, eventos one-shot vía `SharedFlow`/`Channel`, consumidos con `LaunchedEffect`).

### 7.1 Accesibilidad

Hereda v4, con el número canónico en semantics:

| Componente | contentDescription |
|---|---|
| AnimeCard | "194. Konosuba, visto 2 veces" — número canónico + nombre + vecesVisto si > 1 |
| SortToggle | "Más recientes primero" / "Más antiguos primero" |
| FilterChip x2+ | "Mostrar solo animes vistos más de una vez" |
| EmptyStates | Hereda v4 + los dos nuevos del filtro |

---

## 8. Fuera de alcance (v5)

Se hereda todo el fuera-de-alcance de v4 (posters, categorías, cloud, cuentas, drag-to-reorder, multi-select, estadísticas, tablet, widget, notificaciones, CSV/YAML/etc., haptics, swipe-to-delete), **excepto** "animaciones de transición elaboradas", que se reemplaza por la lista cerrada de §4.4.

**Agregados al fuera de alcance:**

- Orden alfabético (A-Z / Z-A).
- Lottie, parallax, shared element transitions, sonido.
- Persistencia del filtro `x2+` (es estado de sesión).
- Stepper de `vecesVisto` dentro de la card.

Esto cierra preguntas previsibles sin volver a abrir el scope.

---

## 9. Decisiones cerradas (v5)

| # | Decisión | Razón | Estado |
|---|---|---|---|
| 1 | Stack: Kotlin + Compose + M3 + Room + DataStore | Moderno, idiomático | Hereda v4 |
| 2 | Theming de dos capas (modo + acento) | Flexibilidad sin acoplar | Hereda v4 |
| 3 | No `dynamicColor` | El acento es elección del usuario | Hereda v4 |
| 4 | Fuente del sistema | No justifica peso ni setup | Hereda v4 |
| 5 | Sin DI framework | Un solo grafo, factory simple alcanza | Hereda v4 |
| 6 | Sin Navigation Compose | Una pantalla con estados | Hereda v4 |
| 7 | Número de lista = posición canónica de la lista ascendente visible | Permite ver `N…1` en descendente | Reemplaza v4 #7 |
| 8 | Búsqueda/filtro conservan el número canónico | El número es referencia de ubicación | Reemplaza v4 #8 |
| 9 | `vecesVisto` independiente del nombre | Cero ambigüedad de parsing | Hereda v4 |
| 10 | `vecesVisto > 1` muestra chip + tinte | Destacar implica más que un chip suelto | Hereda v4 |
| 11 | Tap en tarjeta = editar | El target más grande es la mejor UX | Hereda v4 |
| 12 | Borrar con diálogo + snackbar undo | Doble protección contra tap accidental | Hereda v4 |
| 13 | Buscar en Google abre navegador directo | Sin pantalla intermedia | Hereda v4 |
| 14 | DataStore para preferencias | Moderno, expone Flow | Hereda v4 |
| 15 | Parser de import "tonto" (no parsea `xN`) | Modelo simple gana | Hereda v4 |
| 16 | Export `.txt` no reescribe nombres legítimos | No mutar texto del usuario; `.json` es el backup exacto | Reemplaza v4 #16 |
| 17 | Import default "Combinar" | Menos destructivo | Hereda v4 |
| 18 | SortToggle "Recientes" (DESC, default) / "Antiguos" (ASC). Sin alfabético | El caso principal es ver lo último agregado | Reemplaza v4 #18 |
| 19 | Dual `.txt` / `.json` | Humano vs backup, dos casos reales | Hereda v4 |
| 20 | Detección de formato por contenido | Las URIs de SAF no garantizan extensión | Hereda v4 |
| 21 | `rememberSaveable` para estados UI transitorios | Sobreviven rotación | Hereda v4 |
| 22 | Strip de newlines en nombres al guardar | Evita entradas rotas | Hereda v4 |
| 23 | `Modifier.semantics` con número canónico | Accesibilidad básica | Hereda v4 (actualizada) |
| 24 | Application para init temprano de Room | Cold start detrás del splash | Hereda v4 |
| 25 | Query canónica única (`id ASC`) + derivación en memoria | La numeración canónica exige una fuente; escala real chica | Reemplaza v4 #25 |
| 26 | Modularización de pantalla en archivos por concern | Responsabilidades separadas | Hereda v4 |
| 27 | minSdk 28 | Cobertura suficiente | Hereda v4 |
| 28 | `isInitialLoading` + skeleton (con cláusula de simplificación) | Distingue cargando de vacío | Hereda v4 (con cláusula) |
| 29 | Orden elegido persistido en DataStore, default DESC | Comodidad sin costo | Nueva |
| 30 | Contador visible (total / resultados) en TopBar | Orientación en listas largas | Nueva |
| 31 | Scroll al inicio al cambiar orden | Comportamiento esperado | Nueva |
| 32 | Scroll al item nuevo + highlight temporal | Feedback de la acción de agregar | Nueva |
| 33 | Animaciones sutiles y funcionales, lista cerrada (§4.4) | Pulido sin extravagancia | Nueva |
| 34 | Filtro `x2+` con empty states propios | Feature simple sobre dato existente | Nueva |
| 35 | Chip `xN` clickable → edición | Affordance obvia para corregir conteo | Nueva |
| 36 | `ImportResult` rico + diálogo con resumen | Transparencia en operación sensible | Nueva |
| 37 | Dedup interno del archivo, primero gana | Evita duplicados desde el origen | Nueva |
| 38 | Replace transaccional | Sin estado inconsistente a mitad de operación | Nueva |
| 39 | Política de pending deletes (§6.5) | Coherencia entre estado visual y DB | Nueva |
| 40 | Export con nombre de archivo fechado | Mejor identificación de backups | Nueva |
| 41 | Query de Google codificada + manejo de sin-navegador | Robustez de intent | Nueva |
| 42 | `material-color-utilities` recomendado para contraste | Tokens correctos sin dynamic color | Nueva (optativa) |

---

## 10. Estructura de carpetas

Hereda v4, con estos cambios marcados:

```
app/src/main/java/com/laumar/anilista/
 │
 ├── AniListaApp.kt
 │
 ├── data/
 │   ├── AnimeEntity.kt            → sin cambios
 │   ├── AnimeDao.kt               → getAllCanonical() (id ASC); se eliminan getAll/getAllDesc/findByNameCaseInsensitive
 │   ├── AppDatabase.kt            → sin cambios
 │   └── AppPreferences.kt         → antes ThemePreferences.kt: modo + acento + sort_order
 │
 ├── repository/
 │   └── AnimeRepository.kt        → más activo: parse, dedup, ImportResult, replaceAll transaccional
 │
 ├── viewmodel/
 │   ├── AnimeViewModel.kt         → deriva numeración canónica, orden, filtro, contador, highlight
 │   └── ThemeViewModel.kt         → sin cambios
 │
 ├── ui/
 │   ├── theme/                    → Color.kt / Theme.kt / Type.kt (sin cambios; eventual migración a color-utilities)
 │   ├── screens/
 │   │   ├── AnimeListScreen.kt
 │   │   ├── AnimeListTopBar.kt    → + subtítulo contador con Crossfade + nuevos labels del SortToggle
 │   │   ├── AnimeListContent.kt   → + fila de FilterChips + highlight + AnimatedVisibility en empty states
 │   │   └── AnimeListFileActions.kt
 │   └── components/
 │       ├── AnimeCard.kt          → lee ui.numero (canónico) + chip xN clickable + highlight
 │       ├── AddEditDialog.kt
 │       ├── DeleteConfirmDialog.kt
 │       ├── ImportConfirmDialog.kt→ + resumen del parse (formato, válidas, duplicados, inválidas)
 │       ├── VecesVistoStepper.kt
 │       ├── ThemeBottomSheet.kt
 │       ├── SortToggle.kt         → labels "Recientes" / "Antiguos"
 │       ├── ListFilterChips.kt    → nuevo: Todos / x2+
 │       └── EmptyState.kt         → + variantes del filtro
 │
 ├── utils/
 │   ├── ImportExportUtils.kt      → formatLine no destructivo + ImportResult
 │   └── JsonImportExport.kt       → sin cambios
 │
 └── MainActivity.kt
```

---

## 11. Roadmap de implementación (orden sugerido v5)

Cada fase cierra sola y se puede probar en device real antes de pasar a la siguiente.

**Fase 1 — Orden y numeración (el núcleo)**
1. Query canónica `id ASC`; eliminar queries ASC/DESC duplicadas.
2. Derivación de numeración canónica en ViewModel.
3. Default DESC + persistencia en DataStore.
4. Labels "Recientes" / "Antiguos".
5. Números canónicos en búsqueda.
Conceptos: `combine`, `mapIndexed`, `asReversed`, DataStore con fallback.

**Fase 2 — Orientación y feedback**
1. Contador en TopBar con `Crossfade`.
2. `animateScrollToItem(0)` al cambiar orden.
3. Scroll al item nuevo + highlight con `animateColorAsState`.
Conceptos: `LazyListState`, `LaunchedEffect` con clave de highlight, `tween`.

**Fase 3 — Robustez de import/export**
1. `ImportResult` + dedup interno.
2. Diálogo con resumen.
3. `replaceAll` transaccional.
4. `formatLine` no destructivo.
5. Nombres de archivo fechados.
Conceptos: `@Transaction`, parse en `Dispatchers.IO`, manejo de excepciones.

**Fase 4 — Pulido visual**
1. `AnimatedVisibility` en empty states y clear button.
2. `Crossfade` en contador (si no se hizo en Fase 2).
3. Validar contraste de acentos en device real; evaluar `material-color-utilities`.
Conceptos: enter/exit specs, easing estándar de M3.

**Fase 5 — Features simples**
1. Filtro `x2+` con empty states propios.
2. Chip `xN` clickable.
Conceptos: `FilterChip`, estados derivados adicionales.

**Fase 6 — Calidad**
1. Tests de parsers `.txt` / `.json`.
2. Tests de numeración canónica, orden DESC, búsqueda con números reales.
3. Tests de merge/dedup y de `formatLine`.
Conceptos: tests unitarios puros + Room in-memory para DAO.

---

## 12. Criterios de aceptación

**Orden y numeración**
- [ ] Al abrir, la vista default es Recientes (o la última elegida, persistida).
- [ ] El primer item visible muestra `N` (el total actual).
- [ ] En Antiguos, el primero muestra `1`.
- [ ] En búsqueda, los resultados muestran su número real.
- [ ] Al borrar, renumera; al hacer undo, renumera de vuelta.

**Orientación y feedback**
- [ ] El contador muestra total y `X de N` con búsqueda/filtro.
- [ ] Cambiar de orden scrollea al inicio.
- [ ] Agregar un item scrollea hasta él y lo resalta ~1s.

**Import/export**
- [ ] El diálogo de import muestra formato y conteos antes de confirmar.
- [ ] Duplicados internos del archivo se detectan.
- [ ] Replace es atómico.
- [ ] Export `.txt` no altera nombres que ya terminan en `xN`.

**UI**
- [ ] Las animaciones son sutiles y ninguna bloquea el uso.
- [ ] Empty states entran/salen con transición suave.
- [ ] La app se sigue sintiendo liviana.

---

## 13. Glosario — agregados v5

- `AnimatedVisibility` — mostrar/ocultar un Composable con transición de entrada/salida.
- `Crossfade` — intercambia contenido con fade entre estados.
- `animateColorAsState` — interpola entre dos colores cuando cambia el target.
- `tween` — spec de duración + easing para animaciones.
- `animateScrollToItem(index)` — scroll animado de `LazyListState` hasta un item.
- `Modifier.animateItem()` — anima reposicionamiento de items dentro de Lazy layouts (según versión de Compose).
- `combine` — combina varios Flows en uno, re-emitiendo cuando cualquiera cambia.
- `@Transaction` (Room) — agrupa varias operaciones en una unidad atómica.
- `material-color-utilities` — librería de generación de paletas armónicas con contraste correcto, usable con semilla explícita.

---

## 14. Notas finales

Se heredan las notas de v4: el brief está vivo (primero se actualiza el brief, después se codea); toda reversión de decisión exige justificar por qué el "por qué" original ya no aplica; el stack se amplía solo con justificación; probar en device real desde la Fase 1.

**Nota v5:** este documento no implica rewrite. Se aplica por fases sobre la app existente. Si una fase queda a mitad, la app sigue funcionando igual que en v4: ninguna fase rompe la anterior.