# Defect Record: PlaybookLinter Error Masking and Editor Step Row Selector Collision

- **ID:** DEF-20261110-1030-linter-error-masking-row-selector-collision
- **Status:** Resolved
- **Scope:** Framework & Test/Harness
- **Component:** Neodymium Core (PlaybookLinter) / Aura Manager (AuraTestEditorController, dashboard-editor.js)
- **Severity:** High
- **Date Reported:** 2026-11-10
- **Date Resolved:** 2026-11-10

## 1. Symptom & Impact
1. When AI Review was triggered in the visual editor without a configured LLM provider or during network/quota errors, `PlaybookLinter.lint(...)` caught `Throwable` and returned an empty list. The controller and UI interpreted 0 findings as complete validation success, presenting a misleading green toast (*"✨ AI Review: No issues found — playbook steps look clear and complete."*), falsely validating broken playbooks.
2. In visual editor mode, `editor.html` uses `data-line="${iterStat.index + 1}"` independently for `beforeStepsList`, `stepsList`, and `afterStepsList`. When clicking *"Apply Rewrite"* on step 1 of `steps:`, the query selector `document.querySelector('.step-row[data-line="1"]')` matched step 1 in `beforeStepsList`, silently corrupting the wrong step.
3. Deleting a step left its adjacent inline `.review-suggestion-card` orphaned in the DOM.

## 2. Root Cause Analysis
1. `PlaybookLinter.lint(...)` caught all exceptions to ensure headless test executions were not aborted by optional pre-flight checks. However, this conflated a clean playbook with an execution/provider failure.
2. Duplicate `data-line` values exist across setup, main, and teardown lists, causing global `document.querySelector` lookups by line number to collide.
3. `deleteStep` removed the `.step-row` element without checking for and removing an immediately adjacent sibling `.review-suggestion-card`.

## 3. Detection Gap
Initial tests for Ticket #81 mocked `PlaybookLinter` returning findings directly and tested toolbar rendering without asserting failure feedback or multi-section rewrite anchoring.

## 4. Resolution
1. Introduced `PlaybookLinterResult` record (`findings`, `successful`, `errorMessage`) and `lintWithResult(...)` in `PlaybookLinter.java`. `lint(...)` delegates to `lintWithResult(...).findings()` preserving 100% backward compatibility.
2. Updated `AuraTestEditorController.java` to call `lintWithResult` and return `status: "ERROR"` with an actionable error description when the provider is missing or fails. Added `rawInstruction` to findings payload.
3. Updated `applySuggestionRewrite` in `dashboard-editor.js` to accept button element references and resolve rows via `card.previousElementSibling` without selector ambiguity.
4. Added adjacent suggestion card removal in `deleteStep()` and anchored suggestions using `rawInstruction` first.

## 5. Safety Net Verification
- `org.neodymium.ai.playbook.linter.PlaybookLinterTest`:
  - `testLintWithResultCapturesSuccess`: asserts findings and `successful = true`.
  - `testLintWithResultCapturesFailureWhenNoProvider`: asserts error message and `successful = false`.
  - `testLintWithResultCapturesProviderException`: asserts exception message propagation.
  - `testLintWithResultCapturesFailureWhenDisabled`: asserts configuration bypass.
- `com.xceptance.aura.test.controller.AuraTestEditorControllerTest`:
  - `testAiReviewWhenLinterReturnsFailure`: verifies `status: "ERROR"` when provider fails.
  - `testAiReviewSuccessfulWithFindings`: verifies `rawInstruction` is included in findings.
- `com.xceptance.aura.test.ui.AuraManagerEditorAiReviewToolbarTest`: Selenide browser UI integration test.
