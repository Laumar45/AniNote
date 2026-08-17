# Exploration: AniNote Post-Completion Bug Fixes

## Current State

The AniNote app is functionally complete — Kotlin + Jetpack Compose + Material 3 + Room + DataStore. All features (list, CRUD, search, sort, theme, import/export) are implemented. After completion, the user identified 9 issues spanning bugs, performance, and architecture deviations from the design brief.

## Issues Found

### Bug 1 — Hardcoded green color in SortToggle
- **File**: `ui/components/SortToggle.kt:18`
- **Code**: `val green = Color(0xFF4CAF50)`
- **Severity**: WARNING
- **Brief violation**: §3.1 — accent defines `primary` and is used in chips active, FAB, etc. The SortToggle hardcodes the green accent value instead of using `MaterialTheme.colorScheme.primary`.
- **Fix**: Replace `val green = Color(0xFF4CAF50)` with `MaterialTheme.colorScheme.primary`. Also fix `activeContentColor = Color.White` → `MaterialTheme.colorScheme.onPrimary`, and inactive colors to use scheme tokens.
- **Estimated lines**: 5

### Bug 2 — Unencoded Google search URL
- **File**: `ui/components/AnimeCard.kt:125`
- **Code**: `Uri.parse("https://www.google.com/search?q=$query")`
- **Severity**: CRITICAL
- **Impact**: Special characters (spaces, `&`, `+`, Japanese chars, emojis) break the URL or produce wrong Google results.
- **Fix**: Use `URLEncoder.encode(query, StandardCharsets.UTF_8.toString())` before building the URL.
- **Estimated lines**: 3

### Bug 3 — `pendingImportContent` uses `remember` not `rememberSaveable`
- **File**: `ui/screens/AnimeListScreen.kt:77`
- **Code**: `var pendingImportContent by remember { mutableStateOf<String?>(null) }`
- **Severity**: WARNING
- **Brief violation**: §4.1 and §8 decision #21 — `showThemeSheet`, `showMenu`, `showImportDialog` and `pendingImportIsJson` use `rememberSaveable`. The import content is the one that doesn't.
- **Fix**: Change to `rememberSaveable`. The `String?` type is `Saveable` by default in Compose.
- **Estimated lines**: 1

### Bug 4 — LazyColumn missing `key` parameter
- **File**: `ui/screens/AnimeListScreen.kt:319`
- **Code**: `itemsIndexed(uiState.animes) { index, anime ->`
- **Severity**: WARNING
- **Impact**: Compose can't efficiently diff the list during recomposition. Causes unnecessary recompositions on scroll, especially with large lists.
- **Fix**: Add `key = { anime -> anime.id }` to `itemsIndexed`.
- **Estimated lines**: 1

### Bug 5 — `mode` and `accent` collected at screen top level
- **File**: `ui/screens/AnimeListScreen.kt:71-72`
- **Code**: `val mode by themeViewModel.mode.collectAsStateWithLifecycle()` / `val accent by themeViewModel.accent.collectAsStateWithLifecycle()`
- **Severity**: WARNING
- **Impact**: Every theme change recomposes the ENTIRE AnimeListScreen, including the LazyColumn. Only ThemeBottomSheet uses these values.
- **Fix**: Move `mode` and `accent` collection inside the `if (showThemeSheet)` block, or pass the ThemeViewModel to ThemeBottomSheet and let it collect internally.
- **Estimated lines**: 5

### Bug 6 — 5-flow combine triggers full filter+sort on ANY emission
- **File**: `viewmodel/AnimeViewModel.kt:55-88`
- **Code**: `combine(repository.allAnimes, _query, _sortOrder, _dialog, pendingFlow) { ... }`
- **Severity**: SUGGESTION
- **Impact**: When only `_dialog` or `_pendingDeleteIds` changes, the entire list is re-filtered and re-sorted. The combine operator has no way to know which upstream changed.
- **Fix**: Use `transform` operator or separate the UI state into data (animes + query + sort) and UI-only (dialog, pendingDelete) states. The data flows combine into a filtered/sorted list, and the UI state combines with that list only for rendering.
- **Estimated lines**: 20

### Bug 7 — `modes` and `accents` lists recreated every recomposition
- **File**: `ui/components/ThemeBottomSheet.kt:60,92`
- **Code**: `val modes = listOf(...)` / `val accents = listOf("green", "orange", "blue", "purple")`
- **Severity**: SUGGESTION
- **Impact**: Minor — creates new list objects on every recomposition. Could be `remember` or top-level `val`.
- **Fix**: Move to companion object or top-level `private val`.
- **Estimated lines**: 2

### Bug 8 — DAO findByName is case-sensitive, brief requires case-insensitive dedup
- **File**: `data/AnimeDao.kt:16`
- **Code**: `@Query("SELECT * FROM animes WHERE nombre = :nombre LIMIT 1")`
- **Severity**: WARNING
- **Brief violation**: §5.4 dedup rule — "dos nombres se consideran el mismo anime si `a.trim().lowercase() == b.trim().lowercase()`"
- **Impact**: Importing "Konosuba" then "konosuba" creates duplicates instead of deduplicating.
- **Fix**: Change query to `WHERE LOWER(nombre) = LOWER(:nombre)` or normalize in repository.
- **Estimated lines**: 1

### Bug 9 — Hardcoded Spanish strings in all UI files
- **Files**: AnimeCard.kt, AnimeListScreen.kt, AddEditDialog.kt, DeleteConfirmDialog.kt, ImportConfirmDialog.kt, EmptyState.kt, VecesVistoStepper.kt, ThemeBottomSheet.kt
- **Severity**: SUGGESTION
- **Impact**: No localization support. All UI text is hardcoded in Spanish.
- **Fix**: Extract all user-visible strings to `res/values/strings.xml` and use `stringResource()` in Composables.
- **Estimated lines**: 60-80

## Additional Issues Found

### Issue 10 — AddEditDialog fully-qualified KeyboardOptions import
- **File**: `ui/components/AddEditDialog.kt:47`
- **Code**: `keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(...)`
- **Severity**: SUGGESTION
- **Fix**: Add proper import and use short form.

## Dependency Order

**Phase 1 — Independent fixes (no dependencies):**
1. Bug 1 (SortToggle hardcoded color)
2. Bug 2 (Google URL encoding)
3. Bug 8 (findByName case-insensitive)
4. Bug 7 (ThemeBottomSheet list recreation)

**Phase 2 — State management fixes:**
5. Bug 3 (pendingImportContent rememberSaveable)
6. Bug 4 (LazyColumn key)
7. Bug 5 (mode/accent collection scope)

**Phase 3 — Performance refactor:**
8. Bug 6 (5-flow combine decomposition)

**Phase 4 — Localization (large, independent):**
9. Bug 9 (strings.xml extraction)

## Total Estimated Lines
- Bug 1: 5
- Bug 2: 3
- Bug 3: 1
- Bug 4: 1
- Bug 5: 5
- Bug 6: 20
- Bug 7: 2
- Bug 8: 1
- Bug 9: 70
- **Total**: ~108 lines changed

## Risks
- Bug 6 refactor could introduce regressions in filter/sort behavior if state decomposition is incorrect
- Bug 9 (localization) is a large surface area change — easy to miss a string or break a composable signature
- Bug 5 fix requires careful handling to avoid breaking the ThemeBottomSheet's reactivity

## Design Brief Compliance Summary
| Brief Section | Status | Notes |
|---|---|---|
| §3.1 Accent usage | VIOLATED | SortToggle hardcodes green |
| §4.1 rememberSaveable | VIOLATED | pendingImportContent uses remember |
| §5.4 Dedup rule | VIOLATED | findByName is case-sensitive |
| §6.2 Google search | DEGRADED | URL not encoded |
| §8 Decision #21 | VIOLATED | rememberSaveable consistency |
| §18 SortToggle | COMPLIANT | Sort is chronological only |
| Performance | DEGRADED | Missing key, inefficient combine |
