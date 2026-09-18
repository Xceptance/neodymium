# Defect Log & Post-Mortem Registry

This document tracks diagnosed defects, regressions, and behavioral bugs encountered during development and testing across Neodymium, its test suites, and connected Systems Under Test (SUTs).

The objective is to maintain an actionable learning record: to understand **why** defects occurred, identify detection gaps (**"what did we miss?"**), and ensure safety nets (regression tests, linters, or architectural assertions) prevent recurrence.

---

## Logging Guidelines & Criteria

### When to Log a Defect (In Scope)
- **Framework Regressions & Logic Bugs**: Any semantic bug in Neodymium core, AI engine, state machine, runners, parsers, or reporters.
- **Test Harness / Fixture Failures**: Faulty assertions, broken test setups, incorrect wait conditions, or selector fragility that caused false positives or false negatives.
- **SUT Behavioral Defects**: Confirmed functional, visual, or layout defects detected in the System Under Test (e.g., Verla demo store).
- **Silent Failures or Masked Exceptions**: Situations where errors were swallowed or misclassified.

### When NOT to Log (Out of Scope)
- **Normal TDD Red Phase**: Expected test failures during active test-first development prior to implementing the feature.
- **In-Progress Compilation / Syntax Typos**: Errors resolved during the immediate editing cycle.
- **Transient External Outages**: Temporary network loss, upstream LLM provider 503/429 quota limits, or local OS process termination.

---

## Defect Entry Template

When recording a defect, add a new entry directly under the [Active Defect Records](#active-defect-records) section in **reverse chronological order** (newest entries first).

```markdown
### [DEF-YYYYMMDD-01] Concise Description of Defect
- **Date:** YYYY-MM-DD
- **Component:** `neodymium-core` / `aura-visual` / `playbook-engine` / `verla-fixture` / etc.
- **Scope:** `Framework` | `Test/Harness` | `SUT`
- **Symptom:** Observed failure, error message, or unexpected behavior.
- **Root Cause:** Technical explanation of why the defect occurred.
- **Detection Gap ("What did we miss?"):** Why existing unit tests, linters, or type systems failed to catch this earlier.
- **Resolution:** Summary of code changes made to resolve the issue.
- **Safety Net Added:** Reference to the regression test, assertion, or linter rule preventing recurrence.
```

---

## Active Defect Records

### [DEF-20260918-03] Multi-Turn DOM Amnesia, Tool Thrashing, and Discovery Tool Action Pollution in Agent Tool Loop
- **Date:** 2026-09-18
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-tool`)
- **Scope:** `Framework`
- **Symptom:** In multi-field form steps (such as filling credit card number, expiry date, and CVV), the agent executed redundant discovery tools (`query_dom`), blind exploratory scrolling (`scroll down`), and re-querying across 7 turns. The read-only discovery tools and blind scrolls were erroneously recorded as persistent test actions in the execution report and playbook (`QUERY_DOM`, `SCROLL`).
- **Root Cause:**
  1. `pruneExpiredDomFromConversation` unconditionally wiped the DOM from Turn 1 without verifying if a replacement DOM was being provided for Turn 2 (`requireDomForNextTurn == false`), completely blinding the LLM of selectors.
  2. The continuation prompt actively nudged the model to call `query_dom`.
  3. `query_dom` used case-sensitive CSS selectors, missing camelCase attributes (`cardExpiry`, `cardCvv`), prompting an uncommanded off-screen scroll.
  4. `finishLoop` failed to filter out discovery/inspection tools (`query_dom`, `inspect`, `request_context`), polluting `PlaybookStep.actions` and `PlaybookStep.toolCalls`.
  5. Strict single-action serialization prevented the agent from proposing cohesive form field inputs in a single turn.
- **Detection Gap ("What did we miss?"):** Tests verified that `pruneExpiredDomFromConversation` pruned messages to save tokens, but did not verify that selector accessibility was maintained across multi-action turns or that discovery tools were excluded from recorded playbook actions.
- **Resolution:**
  1. Updated `pruneExpiredDomFromConversation` to preserve DOM/element selectors when no new DOM snapshot is added for the next turn.
  2. Excluded read-only discovery tools (`query_dom`, `inspect`, `request_context`, `inspect_visual`) from recorded `actions` and `toolCalls` in `finishLoop`.
  3. Enabled case-insensitive attribute fallback in `query_dom` JavaScript.
  4. Allowed cohesive form field action batching for multi-input instructions.
- **Safety Net Added:** Added unit and integration tests verifying that multi-field steps execute cleanly without amnesia, that discovery tools are not recorded as playbook actions, and that `query_dom` handles case-insensitive attribute matching.

### [DEF-20260918-02] Elimination of Fragile Natural Language Text Guessing in SemanticIntent
- **Date:** 2026-09-18
- **Component:** `neodymium-core` (`ai-model`, `ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** `SemanticIntent.inferFromInstruction` attempted to deduce step intent from natural language instruction text using hardcoded English substring patterns (`contains(" says now ")`, `contains(" is shown")`, `contains(" is mentioned")`), introducing fragile keyword-guessing heuristics into core Java logic and violating Neodymium's universal, language-neutral design.
- **Root Cause:** Following the removal of PESAP (which previously supplied LLM-classified intent), an ad-hoc keyword-matching method `inferFromInstruction` was introduced in commit `96b9c315f` to guess intent for unclassified steps, embedding English-specific phrasing assumptions into Java core logic.
- **Detection Gap ("What did we miss?"):** Unit tests only verified that `inferFromInstruction` matched specific predefined English test sentences, without verifying language neutrality or handling diverse phrasings.
- **Resolution:** Removed `SemanticIntent.inferFromInstruction` completely. Reverted `AgentToolLoopStep` to rely strictly on explicitly configured or recorded `SemanticIntent` (or `null` when unspecified), allowing the LLM's system prompt instructions to govern action vs. verification execution without heuristic second-guessing.
- **Safety Net Added:** Cleaned `SemanticIntentTest` to eliminate keyword inference tests; verified that `AgentToolLoopStepTest` executes cleanly without keyword guessing.

### [DEF-20260918-01] Premature Step Completion in Multi-Field Action Instructions Due to Single-Action Prompt Nudge
- **Date:** 2026-09-18
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `GermanCheckoutTest.live` ([GermanCheckoutTest_live_perfect_20260918-002600.html](file:///home/rschwietzke/projects/GIT/neodymium-library/target/ai-results/GermanCheckoutTest_live_perfect_20260918-002600.html)), Step 18 (`Warte bis der Text "Thank you for your purchase!" erscheint.`) timed out with `AssertionError: Expected text/pattern "Thank you for your purchase!" was not found anywhere on the page within 3000ms`.
- **Root Cause:** In Step 16 (`Kartennummer ist '4111 1111 1111 1111', Ablaufdatum '12/29' und CVV ist '111'.`), the agent filled `#cardNumber` in Turn 1. In Turn 2, the post-action turn prompt introduced in DEF-20260917-01 (*"If this was an action instruction, invoke 'complete_step' now without performing uncommanded assertions or anticipating subsequent steps"*) combined with Operating Rule 4 (*"the step goal is completely satisfied once the action executes"*) caused the LLM to call `complete_step` prematurely, leaving `#cardExpiry` and `#cardCvv` empty. Clicking purchase in Step 17 failed client-side validation, so the confirmation page never loaded.
- **Detection Gap ("What did we miss?"):** DEF-20260917-01 tested atomic single-action steps to verify that the agent did not perform uncommanded assertions, but did not test compound action steps requiring multiple sequential `fill` or `click` actions within a single instruction.
- **Resolution:**
  1. Updated `AgentToolLoopStep` post-action turn prompt to state: *"If this was an action instruction and all actions/fields requested in the instruction have been executed, invoke 'complete_step' now without performing uncommanded assertions or anticipating subsequent steps. If the instruction explicitly requested additional fields or actions that have not yet been executed, continue executing the remaining actions."*
  2. Updated Operating Rule 4 in `AgentToolLoopStep` to clarify that multi-action instructions are satisfied only when all explicitly commanded actions/fields are fulfilled.
- **Safety Net Added:** Added regression unit test `AgentToolLoopStepTest#testMultiFieldActionInstructionReceivesRefinedTurnPromptAndAllowsSequentialExecution` verifying that a multi-field fill instruction continues across turns until all requested fields are filled before invoking `complete_step`.

### [DEF-20260917-01] Premature Fast-Break in assert_text Wait Loop and Agent Over-Verification in Action Steps
- **Date:** 2026-09-17
- **Component:** `neodymium-core` (`ai-tool`, `ai-pipeline`, `ai-model`)
- **Scope:** `Framework`
- **Symptom:** During strict replay of recorded playbooks (such as `CheckoutTest.replay` on the `bad` dataset), Step 3 (`Locate the first product card and click its 'Add to Cart' or 'Add' button.`) failed unexpectedly with `Tool execution error in 'assert_text': Expected text "CART 1" was not found on element "#cart-btn-anchor"`, aborting execution before reaching Step 4 (`The mini cart quantity is now 1.`).
- **Root Cause:**
  1. Operating rules in `AgentToolLoopStep` universally commanded *"You MUST invoke an assertion tool before calling 'complete_step'"* without separating action steps from verification steps. During live recording, the LLM felt obliged to assert the cart counter after clicking, anticipating the next step and recording an uncommanded `assert_text` into Step 3's playbook.
  2. In `BrowserToolProvider.assert_text`, the polling retry loop contained a premature `if (isTextPresentOnPage(...)) break;`. When the asynchronous HTMX response (`api/cart/add`) began arriving and updated text anywhere on the page, the tool prematurely broke out of its retry loop rather than polling the target element up to `Configuration.timeout`.
- **Detection Gap ("What did we miss?"):** Existing `assert_text` unit tests verified immediate matches and page-wide fallbacks with mock drivers, but did not test asynchronous DOM mutation scenarios where text appears elsewhere on the page while the target element is still updating.
- **Resolution:**
  1. Removed the premature `isTextPresentOnPage(...) -> break` abort from `BrowserToolProvider.assert_text`, ensuring the tool polls the target element for the full `Configuration.timeout` duration.
  2. Differentiated action vs assertion execution in `AgentToolLoopStep` prompts (with keyword guessing subsequently eliminated in [DEF-20260918-02]).
  3. Restructured `AgentToolLoopStep` operating rules into distinct `Action steps` and `Verification steps` sections and updated post-action turn guidance to complete action steps immediately without uncommanded assertions.
- **Safety Net Added:**
  - `BrowserToolProviderStabilityTest#testAssertTextWaitsForAsyncTargetElementUpdateEvenIfTextIsPresentElsewhereOnPage` ensuring `assert_text` polls continuously until target element updates.

### [DEF-20260916-05] Complete Removal of PESAP Tracing & Reporting and Prompt Cleanliness
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-report`, `ai-runner`, `ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** Reports (HTML, Markdown, JSON) continued to render "PESAP (Pre-Execution Semantic Anchor)" category rows and badge cards with zero calls. StateMachineRunner and AiSession logged empty PESAP metrics in console banners, and AgentToolLoopStep appended conversational "What is your next tool call?" trailing prompts to user turns.
- **Root Cause:**
  1. HTML, Markdown, and JSON report generators retained hardcoded category rows, badges, and JavaScript/CSS handlers for PESAP metrics.
  2. StateMachineRunner, AiSession, and InteractiveStateBuilder retained legacy PESAP token accumulators and banner debug statements.
  3. AgentToolLoopStep retained conversational scaffolding ("What is your next tool call?") originally meant for text-completion agents rather than native function-calling APIs.
- **Detection Gap ("What did we miss?"):** Report unit tests asserted the presence of the PESAP row rather than validating that decoupled/deprecated phases are omitted from user-facing accounting.
- **Resolution:**
  1. Removed "PESAP (Pre-Execution Semantic Anchor)" row and all phase badge/JS logic from HtmlReportGenerator and MarkdownReportGenerator.
  2. Removed PESAP accumulation and console tracing from StateMachineRunner, AiSession, and InteractiveStateBuilder.
  3. Removed "What is your next tool call?" prompts from AgentToolLoopStep turns and updated DOM pruning to be content-boundary-based.
  4. Updated PreliminaryReportListenerTest to assert that PESAP is absent from report tables and JSON metrics.
- **Safety Net Added:** Automated assertions in `PreliminaryReportListenerTest#testReportTokenAccountingAndCategoryBreakdown` verifying absence of "PESAP" in HTML, Markdown, and JSON reports.

### [DEF-20260916-04] Silent Test Pass on Mismatched @AiDataSet Filter
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-junit`)
- **Scope:** `Framework`
- **Symptom:** Running test classes like `SearchGermanTest` resulted in `Tests run: 0, Failures: 0, Errors: 0, Skipped: 0` and reported Maven build success without executing any actual test steps.
- **Root Cause:**
  1. `NeodymiumAiRunner.provideTestTemplateInvocationContexts` filtered datasets against method- and class-level `@AiDataSet` annotations using `shouldIncludeDataSet`.
  2. When `@AiDataSet` specified dataset IDs or includes that did not match any dataset defined in the referenced playbook YAML (e.g. `@AiDataSet("perfect")` on `SearchGermanTest` vs playbook datasets `['US', 'DE', 'FIN']`), `filteredDataSets` resulted in an empty list.
  3. The runner returned an empty stream of `TestTemplateInvocationContext`, which JUnit 5 interpreted as 0 invocations, producing a silent green build.
  4. Multiple test classes (`SearchGermanTest`, `EnglishCheckoutTest`, and `RegisterTest` in `basic` and `full`) carried stale `@AiDataSet` annotations or dead test methods from earlier template copies.
- **Detection Gap ("What did we miss?"):** JUnit 5 `@TestTemplate` test engines do not consider zero invocations as an error by default. Test runners did not validate that explicit user-provided `@AiDataSet` filters matched at least one dataset in the playbook before generating test invocations.
- **Resolution:**
  1. Corrected `@AiDataSet("DE")` in `SearchGermanTest` and `@AiDataSet("canada-fr")` in `EnglishCheckoutTest`.
  2. Pruned dead test methods in basic and full `RegisterTest` that referenced non-existent datasets.
  3. Added fail-fast validation in `NeodymiumAiRunner.provideTestTemplateInvocationContexts` that throws `IllegalArgumentException` with available dataset IDs whenever explicit `@AiDataSet` filters match zero datasets.
- **Safety Net Added:** Unit test `NeodymiumAiRunnerTest#testUnmatchedDataSetThrowsException` verifying that unmatched `@AiDataSet` triggers fail-fast `IllegalArgumentException`.

### [DEF-20260916-03] PESAP Pipeline Decoupling & Direct Agent Execution Migration
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** Upfront PESAP (Pre-Execution Step Analysis & Partitioning) introduced mandatory out-of-band LLM calls prior to action execution, created architectural tight coupling with regex/language heuristics, and constrained instruction autonomy in `AgentToolLoopStep`.
- **Root Cause:** PESAP was originally designed as a speculative pre-classifier that partitioned steps and guessed context levels before seeing actual browser execution results. This architectural separation created redundancy with the agent tool loop and violated language neutrality by encouraging intent keyword sniffing and premature DOM exclusion hacks.
- **Detection Gap ("What did we miss?"):** Early AI test designs tested PESAP in isolation via `PesapPreStepTest`, rather than measuring end-to-end latency, multilingual robustness, and tool-loop adaptability without upfront classification overhead.
- **Resolution:**
  1. Decoupled `PesapPreStep` from `ExecuteActionsStep.mapPlaybookStepToPipelineStep()`, enabling direct tool loop execution for live recording steps.
  2. Deprecated `PesapPreStep`, `PesapPrompt`, and `isPesapEnabled()`, defaulting PESAP configuration to `false`.
  3. Streamlined `AgentToolLoopStep` Turn 1 SUT capture to universal `ContextLevel.LEAN` baseline and refined completion/verification prompt guidance.
- **Safety Net Added:** Comprehensive test suite execution across `AgentToolLoopStepTest` (38 tests) and `ExecuteActionsStepTest` (9 tests) ensuring direct agent autonomous execution succeeds without upfront PESAP calls.

### [DEF-20260916-02] Pre-Action Visual Baseline Check Prematurely Aborted Replay for Visual Steps with Mutating Actions
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `SearchTest_German_testSearchDeReplayDe_DE`, step 11 (`Auf der linken Seite wird eine Box mit Kategorien, Farben, Preis und Angebot angezeigt (visual).`) failed during replay with `DivergenceException: Visual SSIM score below threshold (score: 0.5352 < 0.99)` because the page was not scrolled down.
- **Root Cause:**
  1. During live recording, the LLM executed an interactive/viewport action `scroll(direction: "down", yOffset: 300)`. `VerifyOutcomeStep` captured the post-action screenshot at `scrollY = 300` and saved its SSIM matrix as `step.screenshotHash`.
  2. During replay, `VisualBaselineGateStep.executeGate()` was invoked before replaying step actions (at `scrollY = 0`).
  3. `VisualBaselineGateStep` assumed all visual steps (`step.isVisualStep()`) were pure verification steps and evaluated live pre-action screen state against `step.screenshotHash`. When the pre-action screen did not match the post-action baseline (SSIM 0.5352 < 0.99), line 235 threw a `DivergenceException` immediately, preventing `PlaybookToolReplayer.replayStep()` from ever executing the recorded `SCROLL` action.
- **Detection Gap ("What did we miss?"):**
  - Existing unit tests for `VisualBaselineGateStep` only tested visual steps with zero actions or with `ASSERT` actions (`Action("ASSERT", ...)`); none tested steps containing mutating or viewport actions like `SCROLL` or `CLICK`.
  - The pipeline assumed `step.isVisualStep()` implied a pure verification step with no mutating actions, ignoring cases where an LLM calls non-mutating tools or scroll actions during visual assertions.
- **Resolution:**
  1. Added `VisualBaselineGateStep.isPureVerification()` to distinguish steps that only contain assertions/NO-OPs from steps that contain recorded mutating actions or tool calls (`SCROLL`, `CLICK`, etc.).
  2. Updated `VisualBaselineGateStep.executeGate()` to defer visual baseline comparison when `!isPureVerification() && coordinateTarget == null`, allowing recorded actions to execute first without throwing pre-action divergence.
  3. Added `VisualBaselineGateStep.executePostActionCheck()` and wired it into `ExecuteActionsStep`'s `standardFlow` after action replay and post-step state settling/capture to verify the post-action visual outcome against the recorded baseline.
- **Safety Net Added:**
  - Added 4 unit tests in `VisualBaselineGateStepTest`:
    - `testReplayWithMutatingAction_preActionGateDoesNotThrowDivergence()`
    - `testReplayWithMutatingAction_postActionCheckWithMatchingBaseline_succeeds()`
    - `testReplayWithMutatingAction_postActionCheckWithDivergentBaseline_throwsDivergenceException()`
    - `testReplayWithMutatingAction_postActionCheck_supportsHealing_throwsHealingRequiredException()`
  - Added 2 integration tests in `ExecuteActionsStepTest`:
    - `testReplayVisualStepWithActionExecutesActionThenVerifiesPostActionVisualBaseline()`
    - `testReplayVisualStepWithActionThrowsDivergenceWhenPostActionVisualBaselineDiffers()`

### [DEF-20260916-01] Full-Page Visual Assertion Baseline Recorded As Viewport Capture During Multi-Turn Tool Loops
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `VerlaGuestCheckout_Pl_Polish_testCheckoutReplayPerfect`, step 19 (`Na środku ekranu znajduje się zielony znacznik wyboru (visual: full).`) failed during replay with SSIM 0.6878 against the recorded baseline (required >= 0.99), causing a `DivergenceException`.
- **Root Cause:**
  1. In `AgentToolLoopStep`, for visual assertions, `activeContextLevel` is `ContextLevel.VISUAL`, where `ContextLevel.isFullPageScreenshot()` evaluates to `false`. When an agent loop required multiple turns (e.g. recovering from an invalid tool call), subsequent turns refreshed page state using `executor.captureState(activeContextLevel, activeContextLevel.isFullPageScreenshot())`. This captured a 1500x857 viewport screenshot and overwrote `KEY_LAST_STATE`.
  2. In `VerifyOutcomeStep`, for steps without mutating DOM actions (`step.getActions().isEmpty()`), the pipeline previously discarded `KEY_POST_ACTION_STATE` (which had been freshly captured as full-page by `ExecuteActionsStep`) and fell back to `KEY_LAST_STATE` (overwritten with the viewport capture). It hashed that viewport capture while marking `step.setFullPage(true)`.
  3. During replay, `VisualBaselineGateStep` checked `step.isFullPageVisualStep()`, correctly captured a 1500x1122 full-page screenshot, and compared it against the recorded 1500x857 viewport hash, causing SSIM 0.6878 < 0.99 (`DivergenceException`).
- **Detection Gap ("What did we miss?"):**
  - Existing test `testVisualStepDoesNotConsumeStalePostActionState` in `VerifyOutcomeStepTest` specifically tested that viewport visual steps without actions discard `KEY_POST_ACTION_STATE` in favor of viewport `KEY_LAST_STATE`, but did not test full-page visual steps (`(visual: full)` or `step.isFullPageVisualStep()`).
  - Unit tests for `AgentToolLoopStep` did not verify the `isFullPage` flag passed to `captureState` during intermediate turns.
- **Resolution:**
  1. Updated `AgentToolLoopStep` lines 357, 1086, and 1195 so that initial, intermediate, and visual observation state captures evaluate `isFullPage` considering `(step != null && step.isFullPageVisualStep()) || Boolean.TRUE.equals(context.getTransientData().get("KEY_IS_FULL_PAGE_SCREENSHOT")) || activeContextLevel.isFullPageScreenshot()`.
  2. Updated `VerifyOutcomeStep` lines 116-124 to preserve and consume `KEY_POST_ACTION_STATE` whenever `isFullPageReq` is true, even when `step.getActions()` is empty.
- **Safety Net Added:**
  - Added unit test `testVisualStepWithFullPagePreservesPostActionState` in `VerifyOutcomeStepTest`.
  - Added unit test `testMultiTurnFullPageVisualStepPreservesFullPageCapture` in `AgentToolLoopStepTest`.
  - Added full-page capture tracking (`capturedFullPageFlags`) in `MockTargetExecutor`.
