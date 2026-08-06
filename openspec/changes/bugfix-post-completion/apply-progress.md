# Apply Progress: Post-Completion Bug Fixes

## Status

- **Phase**: apply
- **State**: completed
- **Date**: 2026-08-04
- **Completion**: 15/15 tasks implemented across 4 phases
- **Build/test**: NOT run (user compiles manually per AGENTS.md)
- **Strict TDD mode**: active — manual verification checklist provided below

## Completed Tasks

### Phase 1: Data Layer ✅
- [x] 1.1 `AnimeDao.kt` — added `getAllDesc()` (`ORDER BY createdAt DESC`)
- [x] 1.2 `AnimeDao.kt` — replaced `findByName` with `findByNameCaseInsensitive()` (`LOWER(TRIM(...))` comparison)
- [x] 1.3 `AnimeRepository.kt` — exposed `allAnimesDesc`; renamed `findByName` → `findByNameCaseInsensitive`
- [x] 1.4 `AnimeCard.kt` — `searchInGoogle` now URL-encodes the query via `URLEncoder.encode(query, "UTF-8")`
- [x] 1.5 `SortToggle.kt` — labels `1 → 10` / `10 → 1`; colors switched to `MaterialTheme.colorScheme.{primary,onPrimary,outline,surface}`; removed `ui.graphics.Color` import

### Phase 2: ViewModel ✅
- [x] 2.1 `DataState` data class added before `UiState` (animes + query + sortOrder)
- [x] 2.2 Decomposed 5-flow combine: `dataState` (DAO flow via `flatMapLatest` per sortOrder + `_query` + `_sortOrder`) + `uiState` (dataState + `_dialog` + `_pendingDeleteIds` + `_pendingDeleteAnime`). `@OptIn(FlowPreview::class)` on `dataState` (coroutines 1.7/1.8 via transitive deps). Filter moves to `dataState`; pending-delete filtering stays in `uiState`. Kotlin-side sort removed (DAO ORDER BY now does it).
- [x] 2.3 `importAnimes` replace branch: `forEachIndexed` assigns `createdAt = base + index` (base captured once) so batch imports preserve file order with strictly increasing timestamps
- [x] 2.4 Combine-mode dedup uses `findByNameCaseInsensitive` instead of `findByName`

### Phase 3: UI Components ✅
- [x] 3.1 `AnimeListScreen.kt` — `itemsIndexed(uiState.animes, key = { _, anime -> anime.id })` (also fixed block indentation)
- [x] 3.2 `pendingImportContent` → `rememberSaveable`
- [x] 3.3 `mode`/`accent` collection moved from screen top into the `if (showThemeSheet)` block — list no longer recomposes on theme change
- [x] 3.4 `ThemeBottomSheet.kt` — `modes`/`accents` wrapped in `remember`; mode labels stored as resource IDs and resolved in the label composable

### Phase 4: Localization ✅
- [x] 4.1 `res/values/strings.xml` — 48 string resources (sort labels, dialogs, menu, FAB, search, card, empty state, stepper, theme sheet, snackbar action)
- [x] 4.2 All 8 UI composables + `SortToggle` use `stringResource(R.string.*)`; `R` and `stringResource` imports added per file

## Files Changed

| File | Change summary |
|------|----------------|
| `data/AnimeDao.kt` | +`getAllDesc()` query, +`findByNameCaseInsensitive()`, −`findByName` |
| `repository/AnimeRepository.kt` | +`allAnimesDesc` flow, `findByNameCaseInsensitive()` |
| `viewmodel/AnimeViewModel.kt` | +`DataState`, decomposed `dataState`+`uiState`, incremental import timestamps, case-insensitive dedup, +`FlowPreview`/`flatMapLatest` imports |
| `ui/components/AnimeCard.kt` | URL encoding, localized card strings, semantics desc from resources |
| `ui/components/SortToggle.kt` | arrow labels, theme colors, localized labels |
| `ui/screens/AnimeListScreen.kt` | LazyColumn key, `rememberSaveable`, scoped theme collection, localized strings |
| `ui/components/ThemeBottomSheet.kt` | `remember` lists, localized labels (mode labels as resource IDs) |
| `ui/components/AddEditDialog.kt` | localized labels/buttons |
| `ui/components/DeleteConfirmDialog.kt` | localized title/message/buttons |
| `ui/components/ImportConfirmDialog.kt` | localized title/body/buttons, format label from resources |
| `ui/components/EmptyState.kt` | localized titles/hints/semantics (hoisted out of `semantics {}`) |
| `ui/components/VecesVistoStepper.kt` | localized semantics/content descriptions (hoisted) |
| `res/values/strings.xml` | 48 string resources added |

## Deviations / Notes

1. **SortToggle added to localization set** — task 4.1 explicitly lists "sort labels" in coverage, so `SortToggle` was updated in 4.2 even though the task text names 8 files. 9 files total use `stringResource`.
2. **ViewModel snackbar messages NOT localized** — the 4.2 scope is the 8 UI files; ViewModel emits runtime-composed Spanish snackbar strings ("Importaste X animes"…). Localizing them requires an `AndroidViewModel`/resource-id refactor outside this design's scope.
3. **`Color.Transparent` in SortToggle** replaced with `MaterialTheme.colorScheme.surface` — required to fully remove the `ui.graphics.Color` import per task 1.5.
4. **`flatMapLatest` needs `@OptIn(FlowPreview::class)`** — project has no explicit coroutines version (transitive 1.7/1.8); annotation applied at property level on `dataState`.
5. AnimeCard keeps `"  ${anime.nombre}"` and `"x${anime.vecesVisto}"` as literals (layout padding / numeric chip, not translatable copy).

## Manual Verification Checklist (user, in Android Studio)

### Compile
1. Build the app (Build > Make Project). Watch for: R class regeneration, `FlowPreview` opt-in warnings, unused-import warnings.
2. Verify no references to `findByName` remain (should have failed compile if missed).

### Phase 1 — Data & critical
3. **Sort toggle**: tap `1 → 10` / `10 → 1` — list reorders oldest→newest / newest→oldest with correct arrow labels and theme-colored segmented buttons (no hardcoded green/gray).
4. **Google search**: on an anime with special chars (e.g. "Dragon Ball Z & GT" or "Fate/stay night"), tap the search icon — Google opens with a properly encoded URL (ampersand/slash not breaking the query).
5. **Case-insensitive dedup**: import a TXT/JSON containing "naruto" when "Naruto" already exists (combine mode) — the duplicate is skipped.

### Phase 2 — ViewModel
6. **Dialog typing no longer re-sorts**: with sort DESC active, open add dialog and type in the name field — the list behind does NOT re-sort/flicker while typing.
7. **Batch import order**: import a multi-line TXT with **Replace** — list order matches file order exactly (no timestamp collisions collapsing order).
8. **Delete + undo**: delete an anime, tap "Deshacer" in snackbar — item returns; after 4s without undo the item is removed permanently.

### Phase 3 — Composition
9. **LazyColumn keys**: scroll the list, delete an item, undo — no position-scroll jumps; editing an item doesn't shift other cards.
10. **Process death**: pick a file in the import picker, kill the app from Recents, reopen — the pending import content survives (rememberSaveable) and the dialog can still show.
11. **Theme sheet scoping**: open the theme sheet and change accent color while the list is visible — list scroll position/items do not recompose visually; sheet reflects new accent immediately.

### Phase 4 — Localization
12. Walk the app: top bar "Mi lista", menu items, FAB, search placeholder, add/edit dialog, delete dialog, import dialog, empty states, stepper icons (TalkBack descriptions), theme sheet labels — all show the same Spanish text as before (no `%1$s`/`%d` artifacts).
13. **TalkBack**: enable accessibility — card row reads "1. Naruto, visto 3 veces", stepper reads "Veces visto: 3", buttons announce correctly.
14. `strings.xml` parses cleanly (no `&quot;`/`\n` escaping errors — lint Resources check).

## Risks

- **`@OptIn(FlowPreview::class)`**: if the toolchain resolves a coroutines version where `flatMapLatest` no longer requires it, compiler emits a harmless "unnecessary opt-in" warning — not an error.
- **Room query binding**: `LOWER(TRIM(nombre)) = LOWER(TRIM(:nombre))` — Room binds the parameter inside TRIM/LOWER; compiled by KSP at build time. If the SQL binding syntax errors, it fails fast at compile with a clear message.
- **`dataState` + `uiState` initial values**: both default; UI reads only `uiState`. `getExportTxt`/`getExportJson` still read `uiState.value.animes` (unchanged contract).
- **Theme sheet scoping**: `collectAsStateWithLifecycle()` inside a conditional block is valid Compose, but theme state is now only collected while the sheet is open — momentary blank on first open if the flow hasn't emitted (StateFlow has current value, so no blank).
- **Semantics hoisting**: strings used in `semantics {}` lambdas are computed before the modifier — verified in AnimeCard, EmptyState, VecesVistoStepper.
- **Unused import risk**: `remember` still used in AnimeListScreen (SnackbarHostState); `Context` import still used (LocalContext, contentResolver) — no removal needed.

## Next Step

- **sdd-verify**: run the verification phase against the spec scenarios; user performs the manual checklist above as the TDD verification gate (no gradle execution by agents).
