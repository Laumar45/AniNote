# AniNote vs Mihon — Comparativa de Calidad

> Análisis comparativo entre **AniNote** (app personal) y **Mihon** (repo de referencia),
> enfocado en arquitectura, estética y rendimiento.

---

## 1. Arquitectura

### AniNote — 1 módulo, MVVM manual

```
app/
  └── com.laumar.aninote/
        ├── data/        (Room, DataStore)
        ├── repository/  (wrapper delgado sobre DAO)
        ├── viewmodel/   (ViewModel + factories manuales)
        ├── ui/          (theme, screens, components)
        └── utils/       (import/export)
```

- Todo vive en `:app`. No hay separación de módulos.
- DI manual con `ViewModelProvider.Factory` — funciona, pero genera boilerplate y acoplamiento.
- El dominio (lógica de negocio) está mezclado con el ViewModel. No hay capa de interactors puros.

### Mihon — 14 módulos, Clean Architecture estricta

```
app/                → UI + wiring de DI
domain/             → Interactors, modelos puros, CERO dependencias Android
data/               → SQLDelight, repositorios con @ContributesBinding
presentation-core/  → Theme, componentes compartidos, Scaffold custom
presentation-widget/→ Glance widgets
source-api/         → Contratos de extensiones
source-local/       → Fuente manga local
core/common/        → Utilidades, preferencias, helpers de coroutines
core/metro/         → GraphProvider<T> para DI
core/archive/       → Extracción de archivos (libarchive)
core-metadata/      → Parsing de metadatos (kim)
i18n/               → Strings multiplataforma (moko-resources)
telemetry/          → Firebase analytics/crashlytics
baseline-profile/   → Generación de perfiles ART
```

**La diferencia clave**: el módulo `domain/` no puede acceder a Android framework. Eso fuerza una separación real entre lógica de negocio y capa de presentación. En AniNote, el `AnimeViewModel` vive en el mismo paquete que la UI — no hay presión para separar.

### Diagnóstico

| Aspecto | AniNote | Mihon |
|---------|---------|-------|
| Separación de módulos | 1 módulo | 14 módulos con responsabilidades claras |
| Clean Architecture | Parcial (repository layer existe, pero no hay domain puro) | Estricta (domain sin dependencias Android) |
| DI | Manual (`ViewModelProvider.Factory`) | Compile-time (Metro, zero-reflection) |
| Escalabilidad | Requiere refactor para crecer | Agregar módulo = agregar directorio |

---

## 2. Sistema de Diseño y Estética

### AniNote — Material3 estándar

- **Scaffold**: `Scaffold` de M3 sin personalización.
- **Navegación**: Single screen, no necesita librería.
- **Card**: `Row` con texto e iconos apilados — funcional, pero sin personalidad visual.
- **Colores**: Sistema de semilla por acento (green, orange, blue, purple, red). Bien pensado.
- **Tipografía**: `FontFamily.Default` (sistema). Sin carga de fuentes custom.
- **Transiciones**: Ninguna.
- **Scroll**: `LazyColumn` básico.

### Mihon — Design System propio

- **Scaffold custom** (360 líneas): slot `startBar` para NavigationRail, insets ajustados,
  FAB/snackbar reposicionados. No es el Scaffold de M3 — es *el scaffold de Mihon*.
- **Navegación**: Voyager con `TabNavigator`, `Tab` custom, `materialFadeThroughIn/Out`
  entre tabs (transiciones de 200ms).
- **Adaptividad**: `isTabletUi()` detecta tablet → bottom bar en phone, NavigationRail en tablet.
- **Scroll**: `FastScrollLazyColumn` (476 líneas) con thumb arrastrable via `SubcomposeLayout`,
  `Animatable` y gesture exclusion zones. `ScrollbarLazyColumn` con indicadores visuales.
- **Dynamic Color**: MaterialKolor (`com.materialkolor:material-kolor:5.0.0`) para generación
  dinámica de temas.
- **Iconos**: Solo los que necesita. Sin `material-icons-extended`.

### Comparación visual

| Componente | AniNote | Mihon |
|------------|---------|-------|
| Scaffold | M3 estándar | Custom con NavigationRail slot |
| Navegación entre tabs | N/A | Material Fade Through (200ms) |
| Dispositivo adaptivo | No responsive | Phone → bottom bar, Tablet → rail |
| Scroll | LazyColumn básico | FastScrollLazyColumn con thumb |
| Transiciones de pantalla | Sin animación | Material Motion |
| Selector de tema | BottomSheet funcional | BottomSheet + MaterialKolor dynamic |
| Iconos | ~6MB de librería completa | Solo los usados |

---

## 3. Performance — Por qué AniNote es lenta

### Problema 1: Export en main thread 🔴

**Archivo**: `AnimeListFileActions.kt:41,50`

```kotlin
// La serialización bloquea el UI thread
viewModel.getExportTxt()   // ← JSON/TXT serialization EN EL MAIN THREAD
viewModel.getExportJson()  // ← lo mismo

// DESPUÉS viene el dispatcher para escribir archivo
withContext(Dispatchers.IO) { /* escritura */ }
```

Con 500 animes, `getExportJson()` serializa todo el listado en el thread principal.
Esto produce un **frame drop visible** — la UI se congela momentáneamente.

**Fix**:
```kotlin
val content = withContext(Dispatchers.Default) {
    viewModel.getExportTxt()
}
// luego sí, escribir en IO
```

### Problema 2: Doble emisión en cada keystroke 🔴

**Archivo**: `AnimeViewModel.kt:59-75`

```kotlin
val uiState = combine(dataState, _query, _dialog, ...) { ... }
```

`dataState` ya contiene los resultados filtrados por query (línea 40-57). Pero `_query`
también está en el combine externo. Cada tecla produce **dos emisiones** a `uiState`:

1. Una por `dataState` actualizándose (el flow filtrado).
2. Otra por `_query` cambiando directamente.

Resultado: **doble recomposición por cada tecla** → lag al escribir en el search.

**Fix**: eliminar `_query` del combine externo. `dataState` ya lleva `query` dentro.

### Problema 3: material-icons-extended sin usar 🟡

**Archivo**: `app/build.gradle.kts:54`

```kotlin
implementation(libs.androidx.compose.material.icons.extended)
```

Esto agrega **~6MB** al APK y miles de clases dex. AniNote usa ~8 iconos:
`Add`, `Delete`, `Search`, `ContentCopy`, `Close`, `MoreVert`, `Settings`, `SearchOff`.

Es como traer una enciclopedia para leer una página.

**Fix**: usar `material-icons-core` con iconos inline o vector drawables individuales.

### Problema 4: Sin minify en release 🟡

**Archivo**: `app/build.gradle.kts:25`

```kotlin
release {
    isMinifyEnabled = false  // ← todo el código sin optimizar
}
```

Sin R8/ProGuard:
- Más APK (código y recursos sin podar).
- Más tiempo de inicio (más clases que cargar).
- Sin ofuscación de código.

**Fix**: `isMinifyEnabled = true` + reglas ProGuard para Room, Kotlinx Serialization.

### Problema 5: Sin baseline profiles 🟡

Mihon tiene un **módulo entero** dedicado a `baseline-profile/`. Esto le da **20-30% más
rápido en cold start** porque el ART ya tiene compilados los paths calientes
(librerías, composables frecuentes, rutas de navegación).

AniNote no tiene nada de esto.

**Fix**: crear un módulo `:baseline-profile` con `androidx.benchmark` que recorra
la pantalla principal, el diálogo de agregar, y el cambio de tema.

### Problema 6: Triple suscripción DataStore 🟢

**Archivo**: `ThemeViewModel.kt:33-37`

```kotlin
val mode: StateFlow<String> = ThemePreferences.getMode(context).stateIn(...)
val accent: StateFlow<String> = ThemePreferences.getAccent(context).stateIn(...)
val uiState: StateFlow<ThemeUiState> = combine(mode_flow, accent_flow)...
```

Tres suscripciones paralelas a DataStore cuando `uiState` ya combina ambos valores.
El `ThemeBottomSheet` suscribe `mode` y `accent` individualmente en vez de usar `uiState`.

**Fix**: eliminar `mode` y `accent` como flows separados. Usar solo `uiState` en todo.

### Problema 7: Sin Paging 🟢

```kotlin
// AnimeDao.kt
@Query("SELECT * FROM animes ORDER BY nombre ASC")
fun getAllAnimesAsc(): Flow<List<AnimeEntity>>
```

La tabla completa se carga en memoria. Para una lista personal de animes (<500 items)
esto es aceptable. Si crece, habría que migrar a Paging3.

---

## 4. Lo que AniNote hace BIEN

No todo es negativo. Estás aplicando patróns correctos:

| Patrón | Dónde | Evaluación |
|--------|-------|------------|
| `@Immutable` en entity | `AnimeEntity.kt` | Correcto — evita recomposiciones innecesarias |
| `contentType` en LazyList | `AnimeListContent.kt:162` | Ayuda al recycling de items |
| Loading skeleton con pulse | `AnimeListContent.kt:184-207` | UX profesional |
| Undo-delete con delay | `AnimeViewModel.kt:166-185` | Patrón estándar de Material Design |
| Sistema de colores por semilla | `Color.kt` | Elegante y mantenible |
| Accessibility semantics | `AnimeCard.kt:71`, `EmptyState.kt` | Accesibilidad real |
| `dismissSearchOnPointerDown` | `AnimeListContent.kt:176-182` | Modifier reutilizable, bien diseñado |
| Early Room init en Application | `AniNoteApp.kt:17-19` | Oculta cold start detrás del splash |
| Splash screen integration | `MainActivity.kt:42-44` | Bloquea en carga de tema |
| `debounce(250)` en search | `AnimeViewModel.kt:35-37` | Evita queries excesivas |
| `flowOn(Dispatchers.Default)` | `AnimeViewModel.kt:56` | Data pipeline off main thread |
| `stateIn(WhileSubscribed(5000))` | Múltiples archivos | Flows que sobreviven config changes |
| `distinctUntilChanged()` | `AnimeViewModel.kt:37` | Evita emisiones duplicadas |

---

## 5. Plan de Mejora

### 🔴 Alto impacto — hacer ya (1-2 horas)

1. **Habilitar minify** en release builds
2. **Mover export a background** con `withContext(Dispatchers.Default)`
3. **Eliminar `_query` del combine externo** en `AnimeViewModel`
4. **Reemplazar `material-icons-extended`** con `material-icons-core`

### 🟡 Medio impacto — esta semana

5. **Agregar baseline profile** — copiar patrón de Mihon, generar perfiles
6. **Consolidar ThemeViewModel** — usar solo `uiState`, eliminar suscripciones individuales
7. **Agregar `fastForEach`/`fastMap`** en composables (evita allocación de iterators)

### 🟢 Bajo impacto — cuando escale

8. **Multi-module**: separar `data/`, `domain/`, `presentation-core/`
9. **DI framework**: Hilt o Metro para eliminar factories manuales
10. **Paging**: si la lista supera 500+ items consistentemente

---

## 6. Tabla Resumen

| Categoría | AniNote | Mihon | Gap |
|-----------|---------|-------|-----|
| Módulos | 1 | 14 | Alto |
| DI | Manual | Metro (compile-time) | Alto |
| Scaffold | M3 estándar | Custom (360 líneas) | Medio |
| Navegación | Single screen | Voyager + tabs | Medio |
| Adaptividad | None | Phone/Tablet layout | Medio |
| Scroll | LazyColumn | FastScrollLazyColumn | Medio |
| Transiciones | Sin animación | Material Motion | Medio |
| Iconos | ~6MB extended | Solo los usados | Alto |
| Minify | No | Sí | Alto |
| Baseline Profile | No | Sí (módulo dedicado) | Alto |
| Paging | No | SQLDelight + Paging3 | Bajo (para tu caso) |
| `@Immutable` | En entity | En todos los state models | Bajo |
| `fastForEach` | No | Sí | Bajo |
| Accessibility | Parcial | Completa | Bajo |

---

## 7. Conclusión

AniNote tiene **buena base arquitectónica** para un proyecto de aprendizaje. Los problemas
de rendimiento son corregibles en una tarde. La diferencia estética con Mihon es que ellos
tienen un **design system custom** (Scaffold, NavigationBar, FastScroll) mientras tú usas
los componentes estándar de M3.

La prioridad debe ser performance: los problemas de main-thread export y doble recomposición
son los que más afectan la experiencia de usuario. Los cambios de estética pueden esperar.

---

*Generado el 2026-08-17. Basado en Mihon v0.20.4 y AniNote v1.0.*
