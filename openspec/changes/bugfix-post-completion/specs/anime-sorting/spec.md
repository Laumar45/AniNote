# Anime Sorting — New Capability Spec (bugfix-post-completion)

## Current State

- `AnimeDao.getAll()` always executes `ORDER BY createdAt ASC` with no sort parameter.
- `AnimeViewModel` reverses order in memory (`sortedBy` / `sortedByDescending`) inside the 5-flow combine, re-running on every upstream emission.
- `SortToggle` hardcodes the accent green `Color(0xFF4CAF50)` and labels "1 - 10" / "10 - 1".

## Target State

- DAO returns items ordered by `createdAt` with the direction passed as a parameter; the ViewModel stops reversing in memory.
- Toggle labels are "1 → 10" (ASC) and "10 → 1" (DESC); active state uses `MaterialTheme.colorScheme.primary` / `onPrimary`, inactive states use scheme tokens.

## ADDED Requirements

### Requirement: DAO-level directional sort

The system MUST sort the anime list by `createdAt` at the DAO layer, and the sort direction MUST be controllable via a parameter. The ViewModel MUST NOT reverse the list in memory.

#### Scenario: Ascending order

- GIVEN a list containing items created at different times
- WHEN the user selects ASC sort
- THEN the list renders oldest item first

#### Scenario: Descending order

- GIVEN a list containing items created at different times
- WHEN the user selects DESC sort
- THEN the list renders newest item first

#### Scenario: Imported file order preserved

- GIVEN a .txt import of 50 items in a specific order
- WHEN the import completes
- THEN the list preserves the original file order under chronological sort

### Requirement: Theme-token sort toggle colors

The active sort toggle segment MUST use `MaterialTheme.colorScheme.primary` as container/border color and `colorScheme.onPrimary` for its content. The system MUST NOT hardcode accent colors.

#### Scenario: Active state uses theme accent

- GIVEN the sort toggle is rendered
- WHEN ASC is the active selection
- THEN the active segment uses `MaterialTheme.colorScheme.primary` for container and border

### Requirement: Sort toggle labels

The ASC option MUST display "1 → 10" and the DESC option MUST display "10 → 1".

#### Scenario: Labels match design brief (§18)

- GIVEN the sort toggle is rendered
- WHEN inspecting both segment labels
- THEN ASC shows "1 → 10" and DESC shows "10 → 1"
