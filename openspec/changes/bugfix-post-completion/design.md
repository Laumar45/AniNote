# Design: Post-Completion Bug Fixes

## Technical Approach

12 targeted fixes across 4 phases, respecting existing architecture (MVVM + Room + Compose). No new dependencies, no schema changes. Each fix modifies exactly one concern in one file. The design follows the project's established patterns — single ViewModel, single DAO, Compose components in `ui/components/`.

## Architecture Decisions

| Decision | Option A | Option B | Option C | Choice | Rationale |
|----------|----------|----------|----------|--------|-----------|
| **DAO sort direction** | `@RawQuery` with动态参数 | Two queries: `getAllAsc()` + `getAllDesc()` | `CASE WHEN` in single query | **B — Two queries** | Type-safe, Room-verified at compile time, no raw SQL fragility |
| **Import order preservation** | Coroutine with `delay(1)` | `forEachIndexed` with `createdAt = base + index` | UUID-based ordering | **B — Incremental timestamps** | Deterministic, no async race, mirrors file order exactly |
| **Case-insensitive dedup** | DAO `LOWER(nombre)` SQL | Kotlin `trim().lowercase()` before comparison | Both (belt & suspenders) | **B — Kotlin normalization** | Brief §5.4 mandates Kotlin-side dedup; keeps DAO simple |
| **Combine decomposition** | Split into 2 StateFlows | `transform` on single flow | Move filter/sort to Repository | **A — 2 StateFlows** | Clean separation: `dataState` (animes+query+sort) vs `uiState` (dialog+pending). Dialog typing stops triggering sort |
| **Theme state scoping** | Pass lambdas to sheet, collect inside | Keep collection at screen level | Move to ViewModel | **A — Collect inside sheet** | Zero recomposition of list on theme change; minimal diff |
| **rememberSaveable for content** | Custom `Saver` for large strings | Move content to ViewModel state | `rememberSaveable` with default saver | **C — rememberSaveable** | Import content is temporary; ViewModel would hoist unnecessarily. Default saver handles strings up to ~500KB |
| **SortToggle labels** | `"1 → 10"` / `"10 → 1"` | `"A → Z"` / `"Z → A"` | `"Old → New"` / `"New → Old"` | **A — Arrow labels** | Brief §18 specifies exactly this format |

## Data Flow

### Current (5-flow combine)
```
repository.allAnimes ──┐
_query ────────────────┤
_sortOrder ────────────┼──→ combine(all 5) ──→ filter + sort ──→ UiState
_dialog ───────────────┤
_pendingDelete ────────┘
```

### Target (decomposed)
```
repository.allAnimes ──┐
_query ────────────────┼──→ combine(3) ──→ filter + sort ──→ dataState
_sortOrder ────────────┘         │
                                 ├──→ combine(dataState, _dialog, _pending) ──→ UiState
_dialog ───────────────┐         │
_pendingDelete ────────┘─────────┘
```

Data pipeline (animes + query + sort) runs independently. Dialog/pending changes only merge into UiState without re-triggering filter+sort.

## File Changes

| File | Action | Changes |
|------|--------|---------|
| `data/AnimeDao.kt` | Modify | Add `getAllDesc()` query; add `findByNameCaseInsensitive()` |
| `repository/AnimeRepository.kt` | Modify | Expose `allAnimesDesc()`; add `findByNameCaseInsensitive()` |
| `viewmodel/AnimeViewModel.kt` | Modify | Split combine into dataState + uiState; incremental import timestamps; case-insensitive dedup; switch DAO call on sort change |
| `ui/components/SortToggle.kt` | Modify | Labels → `"1 → 10"` / `"10 → 1"`; colors → `MaterialTheme.colorScheme.primary` |
| `ui/components/AnimeCard.kt` | Modify | URL-encode query with `URLEncoder.encode(query, "UTF-8")` |
| `ui/screens/AnimeListScreen.kt` | Modify | `rememberSaveable` for import content; LazyColumn `key = { _, anime -> anime.id }`; move `mode`/`accent` collection inside `showThemeSheet` block |
| `ui/components/ThemeBottomSheet.kt` | Modify | Wrap `modes`/`accents` in `remember`; accept `mode`/`accent` as params (already does) |
| `res/values/strings.xml` | Modify | Add ~40 string resources for all UI text |
| 8 UI component files | Modify | Replace hardcoded strings with `stringResource()` |

## Interfaces / Contracts

```kotlin
// AnimeDao.kt — new queries
@Query("SELECT * FROM animes ORDER BY createdAt DESC")
fun getAllDesc(): Flow<List<AnimeEntity>>

@Query("SELECT * FROM animes WHERE LOWER(TRIM(nombre)) = LOWER(TRIM(:nombre)) LIMIT 1")
suspend fun findByNameCaseInsensitive(nombre: String): AnimeEntity?

// AnimeRepository.kt — new exposed flows
val allAnimesDesc: Flow<List<AnimeEntity>> = dao.getAllDesc()

// AnimeViewModel.kt — decomposed state
data class DataState(
    val animes: List<AnimeEntity> = emptyList(),
    val query: String = "",
    val sortOrder: SortOrder = SortOrder.ASC
)

val dataState: StateFlow<DataState>  // animes + query + sort only
val uiState: StateFlow<UiState>      // merges dataState + dialog + pending
```

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit | Dedup normalization | Verify `trim().lowercase()` comparison in ViewModel |
| Unit | Import timestamp ordering | Verify `createdAt` values are strictly increasing in file order |
| Integration | DAO sort queries | Room in-memory DB test: insert 5 items, verify ASC/DESC ordering |
| E2E | URL encoding | Launch Google search with `"Dragon Ball Z & GT"`, verify encoded URL |
| E2E | Theme recomposition | Change accent while list is visible, verify LazyColumn items not recomposed |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

No migration required. No schema changes. All fixes are code-level behavioral corrections. Rollback is per-file via git.

## Implementation Order (Dependency Graph)

```
Phase 1 — Data & Critical (no inter-dependencies, parallel-safe):
  1. AnimeDao.kt      → getAllDesc(), findByNameCaseInsensitive()
  2. AnimeRepository.kt → expose new DAO methods
  3. SortToggle.kt     → labels + colors
  4. AnimeCard.kt      → URL encoding

Phase 2 — ViewModel (depends on Phase 1 DAO changes):
  5. AnimeViewModel.kt → decompose combine, incremental import, case-insensitive dedup

Phase 3 — Composition (independent of Phase 2):
  6. AnimeListScreen.kt → rememberSaveable, LazyColumn key, scoped collection
  7. ThemeBottomSheet.kt → remember lists

Phase 4 — Localization (independent, last):
  8. strings.xml + all UI files → extract strings
```

## Open Questions

- [ ] Should `findByNameCaseInsensitive` replace the existing `findByName`, or coexist? **Recommendation**: replace — no caller uses the case-sensitive version.
- [ ] For the combine refactor: should `dataState` be exposed publicly, or kept internal? **Recommendation**: internal — only `uiState` is consumed by the screen.

## Key Learnings

1. Room does not support dynamic ORDER BY via query parameters — two explicit queries are the correct pattern.
2. The existing 5-flow combine couples data pipeline with UI-only state, causing unnecessary recomputation on dialog changes.
3. Import order preservation requires explicit `createdAt` assignment since `System.currentTimeMillis()` collapses batch inserts to identical timestamps.
4. `rememberSaveable` handles strings natively without custom Savers for the import content use case.
