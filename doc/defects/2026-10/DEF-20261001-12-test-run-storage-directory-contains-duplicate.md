# [DEF-20261001-12] Test Run Storage Directory Contains Duplicate Dummy console-execution-1.json Files Beside Higher-Indexed Files

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `neodymium-core` (`InteractiveConsoleEngine`, `AuraInteractiveService`, `AuraQueueService`)
- **Scope:** `Framework`
- **Symptom:** Run storage folders for subsequent test classes in a batch contain both `console-execution-1.json` (a dummy 0-step fallback file) and `console-execution-3.json` (the actual test log).
- **Root Cause:** `InteractiveConsoleEngine` and `AuraInteractiveService` used global counters across all test classes in a batch run to index `console-execution-*.json` files. Subsequent test classes received indexes > 1 (e.g. 3). Later, `AuraQueueService` checked for index 1 in the class folder, saw it missing, and created a dummy fallback `console-execution-1.json`. Furthermore, `markRunningOrMissingExecutionsAsSkipped` failed to mark existing completed execution snapshots as processed, triggering fallback creation.
- **Detection Gap ("What did we miss?"):** Tests verified multi-dataset indexing within a single test class, but did not assert per-class file indexing boundaries across multi-class batch execution.
- **Resolution:** Refactored `InteractiveConsoleEngine` and `AuraInteractiveService` to map execution indexes per test class folder. Updated `AuraQueueService.markRunningOrMissingExecutionsAsSkipped` to recognize existing execution files with final statuses.
- **Safety Net Added:** Added unit tests verifying per-class execution file indexing in `InteractiveConsoleEngineTest`.
