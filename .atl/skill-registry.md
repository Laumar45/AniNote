# Skill Registry — AniLista

> Index of agent skills by trigger and path. This is an index, not a summary:
> subagents receive exact paths and read the full SKILL.md source of truth.
> Scanned: 2026-08-04. Scan rules: skip `sdd-*`, `_shared`, `skill-registry`;
> deduplicate by name, preferring project-level skills (none exist in this repo).

## User Skills (C:\Users\Lenovo\.config\opencode\skills)

| Skill | Trigger (from description) | Path | Scope |
|---|---|---|---|
| branch-pr | Creating, opening, or preparing PRs for review | `C:\Users\Lenovo\.config\opencode\skills\branch-pr\SKILL.md` | user |
| chained-pr | PRs over 400 lines, stacked PRs, review slices | `C:\Users\Lenovo\.config\opencode\skills\chained-pr\SKILL.md` | user |
| cognitive-doc-design | Writing guides, READMEs, RFCs, onboarding, architecture, or review-facing docs | `C:\Users\Lenovo\.config\opencode\skills\cognitive-doc-design\SKILL.md` | user |
| comment-writer | PR feedback, issue replies, reviews, Slack messages, or GitHub comments | `C:\Users\Lenovo\.config\opencode\skills\comment-writer\SKILL.md` | user |
| customize-opencode | Editing/creating opencode's own config, agents, subagents, skills, plugins, MCP servers, permission rules | built-in | user |
| go-testing | Go tests, go test coverage, Bubbletea teatest, golden files | `C:\Users\Lenovo\.config\opencode\skills\go-testing\SKILL.md` | user |
| issue-creation | Issue creation, bug reports, feature requests, issue approval | `C:\Users\Lenovo\.config\opencode\skills\issue-creation\SKILL.md` | user |
| judgment-day | Judgment day, dual review, adversarial review, juzgar | `C:\Users\Lenovo\.config\opencode\skills\judgment-day\SKILL.md` | user |
| skill-creator | New skills, agent instructions, documenting AI usage patterns | `C:\Users\Lenovo\.config\opencode\skills\skill-creator\SKILL.md` | user |
| skill-improver | Improve skills, audit skills, refactor skills, skill quality | `C:\Users\Lenovo\.config\opencode\skills\skill-improver\SKILL.md` | user |
| work-unit-commits | Implementation, commit splitting, chained PRs, keeping tests and docs with code | `C:\Users\Lenovo\.config\opencode\skills\work-unit-commits\SKILL.md` | user |

## User Skills (C:\Users\Lenovo\.agents\skills)

| Skill | Trigger (from description) | Path | Scope |
|---|---|---|---|
| android-clean-architecture | Clean Architecture patterns for Android/KMP — module structure, dependency rules, UseCases, Repositories, data layer | `C:\Users\Lenovo\.agents\skills\android-clean-architecture\SKILL.md` | user |
| developing-with-streamlit | ALL Streamlit tasks: creating, editing, debugging, styling, deploying | `C:\Users\Lenovo\.agents\skills\developing-with-streamlit\SKILL.md` | user |
| find-skills | "How do I do X", "find a skill for X" — discover and install agent skills | `C:\Users\Lenovo\.agents\skills\find-skills\SKILL.md` | user |
| frontend-design | Distinctive, intentional visual design for new UI or reshaping existing UI | `C:\Users\Lenovo\.agents\skills\frontend-design\SKILL.md` | user |
| interface-design | Craft-first interface design for dashboards, admin panels, SaaS apps, product UI | `C:\Users\Lenovo\.agents\skills\interface-design\SKILL.md` | user |
| kotlin-coroutines-flows | Kotlin Coroutines and Flow patterns for Android/KMP — structured concurrency, StateFlow, error handling, testing | `C:\Users\Lenovo\.agents\skills\kotlin-coroutines-flows\SKILL.md` | user |
| kotlin-patterns | Idiomatic Kotlin patterns, best practices, coroutines, null safety, DSL builders | `C:\Users\Lenovo\.agents\skills\kotlin-patterns\SKILL.md` | user |
| playwright-cli | Automate browser interactions, test web pages, Playwright tests | `C:\Users\Lenovo\.agents\skills\playwright-cli\SKILL.md` | user |
| styles | Jetpack Compose Styles API integration — component themes, styleable components | `C:\Users\Lenovo\.agents\skills\styles\SKILL.md` | user |
| tanstack-query-expert | TanStack Query (React Query) — async state, fetching, mutations, optimistic updates | `C:\Users\Lenovo\.agents\skills\tanstack-query-expert\SKILL.md` | user |
| testing-setup | Testing strategy for native Android apps — libraries, harnesses, unit/UI/screenshot/e2e tests | `C:\Users\Lenovo\.agents\skills\testing-setup\SKILL.md` | user |

## Excluded by Scan Rules

- `sdd-*` (11 skills: sdd-apply, sdd-archive, sdd-design, sdd-explore, sdd-init, sdd-onboard, sdd-propose, sdd-spec, sdd-tasks, sdd-verify) — managed by the SDD pipeline.
- `_shared`, `skill-registry` — infrastructure, not triggerable.

## Convention / Context Files

| File | Role | Path |
|---|---|---|
| Project agent rules | Agent constraints (Spanish): never run gradle/build/test; report verification steps to user | `C:\Users\Lenovo\AndroidStudioProjects\AniLista\AGENTS.md` |
| Design brief (v3) | Source of truth: stack, closed decisions, folder structure, roadmap | `C:\Users\Lenovo\AndroidStudioProjects\AniLista\design-brief.md` |
| Global agent rules | Persona + Engram memory protocol + skill-loading mandate | `C:\Users\Lenovo\.config\opencode\AGENTS.md` |
| Version catalog | All dependency versions (AGP 8.7.3, Kotlin 2.1.0, Room 2.6.1, etc.) | `C:\Users\Lenovo\AndroidStudioProjects\AniLista\gradle\libs.versions.toml` |
| SDD config | Strict TDD, testing capabilities, phase rules | `C:\Users\Lenovo\AndroidStudioProjects\AniLista\openspec\config.yaml` |

## SDD Artifact Paths

```
Proposal: openspec/changes/{change-name}/proposal.md
Specs:    openspec/changes/{change-name}/specs/
Design:   openspec/changes/{change-name}/design.md
Tasks:    openspec/changes/{change-name}/tasks.md
Verify:   openspec/changes/{change-name}/verify-report.md
Config:   openspec/config.yaml
Archive:  openspec/changes/archive/YYYY-MM-DD-{change-name}/
```
