# Design Brief — Anime List App

> **Versión**: v4 (actualizado tras auditoría de código)
> **Fecha**: 2026-08-08
> **Stack objetivo**: Kotlin · Jetpack Compose · Material 3 · Room · DataStore
> **Modo de construcción**: código escencialmente escrito a mano. La IA se usa solo como apoyo y para consultas, no para generar implementación.

---

## 1. Resumen & principio rector

App Android nativa para reemplazar una lista de animes que actualmente vive en un archivo de texto plano. El objetivo es sumar las comodidades que un editor de texto no da bien: ordenar, buscar, editar entradas puntuales, marcar cuántas veces viste algo, y respaldar/restaurar la lista desde un `.txt`.

**Principio rector**: cada pantalla debe ser simple de usar — intuitiva, sin pasos innecesarios, con acciones accesibles sin fricción. La comodidad de ordenar, buscar y editar sin reescribir todo a mano es el objetivo, no la restricción deFeatures.

**Promesa de fondo**: todo el dato vive local. No hay backend, no hay cuentas, no hay llamadas API de red para datos de animes. La única "salida" hacia afuera es abrir el navegador para buscar en Google, o leer/escribir un `.txt` para importar/exportar. Esto no es un detalle técnico: es la promesa que sostiene varias decisiones de diseño (sin posters, sin sincronización, sin auth).

**Contexto del proyecto**: se construye como ejercicio de aprendizaje. Las decisiones de stack y arquitectura están elegidas para exponer la mayor cantidad de patrones modernos de Android, no para llegar a producción en el menor tiempo posible.

---

## 2. Stack técnico

| Capa | Tecnología | Por qué |
|---|---|---|
| Lenguaje | Kotlin | Estándar moderno de Android, soporte oficial de Compose |
| UI | Jetpack Compose | Toolkit declarativo actual, mejor DX que Views |
| Sistema de diseño | Material 3 | Componentes, tokens, theming de dos capas ya resueltos |
| Persistencia de datos | Room (SQLite tipado) | Queries reactivas vía `Flow`, migraciones explícitas |
| Preferencias (tema) | DataStore Preferences | Reemplazo moderno de `SharedPreferences`, expone `Flow` |
| Async | Kotlin Coroutines + Flow | Base de todo lo async (DB, DataStore, eventos de UI) |
| Navegación | Single screen + estados | Una sola pantalla con estados (lista / diálogo / sheet). No requiere Navigation Compose para MVP |

**Lo que NO está en el stack** (y por qué):
- **Retrofit / OkHttp**: no hay API
- **Coil / Glide**: no hay imágenes remotas
- **Hilt / Koin**: por ahora la app es chica y el ViewModel se instancia manual con un factory simple. Si crece el grafo de dependencias, se puede migrar a DI framework
- **Navigation Compose**: por ahora una sola pantalla con estados. Si se agregan pantallas, se integra Navigation Compose

Si en iteraciones futuras hace falta alguna de estas, se agrega con justificación, no por defecto.

---

## 3. Identidad visual

### 3.1 Sistema de temas — dos capas independientes

La app separa el "modo de iluminación" del "color de acento". Cambiar uno no afecta al otro.

#### Capa 1 — Modo (claro / oscuro / sistema)

Controla fondo, superficies y contraste de texto.

| Token | Modo claro | Modo oscuro |
|---|---|---|
| `background` | `#FAFAF9` (blanco cálido) | `#121212` |
| `surface` (tarjetas) | `#FFFFFF` | `#1E1E1E` |
| `surfaceVariant` (chips, search bar) | `#F0F0EE` | `#262626` |
| `onBackground` (texto principal) | `#1A1A1A` | `#EDEDED` |
| `onSurfaceVariant` (texto secundario) | `#6B6B6B` | `#9E9E9E` |
| `outline` (bordes sutiles) | `#E0E0DD` | `#333333` |

El modo "sistema" sigue la configuración del dispositivo. Los otros dos son override manual del usuario.

#### Capa 2 — Acento (color de marca)

El acento define `primary`, `primaryContainer` y `onPrimaryContainer` en el `ColorScheme`. Se usa en:
- Botón flotante (FAB) de agregar
- Número de lista en cada tarjeta
- Chips activos (sort, "x2/x3" cuando `vecesVisto > 1`)
- Indicador de la barra de búsqueda al enfocar
- Tinte sutil de fondo en tarjetas con `vecesVisto > 1` (ver §6.3)

| Acento | Primary | Carácter |
|---|---|---|
| Verde | `#4CAF50` | Default sugerido. Evoca "completado" |
| Naranja | `#FF7A45` | Cálido, energético |
| Azul | `#4B7BE5` | Neutro, técnico |
| Morado | `#8B6FE0` | Alternativa con más carácter |

**Reglas de uso del acento**:
- **Nunca** se usa como color de fondo de pantalla completa. Solo como acento puntual.
- M3 ya constriñe esto naturalmente: el `primary` aparece en FAB, chips activos, indicadores, no en `background` ni en `surface`.
- Por cada acento hay que definir el `ColorScheme` completo (primary, onPrimary, primaryContainer, onPrimaryContainer, secondary derivado). En Compose se construye con `lightColorScheme(...)` y `darkColorScheme(...)`.

**Decisión cerrada**: no usar `dynamicColor` (M3 toma color del wallpaper del dispositivo). El acento es elección explícita del usuario, no inferido. Si se activara dynamic, el sistema de acentos perdería sentido.

**Nota sobre paletas**: definir 4 acentos × 2 modos = 8 `ColorScheme` a mano es trabajoso pero educativo — enseña qué tokens hay y por qué. Una alternativa pragmática es la biblioteca `material-color-utilities` (la misma que usa M3 para el color dinámico del wallpaper): con un color semilla genera todos los tokens armónicos con contraste correcto. **Recomendación de aprendizaje**: empezar manual con el primer acento para entender la estructura, y migrar a la library cuando la repetición se vuelva molesta o cuando se quiera agregar un quinto acento. Empezar con la library es más rápido pero menos formativo.

### 3.2 Tipografía

- **Fuente**: la del sistema (Roboto en la mayoría de dispositivos). No se introduce fuente custom — para una app de este tamaño no se justifica el peso en el APK ni la complejidad del setup.
- **Jerarquía**:

| Elemento | Estilo | Peso | Color |
|---|---|---|---|
| Título top bar | `titleLarge` | normal | `onBackground` |
| Nombre del anime | `titleMedium` | semibold | `onSurface` |
| Número de lista | `titleMedium` | semibold | `primary` (acento) |
| Metadata (chip "x2") | `labelSmall` | medium | `onPrimaryContainer` |
| Texto secundario (empty state, snackbars largos) | `bodyMedium` | normal | `onSurfaceVariant` |

El número va **antes** del nombre, alineado en la misma línea, con un espacio fijo. Esa composición es la firma visual de la lista.

### 3.3 Forma y espaciado

- **Esquinas**: 12dp en tarjetas. Suficiente para sentirse "suave" sin caer en el estilo infantil de apps de anime.
- **Elevación**: mínima o nula. La diferenciación entre tarjeta y fondo viene del color de superficie (`surface` vs `background`), no de sombra. Es más consistente entre modo claro y oscuro, y menos costoso de renderizar.
- **Espaciado**:
  - Entre tarjetas: 8dp
  - Padding interno de tarjeta: 16dp
  - Padding de pantalla (margen lateral de la lista): 16dp
  - Altura mínima de tarjeta: 56dp (alineada con guidelines de M3 para listas)

---

## 4. Layout por pantalla

### 4.1 Pantalla principal (lista)

De arriba hacia abajo, dentro de un `Scaffold`:

1. **Top bar** (`TopAppBar` de M3)
   - Título: "Mi lista"
   - Acción derecha 1: **SortToggle** — control de orden con 2 estados:
     - **Ascendente** ("1 → 10") — por `createdAt` ascendente (más antiguos primero)
     - **Descendente** ("10 → 1") — por `createdAt` descendente (más recientes primero)
     - Implementación: `SingleChoiceSegmentedButtonRow` de M3 (2 botones segmentados)
     - Default al abrir la app = Ascendente — es el orden esperado después de import, y para entradas nuevas respeta la secuencia temporal
     - Sin ordenamiento alfabético de ningún tipo — solo cronológico por fecha de creación
     - **Implementación**: sort se ejecuta a nivel Room SQL con dos queries separadas (`getAll()` con `ORDER BY createdAt ASC` y `getAllDesc()` con `ORDER BY createdAt DESC`). El ViewModel usa `flatMapLatest` para cambiar entre flows según el sort order seleccionado. Esto es más eficiente que ordenar en memoria.
   - Acción derecha 2: ícono de tema (abre bottom sheet — §4.3)
   - Acción derecha 3: menú overflow (tres puntos) con "Importar" / "Exportar" — §4.4

2. **Barra de búsqueda** (`SearchBar` de M3 o `OutlinedTextField` con `Icons.Search`)
   - **Persistente**, debajo del top bar, no colapsable detrás de un ícono. Buscar es acción frecuente, no secundaria.
   - `placeholder`: "Buscar anime"
   - Botón de "limpiar" (X) cuando hay texto
   - El indicador de foco usa el color `primary` (acento activo)
   - **Estado de carga**: mientras `isInitialLoading = true`, se oculta la lista y se muestra un skeleton shimmer de 12 placeholder cards. Una vez que el Room Flow emite la primera lista real, el skeleton desaparece y se muestra la lista (o el empty state si está vacía).

**Persistencia de estado UI a rotación**: `showThemeSheet`, `showMenu`, `showImportDialog` y `pendingImportIsJson` usan `rememberSaveable` (no `remember`) para sobrevivir a rotación de pantalla. El query de búsqueda vive en el ViewModel (`StateFlow`), así que ya persiste naturalmente.

3. **Lista** (`LazyColumn`)
   - Cada ítem es una `AnimeCard` (ver §9)
   - Espaciado entre items: 8dp
   - Padding inferior: 88dp para que el FAB no tape la última tarjeta al scroll

4. **FAB**
   - **Lista con items**: `FloatingActionButton` estándar
     - Posición: bottom-end
     - Ícono: `Icons.Default.Add`
     - Color: `primary` (acento activo)
   - **Lista vacía**: `ExtendedFloatingActionButton` con texto **"Agregar anime"**
     - Mayor affordance cuando no hay otra pista visual de qué hace el botón
     - Se "contrae" al `FloatingActionButton` estándar en cuanto aparece el primer item (transición automática según `lista.isEmpty()`)
   - Ambos casos: abre el diálogo de agregar (ver §4.5 para el caso vacío)

### 4.2 Diálogo agregar / editar

`AlertDialog` de M3, modal (no pantalla completa).

**Campos**:
- `OutlinedTextField` — nombre del anime
  - `label`: "Nombre"
  - `singleLine = true`
  - `imeAction = Done` cuando es el único campo, para cerrar teclado
- `OutlinedTextField` numérico — veces visto
  - `label`: "Veces visto"
  - `default = 1`
  - `keyboardType = Number`
  - Validación: entero ≥ 1

**Botones**:
- "Cancelar" (text button) — descarta cambios, cierra diálogo
- "Guardar" (text button, color `primary`) — persiste, cierra diálogo
- En modo edición, el título del diálogo es "Editar"; en modo agregar, es "Nuevo anime"

**Validación**:
- Nombre vacío → botón "Guardar" deshabilitado
- Nombre con saltos de línea (`\r`, `\n`) → se reemplazan por espacios antes de guardar (`replace(Regex("[\\r\\n]+"), " ")`). Un nombre pegado desde un documento multilínea no debería crear entradas rotas
- Veces visto = 0 o negativo → no se permite (el input lo bloquea con `keyboardType = Number` y validación en el `onValueChange`)

### 4.3 Bottom sheet de tema

`ModalBottomSheet` de M3.

**Contenido**:

1. **Selector de modo** — `SegmentedButton` o fila de 3 opciones:
   - Claro
   - Oscuro
   - Sistema
   - La opción activa queda marcada visualmente (M3 lo hace con el `selected` parameter)

2. **Selector de acento** — fila horizontal de círculos de color:
   - 4 círculos de 40dp, uno por acento
   - El seleccionado tiene un borde de 2dp en `onBackground` y un check adentro
   - Tocar uno cambia el acento instantáneamente (sin "aplicar" — es previsualización en vivo)

**Cierre**: tap fuera del sheet, swipe down, o back gesture. El sheet no tiene botón "guardar" — los cambios son inmediatos.

### 4.4 Menú overflow (Import / Export)

`DropdownMenu` anclado al ícono de tres puntos en la top bar.

- **"Importar"** → lanza `ActivityResultContracts.OpenDocument` (Storage Access Framework). El formato (`.txt` o `.json`) se detecta por la extensión del archivo elegido. Se aceptan ambos sin que el usuario tenga que elegir.
- **"Exportar (.txt)"** → lanza `ActivityResultContracts.CreateDocument` con nombre sugerido `anime_list.txt`. Produce el formato "humano" (§5.4 Formato .txt) — pierde `vecesVisto` al re-importar.
- **"Exportar (.json)"** → lanza `ActivityResultContracts.CreateDocument` con nombre sugerido `anime_list.json`. Produce el formato "backup completo" (§5.4 Formato JSON) — preserva `vecesVisto`.

3 items en el menú sigue siendo limpio para M3 `DropdownMenu`. Si en el futuro se suman más formatos, conviene pasar a sub-menú o diálogo de elección.

**Cuándo usar cada formato**:
- **.txt**: versión "legible" del estado actual (compartir, leer, migrar desde el bloc de notas original)
- **.json**: backup completo donde se quiere preservar `vecesVisto` y el orden exacto. Opción correcta para restauración después de cambios importantes

**Nota sobre overlays**: si el bottom sheet de tema (§4.3) está abierto al momento de tocar cualquier opción, se cierra primero (`sheetState.hide()`) antes de lanzar el SAF. Evita diálogos apilados y gesture conflicts.

### 4.5 Estados vacíos

Dos estados diferenciados, mensajes distintos:

**Lista completamente vacía** (sin búsqueda activa):
- Ícono Material grande (`Icons.Default.MenuBook` o similar — afinar en iteración visual)
- Texto principal: **"Empezá agregando tu primer anime"**
- Subtítulo: **"Tocá el botón + para crear uno, o importá tu lista desde un .txt"**
- FAB visible y prominente

**Búsqueda sin resultados** (lista tiene items pero ninguno matchea el query):
- Ícono pequeño (32dp): `Icons.Default.SearchOff` — sutil pero ayuda a diferenciar visualmente este estado del "lista vacía"
- Texto: `Ningún anime matchea "{query}"`
- Botón inline: "Limpiar búsqueda" — vacía el `OutlinedTextField` y devuelve el foco

---

## 5. Modelo de datos y comportamiento

### 5.1 Persistencia local

Dos fuentes de datos, dos tecnologías, una filosofía común (todo reactivo vía `Flow`):

**Room — datos de la lista**:

```kotlin
@Entity(tableName = "animes")
data class AnimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val vecesVisto: Int = 1,
    val createdAt: Long = System.currentTimeMillis()  // útil para orden secundario
)
```

DAO expone queries como `Flow<List<AnimeEntity>>` para que la UI reaccione automáticamente a cambios (insert, update, delete). El ordenamiento se resuelve a nivel SQL, no en memoria, con dos queries separadas:
- `getAll()` → `ORDER BY createdAt ASC` (más antiguos primero)
- `getAllDesc()` → `ORDER BY createdAt DESC` (más recientes primero)

Además, el DAO incluye:
- `findByNameCaseInsensitive(nombre)` → compara con `LOWER(TRIM(nombre))` para la deduplicación en import-merge sin distinguir mayúsculas ni espacios extra
- `deleteAll()` e `insertAll(...)` → operaciones batch para el import con "Reemplazar"

**DataStore — preferencias de tema**:

```kotlin
// ThemePreferences.kt
val MODE_KEY = stringPreferencesKey("mode")          // "light" | "dark" | "system"
val ACCENT_KEY = stringPreferencesKey("accent")      // "green" | "orange" | "blue" | "purple"
```

Ambas fuentes se leen desde el `ViewModel` con `.stateIn(viewModelScope, ...)` para tener un `StateFlow<UiState>` que la UI consume con `collectAsStateWithLifecycle()`.

**Decisión deliberada — `stringPreferencesKey` vs `intPreferencesKey`**: aunque el acento es un enum cerrado con 4 valores, se usa `stringPreferencesKey` por dos razones: (1) el archivo DataStore queda legible si lo abrís con un editor de prefs (`accent = "green"` en vez de `accent = 2`); (2) la diferencia de performance es nula a esta escala. Trade-off documentado, no un descuido.

### 5.2 Número de lista — posición visual

**Decisión cerrada**: el número que se ve en cada tarjeta **es la posición visual** en la lista filtrada/ordenada actual (1, 2, 3...). Se calcula como `index + 1` en el `LazyColumn`.

**Implicaciones**:
- El número **cambia** al borrar, ordenar, o filtrar — refleja dónde está el item *ahora*, no cuándo se creó.
- Si borrás el item "1.", el que era "2." pasa a ser "1."
- Al buscar, los números se recalculan según la posición en la lista filtrada.
- No hay que reescribir la DB al reordenar.

**Comportamiento durante búsqueda**: el número se recalcula. Si Konosuba es "1." y se busca "isekai", isekai nonbiri aparece como "1." (primer resultado), no "2."

**Implementación del sort a nivel SQL**: el orden no se aplica en memoria. El ViewModel usa `flatMapLatest` para cambiar entre los flows de `getAll()` (ASC) y `getAllDesc()` (DESC) según el sort order seleccionado. Room filtra y ordena antes de emitir al Flow, evitando cargar toda la lista en memoria para ordenarla.

### 5.x Estado de carga inicial

**Decisión documentada**: la UI distingue entre "lista cargando" y "lista vacía" con un flag `isInitialLoading` en el `UiState`.

**Problema que resuelve**: Room expone `Flow<List<T>>` que emite vacío mientras carga. Sin un flag explícito, la UI muestra el empty state ("Empezá agregando...") durante el primer frame, parpadeando antes de mostrar datos reales.

**Implementación**:
- `DataState.isInitialLoading = true` como valor inicial
- Se pone en `false` solo después de la primera emisión real del Room Flow
- Mientras `isInitialLoading = true`, se muestra un skeleton shimmer de 12 placeholder cards que ocupa toda el área de la lista
- El skeleton usa `InfiniteTransition` con alpha pulsante para feedback visual de carga

**Diferencia con empty state**: `isInitialLoading = true` → skeleton. `isInitialLoading = false` y lista vacía → empty state ("Empezá agregando..."). Son estados mutuamente excluyentes.

### 5.3 vecesVisto — independiente del nombre

**Decisión cerrada**: `vecesVisto` **nunca se infiere del nombre**. Es un campo independiente que el usuario edita manualmente desde el diálogo agregar/editar.

Implicaciones concretas:
- El parser de import **no** busca el sufijo `xN` en el nombre. Si la línea dice `"2. Konosuba x2"`, el nombre guardado es literalmente `"Konosuba x2"` y `vecesVisto = 1`.
- El usuario, al editar esa entrada, decide: o limpia el nombre (queda `"Konosuba"`, `vecesVisto = 1`, sin chip) o sube `vecesVisto` a 2 (queda `"Konosuba x2"`, `vecesVisto = 2`, con chip y tinte — visualmente redundante, pero es problema del usuario, no del modelo).
- Esto elimina la necesidad de regex, ambiguëdades con "Spy x Family" o "Foxxy", y separa la representación visual de los datos.

**Tratamiento visual cuando `vecesVisto > 1`** (dos capas):

1. **Chip "xN"** a la derecha del nombre:
   - Color de fondo: `primaryContainer`
   - Color de texto: `onPrimaryContainer`
   - Estilo: `AssistChip` de M3 o `SuggestionChip` (afinar en implementación)
   - Solo visible si `vecesVisto > 1` (no se muestra "x1")

2. **Tinte de fondo en la tarjeta**:
   - Color: `primaryContainer.copy(alpha = 0.18f)`
   - Solo cuando `vecesVisto > 1`
   - Permite identificar de un vistazo, mientras se scrollea, qué entradas ya se vieron más de una vez
   - El alpha 0.18 es un balance entre visibilidad (probado en luz baja y pantallas con brillo bajo) y sutileza. **Validar en device real antes de cerrar la implementación**; si en pruebas con luz solar directa el tinte desaparece, subir a 0.22

El número de lista conserva su color estándar (no se intensifica) para no competir con el tinte.

**Por qué tinte y no borde/ícono**:
- Borde exige definir grosor/color por acento y complica el theming.
- Ícono (👁️, ⭐) introduce decisiones de estilo gratuitas.
- El tinte usa un token M3 que ya existe y se adapta solo al acento activo.

### 5.4 Import / Export

Dos formatos soportados, cada uno con su caso de uso:

- **`.txt`**: formato "humano", el original del bloc de notas. Pierde `vecesVisto` (se setea en 1 al importar) porque el archivo no tiene un campo explícito. Es el formato para migración desde el bloc de notas, o para tener una versión legible de la lista.
- **`.json`**: formato de backup completo. Preserva `vecesVisto`. Es la opción correcta para restauración, o para backup antes de cambios importantes.

La elección es del usuario en cada export. En import, el formato se detecta por la extensión del archivo.

#### Formato .txt

Un anime por línea:
```
1. Nombre del anime
2. Otro anime x2
3. Tercero x3
```

- El número al inicio es decorativo. Se ignora al importar y se recalcula al exportar.
- El sufijo `xN` se trata como parte del nombre (no se parsea — ver §5.3).
- Espacios extra al inicio/fin de línea se trimean. Espacios internos se preservan.
- Líneas vacías o que no matcheen el patrón se ignoran, pero se reportan al usuario en el snackbar post-importación (ver más abajo). El parser nunca falla: siempre devuelve un `ParseResult` con la lista de animes válidos y el conteo de ignoradas.

#### Parser .txt (import)

```kotlin
data class ParseResult(
    val animes: List<String>,  // nombres limpios, listos para guardar
    val ignoredCount: Int      // líneas descartadas (vacías, mal formadas, sin nombre tras trim)
)

fun parseFile(content: String): ParseResult {
    val allLines = content.lines()
    val animes = allLines
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { it.replaceFirst(Regex("^\\d+\\.\\s*"), "").trim() }
        .filter { it.isNotEmpty() }
    return ParseResult(
        animes = animes,
        ignoredCount = allLines.size - animes.size
    )
}

// Todas las entradas se importan con vecesVisto = 1
fun importAnimes(content: String): List<Pair<String, Int>> =
    parseFile(content).animes.map { it to 1 }
```

**Importante**: `ignoredCount` se reporta al usuario en el snackbar post-importación (ver más abajo). El parser **nunca falla** — siempre devuelve un `ParseResult` con lista posiblemente vacía y conteo de ignoradas.

#### Formateador .txt (export)

```kotlin
fun formatLine(position: Int, name: String, vecesVisto: Int): String {
    // Strip de cualquier sufijo xN preexistente antes de re-aplicar el actual.
    // Garantiza exportación predecible sin importar el estado histórico del nombre.
    val cleanName = name.replace(Regex("\\s+x\\d+$"), "")
    val base = "$position. $cleanName"
    return if (vecesVisto > 1) "$base x$vecesVisto" else base
}
```

Reglas:
- `vecesVisto == 1` → no se agrega sufijo (la mayoría de los animes, mantener el archivo limpio)
- `vecesVisto > 1` → se agrega ` xN` siempre, después de limpiar cualquier sufijo preexistente
- **No se duplica** el sufijo: si el nombre tenía "x2" y `vecesVisto = 2`, queda "Nombre x2" (no "Nombre x2 x2")
- **No produce combinaciones raras**: si el nombre tenía "x2" y `vecesVisto = 3`, queda "Nombre x3" (no "Nombre x2 x3")

#### Formato JSON

Schema (versión 1):
```json
{
  "version": 1,
  "animes": [
    { "nombre": "One punch man", "vecesVisto": 1 },
    { "nombre": "Konosuba", "vecesVisto": 2 }
  ]
}
```

- `version` es para forward compatibility. Si en el futuro cambia el schema, el importador puede parsear y migrar
- **No incluye `createdAt`**: es un campo interno, se regenera en import según el orden del array
- El array preserva el orden: el orden de los elementos en el JSON es el "orden original" al re-importar (mapea a `createdAt` ASC al insertar)
- **Diferencia clave con .txt**: JSON **preserva `vecesVisto`**. Si hacés export JSON → import JSON, recuperás todo tal cual.

**Library**: `kotlinx.serialization` (oficial de JetBrains, Kotlin-first, sin reflection). Requiere:
- Plugin Gradle: `org.jetbrains.kotlin.plugin.serialization`
- Dependencia: `org.jetbrains.kotlinx:kotlinx-serialization-json`
- Data classes anotadas con `@Serializable`

Por qué `kotlinx.serialization` y no Moshi/Gson:
- Kotlin-first: usa las clases nativas de Kotlin, no requiere Kapt ni reflection
- Compilación en build time (no runtime): más rápido, menos errores
- Es el estándar moderno en el ecosistema Kotlin

#### Parser JSON (import)

**Data classes**:
```kotlin
@Serializable
data class AnimeJson(
    val nombre: String,
    val vecesVisto: Int
)

@Serializable
data class AnimeListJson(
    val version: Int = 1,
    val animes: List<AnimeJson> = emptyList()
)
```

**Parser**:
```kotlin
fun parseJson(content: String): List<Pair<String, Int>> {  // (nombre, vecesVisto)
    val parsed = Json { ignoreUnknownKeys = true }
        .decodeFromString<AnimeListJson>(content)
    require(parsed.version == 1) { "Versión de schema no soportada: ${parsed.version}" }
    return parsed.animes.map { it.nombre to it.vecesVisto }
}
```

`ignoreUnknownKeys = true` permite tolerar campos extra en archivos de versiones futuras del schema, sin fallar el import.

#### Formateador JSON (export)

```kotlin
fun serializeJson(animes: List<AnimeEntity>): String {
    val data = AnimeListJson(
        version = 1,
        animes = animes.map { AnimeJson(it.nombre, it.vecesVisto) }
    )
    return Json { prettyPrint = true }.encodeToString(data)
}
```

`prettyPrint = true` para que el archivo sea legible y diffable.

**Configuración recomendada del `Json`**: combinar ambas configs en una sola instancia reusable:
```kotlin
val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
```

#### Comportamiento al importar

Al elegir el archivo (vía SAF), se detecta el formato **por contenido** (no por extensión):
- Si el contenido empieza con `{` → se parsea como JSON, **preservando `vecesVisto`** del archivo
- Cualquier otro caso → se parsea como texto plano, con todas las entradas en `vecesVisto = 1`

**Por qué no por extensión**: las URIs de SAF no siempre incluyen la extensión en el string (ej: `content://com.android.providers.downloads.documents/document/147`). Detectar por contenido es robusto y no depende del comportamiento del file picker del sistema.

Después, diálogo de confirmación con dos opciones:

- **Reemplazar**: borra todas las entradas actuales y carga del archivo
- **Combinar**: agrega las del archivo

**Default sugerido**: Combinar, porque es menos destructivo. Si el usuario hace un import accidental, no pierde su lista actual.

**Deduplicación en Combinar**: dos nombres se consideran el mismo anime si `a.trim().lowercase() == b.trim().lowercase()`. La query al DAO (`findByName` o equivalente) trae todos los existentes y el filtrado se hace en Kotlin — no hace falta `LIKE` en SQL para este caso. Si hay coincidencia:
- En **import .txt**: se ignora el duplicado (no se "suman" `vecesVisto` porque ambos vienen en 1)
- En **import .json**: se ignora el duplicado (no se suman `vecesVisto` — el existente gana, el del archivo se descarta)

La normalización `trim().lowercase()` se aplica **solo** para la comparación; el nombre guardado en la DB es el original del archivo.

**No normalización de acentos**: 'Pokémon' y 'Pokemon' se consideran distintos en la comparación. Para normalizar se necesitaría `java.text.Normalizer.normalize(s, NFD).replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")` antes de la comparación. Se puede implementar si el usuario lo requiere.

#### Post-importación

Snackbar con conteo (una sola vez por import), con tres variantes según el resultado:

| Resultado del import | Mensaje |
|---|---|
| 0 importadas, 0 ignoradas (archivo vacío) | "No se encontraron animes en el archivo" |
| N importadas, 0 ignoradas | "Importaste N animes. Si tenés 'x2' en algún nombre, editalo desde la tarjeta y mové el conteo a 'Veces visto'." |
| N importadas, M ignoradas | "Importaste N animes (M líneas ignoradas por formato inválido). Si tenés 'x2' en algún nombre, editalo desde la tarjeta y mové el conteo a 'Veces visto'." |

El reporte de líneas ignoradas es **información honesta**: si el usuario importa un archivo de 100 líneas y solo 3 son válidas, lo ve. No es un error, es señal de que el archivo tenía formato distinto al esperado.

#### Round-trip

**En .txt**: el ciclo importar → editar → exportar produce una lista **lógicamente equivalente** (mismos animes, mismos `vecesVisto`), pero **no byte-idéntica**: el formato de cada línea, espacios extra, número de líneas vacías entre entradas, y orden de campos pueden variar.

**En .json**: el round-trip es **mucho más cercano a byte-idéntico** — el schema es estructurado, los campos son explícitos, y `prettyPrint = true` da un formato estable. Sigue sin ser byte-idéntico estricto (el orden de claves en JSON no está garantizado por `@Serializable`, el pretty-printing puede tener variaciones), pero un diff va a mostrar solo cambios reales.

Se documenta porque en iteraciones futuras podría usarse para comparar backups automáticos. Si en algún momento se quiere byte-equivalencia estricta en JSON, fijar `prettyPrint = false` y un orden de campos explícito (con un encoder custom o similar).

#### Edge cases documentados

**Formato .txt**:

| Caso | Comportamiento |
|---|---|
| Archivo vacío / 0 líneas válidas | Snackbar "No se encontraron animes en el archivo" |
| M líneas ignoradas entre N importadas | Snackbar "Importaste N animes (M líneas ignoradas por formato inválido)" |
| Líneas sin número al inicio | Se importan igual (la regex de número es opcional) |
| Líneas con espacios extra al inicio/fin | Se trimean |
| Espacios internos múltiples ("Komi-san  x2") | Se preservan tal cual (es parte del nombre) |
| Nombre con acentos / japonés / emojis | UTF-8, se preserva tal cual |
| Nombre con "x" en el medio ("Spy x Family") | Se preserva tal cual, sin parsing |
| Nombre con sufijo `xN` preexistente y `vecesVisto` que coincide | Se acepta (no se duplica en export) |
| Nombre con sufijo `xN` preexistente y `vecesVisto` que NO coincide | Se reescribe el sufijo en export (regex strip en `formatLine`) |
| Cancelar el SAF sin elegir archivo | No pasa nada, no se muestra error |

**Formato JSON**:

| Caso | Comportamiento |
|---|---|
| Archivo .json malformado (sintaxis inválida) | Snackbar "Archivo JSON inválido" |
| Archivo .json con `version != 1` | Snackbar "Versión de schema no soportada: X" |
| Archivo .json con `animes` faltante o no es array | Snackbar "Archivo JSON inválido" |
| Archivo .json con campos extra desconocidos | Se ignoran (`ignoreUnknownKeys = true`) — forward compat |
| Archivo .json con array `animes` vacío | Se importa como 0 animes, mismo snackbar que .txt vacío |
| Archivo .json con `vecesVisto <= 0` | Skip de esa entrada con warning (no abortar import entero) |
| Cancelar el SAF sin elegir archivo | No pasa nada, no se muestra error |

---

## 6. Microinteracciones

| Acción | Comportamiento |
|---|---|
| Tap en tarjeta | Abre el diálogo de edición |
| Tap en FAB | Abre el diálogo de agregar (vacío) |
| Borrar | Diálogo de confirmación ("¿Borrar X?"). Si confirma → snackbar con "Deshacer" durante 4 segundos. Si el usuario cancela el diálogo, no pasa nada. Si expira el snackbar, el borrado es definitivo. |
| Copiar | Snackbar breve "Copiado" (~1.5s) |
| Buscar en Google | Lanza `Intent.ACTION_VIEW` con `https://www.google.com/search?q={nombre_limpio}`. Sin pantalla de confirmación intermedia. "Limpio" = nombre tal cual, sin transformaciones. |
| Cambiar tema | Transición instantánea, sin animación larga. Es una app utilitaria. |
| Cambiar acento | Cambio en vivo, mientras el bottom sheet está abierto. El usuario previsualiza. |
| Cambiar modo | Idem. |

**Implementación de los snacks**:
- Un único `SnackbarHostState` en el `Scaffold` de la pantalla principal.
- Los mensajes se disparan vía `LaunchedEffect` con eventos one-shot del ViewModel (`SharedFlow<Event>`) — no acumular mensajes en estado UI.

### 6.1 Accesibilidad (semantics)

`Modifier.semantics` en componentes clave para que TalkBack lea contenido descriptivo:

| Componente | contentDescription |
|---|---|
| `AnimeCard` | `"1. Konosuba, visto 2 veces"` — posición + nombre + vecesVisto si > 1 |
| `VecesVistoStepper` | `"Veces visto: 3"` — valor actual |
| `EmptyState` (lista vacía) | `"Lista vacía. Tocá el botón agregar para crear un anime."` |
| `EmptyState` (sin resultados) | `"Sin resultados para {query}"` |

**Regla**: `contentDescription` se arma en runtime con los datos del estado, no es estático. Los botones de acción (copiar, Google, borrar) ya tienen `contentDescription` implícito por el `Icon` + `IconButton` de M3.

---

## 7.  Fuera de alcance (no implementado)

**Diseño / contenido**:
- Imágenes o posters de los animes
- Categorías, etiquetas, o cualquier agrupamiento más allá de `vecesVisto`
- Cuentas de usuario

**Funcionalidad**:
- Multi-select / bulk delete
- Historial de cambios o papelera de reciclaje (más allá del undo del snackbar)
- Detección de duplicados al agregar manualmente (el dedup aplica solo en import-merge)
- Compartir un anime individual (solo se exporta la lista completa)
- Estadísticas ("viste 47 animes, 8 más de una vez")
- **Feedback háptico** en acciones destructivas (borrar, undo). Se justifica especialmente si se introduce swipe-to-delete u otros gestos donde la confirmación táctil aporta valor real.

**Plataforma**:
- Layout dedicado para tablet
- Widget de pantalla de inicio
- Notificaciones
- Soporte de Android antiguo (target min SDK 28 / Android 9.0 — amplia mayoría del mercado y simplifica APIs)

**Formatos de import/export adicionales**:
- CSV (con comillas y escapes)
- YAML, XML, TOML
- Markdown (con headings por categoría, etc.)
- Cualquier otro formato que no sea .txt o .json

El binario .txt (humano, migración) + .json (machine, backup) cubre los dos casos de uso reales. Sumar más formatos es feature creep sin valor agregado claro.

Esto cierra preguntas previsibles y mantiene el scope chico. Pero la lista está abierta a evolución.

---

## 8. Decisiones cerradas (resumen con justificación)

| # | Decisión | Razón |
|---|---|---|
| 1 | Stack: Kotlin + Compose + M3 + Room + DataStore | Moderno, idiomático, base para crecer |
| 2 | Theming de dos capas (modo + acento) | Separar "iluminación" de "marca" da flexibilidad sin acoplar |
| 3 | No `dynamicColor` (wallpaper) | El acento es elección del usuario, no inferida del sistema |
| 4 | Fuente del sistema (Roboto) | Se puede cambiar a una fuente custom si se desea personalización visual |
| 5 | Sin DI framework (Hilt/Koin) | Un solo grafo, ViewModel con factory simple alcanza |
| 6 | Sin Navigation Compose | Una sola pantalla con estados, no hay rutas |
| 7 | Número de lista = posición visual (`index + 1`), no el `id` de Room | El número refleja dónde está el item ahora, no cuándo se creó. Cambia al borrar, ordenar o filtrar |
| 8 | Número se recalcula al filtrar/buscar | El usuario siempre ve secuencia 1, 2, 3... independientemente del query u orden activo |
| 9 | `vecesVisto` independiente del nombre | Cero ambigüedad de parsing, modelo limpio |
| 10 | `vecesVisto > 1` muestra chip + tinte | "Destacar" implica más que un chip suelto |
| 11 | Tap en tarjeta = editar (sin ícono dedicado) | El target más grande es la mejor UX para la acción primaria |
| 12 | Borrar con diálogo de confirmación + snackbar undo | Tap accidental en ícono pequeño es un riesgo real. Diálogo first, undo second = doble protección |
| 13 | Buscar en Google abre navegador directo | Sin pantalla intermedia, el nombre es suficiente contexto |
| 14 | DataStore (no SharedPreferences) para tema | Reemplazo moderno, expone `Flow`, consistente con Room |
| 15 | Import parser es "tonto" (no parsea `xN`) | El usuario reconcilia manualmente, modelo simple gana |
| 16 | Export agrega sufijo solo si `vecesVisto > 1` | Mantiene el archivo limpio para la mayoría de los casos |
| 17 | Import: default "Combinar" (no "Reemplazar") | Menos destructivo ante error |
| 18 | `SortToggle` con 2 estados: Ascendente (`createdAt` ASC, "1 → 10") y Descendente (`createdAt` DESC, "10 → 1"). Default = Ascendente. Ubicado en el TopBar. Sin ordenamiento alfabético | Solo importa el orden cronológico. El usuario quiere ver qué agregó primero/último, no ordenar por nombre. Mantenerlo en el TopBar reduce clicks y lo hace accesible siempre |
| 19 | Soporte dual de import/export: `.txt` (humano) y `.json` (machine backup). `.txt` no preserva `vecesVisto`; `.json` sí | Separa "formato para leer/compartir" de "formato para backup/restauración". Cubre los dos casos de uso reales sin agregar complejidad innecesaria |
| 20 | Import: auto-detección de formato por contenido (no por extensión) | Las URIs de SAF no siempre incluyen `.json` en el string. Detectar por contenido (`startsWith("{")`) es robusto y no depende del file picker |
| 21 | `rememberSaveable` para estados UI transitorios (sheet, menú, dialogs) | Sobreviven rotación de pantalla. El query vive en ViewModel, no necesita `rememberSaveable` |
| 22 | Strip de newlines en nombres al guardar | Evita entradas rotas si el usuario pega texto multilínea desde un documento |
| 23 | `Modifier.semantics` en componentes clave | Accesibilidad básica: TalkBack lee posición + nombre + vecesVisto en tarjetas, valor en stepper, estados vacíos descriptivos |
| 24 | `AniListaApp` (Application) para init temprano de Room | Mueve el cold start de Room detrás del splash del sistema, no detrás del primer render de MainActivity |
| 25 | Sort a nivel Room SQL (`ORDER BY createdAt ASC/DESC`), no en memoria | Más eficiente: Room filtra y ordena antes de emitir al Flow. `flatMapLatest` en ViewModel cambia entre queries según sort order. Evita cargar toda la lista en memoria para ordenar |
| 26 | Modularización de pantalla en 4 archivos (TopBar, Content, FileActions, Screen) | Separa responsabilidades sin introduce Navigation Compose. Cada archivo tiene un concern claro. Facilita testing y mantenimiento |
| 27 | minSdk = 28 (Android 9) | Cobertura suficiente (>95% del mercado activo). Permite usar APIs sin compat shims innecesarios |
| 28 | Loading skeleton con shimmer para estado de carga inicial | Distingue "cargando" de "vacío" explícitamente. Evita el parpadeo del empty state en el primer frame |

---

## 9. Estructura de carpetas

```
app/src/main/java/com/laumar/anilista/
│
├── AniListaApp.kt                → Application: init temprano de Room DB en onCreate()
│
├── data/
│   ├── AnimeEntity.kt            → @Entity con id, nombre, vecesVisto, createdAt
│   ├── AnimeDao.kt               → queries: getAll() (ASC/DESC), insert, update, delete, findByNameCaseInsensitive, deleteAll, insertAll
│   ├── AppDatabase.kt            → @Database, singleton. Sin fallbackToDestructiveMigration en release. Versión 1, sin schema migrations
│   └── ThemePreferences.kt       → DataStore: modo + acento, expuesto como Flow
│
├── repository/
│   └── AnimeRepository.kt        → intermediario entre DAO y ViewModel
│
├── viewmodel/
│   ├── AnimeViewModel.kt         → estado de la lista, query, orden (flatMapLatest con sort SQL), eventos one-shot via Channel
│   └── ThemeViewModel.kt         → estado del tema, lee/escribe DataStore
│
├── ui/
│   ├── theme/
│   │   ├── Color.kt              → tokens por acento (4 paletas x 2 modos = 8 ColorSchemes)
│   │   ├── Theme.kt              → composición: toma modo + acento, devuelve MaterialTheme
│   │   └── Type.kt               → Typography de M3 (titleLarge, labelSmall, etc.)
│   │
│   ├── screens/
│   │   ├── AnimeListScreen.kt    → orquestador principal (Scaffold, eventos, overlays)
│   │   ├── AnimeListTopBar.kt    → TopBar extraído: SortToggle + ícono tema + menú overflow
│   │   ├── AnimeListContent.kt   → Search field + LazyColumn + empty states + loading skeleton
│   │   └── AnimeListFileActions.kt → Launchers SAF para import/export (extraído del screen monolítico)
│   │
│   └── components/
│       ├── AnimeCard.kt          → tarjeta individual (Row con background, no Card M3) + semantics
│       ├── AddEditDialog.kt      → diálogo agregar/editar con VecesVistoStepper
│       ├── DeleteConfirmDialog.kt→ diálogo de confirmación de borrado
│       ├── ImportConfirmDialog.kt→ diálogo de confirmación de import (Reemplazar / Combinar)
│       ├── VecesVistoStepper.kt  → stepper +/- para veces visto (con semantics)
│       ├── ThemeBottomSheet.kt   → selector de modo (SegmentedButton) + acento (círculos de color)
│       ├── SortToggle.kt         → control de orden ASC/DESC por `createdAt` (TopBar)
│       └── EmptyState.kt         → estado vacío (lista vacía o búsqueda sin resultados, con semantics)
│
├── utils/
│   ├── ImportExportUtils.kt      → parseTxtFile(), formatTxtLine(), formatTxtExport()
│   └── JsonImportExport.kt       → parseJson(), serializeJson(), AnimeJson, AnimeListJson (@Serializable)
│
└── MainActivity.kt               → punto de entrada, wiring de ViewModels con factories, hostea el AnimeListScreen
```

---

## 10. Roadmap de implementación sugerido (orden de aprendizaje)

Este orden no es arbitrario. Cada paso construye sobre el anterior y expone conceptos nuevos sin saltos bruscos. **Seguí este orden** aunque ya sepas hacer todo — la idea es que cada capa se entienda bien antes de pasar a la siguiente.

### Paso 1 — Theming (sin datos todavía)
**Conceptos a aprender**:
- `MaterialTheme` en Compose
- `ColorScheme` (light + dark) de M3
- `Typography` de M3
- `Surface` y tokens semánticos (`colorScheme.background`, `colorScheme.surface`, etc.)
- `isSystemInDarkTheme()` para modo sistema
- `CompositionLocal` (lo que usa M3 por debajo para que `MaterialTheme.colorScheme.primary` funcione en cualquier Composable hijo)

**Deliverable**: app que abre con un fondo y un texto. Cambiar manualmente `darkColorScheme` a `lightColorScheme` cambia la apariencia. Nada interactivo todavía.

### Paso 2 — Data layer (Room + DataStore)
**Conceptos a aprender**:
- `@Entity`, `@Dao`, `@Database`, `@Query`
- `Flow<List<T>>` como retorno de queries reactivas
- `suspend` functions vs `Flow`
- Coroutines: `viewModelScope`, `Dispatchers.IO`
- `Room.databaseBuilder` y singleton pattern
- DataStore: `preferencesDataStore`, `stringPreferencesKey`, `edit { }`, `data.map { }`

**Deliverable**: insertar y leer animes desde un `ViewModel` con un log en `Logcat` que muestre la lista. Sin UI todavía.

### Paso 3 — ViewModel y estado
**Conceptos a aprender**:
- `ViewModel` + `viewModelFactory` (sin Hilt)
- `StateFlow` y `MutableStateFlow`
- `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue)`
- `collectAsStateWithLifecycle()` en Composables
- `UiState` como `data class` que agrupa todo el estado de una pantalla
- Eventos one-shot: `SharedFlow<UiEvent>` con `Channel` por debajo. Se consumen con `LaunchedEffect` + `flow.collect { }` — **NO** `collectAsStateWithLifecycle()`, que no maneja eventos únicos correctamente (re-emitiría en cada recomposición). `collectAsStateWithLifecycle()` es solo para estado (`StateFlow<T>`) que debe sobrevivir a recomposiciones. La distinción importa: estado se "lee", eventos se "observan una vez".

**Deliverable**: ViewModel expone un `StateFlow<UiState>` con la lista de animes. La UI muestra los nombres en una columna.

### Paso 4 — UI básica (lista, tarjeta, FAB)
**Conceptos a aprender**:
- `Scaffold` y su slot system (topBar, floatingActionButton, snackbarHost)
- `LazyColumn` y `items()`
- `FloatingActionButton` y `ExtendedFloatingActionButton` (este último se usa en empty state — ver §4.1)
- `TopAppBar` con actions
- Estado local: `remember { mutableStateOf(...) }` vs `rememberSaveable`
- Render condicional: `if (lista.isEmpty()) ExtendedFAB() else FAB()` (la transición automática entre los dos FABs según el estado de la lista)

**Deliverable**: lista scrolleable con tarjetas. FAB cambia entre `ExtendedFloatingActionButton` (lista vacía, con texto "Agregar anime") y `FloatingActionButton` estándar (lista con items) automáticamente. Sin interactividad más allá del scroll.

### Paso 5 — Diálogos y formularios
**Conceptos a aprender**:
- `AlertDialog` de M3
- `OutlinedTextField` con `value`, `onValueChange`, `label`, `singleLine`
- `KeyboardOptions` (`keyboardType = KeyboardType.Number`)
- `KeyboardActions` (`imeAction = Done` cierra teclado)
- Estado del diálogo controlado desde el ViewModel

**Deliverable**: FAB abre diálogo. Llenar nombre + veces visto y guardar persiste. Recargar la app muestra el anime.

### Paso 6.A — Acciones simples por tarjeta (copiar, Google)
**Conceptos a aprender**:
- `IconButton` y la fila de íconos en `Row` con `Arrangement.End`
- `Intent` en Android: `Intent.ACTION_VIEW` + `Uri`
- `LocalContext.current` para acceder al `Context` y lanzar intents
- `ClipboardManager` del sistema (para "Copiar")
- Snackbar con `SnackbarHostState` + `LaunchedEffect` para mostrar

**Deliverable**: copiar y Google funcionan en cada tarjeta. Los otros dos íconos (editar y borrar) se cubren en el siguiente sub-paso.

### Paso 6.B — Editar y borrar con undo (el sub-paso interesante)

**Conceptos a aprender**:
- **Editar**: invocar el mismo diálogo del Paso 5, pasando la entity existente. La entity viaja como parámetro al Composable del diálogo, o se mantiene en el estado del ViewModel (mejor).
- **Borrar con confirmación + undo** (la parte desafiante):
  1. Tap en ícono de borrar → mostrar diálogo de confirmación (`AlertDialog` de M3). Esto **no** toca Room todavía.
  2. Si el usuario confirma → marcar el item como "pending delete" en el estado del ViewModel (se oculta de la lista).
  3. Lanzar una coroutine con `delay(4000)` usando un `Job` cancelable, guardado como referencia en el ViewModel.
  4. Si el snackbar de "Deshacer" se toca → cancelar el `Job` con `job.cancel()`, desmarcar el item.
  5. Si expira el delay → ejecutar `dao.delete(id)`. El `Flow` se actualiza y el item desaparece de la lista naturalmente.
  6. **Interacción con búsqueda y orden**: el pending delete vive como un `Set<Long>` de IDs en el ViewModel, **no** en la lista derivada. Si el usuario cambia el filtro o el orden mientras hay un pending delete, el item oculto sigue oculto (no se re-renderiza en la lista filtrada/ordenada actual). Si toca "Deshacer" después de cambiar el filtro, el item vuelve a aparecer en la posición que le corresponda en la lista filtrada actual. Esto es importante: el pending delete es independiente de la lista visual, sobrevive a cambios de query/orden.
- **Eventos one-shot**: `SharedFlow<UiEvent>` con `extraBufferCapacity = 1` y `BufferOverflow.DROP_OLDEST` para que un mensaje rápido no se acumule. Alternativa: `Channel(Channel.BUFFERED)` consumido en un `LaunchedEffect`.
- **`Job` cancellation**: el patrón fundamental de "acción con timeout + cancelable" en coroutines. Se aprende una vez y se reusa en infinidad de lugares.
- **Separación de estado persistente (Room) y estado visual (pending delete)**: la DB es la fuente de verdad para datos confirmados; el ViewModel tiene estado efímero que afecta solo lo que se ve en el momento.

**Por qué este patrón y no `dao.delete()` directo**: borrar de Room es inmediato y la UI se actualiza vía Flow, pero el usuario necesita 4 segundos para arrepentirse. Si borrás de Room y después intentás restaurar con `dao.insert()`, la lista ya se reordenó, los números visibles cambiaron, y la "restauración" no vuelve al mismo lugar. La solución correcta es: borrar visualmente primero, esperar el undo, y solo entonces commit a Room. Es más código pero el comportamiento es predecible.

**Snackbar action**: `SnackbarHostState.showSnackbar(message, actionLabel = "Deshacer", duration = SnackbarDuration.Short)` devuelve el `SnackbarResult` (`Dismissed` o `ActionPerformed`). El ViewModel consume este resultado en una coroutine.

**Sobre la complejidad de este patrón**: el `Job` cancelable + estado efímero + separación persistente/visual es un pico de dificultad para el Paso 6 del roadmap. **Si el patrón se siente overwhelming**, es totalmente válido implementar el delete directo de Room (sin undo) en este paso, y volver al patrón completo en el Paso 11 (pulir). El patrón es transferible a infinidad de otros lugares (timers, auto-save, debounce, búsquedas con delay), así que vale la pena aprenderlo en algún momento — solo no tiene que ser acá si te trabás. El brief asume que lo querés hacer acá, pero el orden se puede invertir.

**Deliverable**: tap en tarjeta abre el diálogo de edición. Tap en borrar muestra diálogo de confirmación. Si confirma, el item se oculta y aparece snackbar con "Deshacer" 4 segundos. Si toca "Deshacer", el item vuelve a su posición original. Si no toca nada, desaparece definitivamente (y al recargar la app, tampoco está).

### Paso 7 — Búsqueda y orden
**Conceptos a aprender**:
- `SearchBar` o `OutlinedTextField` con leading icon
- `filter { }` y `sortedBy { }` sobre `Flow` o listas en memoria
- `derivedStateOf` para mantener la lista derivada sincronizada con query + sort
- `SingleChoiceSegmentedButtonRow` de M3 para el `SortToggle` con 2 estados (Ascendente / Descendente)
- Integración del `SortToggle` en el `TopAppBar` actions (slot system)

**Deliverable**: barra de búsqueda filtra en vivo. `SortToggle` en el TopBar cicla entre Ascendente y Descendente. La lista se recalcula automáticamente cuando cambia query u orden. El número de lista se actualiza solo (ver §5.2).

### Paso 8 — Bottom sheet de tema + DataStore aplicado
**Conceptos a aprender**:
- `ModalBottomSheet` de M3
- `rememberModalBottomSheetState`
- `SegmentedButton` de M3 (o `FilterChip` si se prefiere)
- Integrar `ThemeViewModel` con la UI: el `MainActivity` envuelve todo en `MaterialTheme` cuyo `colorScheme` viene del estado del ViewModel
- Reactividad de DataStore: cambiar el acento en el sheet cambia toda la UI al instante

**Deliverable**: sheet de tema funciona, los cambios son persistentes (matar la app y reabrir mantiene el tema).

### Paso 9 — Empty states
**Conceptos a aprender**:
- Renderizado condicional en Compose: `if (lista.isEmpty()) EmptyState() else LazyColumn(...)`
- Reutilización de componentes: el mismo `EmptyState` con distinto copy según contexto
- `Icons.Default.*` y búsqueda en la librería de Material

**Deliverable**: lista vacía muestra el empty state. Búsqueda sin resultados muestra el otro empty state.

### Paso 10 — Import / Export
**Conceptos a aprender**:
- `ActivityResultContracts.OpenDocument` y `CreateDocument`
- `rememberLauncherForActivityResult` para integrar SAF con Compose
- Leer archivos: `ContentResolver.openInputStream(uri)` + `bufferedReader().readText()`
- Escribir archivos: `ContentResolver.openOutputStream(uri)` + `bufferedWriter().write(text)`
- `Uri` y `ContentResolver` — conceptos base de Android
- Detección de formato por extensión de archivo al importar (`.txt` vs `.json`)
- **kotlinx.serialization** (solo para el formato JSON):
  - Plugin Gradle: `org.jetbrains.kotlin.plugin.serialization` (en `build.gradle.kts` a nivel app)
  - Dependencia: `org.jetbrains.kotlinx:kotlinx-serialization-json`
  - `@Serializable` en data classes
  - `Json.encodeToString(obj)` para serializar
  - `Json.decodeFromString<T>(string)` para deserializar
  - `Json { }` configuration: `prettyPrint = true`, `ignoreUnknownKeys = true`
- Manejo de `IOException`, `JsonDecodingException`, y `IllegalArgumentException` (la `require` del parser JSON) en coroutines

**Deliverable**: importar un .txt o .json carga la lista preservando el formato. Exportar a .txt produce el formato humano (sin `vecesVisto`); exportar a .json produce el backup completo con `vecesVisto` y round-trip mucho más cercano a byte-idéntico.

### Paso 11 — Pulir
**Conceptos a aprender**:
- Edge cases reales (probar import con archivo vacío, con líneas mal formadas, etc.)
- Validación de inputs (¿qué pasa si el usuario pega un nombre con saltos de línea?)
- `Modifier.semantics` para accesibilidad básica
- Probar en modo claro, oscuro, y con los 4 acentos
- Probar rotación de pantalla: `rememberSaveable` para el query de búsqueda y el estado del sheet

**Deliverable**: la app se siente sólida. No hay crashes en casos borde obvios.

---

## 11. Glosario de conceptos a investigar

Lista no exhaustiva. Conforme avances, vas a encontrar términos que requieren búsqueda. Estos son los principales para que sepas por dónde arrancar:

**Compose básico**:
- `@Composable` — funciones que describen UI
- Recomposición — cuándo se re-ejecuta un Composable
- `State` — fuente de verdad que dispara recomposición
- `Modifier` — cómo se comporta/posiciona un Composable
- `Slot-based API` — el patrón de M3 (Scaffold, TopAppBar, etc.)

**Compose state**:
- `remember` vs `rememberSaveable` — sobrevive o no a rotación
- `mutableStateOf` — el `State<T>` más básico
- `derivedStateOf` — estado calculado, evita recomposiciones innecesarias
- `collectAsStateWithLifecycle()` — colectar Flows respetando el lifecycle

**Material 3**:
- `ColorScheme` — los tokens semánticos de color
- `Typography` — titleLarge, bodyMedium, labelSmall, etc.
- `Shapes` — small, medium, large corner radius
- `Surface` y `tonalElevation` vs `shadowElevation`
- Componentes: `Card`, `OutlinedCard`, `AssistChip`, `FilterChip`, `FloatingActionButton`, `ModalBottomSheet`, `Snackbar`

**Coroutines y Flow**:
- `suspend` function — función pausable
- `Flow` — stream de valores
- `StateFlow` — Flow con estado actual
- `SharedFlow` — Flow para eventos (sin estado actual)
- `viewModelScope` — scope atado al ViewModel
- `Dispatchers.IO` vs `Main` — para operaciones bloqueantes
- `collectLatest` vs `collect` — comportamiento con valores rápidos

**Room**:
- `@Entity`, `@PrimaryKey`, `@ColumnInfo`
- `@Dao`, `@Query`, `@Insert`, `@Update`, `@Delete`
- `Flow` en queries — qué lo hace reactivo
- `Room.databaseBuilder` + `addMigrations`

**DataStore**:
- `Context.dataStore` delegate
- `stringPreferencesKey`, `intPreferencesKey`, etc.
- `data.map { }` para transformar
- `edit { }` para escribir

**kotlinx.serialization** (librería para JSON):
- Plugin Gradle: `org.jetbrains.kotlin.plugin.serialization` (en el bloque `plugins { }` del `build.gradle.kts` del módulo `app`)
- Dependencia: `org.jetbrains.kotlinx:kotlinx-serialization-json`
- `@Serializable` en data classes — habilita (de)serialización
- `Json.encodeToString(obj)` — serializa un objeto a String
- `Json.decodeFromString<T>(string)` — deserializa un String a objeto
- `Json { prettyPrint = true; ignoreUnknownKeys = true }` — configuración reusable
- `JsonDecodingException` — la excepción que se lanza si el JSON está malformado

**Android system**:
- `Intent` y sus actions (`ACTION_VIEW`, etc.)
- `Uri` — qué es y cómo se construye
- `Context` — puerta de entrada al sistema
- `ContentResolver` — leer/escribir vía SAF
- `ClipboardManager` — copy/paste del sistema
- `LocalContext.current` — acceder al Context desde Compose
- `Lifecycle` — `onCreate`, `onStart`, `onResume`, etc.

---

## 12. Notas finales

- El brief está vivo: si durante la implementación aparece una decisión que el brief no cubre, **primero** se actualiza el brief, **después** se codea. Tener el doc desactualizado es peor que no tenerlo.
- Cada decisión cerrada tiene un "por qué". Si en algún momento querés revertir una, no alcanza con "no me gusta" — hay que justificar por qué el "por qué" original ya no aplica.
- El stack está elegido para aprender. Si en algún momento sumás algo (Hilt, Coil, Navigation), justificalo en el brief, no lo metas "porque sí".
- Probar en device real desde el Paso 1. El emulador sirve para iterar rápido, pero muchas cosas de Compose (especialmente animaciones y gestos) se ven distinto en hardware real.
