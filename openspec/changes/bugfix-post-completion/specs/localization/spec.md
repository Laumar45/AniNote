# Localization — New Capability Spec (bugfix-post-completion)

## Current State

- `res/values/strings.xml` contains only `app_name`.
- All user-facing strings (labels, dialog titles, snackbars, content descriptions, sort labels, placeholders) are hardcoded Spanish literals across 8 UI files: AnimeCard, AnimeListScreen, AddEditDialog, DeleteConfirmDialog, ImportConfirmDialog, EmptyState, VecesVistoStepper, ThemeBottomSheet.

## Target State

- All user-facing strings are extracted to `strings.xml` with semantic keys.
- Composables and icons resolve text via `stringResource()` / `stringResource(R.string.*)`.

## ADDED Requirements

### Requirement: Resource-backed UI strings

The system MUST NOT hardcode user-facing string literals in composables. All visible text, accessibility content descriptions, and snackbar messages MUST be resolved from string resources.

#### Scenario: All rendered text comes from resources

- GIVEN the app is running
- WHEN any screen, dialog, or bottom sheet is rendered
- THEN every visible text and content description comes from a string resource

### Requirement: Complete string catalog

`strings.xml` MUST contain an entry with a proper key for every user-facing string used by the app (sort labels, dialog titles, buttons, placeholders, snackbar messages, menu items, content descriptions).

#### Scenario: Catalog covers all UI strings

- GIVEN `res/values/strings.xml`
- WHEN it is inspected
- THEN it contains all user-facing strings with semantic, non-generic keys
