# Agent Instructions

## General
- **Confirm First:** Never implement without user confirmation.
- **Java First:** Prefer Java for scripting/agent tasks over Python/Bash, unless standard Unix tooling fits perfectly.
- **Workflow:** Check `specifications/openspec/changes/` for active changes and delta specs before implementing. Use `/opsx-*` workflows.

## Coding Standards
- **TDD:** Write unit/integration tests before implementing new functionality. Ensure full coverage.
- **Style:** Allman code style (new line braces), document non-obvious logic and all public API, JDK 21 features.
- **Attribution:** Mark exclusively AI-created files with the model name in class comments (e.g. `// AI-generated: <Model Name in use>`). Dynamically detect and expose the actual model name in use (e.g., `Gemini 2.5 Flash` or `Gemini 2.5 Pro`).
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
- **Extensive Documentation:** For detailed guidelines, directory layouts, and step replays instructions, refer to [doc/aura-visual-defect-sandbox.md](file:///home/rschwietzke/projects/GIT/neodymium-library/doc/aura-visual-defect-sandbox.md).