# [DEF-20261116-1035] Queue Counter Multi-Directory Ingestion and Cancellation Phantom YAML Test

- **Status:** `Resolved`
- **Opened:** 2026-11-16 10:35
- **Closed:** 2026-11-16 10:41
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:**
  1. Live execution counters inflate beyond scheduled tests (e.g. "12 of 9 tests (1 running)").
  2. Cancelling an active YAML test creates an extra phantom test under class `yaml` with status `skipped`, inflating report test totals from N to N+1.
- **Root Cause:**
  1. `AuraQueueService.ingestBatchExecutionFiles` scans both `storage/runs/` and `target/aura-sandbox/allure-results/` using `ingestedExecutionFiles.contains(execFile.getAbsoluteFile())`. Because paths differ between the two directories, the set check fails to deduplicate, incrementing `globalTestsRun` multiple times for the same execution.
  2. `InteractiveConsoleEngine.extractTestClassFolder` uses `tn.substring(tn.lastIndexOf('.') + 1)`. When `testName` is a YAML path (e.g., `tests/posters_negative_login.yaml`), this truncates the path to `"yaml"` and writes orphan execution JSON files to `<runId>/yaml/`.
  3. `AuraQueueService.stopCurrentTest()` pushed cancellation state with `"testName": activeFileStr` without providing an explicit sanitized runner class name.
- **Detection Gap ("What did we miss?"):** Tests for `AuraQueueService` and `InteractiveConsoleEngine` ran against single isolated directory fixtures and tested only standard Java class names (e.g. `MyClassTest`), never asserting deduplication across dual storage trees or checking cancellation of YAML-based playbooks.
- **Resolution:**
  1. Deduplicate in `AuraQueueService.ingestBatchExecutionFiles` using canonical relative keys (`className + "/" + execFile.getName()`).
  2. Enhance `InteractiveConsoleEngine.extractTestClassFolder` to detect file extensions (`.yaml`, `.yml`, `.steps`) and resolve the canonical runner class name (`Aura_<sanitized>_Test`).
  3. Ensure `AuraQueueService.stopCurrentTest()` supplies the active batch runner class name in cancellation payloads.
- **Safety Net Added:**
  - `AuraQueueServiceTest#testIngestBatchExecutionFilesDeduplicatesAcrossMultipleStorageRoots`
  - `StructuredFolderExecutionTest#testExtractTestClassFolderFromYamlTestPath`
  - `StructuredFolderExecutionTest#testExtractTestClassFolderFromRawYamlString`
  - `StructuredFolderExecutionTest#testExtractTestClassFolderFromTestClassAttribute`
