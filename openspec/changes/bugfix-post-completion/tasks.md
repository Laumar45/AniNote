# Tasks: Post-Completion Bug Fixes

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~125 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | auto-chain |
| Chain strategy | size-exception |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: Data Layer (must be first)

- [x] 1.1 **Add `getAllDesc()` to AnimeDao.kt** — Add `@Query("SELECT * FROM animes ORDER BY createdAt DESC") fun getAllDesc(): Flow<List<AnimeEntity>>`. Lines: +2.
- [x] 1.2 **Replace `findByName` with `findByNameCaseInsensitive()` in AnimeDao.kt** — Replace existing query: `@Query("SELECT * FROM animes WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(:nombre)) LIMIT 1") suspend fun findByNameCaseInsensitive(nombre: String): AnimeEntity?`. Remove old `findByName`. Lines: ~±2.
- [x] 1.3 **Expose new DAO methods in AnimeRepository.kt** — Add `val allAnimesDesc: Flow<List<AnimeEntity>> = dao.getAllDesc()`. Replace `findByName` call with `findByNameCaseInsensitive`. Lines: ~±3.
- [x] 1.4 **Fix URL encoding in AnimeCard.kt** — Add `import java.net.URLEncoder`. Change `searchInGoogle` to: `val encoded = URLEncoder.encode(query, "UTF-8")` then `Uri.parse("https://www.google.com/search?q=$encoded")`. Lines: +3, ±1.
- [x] 1.5 **Fix SortToggle labels and colors in SortToggle.kt** — Change labels from `"1 - 10"` / `"10 - 1"` to `"1 → 10"` / `"10 → 1"`. Replace hardcoded `green`/`Color.White`/`Color.Gray` with `MaterialTheme.colorScheme.primary`, `MaterialTheme.colorScheme.onPrimary`, `MaterialTheme.colorScheme.outline`. Remove `import androidx.compose.ui.graphics.Color`. Lines: ~±8.

## Phase 2: ViewModel (depends on Phase 1)

- [x] 2.1 **Add `DataState` data class in AnimeViewModel.kt** — Add before `UiState`: `data class DataState(val animes: List<AnimeEntity> = emptyList(), val query: String = "", val sortOrder: SortOrder = SortOrder.ASC)`. Lines: +5.
- [x] 2.2 **Decompose 5-flow combine into `dataState` + `uiState`** — Create `val dataState: StateFlow<DataState>` combining `repository.allAnimes`/`repository.allAnimesDesc` + `_query` + `_sortOrder` with `flatMapLatest` to pick ASC/DESC DAO flow. Create `val uiState: StateFlow<UiState>` combining `dataState` + `_dialog` + `_pendingDeleteIds` + `_pendingDeleteAnime` (no filter/sort — just merge). Lines: ~+20, -15.
- [x] 2.3 **Fix incremental import timestamps** — In `importAnimes` replace block that maps entities: use `forEachIndexed` to assign `createdAt = System.currentTimeMillis() + index` on each `AnimeEntity`. Lines: ~±5.
- [x] 2.4 **Fix case-insensitive dedup in combine mode** — Replace `repository.findByName(nombre)` with `repository.findByNameCaseInsensitive(nombre)` in the combine import path. Lines: ±1.

## Phase 3: UI Components (independent of Phase 2)

- [x] 3.1 **Add LazyColumn key in AnimeListScreen.kt** — Change `itemsIndexed(uiState.animes)` to `itemsIndexed(uiState.animes, key = { _, anime -> anime.id })`. Lines: ±1.
- [x] 3.2 **Change `pendingImportContent` to `rememberSaveable`** — Change `var pendingImportContent by remember { mutableStateOf<String?>(null) }` to `var pendingImportContent by rememberSaveable { mutableStateOf<String?>(null) }`. Lines: ±1.
- [x] 3.3 **Move `mode`/`accent` collection inside ThemeBottomSheet scope** — Remove lines 71-72 (`val mode by ...`, `val accent by ...`) from top of `AnimeListScreen`. Inside the `if (showThemeSheet)` block, add local `val mode by themeViewModel.mode.collectAsStateWithLifecycle()` and `val accent by themeViewModel.accent.collectAsStateWithLifecycle()`. Pass these to the sheet. Lines: ~±4.
- [x] 3.4 **Wrap `modes`/`accents` in `remember` in ThemeBottomSheet.kt** — Change `val modes = listOf(...)` to `val modes = remember { listOf(...) }` and same for `accents`. Add `import androidx.compose.runtime.remember`. Lines: +2, ±2.

## Phase 4: Localization (last, largest surface)

- [x] 4.1 **Create `res/values/strings.xml` with all user-facing strings** — Add ~40 string resources covering: sort labels, dialog titles/buttons, menu items, snackbar messages, content descriptions, placeholders, empty state text. Lines: +~45.
- [x] 4.2 **Update all 8 UI Composables to use `stringResource()`** — Replace hardcoded Spanish literals in AnimeCard, AnimeListScreen, AddEditDialog, DeleteConfirmDialog, ImportConfirmDialog, EmptyState, VecesVistoStepper, ThemeBottomSheet with `stringResource(R.string.*)`. Add `import androidx.compose.ui.res.stringResource` to each file. Lines: ~±40.

## Key Learnings

1. Room does not support dynamic ORDER BY parameters — two explicit ASC/DESC queries is the correct pattern.
2. Decomposing a 5-flow combine into dataState + uiState prevents dialog typing from re-triggering filter+sort.
3. System.currentTimeMillis() collapses batch inserts to identical timestamps — explicit incremental assignment is required.
4. rememberSaveable handles strings natively without custom Savers for the import content use case.
5. Scoping theme collection inside the sheet prevents unnecessary recomposition of the entire LazyColumn.
