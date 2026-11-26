# [DEF-20260919-06] Replay Failure on Compound Step with Coalesced Actions and Missing 'submit' Keyword in partitionToolCallsAndActions

- **Status:** Resolved
- **Opened:** 2026-09-19
- **Closed:** 2026-09-19
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-runner`)
- **Scope:** `Framework`
- **Symptom:** `CartTest.liveNormal` succeeds with an expected bug, but its recorded playbook fails during `CartTest.replayNormal` in `REPLAY_STRICT` mode with: `No recorded tool calls found for step 'Submit the promo code form.' in REPLAY_STRICT mode. Companion JSON recording file is missing or step was not recorded.`
- **Root Cause:**
  1. `matchesSubStep` in `AgentToolLoopStep` checked keywords `click`, `press`, `select`, `choose`, `add`, but omitted `submit`. The submit button click was not matched to 'Submit the promo code form.' and fell back to 'type 'FREEGIFT' into it', leaving the submit sub-step with 0 recorded tool calls.
  2. In `ExecuteActionsStep`, unrolled child sub-steps of a compound step were strictly required to have recorded tool calls in `REPLAY_STRICT` mode, failing on legitimate coalesced sub-steps where a single preceding action (e.g. `fill` clearing and typing) satisfied multiple milestones.
  3. When an assertion failed in `AgentToolLoopStep`, Stop Criterion 2 re-threw `AssertionError` before appending the in-flight tool call to `executedCalls`, preventing the failed assertion from being recorded and partitioned to its sub-step in the companion JSON.
- **Detection Gap ("What did we miss?"):** Existing compound turn group tests only tested 1:1 sub-step-to-tool-call mappings and did not verify replay unrolling of compound steps with coalesced sub-steps or expected defect assertions.
- **Resolution:**
  1. Added `submit` to `matchesSubStep` for `click` in `AgentToolLoopStep`.
  2. Recorded the in-flight assertion `ToolCall` and mapped failed `Action` in `executedCalls` when Stop Criterion 2 triggers in `AgentToolLoopStep`, ensuring failed assertions are captured in companion JSON playbooks.
  3. Updated `ExecuteActionsStep` to allow child sub-steps of a compound parent (`step.getParent() != null`) with 0 tool calls to complete as coalesced no-ops in `REPLAY_STRICT` mode instead of throwing `ConclusiveFailureException`.
- **Safety Net Added:** Unit regression tests in `AgentToolLoopStepTest` (verifying `submit` matching and failed assertion recording) and `ExecuteActionsStepTest` (verifying unrolled compound replay with coalesced sub-steps).
