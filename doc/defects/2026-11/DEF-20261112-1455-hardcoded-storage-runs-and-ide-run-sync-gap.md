# [DEF-20261112-1455] Hardcoded Storage Runs Path and IDE Run Synchronization Gap

- **Status:** `Resolved`
- **Opened:** 2026-11-12 14:55
- **Closed:** 2026-11-12 15:20
- **Component:** `neodymium-core` / `aura-manager`
- **Scope:** `Framework & Config/Environment`
- **Symptom:** Tests executed within IDEs (IntelliJ IDEA, Eclipse, VS Code) saved execution JSONs to `target/aura-sandbox/allure-results/` but did not synchronize to the base directory monitored by Aura Manager (`storage/runs`), leaving IDE runs invisible in Aura Manager without manual directory copying. Additionally, `"storage/runs"` was hardcoded across multiple engine and service classes (`InteractiveConsoleEngine`, `AuraInteractiveService`, `AuraQueueService`), preventing custom storage base directory configurations from taking effect.
- **Root Cause:**
  1. `InteractiveConsoleEngine.java` directly wrote execution logs only to `allure.results.directory`, `neodymium.ai.consoleExecutionLogs.directory`, and `neodymium.ai.diskReport.directory`. It lacked a dual-write mechanism targeting the persistent Aura run directory.
  2. The path `"storage/runs"` was hardcoded across `InteractiveConsoleEngine.java`, `AuraInteractiveService.java`, and `AuraQueueService.java` instead of querying `AiConfiguration`.
  3. Aura Manager's `AuraReportDataService.java` expected test runs strictly in `aura.report.storage.runs.base-dir` (`storage/runs`), while Neodymium lacked a corresponding unified configuration property and attachment synchronization for custom developer artifacts, execution logs, and recordings.
- **Detection Gap ("What did we miss?"):**
  Existing test suites (`InteractiveConsoleEngineTest`, `RunStorageSyncServiceTest`) tested local directory writes and storage sync independently using mocked or isolated directory paths, never validating the cross-component flow where an IDE run saves state and is expected to be immediately queryable by Aura Manager's storage directory resolver.
- **Resolution:**
  1. Added `AiConfiguration#getAuraStorageRunsDirectory()` reading `neodymium.ai.storageRuns.directory`, falling back to `aura.report.storage.runs.base-dir`, and defaulting to `"storage/runs"`.
  2. Replaced hardcoded `"storage/runs"` literals in `InteractiveConsoleEngine`, `AuraInteractiveService`, and `AuraQueueService` with `AiConfiguration.getInstance().getAuraStorageRunsDirectory()`.
  3. Added dual-writing in `InteractiveConsoleEngine#saveLocalExecutionLog` so that whenever `getAuraStorageRunsDirectory()` exists or is configured, execution states (`console-execution-*.json`), execution event logs (`execution.log`), playbooks, videos, and custom attachments are synchronized directly into `{auraStorageRunsDir}/{runId}/{testClass}/`.
  4. Added `ExecutionAttachment` model and `Neodymium.addAttachment(...)` API for unified artifact attachment handling with `openByDefault` variability.
  5. Updated `AuraReportDataService` to verify attachment disk existence prior to populating `dto.setAttachments(...)`.
- **Safety Net Added:**
  - `AiConfigurationTest.testAuraStorageRunsDirectoryResolution`
  - `InteractiveConsoleEngineTest.testDualWriteToAuraStorageRunsDirectoryAndCustomAttachments`
  - `NeodymiumAttachmentTest.testAddFileAttachment`, `testAddTextAttachment`, `testClearAttachments`
  - `TestExecutionDtoTest.testDeserializationWithAttachments`
