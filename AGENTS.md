# Agent Instructions

> [!CRITICAL]
> ## MANDATORY APPROVAL PROTOCOL (ZERO EXCEPTIONS)
> **NEVER modify files or execute state-changing operations without explicit user approval first.**
> 
> 1. **Phase 1 — Investigation & Proposal Only:**
>    - You MAY read files (`view_file`, `grep_search`), inspect logs, or use `code-review-graph` tools.
>    - You MUST re-interpret requests like "Fix X" or "Implement Y" as "Diagnose/Plan X and propose a solution".
>    - Present your diagnosis and detailed implementation plan to the user.
>    - **HARD STOP:** End your turn with a clear request for approval (e.g. *"Do you approve this plan to proceed with implementation?"*) without invoking any edit tools.
> 
> 2. **Phase 2 — Implementation:**
>    - Do NOT call editing tools (`replace_file_content`, `multi_replace_file_content`, `write_to_file`) or run state-mutating shell commands until the user responds with explicit approval in a subsequent turn.

## General
- **Java First:** Prefer Java for scripting/agent tasks over Python/Bash, unless standard Unix tooling fits perfectly.
- **Workflow:** Check `specifications/openspec/changes/` for active changes and delta specs before implementing. Use `/opsx-*` workflows.
- **Secrets & Security:** You must never directly store any secrets, such as API keys, in source code, configuration files (e.g. `neodymium.properties`, `ai.properties`), or templates. All API keys and credentials must be injected dynamically via environment variables (such as `GEMINI_API_KEY`) or passed at run-time as JVM arguments (e.g., `-Dneodymium.ai.apiKey=...`). Before you commit anything, verify your staged changes do not contain keys by running `git diff --cached | grep -E "(apiKey|API_KEY|AQ\.[a-zA-Z0-9_\-]{10,})"`. If a secret is accidentally committed, notify the user immediately.
- **Universal Framework (No SUT Hardcodings):** Neodymium Aura is a universal, application-agnostic framework, not tied to any specific SUT or domain. The core engine and PESAP must never contain domain-specific heuristics, SUT hardcodings, or site-specific assumptions.

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
