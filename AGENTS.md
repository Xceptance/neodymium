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
- **Strict Java:** Aggressive `final` modifiers (variables, args, methods, fields). NO inline FQCNs; use explicit top imports. Unused imports and variables are strictly prohibited.

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