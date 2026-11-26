# [DEF-20260918-01] Premature Step Completion in Multi-Field Action Instructions Due to Single-Action Prompt Nudge

- **Status:** Resolved
- **Opened:** 2026-09-18
- **Closed:** 2026-09-18
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `GermanCheckoutTest.live` ([GermanCheckoutTest_live_perfect_20260918-002600.html](file:///home/rschwietzke/projects/GIT/neodymium-library/target/ai-results/GermanCheckoutTest_live_perfect_20260918-002600.html)), Step 18 (`Warte bis der Text "Thank you for your purchase!" erscheint.`) timed out with `AssertionError: Expected text/pattern "Thank you for your purchase!" was not found anywhere on the page within 3000ms`.
- **Root Cause:** In Step 16 (`Kartennummer ist '4111 1111 1111 1111', Ablaufdatum '12/29' und CVV ist '111'.`), the agent filled `#cardNumber` in Turn 1. In Turn 2, the post-action turn prompt introduced in DEF-20260917-01 (*"If this was an action instruction, invoke 'complete_step' now without performing uncommanded assertions or anticipating subsequent steps"*) combined with Operating Rule 4 (*"the step goal is completely satisfied once the action executes"*) caused the LLM to call `complete_step` prematurely, leaving `#cardExpiry` and `#cardCvv` empty. Clicking purchase in Step 17 failed client-side validation, so the confirmation page never loaded.
- **Detection Gap ("What did we miss?"):** DEF-20260917-01 tested atomic single-action steps to verify that the agent did not perform uncommanded assertions, but did not test compound action steps requiring multiple sequential `fill` or `click` actions within a single instruction.
- **Resolution:**
  1. Updated `AgentToolLoopStep` post-action turn prompt to state: *"If this was an action instruction and all actions/fields requested in the instruction have been executed, invoke 'complete_step' now without performing uncommanded assertions or anticipating subsequent steps. If the instruction explicitly requested additional fields or actions that have not yet been executed, continue executing the remaining actions."*
  2. Updated Operating Rule 4 in `AgentToolLoopStep` to clarify that multi-action instructions are satisfied only when all explicitly commanded actions/fields are fulfilled.
- **Safety Net Added:** Added regression unit test `AgentToolLoopStepTest#testMultiFieldActionInstructionReceivesRefinedTurnPromptAndAllowsSequentialExecution` verifying that a multi-field fill instruction continues across turns until all requested fields are filled before invoking `complete_step`.
