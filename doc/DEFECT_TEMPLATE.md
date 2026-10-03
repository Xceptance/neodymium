# Defect Record Specification & Template

This document defines the defect tracking workflow, lifecycle states, classification taxonomy, and post-mortem template for all Neodymium development.

To eliminate Git merge conflicts across concurrent branches, **never record defects in a single shared file**. Every defect must be logged in its own dedicated Markdown file under `doc/defects/YYYY-MM/`.

---

## 1. File Location & Naming Convention

- **Directory:** `doc/defects/YYYY-MM/` (partitioned by defect creation year and month)
- **File Format:** `DEF-YYYYMMDD-HHmm-<slug>.md`
  - `YYYYMMDD`: 8-digit date (e.g. `20261003`)
  - `HHmm`: 4-digit local timestamp for natural intraday chronological sorting (e.g. `1956`)
  - `<slug>`: 2 to 6 kebab-case words describing the failure mechanism (e.g. `vector-healing-bypass`, `script-tag-json-truncation`)
- **Example:** `doc/defects/2026-10/DEF-20261003-1956-vector-healing-bypass.md`

---

## 2. Defect Lifecycle States

| Status | Definition | When to Use |
| :--- | :--- | :--- |
| **`Open`** | Diagnosed, reproduced, or identified, but fix and safety net are pending. | In Phase 1 proposal before writing code. |
| **`Partially Resolved`** | Immediate mitigation or workaround applied, but full solution or edge cases remain open. | Temporary hotfix or partial PR. Requires a `- **Pending Items:**` section. |
| **`Resolved`** | Defect completely fixed, verified, and protected by a permanent automated regression test. | In Phase 2 implementation alongside code and test commits. |
| **`WontFix`** | Investigated and determined to be an external upstream constraint or intentional SUT behavior. | Closed without code change; explanation in Resolution. |

---

## 3. Scope Classification Taxonomy

When documenting a defect, classify its fundamental fault domain in the `Scope:` field:

| Scope | Domain & Coverage | Typical Neodymium Scenarios |
| :--- | :--- | :--- |
| **`Framework`** | Core engine, execution pipeline, state machine, reporters, runners, and locators. | DOM serializer unescaped script tags, pseudo-class parser failures, race conditions in console pause/resume. |
| **`AI/Prompt`** | System prompts, prompt engineering, VLM visual grounding/markers, LLM provider client adapters, token budgeting. | Premise bias during visual RCA, coordinate replay drift caused by proactive markers, token budget exhaustion on multi-tool turns. |
| **`Test/Harness`** | Test suites, test assertions, mocks, test pages, and synthetic fixtures. | Mock LLM provider class-cast in data tests, `EmbeddedHtmlServer` port collisions, flaky wait timeouts, false-positive assertions. |
| **`SUT`** | Bugs inside the System Under Test (e.g., Verla demo store, demo shopping carts). | Broken checkout submit button, form validation errors, missing accessibility attributes, responsive layout breakage. |
| **`Infra/Build`** | Build system, CI/CD, Maven plugins, FFmpeg, Docker, and browser driver binaries. | Video recording failure due to FFmpeg framerate/directory creation order, Surefire/failsafe execution order discrepancies. |
| **`Config/Environment`** | Properties files, JVM arguments, browser capabilities/profiles, headless display resolutions, and OS quirks. | `neodymium.properties` or `ai.properties` key resolution, headless Chrome viewport clipping, OS-specific path separators. |
| **`Doc/Spec`** | OpenSpec specifications, playbook syntax guides, public API Javadoc, or architectural manuals. | Outdated playbook YAML syntax documentation, misleading tool schema definitions. |

*(Composite values like `Framework & AI/Prompt` or `Test/Harness & Config/Environment` are allowed if a defect crosses multiple boundaries).*

---

## 4. Agent Post-Mortem Quality Rubrics

When authoring a defect record, agents must adhere to the following standards:

1. **Root Cause Depth (Zero Vagueness):**
   - ❌ *Unacceptable:* "The test failed because of a NullPointerException."
   - ✔️ *Acceptable:* "`TakeScreenshotsThread` attempted to dereference `driver` before Selenide initialized the WebDriver instance during asynchronous runner startup."
2. **Detection Gap ("What Did We Miss?"):**
   - ❌ *Unacceptable:* "We didn't have a test for this."
   - ✔️ *Acceptable:* "Existing unit tests passed a pre-initialized mock WebDriver in `@BeforeEach`, masking the asynchronous driver lifecycle that occurs in live AI runners."
3. **Safety Net Verification:**
   - ❌ *Unacceptable:* "Ran mvn test."
   - ✔️ *Acceptable:* "Added `@Test` `BrowserToolsTest.testBrowserAssertUrlExecutionAndSchema` explicitly asserting non-empty URL handling with `{ expectedUrl: '', negated: true }`."

---

## 5. Defect Template (Copy & Paste)

```markdown
# [DEF-YYYYMMDD-HHmm] Concise Summary of Defect

- **Status:** `Open` | `Partially Resolved` | `Resolved` | `WontFix`
- **Opened:** YYYY-MM-DD HH:mm
- **Closed:** YYYY-MM-DD HH:mm
- **Component:** `neodymium-core` / `aura-manager` / etc.
- **Scope:** `Framework` | `AI/Prompt` | `Test/Harness` | `SUT` | `Infra/Build` | `Config/Environment` | `Doc/Spec`
- **Symptom:** Observed failure, error message, or unexpected behavior.
- **Root Cause:** Technical explanation of the underlying code mechanism.
- **Detection Gap ("What did we miss?"):** Why existing tests/linters failed to catch this earlier.
- **Resolution:** Summary of code changes made.
- **Safety Net Added:** Reference to permanent automated regression test class and method.
```
