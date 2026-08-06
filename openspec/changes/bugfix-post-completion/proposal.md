# Proposal: Post-Completion Bug Fixes (bugfix-post-completion)

## Intent

AniLista is feature-complete but carries 12 bugs/deviations: a critical URL-encoding bug, broken sort semantics, a data-integrity dedup violation, recomposition/performance issues, and hardcoded UI strings. Fix them without changing architecture or adding features, restoring design-brief compliance (§3.1, §4.1, §5.4, §18).

## Scope

**In** (12 issues; sort fixes 1–4 user-confirmed):

| # | Issue | File |
|---|-------|------|
| 1 | DAO-level sort, no in-memory reversal | AnimeDao.kt |
| 2 | Import preserves order (incremental `createdAt`) | AnimeViewModel.kt |
| 3 | Toggle labels "1 → 10" / "10 → 1" (§18) | SortToggle.kt |
| 4 | Toggle uses `colorScheme.primary` (§3.1) | SortToggle.kt |
| 5 | URL-encode Google search (CRITICAL) | AnimeCard.kt |
| 6 | `rememberSaveable` for import content (§4.1) | AnimeListScreen.kt |
| 7 | LazyColumn stable `key` | AnimeListScreen.kt |
| 8 | Scoped mode/accent collection | AnimeListScreen.kt |
| 9 | Decompose 5-flow combine | AnimeViewModel.kt |
| 10 | Hoist modes/accents lists | ThemeBottomSheet.kt |
| 11 | Case-insensitive dedup `trim().lowercase()` (§5.4) | AnimeDao.kt |
| 12 | Extract strings to `strings.xml` (lowest priority) | 8 UI files |

**Out**: new features, UI redesign, DI, Navigation Compose, architecture changes, schema migrations.

## Non-Goals

- No alphabetical sort (brief §18: chronological only).
- No dependency upgrades or test-infra additions (config.yaml).
- Theme behavior unchanged — token + recomposition fixes only.

## Decisions

- **Decided**: all 4 sort fixes in; phases ordered by criticality; localization last; KeyboardOptions import cleanup folded into Phase 3 (1 line).
- **Open**: dedup site (DAO SQL vs Kotlin normalization — brief §5.4 suggests Kotlin); combine refactor shape (split data/UI StateFlows vs `transform`).

## Capabilities (contract for sdd-spec)

**New** (`openspec/specs/` currently empty):
- `anime-sorting`: sort param, labels, colors (#1, 3, 4)
- `anime-import`: incremental timestamps, dedup (#2, 11)
- `anime-search`: encoded Google URL (#5)
- `anime-list`: stable keys, saveable state, efficient recompute (#6, 7, 9)
- `theme-selection`: scheme tokens, scoped collection, hoisted lists (#4, 8, 10)
- `localization`: resource-backed strings (#12)

**Modified**: None (no existing specs).

## Phases

| Phase | Items | Est lines |
|-------|-------|-----------|
| 1 — Data & critical | #1–5, #11 | ~23 |
| 2 — State & composition | #6–8 | ~7 |
| 3 — Perf & hygiene | #9–10, KeyboardOptions | ~23 |
| 4 — Localization | #12 | ~70 |

## Approach

Fix at source: sort via DAO parameter; incremental timestamps on import; M3 tokens in toggle/theme; UI state splits data vs UI-only flows. Total ~123 changed lines — within the 400-line review budget.

## Affected Areas

`data/AnimeDao.kt`; `viewmodel/AnimeViewModel.kt`; `ui/screens/AnimeListScreen.kt`; `ui/components/{SortToggle,AnimeCard,ThemeBottomSheet,AddEditDialog,DeleteConfirmDialog,ImportConfirmDialog,EmptyState,VecesVistoStepper}.kt`; `res/values/strings.xml`.

## Risks

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Combine refactor regresses filter/sort | Med | Behavior checklists; revert per-file |
| Localization misses strings / breaks signatures | High | Compile + tap every screen (user-verified) |
| Scoped collection breaks theme reactivity | Low | Keep collection inside sheet scope |

## Rollback Plan

Every fix is small and file-local — revert per file via git; sort refactor and combine decomposition revert independently if regression appears. No schema changes → no data migration.

## Dependencies

- design-brief.md v3 (source of truth: §3.1, §4.1, §5.4, §18).

## Success Criteria

- [ ] Google search URL with spaces/`&`/Japanese chars opens correct results
- [ ] Import order preserved; "konosuba" vs "Konosuba" dedupe
- [ ] DESC/ASC correct without ViewModel reversal; labels match §18
- [ ] Smooth scroll on large lists; theme change does not recompose the list
- [ ] No hardcoded UI strings remain; app builds (user-verified)
