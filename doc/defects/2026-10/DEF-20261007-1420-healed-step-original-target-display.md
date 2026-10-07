# [DEF-20261007-1420] Healed Step Action Expected Target Not Recorded in Console Execution Log or UI Views

- **Status:** `Resolved`
- **Opened:** 2026-10-07 14:05
- **Closed:** 2026-10-07 14:20
- **Component:** `neodymium-core`, `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When a playbook step action underwent runtime locator self-healing (e.g., `#selectLanguage` healed to `#searchLanguage`), `console-execution-*.json` and the UI (Test Execution Report, Playbook side panel, and Interactive Console) displayed only the healed target (or the unhealed target prior to earlier fixes), without presenting what the original expected action target was before healing took place.
- **Root Cause:** 
  1. `Action.java` and `ReportActionEntry` in `TestExecutionReport.java` lacked dedicated fields (`expectedTarget`) to preserve the pre-healed locator string.
  2. `PlaybookToolReplayer.java` did not assign `expectedTarget` from `canonicalAction` to `resolvedAction` during runtime event dispatching.
  3. `PreliminaryReportListener.java` only populated `target` and `resolvedTarget`, neglecting to store `expectedTarget`.
  4. `InteractiveStateBuilder.java` and `LocalRunJsonStorageService.java` did not serialize or persist `expectedTarget`, `originalTarget`, or `healedFrom` attributes on action JSON objects.
  5. UI templates and renderers (`report-manager.js`, `side-panel-playbook.html`, `interactive_console.js`, `HtmlReportGenerator.java`) did not render a strikethrough badge denoting `🧬 Healed from: <target>` when an action target was self-healed.
- **Detection Gap ("What did we miss?"):** Previous healing tests in `PreliminaryReportListenerTest` and `RunStorageSyncServiceTest` only asserted `isHealed()` and `resolvedTarget`, but did not assert whether `expectedTarget`, `originalTarget`, or `healedFrom` were preserved in execution state and serialized JSON logs.
- **Resolution:**
  1. Added `expectedTarget` field, `@JsonProperty("expectedTarget")`, `@JsonAlias({"expectedTarget", "originalTarget", "healedFrom"})`, and getters/setters in `Action.java` and `TestExecutionReport.ReportActionEntry`.
  2. Updated `PlaybookToolReplayer.java` in `dispatchActionEvent` to set `resolvedAction.setExpectedTarget(origTarget)` and `resolvedAction.setHealed(true)`.
  3. Updated `PreliminaryReportListener.java` to set `actionEntry.setExpectedTarget(origTarget)`.
  4. Updated `InteractiveStateBuilder.java` to serialize `expectedTarget`, `originalTarget`, and `healedFrom` in `serializeReportActions` and `serializeStep`.
  5. Updated `LocalRunJsonStorageService.java` to preserve `expectedTarget`, `originalTarget`, and `healedFrom` when writing `console-execution-*.json`.
  6. Updated `report-manager.js`, `side-panel-playbook.html`, `interactive_console.js`, and `HtmlReportGenerator.java` to render the `🧬 Healed from: <originalTarget>` pill badge with strikethrough text.
- **Safety Net Added:**
  - `InteractiveConsoleEngineTest.testInteractiveConsoleStateJsonReflectsHealedActionTarget`: asserts `expectedTarget`, `originalTarget`, and `healedFrom` in generated interactive state JSON.
  - `PreliminaryReportListenerTest.testHealedActionEventWithCanonicalAndResolvedSelectors`: asserts `actionEntry.getExpectedTarget()` equals the recorded canonical selector.
  - `PlaybookToolReplayTest.testOfflineReplayHealsTargetUsingLiveCandidates`: asserts `aee.getResolvedAction().getExpectedTarget()` matches canonical recorded target.
  - `RunStorageSyncServiceTest.testHealedActionTargetPreservedInStorageService`: asserts `expectedTarget`, `originalTarget`, and `healedFrom` are deserialized and preserved in run storage.
