# [DEF-20261124-1302] InteractiveConsoleEngine Cross-Run Folder Overwrite and Stale report-manager.js Browser Cache

- **Status:** `Resolved`
- **Opened:** 2026-11-24 13:02
- **Closed:** 2026-11-24 13:08
- **Component:** `neodymium-core` (`InteractiveConsoleEngine`, `AuraInteractiveService`), `aura-manager` (`index.html`)
- **Scope:** `Core Engine` & `UI / Report Rendering`
- **Symptom:**
  1. When multiple test runs executed sequentially inside the same Manager JVM (for example `run_20261124_124651` followed by `run_20261124_125132` and `run_20261124_125305`), inspecting `run_20261124_124651` showed `0 LLM call(s)`, `Start Timestamp` from `run_20261124_125305` (`12:54:03` / `5,980 ms`), and offline-healed selector metadata (`#searchField -> #searchInput`) even though `run.json`, `execution-1.log`, and the `12:47:58` preliminary report recorded `1` LLM call (`20,092` tokens, `$0.0061`).
  2. Browser tabs that had previously loaded `aura-manager` kept serving the pre-fix `report-manager.js?v=1.1.7`, displaying `PENDING`, `-` duration, and `0` counts on healed steps even after `neodymium-manager` was restarted.
- **Root Cause:**
  1. `InteractiveConsoleEngine` initialized `private static final String RUN_FOLDER = initializeRunFolder()` once per JVM when loaded for the first run (`run_20261124_124651`). `AuraInteractiveService.pushState(...)` updated `engine.setRunId(incomingRunId)` and called `engine.pushState(body)`, but `InteractiveConsoleEngine.saveLocalExecutionLog(...)` still read static `getRunFolder()` instead of resolving `runFolder` from `parsedState.get("runId")`, causing every later run in the same Manager JVM to overwrite the first run's `console-execution-1.json` in `storage/runs/` and `target/ai-reports/`. Additionally, `AuraInteractiveService.resetExecutionIndexes()` cleared only its own maps and did not reset `InteractiveConsoleEngine.resetExecutionIndexes()`.
  2. `aura-manager/src/main/resources/templates/index.html` retained query cache-buster `?v=1.1.7` after edits to `report-manager.js`.
- **Detection Gap ("What did we miss?"):**
  Existing unit tests in `InteractiveConsoleEngineTest` and `AuraInteractiveServiceTest` pushed single-run payloads or omitted `"runId"` from state JSON on a shared `InteractiveConsoleEngine`, so sequential multi-run persistence into distinct `runId` folders was never exercised.
- **Resolution:**
  1. Trimmed raw IDs in `InteractiveConsoleEngine.normalizeRunFolder(String)` and extracted `extractPayloadRunFolder(JsonObject)`; updated `InteractiveConsoleEngine.saveLocalExecutionLog(...)` to resolve `runFolder` from `parsedState.get("runId")` when present (falling back to `getEffectiveRunFolder()` when absent) and scope execution indexing by `payloadRunFolder + "::" + testClassFolder`.
  2. Synchronized `AuraInteractiveService.resetExecutionIndexes()` with `InteractiveConsoleEngine.resetExecutionIndexes()` (aligned with upstream multi-run lifecycle hardening from PR #574).
  3. Bumped `report-manager.js` cache-buster in `aura-manager/src/main/resources/templates/index.html` from `?v=1.1.7` to `?v=1.1.9` and updated the assertion in `StaticResourceSyntaxTest`.
- **Safety Net Added:**
  - `InteractiveConsoleEngineTest#testPushStateWithDistinctRunIdsIsolatesRunFoldersAndPreservesEarlierRun`
  - `AuraInteractiveServiceTest#testPushStateAcrossDistinctRunIdsIsolatesRunFoldersAndPreservesEarlierRun`
  - Updated `StaticResourceSyntaxTest#testRunReportStatusFilterStateTransitions` cache-buster assertion (`?v=1.1.9`)
  - Verified with `mvn test -pl neodymium-core -Dtest=InteractiveConsoleEngineTest,AuraInteractiveServiceTest`, `mvn test -pl aura-manager -Dtest=StaticResourceSyntaxTest`, and `mvn install -DskipTests -Dmaven.javadoc.skip=true -pl neodymium-core,aura-manager`.
