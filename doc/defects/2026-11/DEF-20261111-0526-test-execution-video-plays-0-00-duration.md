# [DEF-20261111-0526] Test Execution Video Plays 0:00 Duration in Aura Report

- **Status:** `Resolved`
- **Opened:** 2026-11-11 05:15
- **Closed:** 2026-11-11 11:22
- **Component:** `aura-manager` (`AuraReportViewController.java`, `report-manager.js`), `neodymium-core` (`VideoWriter.java`, `NeodymiumAiRunner.java`), `neodymium-classic` (`VideoWriter.java`)
- **Scope:** `Framework & Infra/Build`
- **Symptom:** Videos attached to test executions in Aura Manager report display 0:00 seconds and fail to play or scrub. Furthermore, clicking on non-video attachments (such as screenshots) in the side panel could be hijacked by test video URLs.
- **Root Cause:**
  1. In the test execution table, the row attribute `data-test-class` used the fully qualified class name (FQCN), whereas run directories under `storage/runs/{runId}/` are named using simple class names.
  2. `AuraReportViewController.serveRunFile` attempted to locate the video using the FQCN path `storage/runs/{runId}/{testClass}/{filename}`. Upon failing, it fell back to an unindexed file under `target/aura-sandbox/allure-results/video-1.mp4`, which was incomplete and lacked the MP4 `moov` atom header.
  3. FFmpeg encoding commands lacked `-movflags +faststart`, preventing HTML5 video players from streaming before full download.
  4. An artificial `[0.5, 60.0]` clamp on framerate rewrote playback durations instead of preserving the exact duration of the test.
  5. Initial FFmpeg process timeout terminated without cleaning up the partial video file on disk, leaving a 0-duration file.
  6. In `NeodymiumAiRunner`, duplicate video recording threads were triggered concurrently with `BrowserRunner` when both automatic recording and AI runner recording hooks ran.
  7. In `report-manager.js`, `openAttachmentWindow` evaluated `data-video-url` before `url`, and class name normalization was duplicated across functions with an arbitrary `'DefaultTestClass'` fallback literal.
- **Detection Gap ("What did we miss?"):** Previous controller tests mocked file paths using simple names or explicit existing paths without testing FQCN paths or inspecting browser playback of fallback artifacts. Furthermore, integration tests verified file creation on disk without validating that `-movflags +faststart` placed the `moov` atom at the beginning of the container, or verifying that framerate calculation preserves actual test duration without clamping.
- **Resolution:** 
  1. Updated `AuraReportViewController.serveRunFile` and `report-manager.js` to normalize test identifiers via `resolveSimpleTestClassName`, eliminate `'DefaultTestClass'`, enforce RFC 5987 / UTF-8 `ContentDisposition`, add `X-Content-Type-Options: nosniff`, validate path segments against traversal, and remove dead sandbox checks.
  2. Added `-movflags +faststart` to FFmpeg commands in `VideoWriter` for both initial piping and framerate adjustment in both `neodymium-core` and `neodymium-classic`, removed the artificial `[0.5, 60.0]` framerate clamp to ensure video duration matches the real test duration, cleaned up partial files on initial recording timeout, and restored original videos on adjustment failure.
  3. Added an invocation-scoped active recording guard in `NeodymiumAiRunner.executeBeforeEach` (`this.browser != null && filmAutomatically()`) to prevent concurrent duplicate video recordings when `BrowserRunner` has already initiated filming without risking thread cross-talk in parallel test execution.
  4. Deduplicated class-name normalization in `report-manager.js` into `resolveSimpleTestClassName(element)` and made `VideoWriter.getProcessBuilder()` package-private.
- **Safety Net Added:** Added unit tests `AuraReportViewControllerTest.testServeRunFileWithFqcnNormalizedToSimpleClassName`, `testServeRunFileWithMethodSeparatorNormalizedToSimpleClassName`, `testServeRunFilePathTraversalReturns404`, `testServeRunFileDirectoryReturns404`, and `testServeRunFileContentTypes`, added `org.neodymium.common.recording.writers.VideoWriterTest.testFfmpegCommandIncludesFaststartFlag` and `testFramerateAdjustmentIncludesFaststartFlag`, and added `NeodymiumAiRunnerTest.testAiInvocationExtensionDoesNotDuplicateBrowserRunnerFilming`.
