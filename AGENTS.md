# Agent Instructions

## General
- **Confirm First:** Never implement without user confirmation.
- **Java First:** Prefer Java for scripting/agent tasks over Python/Bash, unless standard Unix tooling fits perfectly.
- **Workflow:** Check `specifications/openspec/changes/` for active changes and delta specs before implementing. Use `/opsx-*` workflows.

## Coding Standards
- **TDD:** Write unit/integration tests before implementing new functionality. Ensure full coverage.
- **Style:** Allman code style (new line braces), document non-obvious logic and all public API, JDK 21 features.
- **Attribution:** Mark exclusively AI-created files with the model name in class comments (e.g. `// AI-generated: <NAME and MODEL>`).
- **Headers:** Add MIT license header to all new source files.
- **Strict Java:** Aggressive `final` modifiers (variables, args, methods, fields). NO inline FQCNs; use explicit top imports.

## Dependencies & Git
- **Dependencies:** ALWAYS ask permission before adding. Document in `NOTICE.md` and `doc/3rd-party-licenses/`.
- **Git:** No fast-forward merges. Ask before stashing. Branch naming: `(feat|fix|chore|docs)/kebab-case`.

## Testing & Specifications
- **Maintenance:** Tests MUST be created/updated for any logic or UI changes (reinforces TDD).