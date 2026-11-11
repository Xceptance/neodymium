# [DEF-20261111-0526] Test Execution Video Plays 0:00 Duration in Aura Report

- **Status:** Resolved
- **Opened:** 2026-11-11 05:15
- **Closed:** 2026-11-11 05:30
- **Component:** `aura-manager` (`AuraReportViewController.java`, `report-manager.js`), `neodymium-core` (`VideoWriter.java`, `NeodymiumAiRunner.java`)
- **Scope:** `Framework`
- **Symptom:** Videos attached to test executions in Aura Manager report display 0:00 seconds and fail to play or scrub.
- **Root Cause:** In the test execution table, the row attribute `data-test-class` uses the fully qualified class name (FQCN), whereas run directories under `storage/runs/{runId}/` are named using simple class names. `AuraReportViewController.serveRunFile` attempted to locate the video using the FQCN path `storage/runs/{runId}/{testClass}/{filename}`. Upon failing, it fell back to an unindexed file under `target/aura-sandbox/allure-results/video-1.mp4`, which was incomplete and lacked the MP4 `moov` atom header. Additionally, FFmpeg did not specify `-movflags +faststart` during encoding and framerate adjustment, and `NeodymiumAiRunner` spawned duplicate recording threads concurrently with `BrowserRunner`.
- **Detection Gap ("What did we miss?"):** Previous controller tests mocked file paths using simple names or explicit existing paths without testing FQCN paths or inspecting browser playback of fallback artifacts. Furthermore, integration tests verified file creation on disk without validating that `-movflags +faststart` placed the `moov` atom at the beginning of the container.
- **Resolution:** 
  1. Updated `AuraReportViewController.serveRunFile` and `report-manager.js` to normalize FQCNs to simple class names when resolving execution media and removed the unindexed root fallback to `target/aura-sandbox/allure-results/{filename}`.
  2. Added `-movflags +faststart` to FFmpeg commands in `VideoWriter` for both initial piping and framerate adjustment.
  3. Added an active recordings guard in `NeodymiumAiRunner.executeBeforeEach` to prevent concurrent duplicate video recordings when `BrowserRunner` has already initiated filming.
- **Safety Net Added:** Added unit test `AuraReportViewControllerTest.testServeRunFileWithFqcnNormalizedToSimpleClassName` verifying that FQCN paths resolve to simple class name directories, and updated `VideoWriterTest` / `VideoWriter` process builder assertions.
