# Skill: Global Project Guardrails & Java Coding Standards

## Description
This skill activates automatically on every task involving code modifications, additions, testing, dependency adjustments, or refactoring in this workspace. It ensures strict compliance with our architecture, testing workflows, and styling guardrails.

## Triggers
- Any file creation, modification, or deletion.
- Opening a new implementation plan or reasoning loop.
- Dependency changes or Git branch management actions.

---

## Requirements & Execution Rules

### 1. Workflow & Context Alignment
* **Confirm First:** Never implement any code changes without explicit user confirmation.
* **Java First:** Always prefer Java for scripting or agent-specific tasks over Python or Bash, unless standard Unix tooling fits the requirement perfectly.
* **Workflow Audit:** Before implementing, check `specifications/openspec/changes/` for active changes and delta specifications. Use `/opsx-*` workflows exclusively.

### 2. Strict Coding & Architectural Standards
* **TDD Enforcement:** Write unit and integration tests *before* implementing new functionality. Ensure full coverage.
* **Code Style (Allman):** Use Allman code style (braces on a new line). Document all non-obvious logic and the entirety of the public API. Leverage JDK 21 features.
* **License Headers:** 
  * Add the **GNU AGPLv3** license header to all AI-related source files.
  * Add the **MIT** license header to all other source files.
* **Aggressive Java Constraints:** 
  * Use `final` modifiers aggressively for variables, arguments, methods, and fields.
  * NO inline Fully Qualified Class Names (FQCNs); use explicit top-level imports.
  * Unused imports and unused variables are strictly prohibited.

### 3. Dependencies & Git Hygiene
* **Dependencies:** ALWAYS ask for user permission before adding any new dependency. Document approved dependencies immediately in `NOTICE.md` and `doc/3rd-party-licenses/`.
* **Git Actions:** No fast-forward merges allowed. Ask the user before executing a stash. 
* **Branch Naming Convention:** Enforce the following format: `(feat|fix|chore|docs)/kebab-case`.

### 4. Testing & Specifications
* **Maintenance:** Tests MUST be created or updated for any logic or UI changes to reinforce the TDD approach.
* **Aura AI Test Sandbox Execution:**
  * To test Neodymium Aura AI features (visual audits, dHash baselines, parameterizations, multi-port, offline replays, or dynamic visual defects), utilize the self-contained **Aura Test Suite Hub** under `src/test/resources/ai-test-pages/AuraGlanceTest/`.
  * **Server Infrastructure:** These are served dynamically on random free ports (HTTP + HTTPS self-signed cert `keystore.p12`) via `EmbeddedHtmlServer.java`. No external running app is required.
  * **Test Implementation:** Write tests in `AuraGlanceTest.java` and `AuraFeatureMatrixTest.java` extending `BaseAiTest`. Inject parameterized data directly from `AuraFeatureMatrixTest.json`.
  * **Reference Material:** For detailed guidelines, directory layouts, and step replay instructions, parse and follow `doc/aura-visual-defect-sandbox.md`.

---

## Iterative Rework Loop
1. **Pre-implementation:** Parse `specifications/openspec/changes/` and submit an Implementation Plan to the user for confirmation.
2. **Post-implementation:** Run the local build and test suite. Validate that all new files have proper Allman formatting, `final` modifiers, explicit imports, correct license headers, and no unused components.
3. If any constraint is violated, discard the diff, refactor, and re-verify before presenting the final result.
