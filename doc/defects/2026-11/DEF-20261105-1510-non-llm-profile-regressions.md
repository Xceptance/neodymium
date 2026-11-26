# [DEF-20261105-1510] Non-LLM Test Profile Regressions and Precedence Drift

- **Status:** `Resolved`
- **Opened:** 2026-11-05 15:10
- **Closed:** 2026-11-05 15:58
- **Component:** `neodymium-core`
- **Scope:** `Framework & Test/Harness & Config/Environment`
- **Symptom:** Running the non-LLM test profile (`mvn test -Pnon-llm-tests`) resulted in 17 failures and 21 errors across 10 test suites:
  1. `ExecuteActionsStepTest` (20 errors + 4 failures), `StateMachineRunnerTest` (1 failure), and `RunnerIntegrationTest` (1 error) crashed with `NullPointerException: Cannot invoke "java.util.List.size()" because "flatSteps" is null` during action step mapping in replay mode.
  2. `PropertyPrecedenceOrderTest` (5 failures) and `TokenBudgetGuardTest` (2 failures) failed with token budget and turn limit mismatches (expected `500,000` tokens but was `750,000`; expected `100,000` step tokens but was `200,000`; expected `15` step max turns but was `20`).
  3. `InteractiveReasoningAndActionsUiTest` (1 failure) failed because `.llm-communications-section` inside `.step-secondary-details` was hidden behind a collapsed "More details" toggle.
  4. `InteractiveViewStateTest` (1 failure) failed because toolbar Back button was unconditionally disabled on step index 1 (`s.index === 1`) even when step 0 was a 0-based non-include step, and `.substep-warning-info` was suppressed on multi-action step failures.
  5. `InteractiveViewStandaloneUiTest` (1 failure) failed because `applyState(state)` cleared `currentPauseId = null` when `state.status === "running"` before the `pause` event listener could render `#finalSaveOverlay.active`.
  6. `PlaybookParserTest` (1 failure) failed because `testParseStringStartingWithDataOrBefore` asserted `getSteps().size() == 2` on a playbook with `before:` and `steps:` blocks, when `getSteps()` now only returns main steps (1 step).
  7. `QualityJudgePromptTest` (1 failure) failed asserting `"Neodymium Text Pseudo-Selectors"` in the judge system prompt instead of the modernized `"Playwright Text Matchers & Pseudo-Classes"`.
- **Root Cause:**
  1. **NPE in `ExecuteActionsStep.findStepIndexInFlatList`:** A Git merge resolution in PR #507 (`d18585f0e`) dropped the early null check on `flatSteps` while retaining calls to `flatSteps.size()`.
  2. **Global Config Precedence Leaking Into Unit Tests:** Commits `60bc914b` and `040a6d4f` uncommented operational limit overrides in root `config/ai.properties` (`neodymium.ai.tokenBudget.input = 750000`, `neodymium.ai.step.maxTokens = 200000`, `neodymium.ai.step.maxTurns = 20`). Because Maven Surefire in `neodymium-core` defaults to `workingDirectory=${session.executionRootDirectory}`, `AiConfiguration` loaded these overrides from the repository root instead of verifying the framework's internal default constants (`500,000`, `100,000`, and `15`).
  3. **Interactive Console UI State & Details Toggling:**
     - In `interactive_console.js`, secondary step details (LLM communications, timing, DOM, screenshots) were moved into `.step-secondary-details` behind a `.step-more-details-btn` toggle for active steps, causing tests expecting immediate visibility without expansion to fail.
     - Back button toolbar state in `checkActionApprovals` checked `s.index === 0 || s.index === 1` as a hardcoded first-step heuristic, which erroneously disabled step 1 even when step 0 was the first step.
     - `applyState(state)` checked `if (state.status === 'running')` before checking `state.pauseId`, resetting `currentPauseId` to `null` on running test instances even when a valid pause token was active.
     - Multi-action step warnings (`hasMultipleActions`) were dropped during the component rewrite, preventing `.substep-warning-info` from rendering on composite action failures.
  4. **Spec Evolution Drift in Unit Tests:** `PlaybookParser` split lifecycle blocks into `beforeSteps`, `steps`, and `afterSteps`, while `QualityJudgePrompt` updated its locator rules to Playwright text matchers without updating the legacy assertion strings in the respective test suites.
- **Detection Gap ("What did we miss?"):**
  - Continuous integration workflows running isolated sub-modules did not run the full root `non-llm-tests` profile against the top-level repo workspace, masking the interplay between root `config/ai.properties` and core unit tests.
  - Interactive console UI tests lacked automated coverage for the collapsed secondary details toggle and the SSE handshake state machine when `pushState()` vs. `broadcastSseEvent()` is invoked.
- **Resolution:**
  - Added null/empty safeguard `if (flatSteps == null || flatSteps.isEmpty()) return -1;` to `ExecuteActionsStep.java` and removed duplicate dead identity iteration.
  - Restored default properties in `config/ai.properties` (`neodymium.ai.tokenBudget.input = 500000`, commented `# neodymium.ai.step.maxTokens = 100000`, commented `# neodymium.ai.step.maxTurns = 15`), allowing internal framework defaults to govern baseline unit tests.
  - In `interactive_console.js`:
    - Fixed Back button check to use `allStepIndices[0]` dynamically.
    - Updated `applyState(state)` to prioritize `state.pauseId` over `state.status === 'running'`, preserving active pause tokens during state re-renders.
    - Restored multi-action check to `isSubstep` for displaying `.substep-warning-info` on composite step failures.
  - In `InteractiveConsoleEngine.java`: Added `hasSseClients()` to allow deterministic waiting for active client streams.
  - In UI tests:
    - Updated `InteractiveReasoningAndActionsUiTest` to expand `.step-more-details-btn` before inspecting `.llm-communications-section`.
    - Updated `InteractiveViewStandaloneUiTest` to wait on `consoleEngine.hasSseClients()` and use `pushState()` for state broadcasts.
  - In unit tests: Updated `PlaybookParserTest` and `QualityJudgePromptTest` assertions to align with current architectural specifications.
- **Safety Net Added:**
  - Verified across `ExecuteActionsStepTest`, `PropertyPrecedenceOrderTest`, `TokenBudgetGuardTest`, `InteractiveViewStateTest`, `InteractiveViewStandaloneUiTest`, `InteractiveReasoningAndActionsUiTest`, `PlaybookParserTest`, `QualityJudgePromptTest`, `StateMachineRunnerTest`, and `RunnerIntegrationTest` (100/100 tests passing).
  - Protected under full Maven test profile `mvn test -Pnon-llm-tests`.
