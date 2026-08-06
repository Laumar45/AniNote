# Theme Selection — New Capability Spec (bugfix-post-completion)

## Current State

- `mode` and `accent` are collected at the top of `AnimeListScreen` (`collectAsStateWithLifecycle`), so every theme change recomposes the entire screen including the LazyColumn, even though only `ThemeBottomSheet` consumes those values.
- `ThemeBottomSheet` constructs the `modes` and `accents` option lists inside its body, recreating them on every recomposition.

## Target State

- `mode`/`accent` are collected only within the `ThemeBottomSheet` scope; `AnimeListScreen` does not subscribe to theme flows.
- `modes`/`accents` are defined once (hoisted top-level or wrapped in `remember`).
- Theme behavior is unchanged: the sheet still updates reactively.

## ADDED Requirements

### Requirement: Scoped theme state collection

The system MUST collect `mode` and `accent` only inside the ThemeBottomSheet scope. `AnimeListScreen` MUST NOT collect theme flows, so theme changes do not recompose the whole screen.

#### Scenario: Theme change does not recompose the list

- GIVEN the theme sheet is closed
- WHEN the accent changes elsewhere (e.g. preferences)
- THEN the underlying list screen does not recompose unnecessarily

### Requirement: Stable theme option lists

The `modes` and `accents` option lists MUST be defined once (top-level, companion object, or `remember`) and MUST NOT be recreated on every recomposition.

#### Scenario: Lists survive recomposition

- GIVEN the theme sheet is open
- WHEN any recomposition occurs inside the sheet
- THEN the option lists are not reallocated

### Requirement: Reactive theme sheet (baseline)

When the sheet is open, the selected mode and accent indicators MUST update immediately when the user changes them, without re-rendering the underlying list.

#### Scenario: Mode change updates sheet reactively

- GIVEN the theme sheet is open
- WHEN the user selects a different mode
- THEN the sheet selection updates reactively and the list behind it is not recomposed
