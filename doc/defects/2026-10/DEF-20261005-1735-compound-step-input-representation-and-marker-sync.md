# [DEF-20261005-1735] Compound Step Input Representation and Dynamic Marker Sync

- **Status:** `Resolved`
- **Opened:** 2026-10-05 15:45
- **Closed:** 2026-10-05 17:35
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:**
  1. In the preliminary HTML test report (`CheckoutTest_live_tailwind-by-claude_20261005-153733.html`), Step 3 used proactive visual markers (`mark_elements`) during turn execution, but the report step table summary completely lacked the `🎯 MARKER` badge.
  2. In the same test run, Step 17.1 (the first child sub-step of `Fill out the shipping address form:`) displayed a `⚡ REPLAY` badge and showed an action `SELECT #shipping-country US` during a `FORCE_RECORDING` run where the compound parent step was executed via `🤖 LLM`. Furthermore, `matchesSubStep` miscorrelated `SELECT` to `Country` because `inst.contains("add")` substring-matched "address", improperly slicing the action.
  3. `TestExecutionReport.ReportStepEntry.getStepMode()` suffered from cyclic mutual recursion when resolving parent and child modes, throwing a `StackOverflowError` that aborted report writing.
- **Root Cause:**
  1. `PlaybookStep.setMarker(true)` is activated dynamically when the LLM invokes `mark_elements` inside `AgentToolLoopStep`. However, `PreliminaryReportListener.onStepFinished` copied only standard status and bug details into `targetStep`, omitting `pbStep.isMarker()` and `pbStep.isVisualOrLayoutStep()`. As a result, dynamic visual marker flags were lost during report model synchronization.
  2. Sub-steps declared under a step in playbook YAML (e.g., form fields) serve as convenient structured input data for human authoring and readability rather than independent execution steps ("the main flow is in the initial line"). Calling `partitionToolCallsAndActions` in `AgentToolLoopStep` to slice compound parent actions into child sub-steps was fragile, violated language agnosticism, and produced empty tool call lists on children. Additionally, `PreliminaryReportListener` contained a fallback heuristic that if `targetStep.getActions().size() == pbStep.getSubSteps().size()`, it forcibly sliced parent actions into child sub-steps 1-to-1. In the user's test, 7 form actions equaled 7 child sub-steps, causing `PreliminaryReportListener` to inject the first action (`SELECT #shipping-country US`) into child step 17.1 (`Country`). Because child 17.1 now possessed an action but zero LLM calls, the HTML report generator fallback displayed `⚡ REPLAY` on child 17.1.
  3. Circular lookup between parent and child step modes caused unbounded recursion.
- **Detection Gap ("What did we miss?"):**
  1. Existing unit tests for `PreliminaryReportListener` only asserted marker badges when `PlaybookStep.setMarker(true)` was statically declared before step execution, never when toggled dynamically mid-turn via `mark_elements`.
  2. Sub-step tests asserted `partitionToolCallsAndActions` using mocked static inputs rather than evaluating report generation and language neutrality on live compound steps.
- **Resolution:**
  1. In `PreliminaryReportListener.java` (`onStepFinished`), added synchronization of `targetStep.setMarker(pbStep.isMarker())` and `targetStep.setVisual(pbStep.isVisualOrLayoutStep())`.
  2. In `PreliminaryReportListener.java` (`onStepFinished`), completely eliminated the artificial 1-to-1 action slicing fallback (`targetStep.getActions().size() == pbStep.getSubSteps().size()`).
  3. In `AgentToolLoopStep.java` (`finishLoop`), removed `partitionToolCallsAndActions`. All actions executed during a compound LLM turn remain on the parent step. Child sub-steps retain `SUCCESS` status and proportional duration as milestone entries without artificial action fragmentation.
  4. In `TestExecutionReport.java`, linked parent references (`setParent(this)` in `addSubStep`) and broke recursion in `getStepMode()`.
  5. In `HtmlReportGenerator.java` (`appendStepBadges`), suppressed fallback `⚡ REPLAY` badges on child sub-steps (`step.getParent() != null`).
  6. In `AgentToolLoopStep.java` (`matchesSubStep`), decoupled `select` from `click` and enforced whole-word matching `\badd\b` so `address` never triggers a false positive match.
- **Safety Net Added:**
  - `PreliminaryReportListenerTest.testOnStepFinishedPropagatesDynamicMarkerAndVisualFlags`: verifies that dynamic mid-step marker and visual flags propagate to report models and HTML badges.
  - `HtmlReportGeneratorTest.testChildSubStepDoesNotRenderReplayBadgeWhenParentIsLlm`: verifies that child sub-steps do not render fallback replay badges when parent executed via LLM.
  - `SubStepReportingAndScopingTest.testCompoundStepRetainsActionsOnParentWithoutArtificialPartitioning`: verifies that compound steps retain actions on the parent and child steps do not receive fragmented actions.
  - `AgentToolLoopStepTest.testMatchesSubStepDoesNotMatchAddressAsAdd`: verifies language-neutral whole-word boundary matching.
