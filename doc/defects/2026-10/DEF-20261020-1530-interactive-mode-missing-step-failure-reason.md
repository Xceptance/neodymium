# [DEF-20261020-1530] Interactive Mode Missing or Misattributed Step Failure Reason

- **Status:** `Resolved`
- **Opened:** 2026-10-20 15:15
- **Closed:** 2026-10-20 15:30
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** When a test step fails in interactive mode, the user was presented with a red failed step card and paused runner controls, but no failure reason or exception message was shown on the step card or in the details panel. Furthermore, if the step had AI reasoning, the AI's pre-execution plan was erroneously displayed inside a box labeled "Failure Reason", instead of the actual failure cause.
- **Root Cause:**
  1. **Missing Step Card Failure Reason Element (`interactive_console.js`):** `renderStepCard` lacked a dedicated failure reason block. It only checked `step.reasoning` and conditionally swapped the label to `"Failure Reason"` if `isFailed` was true. As a result, standard steps or uncaught Selenium/assertion exceptions without prior AI reasoning displayed no failure message at all, while steps with AI reasoning displayed the model's pre-execution intent under the "Failure Reason" header.
  2. **Schema Drift in Failure Serialization (`InteractiveStateBuilder.java`):** `InteractiveStateBuilder.serializeStep` serialized failure reasons to `"error"` and `"failureReason"`, but omitted `"errorMessage"`. In `interactive_console.js`, `buildStepDetailsHtml` (used in the right-column inspector and mobile accordion) strictly evaluated `if (step.errorMessage)`, causing the error details box to be omitted.
  3. **Missing Fallback to Transient Step Failure:** When a step failed dynamically, if `step.getFailureReason()` was unset on the `PlaybookStep` instance, `InteractiveStateBuilder` did not fall back to `context.getTransientData().get("KEY_LAST_STEP_FAILURE")`. Additionally, top-level state only computed error messages when `runnerStatus` was `"failed"`, ignoring `"paused"` with a failed step.
  4. **Sub-Steps & Suggest Fix Dialog Omissions:** Sub-step rows in `subStepsTreeHtml` omitted failure text for failed child steps, and the AI Fix Suggestion modal omitted the failure context that justified the suggestion.
- **Detection Gap ("What did we miss?"):** Previous unit tests verified that `stateJson` contained `"failureReason"` or that mocked fixtures contained `"errorMessage"`, but did not assert step card DOM rendering and fallback behavior when `step.reasoning` was null.
- **Resolution:**
  1. Updated `InteractiveStateBuilder.java` to set `"failureReason"`, `"error"`, and `"errorMessage"` across state, steps, and sub-steps, with fallback to `KEY_LAST_STEP_FAILURE` on active failed steps and when runner status is `"paused"`.
  2. Updated `InteractiveConsoleListener.java` to guarantee fallback to `error.toString()` if `error.getMessage()` is null, ensure `step.setStatus(PlaybookStepStatus.FAILED)` and `step.setFailed(true)` before pushing state, and pass `failureReason` in `fixEvent` payloads.
  3. Added a dedicated `.inline-reasoning-bubble.failed` callout on the step card in `interactive_console.js` with error icon, expandable message preview, and one-click copy button, while keeping `inlineReasoningHtml` strictly for AI Reasoning.
  4. Updated `buildStepDetailsHtml` and `hasDetails` to recognize `step.failureReason || step.errorMessage || step.error`.
  5. Added inline failure message display under failed sub-step rows and in the "Suggest Fix" overlay dialog.
- **Safety Net Added:**
  - `com.xceptance.neodymium.ai.console.InteractiveConsoleEngineTest#testBuildStateJsonSerializesFailureReason`
  - `com.xceptance.neodymium.ai.console.InteractiveConsoleEngineTest#testBuildStateJsonSerializesFailureReasonWhenPaused`
  - `com.xceptance.neodymium.ai.console.InteractiveConsoleEngineTest#testBuildStateJsonFallsBackToTransientFailureMessageWhenStepFailureReasonNull`
  - `com.xceptance.neodymium.ai.console.InteractiveConsoleEngineTest#testBuildStateJsonSerializesSubStepFailureMessage`
  - `org.neodymium.ai.event.InteractiveConsoleListenerTest#testPauseOnStepFailureSuggestFixResolvesStepByIndexAndStoresState`
