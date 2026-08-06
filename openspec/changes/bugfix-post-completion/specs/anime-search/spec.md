# Anime Search — New Capability Spec (bugfix-post-completion)

## Current State

- In-app filter works correctly: the ViewModel filters with `contains(query, ignoreCase = true)`.
- The Google search launch builds the URL by raw string interpolation — `"https://www.google.com/search?q=$query"` — so spaces, `&`, `+`, Japanese characters, and emojis produce invalid URLs or wrong results (CRITICAL, issue #5).

## Target State

- In-app filter behavior is unchanged (documented baseline).
- The Google search URL is built with the query percent-encoded (UTF-8) before launching the intent.

## ADDED Requirements

### Requirement: Case-insensitive in-app search (baseline)

The system MUST filter the anime list by substring, ignoring case. Search behavior MUST NOT regress.

#### Scenario: Case-insensitive substring match

- GIVEN a list containing "Konosuba" and "made in abyss"
- WHEN the user types "konosuba"
- THEN only the matching anime remains visible

### Requirement: Encoded Google search URL

When launching Google search for an anime name, the system MUST percent-encode the query with UTF-8 before building the URL, so any character (spaces, `&`, `+`, CJK, emojis) yields correct results.

#### Scenario: Query with spaces and ampersand

- GIVEN an anime named "Dragon Ball Z & GT"
- WHEN the user taps the Google search icon
- THEN the launched URL contains the percent-encoded query and opens valid, correct Google results

#### Scenario: Query with non-ASCII characters

- GIVEN an anime name containing Japanese characters (e.g. "物語")
- WHEN the user taps the Google search icon
- THEN the URL is encoded in UTF-8 and the search returns the intended results
