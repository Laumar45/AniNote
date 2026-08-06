# Anime List — New Capability Spec (bugfix-post-completion)

## Current State

- `LazyColumn` uses `itemsIndexed(uiState.animes)` without a `key`, so Compose diffs by index — deleting item 1 remounts many items and scroll is janky on large lists.
- `pendingImportContent` uses `remember`, so the pending file content is lost on rotation.
- The ViewModel's 5-flow `combine` re-runs filter + sort on every upstream emission, including dialog and pending-delete changes that do not affect the list.

## Target State

- LazyColumn items keyed by `anime.id`.
- `pendingImportContent` uses `rememberSaveable`.
- The combine is decomposed so UI-only state changes (dialog, pending delete) do not re-run filter + sort.

## ADDED Requirements

### Requirement: Stable list item keys

The LazyColumn MUST assign each item the key `anime.id` so Compose can diff and reuse items by identity instead of index.

#### Scenario: Delete recomposes only affected items

- GIVEN a list with 100 items
- WHEN item 1 is deleted
- THEN only the affected items recompose/remount and remaining item state is preserved by id

### Requirement: Saveable import content

The pending import file content MUST survive configuration changes. The system MUST store it with `rememberSaveable` (or equivalent saveable mechanism).

#### Scenario: Rotation preserves import dialog

- GIVEN the import confirm dialog is open with a selected file
- WHEN the screen rotates
- THEN the pending content and dialog remain intact

### Requirement: Efficient recompute on UI-only changes

The system MUST NOT re-run the filter + sort pipeline when only dialog or pending-delete state changes. The data pipeline (animes + query + sort) MUST be independent from UI-only flows.

#### Scenario: Dialog typing does not re-sort list

- GIVEN a list with a large number of items
- WHEN the user types in the add/edit dialog
- THEN the list is not re-filtered or re-sorted
