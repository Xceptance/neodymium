# [DEF-20261002-03] Report Video Attachment 404 Failure Due to Mismatched Test Class Folder Name in Run Storage

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`InteractiveConsoleEngine.java`), `aura-manager` (`AuraReportViewController.java`)
- **Scope:** `Framework`
- **Symptom:** Video recordings were missing in the report UI and returned HTTP 404 when requested via `/api/runs/{runId}/{testClass}/{filename}`.
- **Root Cause:** `InteractiveConsoleEngine.attachVideoToLatestExecutionLog` constructed target directories using raw test names with dataset suffixes (e.g. `WikipediaSearchTest___dataset_1`) instead of extracting the clean test class name (`WikipediaSearchTest`) required by `AuraReportViewController`.
- **Detection Gap ("What did we miss?"):** Unit tests created single dummy folders without verifying end-to-end HTTP URL resolution in `AuraReportViewController`.
- **Resolution:** Updated `InteractiveConsoleEngine.attachVideoToLatestExecutionLog` to use `extractTestClassFolder` to consistently resolve `<testClass>` for video storage and report attachment.
- **Safety Net Added:** Added unit test coverage verifying `/api/runs` URL structure and video file attachment in run folders.
