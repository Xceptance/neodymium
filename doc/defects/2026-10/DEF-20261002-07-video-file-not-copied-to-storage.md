# [DEF-20261002-07] Video File Not Copied to storage/runs Storage Directory During Test Teardown

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`InteractiveConsoleEngine.java`)
- **Scope:** `Framework`
- **Symptom:** Test runs triggered from Aura Manager (such as `run_20261002_131742`) created `storage/runs/<runId>/<testClass>/console-execution-1.json` but no video file was copied to `storage/runs/<runId>/<testClass>/video-1.mp4`.
- **Root Cause:** `InteractiveConsoleEngine.attachVideoToLatestExecutionLog` only populated `getConsoleExecutionLogsDirectory()` (`target/aura-sandbox/allure-results`) and `getDiskReportDirectory()` (`target/ai-reports`), omitting `storage/runs`.
- **Detection Gap ("What did we miss?"):** Integration tests checked video relocation in `target/ai-reports` or `target/aura-sandbox/allure-results` without verifying `storage/runs` root storage.
- **Resolution:** Updated `InteractiveConsoleEngine.attachVideoToLatestExecutionLog` to discover all active storage root directories (including `storage/runs`), copy the video into `<storageRoot>/<runFolder>/<testClass>/video-1.mp4`, and update `console-execution-*.json` across all storage targets.
- **Safety Net Added:** Added unit test coverage verifying video relocation and JSON tagging across `storage/runs` and `target/ai-reports`.
