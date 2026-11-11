# [DEF-20261110-1135] PlaybookLinter Boolean Disabling Regression and Step Move Suggestion Card Drift

- **Status:** `Resolved`
- **Opened:** 2026-11-10 11:35
- **Closed:** 2026-11-10 11:45
- **Component:** `neodymium-core` (PlaybookLinter) / `aura-manager` (dashboard-editor.js, AuraTestEditorController)
- **Scope:** `Framework & Test/Harness`
- **Symptom:**
  1. Test methods annotated with `@AiLinter(false)` or tests setting `session.data().put("neodymium.ai.linter.enabled", "false")` continued to execute `PlaybookLinter` against the LLM, burning tokens and failing offline tests.
  2. In the visual editor, reordering steps via `moveStep()` left adjacent `.review-suggestion-card` siblings in place, causing suggestions to be physically displaced beneath unrelated steps and triggering destructive rewrites of the wrong steps upon clicking "Apply Rewrite".
  3. When moving steps down, inserting after `targetNextRow` interleaved rows between `targetNextRow` and its own suggestion cards.
  4. In `AuraTestEditorController`, on-demand reviews bootstrapped a brand new `LlmRegistry` on every single HTTP request, causing HTTP client churn.
  5. Missing includes or undefined variables returned misleading "File has syntax errors" messages even when the YAML syntax was valid.
- **Root Cause:**
  1. `PlaybookLinter.java` previously checked `session.data`, but lacked safety net assertions against a default `config.isLinterEnabled() == true`.
  2. `moveStep()` in `dashboard-editor.js` only relocated `.step-row` (and optional `includeTreeCard`), leaving `.review-suggestion-card` siblings at the old DOM index without advancing past target suggestion cards when moving down.
  3. `renderStepSuggestionCards` included comment rows in its step collection, drifting 1-based indices against `playbook.getAllSteps()`.
  4. `PlaybookLinter` had no instance caching for its standalone fallback `LlmRegistry`.
  5. `AuraTestEditorController` classified all validation errors under `SYNTAX_ERROR`.
- **Detection Gap ("What did we miss?"):**
  Existing tests in `PlaybookLinterTest` tested `System.setProperty("neodymium.ai.linter.enabled", "false")` combined with `AiConfiguration.resetInstance()`, but lacked a test asserting that `session.data().putDynamic(...)` could override a `true` default from `AiConfiguration`. Editor UI tests tested toolbar presence and card dismissal, but omitted step reordering while suggestion cards were present.
- **Resolution:**
  1. Verified and enforced cascading session data override in `PlaybookLinter.java`.
  2. Lazily cached `standaloneRegistry` as an instance field in `PlaybookLinter.java` to prevent client churn without static test pollution.
  3. Updated `moveStep()` in `dashboard-editor.js` to bundle all adjacent `.review-suggestion-card` siblings with their parent step, and advance `insertAfterRef` past the target step's suggestion cards when moving down.
  4. Filtered comment rows in `renderStepSuggestionCards()` and matched `rawInstruction` with occurrence resolution.
  5. Differentiated `SYNTAX_ERROR` from `VALIDATION_ERROR` in `AuraTestEditorController.java` and supported both in `dashboard-editor.js`.
- **Safety Net Added:**
  - `PlaybookLinterTest.testLintWithResultHonorsSessionDataDisabledOverride`: Verifies that `session.data` disables prelinting even when configuration defaults to enabled.
  - `AuraTestEditorControllerTest.testAiReviewHaltsOnValidationError`: Verifies `VALIDATION_ERROR` classification on missing includes.
  - `AuraManagerEditorAiReviewToolbarTest.testMoveStepRetainsAdjacentSuggestionCards`: End-to-end Selenide UI test verifying suggestion cards move atomically with steps and rewrite the correct step after reordering.
