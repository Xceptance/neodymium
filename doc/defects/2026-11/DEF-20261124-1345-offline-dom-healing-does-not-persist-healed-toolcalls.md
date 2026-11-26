# [DEF-20261124-1345] Offline DOM Self-Healing Does Not Persist Healed ToolCalls and Never Converges to Clean Replay

- **Status:** `Resolved`
- **Opened:** 2026-11-24 13:45
- **Closed:** 2026-11-24 13:49
- **Component:** `neodymium-core` (`PlaybookToolReplayer`)
- **Scope:** `Core Engine`
- **Symptom:**
  When `REPLAY_WITH_HEALING` healed a changed locator offline via `DomFeatureVector` similarity (without calling an LLM), every subsequent test run triggered offline DOM healing again and stayed in `HEALED` status forever instead of converging back to clean `RECORDING` / `SUCCESS` replay.
- **Root Cause:**
  1. `PlaybookToolReplayer.syncStepActionOnSuccess(...)` updated `step.getActions().get(callIndex)` with the healed selector, `expectedTarget`, and `healed = true`, **but never updated `step.getToolCalls()` or `domFeatureVector`**.
  2. When `PlaybookRecorder` overwrote the playbook JSON on disk after the passing healed run, `toolCalls` still contained the broken pre-healing selector and stale `DomFeatureVector`, so `PlaybookToolReplayer.replayStep(...)` replayed the broken `toolCalls` entry and re-ran DOM similarity healing on every subsequent run.
  3. On a clean unhealed replay (`isHealed == false`), `syncStepActionOnSuccess(...)` also left stale `healed = true`, `expectedTarget`, `originalTarget`, and `healedFrom` fields on `Action`.
- **Detection Gap ("What did we miss?"):**
  Existing tests in `PlaybookToolReplayTest` verified the executed call arguments on a single replay call (and three multi-phase tests inadvertently depended on `step.getToolCalls()` remaining unmutated across calls), never asserting that `step.getToolCalls()` was persisted with the healed selector while preserving `${variable}` placeholders or verifying a two-run convergence back to `PlaybookStepStatus.SUCCESS`.
- **Resolution:**
  1. Iterated over a defensive copy `new ArrayList<>(step.getToolCalls())` in `PlaybookToolReplayer.replayStep(...)`.
  2. Attached the matched live `DomFeatureVector` (`best`) in `PlaybookToolReplayer.attemptHealing(...)` via `buildHealedToolCall(...)`.
  3. Extended `syncStepActionOnSuccess(...)` (across all 4 execution sites) with `rawCall` and added `syncHealedToolCall(...)` to clone `rawCall.arguments()` (preserving `${variable}` placeholders) while updating locator/coordinate/DOM-vector fields into `step.setToolCalls(...)`, syncing `domFeatureVector` onto `Action`/`PlaybookStep`, and clearing stale `healed`/`expectedTarget`/`originalTarget`/`healedFrom` markers on clean replays (`!isHealed`).
- **Safety Net Added:**
  - `PlaybookToolReplayTest#testOfflineDomHealingUpdatesToolCallsWhilePreservingVariablePlaceholders`
  - `PlaybookToolReplayTest#testOfflineHealedStepConvergesToCleanSuccessOnSubsequentReplay`
  - Verified with `mvn test -pl neodymium-core -Dtest=PlaybookToolReplayTest,PlaybookToolReplayerTest,CoordinateReplayGuardTest,PlaybookRecorderTest,ExecuteActionsStepTest` and `mvn install -DskipTests -Dmaven.javadoc.skip=true -pl neodymium-core,aura-manager`.
