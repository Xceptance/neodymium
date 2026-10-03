# [DEF-20260928-06] Programmatic AiSession Replay Drops Step Status From Companion JSON Breaking 0-Action Step Replay

- **Status:** Resolved
- **Opened:** 2026-09-28
- **Closed:** 2026-09-28
- **Component:** `neodymium-core` (`ai-session` / `playbook-replay` / `programmatic-execution`)
- **Scope:** `Framework`
- **Symptom:** Programmatic tests (`session.execute(...)`) containing steps recorded with 0 actions (such as verification steps completed via `complete_step` or conditional branches) passed during `FORCE_RECORDING`, but failed in `REPLAY_STRICT` with `ConclusiveFailureException: No recorded tool calls found for step '...' in REPLAY_STRICT mode`, and in `REPLAY_WITH_HEALING` by triggering unexpected live LLM fallback calls and failing replay metrics.
- **Root Cause:** In `AiSession.java`, step merging during replay copied `actions`, `toolCalls`, and hashes from `sessionSteps` to parsed inline steps, but omitted `status`, `failed`, and `failureReason`. Consequently, `parsed.getStatus()` remained `PlaybookStepStatus.PENDING`, causing `ExecuteActionsStep`'s `isRecordedCompletedStep` check to evaluate to `false`.
- **Detection Gap ("What did we miss?"):** File-based playbooks loaded their step status directly from JSON via `YamlPlaybookParser`, satisfying `isRecordedCompletedStep`. Programmatic test suites lacked unit tests verifying step status preservation across inline playbooks with 0-action steps.
- **Resolution:** Added copying of `status`, `failed`, and `failureReason` in `AiSession.java` step merging, and updated Step 9 of `ForwardIntegrationTest_testForwardSynonyms_Chrome_headless.json` with an explicit `assert_title` call.
- **Safety Net Added:** Added unit regression assertions in `AiSessionReplayTest` confirming that 0-action recorded steps retain `status` and replay successfully without triggering live LLM execution or `ConclusiveFailureException`.
