# [DEF-20261006-1645] Playbook Content Hash Invalidation and Visual Gate Online Healing Bypass Failure

- **Status:** `Resolved`
- **Opened:** 2026-10-06 16:45
- **Closed:** 2026-10-06 16:56
- **Component:** `neodymium-core`
- **Scope:** `Framework & Doc/Spec`
- **Symptom:**
  1. Authoring step `Verify checkout button (visual: min-score: 0.98)` in unquoted YAML threw SnakeYAML `mapping values are not allowed here` because plain YAML scalars cannot contain multiple colons (`: `).
  2. Modifying the YAML playbook parameter to `(visual: min-score=0.98)` had no effect in subsequent test executions because `NeodymiumAiRunner` unconditionally preferred the existing companion JSON recording without invalidating stale recordings when the source YAML content hash changed.
  3. During visual replay divergence (`0.9870 < 0.99`), `VisualBaselineGateStep.executeGate()` threw `HealingRequiredException`. However, because `visualBaselineGateStep.executeGate()` was invoked outside the `TryCatchStep` pipeline in `ExecuteActionsStep`, the exception escaped uncaught and crashed the test instead of triggering online visual healing with `AgentToolLoopStep`.
- **Root Cause:**
  1. `NeodymiumAiRunner` computed source YAML hashes for informational logging into `KEY_YAML_MISMATCH_WARNING` but never discarded candidate JSON recordings upon hash divergence in `AUTO` or `REPLAY_WITH_HEALING` modes, nor aborted in `REPLAY_STRICT`.
  2. `computeResourceSha256` hashed raw bytes without line ending normalization, risking false-positive hash mismatches across Git platforms with CRLF vs LF checkouts.
  3. `ExecuteActionsStep.execute` called `visualBaselineGateStep.executeGate(contextState)` prior to constructing and pushing `tryCatch` and `endHookStep`, so `HealingRequiredException` escaped uncaught out of the pipeline step execution.
- **Detection Gap ("What did we miss?"):**
  Existing tests in `NeodymiumAiRunnerTest` verified candidate discovery when recordings existed, but did not test scenarios where the source YAML was modified after recording generation. In `ExecuteActionsStepTest`, tests checked `TryCatchStep` exception handling during normal tool execution, but did not test the early `visualBaselineGateStep.executeGate()` visual verification path for `HealingRequiredException` recovery.
- **Resolution:**
  1. Updated `computeResourceSha256` to normalize line endings (`\r\n` and `\r` to `\n`) before calculating SHA-256 digests.
  2. In `NeodymiumAiRunner`, added `readRecordingSourceYamlHash` and cross-checked source YAML and companion JSON hashes prior to mode dispatch:
     - In `AUTO` and `REPLAY_WITH_HEALING`, discarded stale recordings, logged warnings to `KEY_EXECUTION_WARNINGS`, and fell back to `LLM_RECORDING` to regenerate the recording.
     - In `REPLAY_STRICT`, threw `IllegalStateException` preventing execution against stale baselines.
     - In recording modes (`LLM_RECORDING`, `FORCE_RECORDING`), skipped candidate hash checks.
  3. In `ExecuteActionsStep`, wrapped `visualBaselineGateStep.executeGate(contextState)` in a try-catch block for `HealingRequiredException`, routing to `endHookStep`, `verifyStep`, and `AgentToolLoopStep` when healing is supported and enabled.
  4. Updated `DOCUMENTATION.md` to recommend `=` for directive parameters (e.g. `(visual: min-score=0.98)`) and documented the SnakeYAML unquoted colon trap.
- **Safety Net Added:**
  - `NeodymiumAiRunnerTest.testComputeResourceSha256LineEndingInvariance`
  - `NeodymiumAiRunnerTest.testAutoModeDiscardsStaleRecordingWhenSourceYamlHashDiffers`
  - `NeodymiumAiRunnerTest.testReplayStrictThrowsWhenSourceYamlHashDiffers`
  - `ExecuteActionsStepTest.testReplayWithHealingTriggersAgentToolLoopStepOnVisualBaselineMismatch`
