# Agent Instructions

## General
- **Confirm First:** Never implement without user confirmation.
- **Java First:** Prefer Java for scripting/agent tasks over Python/Bash, unless standard Unix tooling fits perfectly.
- **Workflow:** Check `specifications/openspec/changes/` for active changes and delta specs before implementing. Use `/opsx-*` workflows.
- **Secrets & Security:** You must never directly store any secrets, such as API keys, in source code, configuration files (e.g. `neodymium.properties`, `ai.properties`), or templates. All API keys and credentials must be injected dynamically via environment variables (such as `GEMINI_API_KEY`) or passed at run-time as JVM arguments (e.g., `-Dneodymium.ai.apiKey=...`). Before you commit anything, verify your staged changes do not contain keys by running `git diff --cached | grep -E "(apiKey|API_KEY|AQ\.[a-zA-Z0-9_\-]{10,})"`. If a secret is accidentally committed, notify the user immediately.

## Coding Standards
- **TDD:** Write unit/integration tests before implementing new functionality. Ensure full coverage.
- **Style:** Allman code style (new line braces), document non-obvious logic and all public API, JDK 21 features.
- **Headers:** Add GNU AGPLv3 license header to all AI-related source files, but MIT license header for the rest.
- **Attribution:** Do not use inline comments like `// AI-generated: <Model>`. Use `@author` tags on class/interface Javadoc level instead (e.g., `@author AI-generated: <Model Name>`), and always add a second author tag for `Xceptance GmbH 2026`.
- **Strict Java:** Aggressive `final` modifiers (variables, args, methods, fields). NO inline FQCNs; use explicit top imports. Unused imports and variables are strictly prohibited.
- **Documentation:** Write proper method comments, inline comments, class comments, and clearly document what fields are for. Make it as useful for a human as possible. Also keep in mind, that this is also documentation for AI to reconstruct decisions.
- **Imports:** Always import fully qualified. Use static imports such as `Assertions.assertEquals`, only when it increases readability and the import is used multiple times. Remove unused imports.

## Dependencies & Git
- **Dependencies:** ALWAYS ask permission before adding. Document in `NOTICE.md` and `doc/3rd-party-licenses/`.
- **Git:** No fast-forward merges. Ask before stashing. Branch naming: `(feat|fix|chore|docs)/kebab-case`.

## Testing & Specifications
- **Maintenance:** Tests MUST be created/updated for any logic or UI changes (reinforces TDD).

## Aura AI Test Sandbox
- **Aura Glance Sandbox:** To test any Neodymium Aura AI features (such as visual audits, dHash baselines, parameterizations, multi-port, offline replays, or dynamic visual defects), utilize our self-contained **Aura Test Suite Hub** under `src/test/resources/ai-test-pages/AuraGlanceTest/`.
- **Server Ports:** Served dynamically on random free ports (HTTP + HTTPS self-signed cert `keystore.p12`) via `EmbeddedHtmlServer.java` (no external running app required!).
- **How to Use:** Write tests in `AuraGlanceTest.java` and `AuraFeatureMatrixTest.java` extending `BaseAiTest`. Inject parameterized data from `AuraFeatureMatrixTest.json`.
- **Extensive Documentation:** For detailed guidelines, directory layouts, and step replays instructions, refer to [doc/aura-visual-defect-sandbox.md](doc/aura-visual-defect-sandbox.md).

<!-- code-review-graph MCP tools -->
## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes_tool` or `query_graph_tool` instead of Grep
- **Understanding impact**: `get_impact_radius_tool` instead of manually tracing imports
- **Code review**: `detect_changes_tool` + `get_review_context_tool` instead of reading entire files
- **Finding relationships**: `query_graph_tool` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview_tool` + `list_communities_tool`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool | Use when |
| ------ | ---------- |
| `detect_changes_tool` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context_tool` | Need source snippets for review — token-efficient |
| `get_impact_radius_tool` | Understanding blast radius of a change |
| `get_affected_flows_tool` | Finding which execution paths are impacted |
| `query_graph_tool` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes_tool` | Finding functions/classes by name or keyword |
| `get_architecture_overview_tool` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes_tool` for code review.
3. Use `get_affected_flows_tool` to understand impact.
4. Use `query_graph_tool` pattern="tests_for" to check coverage.
