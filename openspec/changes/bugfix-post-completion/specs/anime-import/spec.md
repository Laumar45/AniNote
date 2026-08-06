# Anime Import — New Capability Spec (bugfix-post-completion)

## Current State

- The replace path builds all entities in a tight loop and inserts them with `insertAll`; each entity captures `System.currentTimeMillis()` at construction, so a batch shares identical (or near-identical) timestamps and file order is lost after sorting.
- `AnimeDao.findByName` uses exact equality (`WHERE nombre = :nombre LIMIT 1`), so dedup is case-sensitive and ignores whitespace.

## Target State

- Import assigns strictly incremental `createdAt` values that mirror file order, so chronological sort reflects the original file.
- Combine-mode dedup compares normalized names: `a.trim().lowercase() == b.trim().lowercase()`.

## ADDED Requirements

### Requirement: Import preserves file order

When importing animes from a file (replace or combine), the system MUST assign incremental `createdAt` values in file order so items appear in the list in the same relative order as the source file.

#### Scenario: TXT order preserved on combine

- GIVEN a .txt file with 10 animes in a specific order
- WHEN the user imports it with Combine
- THEN the list shows the 10 animes in the original file order

#### Scenario: JSON values and order preserved on replace

- GIVEN a .json file with animes including `vecesVisto` values
- WHEN the user imports it with Replace
- THEN each anime keeps its `vecesVisto` and the list order matches the file order

### Requirement: Case-insensitive trimmed dedup

In combine mode, the system MUST treat two names as the same anime when `a.trim().lowercase() == b.trim().lowercase()`. The system MUST skip inserting the duplicate and SHOULD report the count of skipped duplicates in the result snackbar.

#### Scenario: Case-variant duplicate skipped

- GIVEN the list already contains "Naruto"
- WHEN the user imports a file containing "naruto" with Combine
- THEN "naruto" is treated as a duplicate, not re-inserted, and the snackbar reports the skipped duplicate count
