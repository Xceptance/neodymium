# [DEF-20261111-1205] Aura Video Serving Lacks HTTP Range Streaming and Leaks Empty Video Files

- **Status:** `Resolved`
- **Opened:** 2026-11-11 11:55
- **Closed:** 2026-11-11 12:10
- **Component:** `aura-manager` (`AuraReportViewController.java`, `TestExecutionDto.java`, `run-report.html`), `neodymium-core` (`VideoWriter.java`), `neodymium-classic` (`VideoWriter.java`, `VideoWriterTest.java`)
- **Scope:** `Framework & Infra/Build`
- **Symptom:**
  1. HTML5 video playback in Aura Manager could not scrub or seek forward into unbuffered video sections, and Safari/iOS failed to play videos due to missing HTTP byte-range streaming (`206 Partial Content`).
  2. Tests failing prior to the first screenshot (such as failures during `@BeforeEach` or early abort) left corrupt 0-byte ghost video files on disk that appeared as broken 0:00 duration recordings in the report.
  3. Configured storage base directories (`aura.report.storage.runs.base-dir`) were ignored by `AuraReportViewController.serveRunFile`.
  4. If framerate adjustment restoration failed due to OS process file locks (e.g. on Windows), temporary recording files were permanently leaked in the temp folder.
- **Root Cause:**
  1. `AuraReportViewController.serveRunFile` returned standard `200 OK` via `ResponseEntity<Resource>` without evaluating HTTP `Range` headers, preventing byte-range streaming via Spring's `ResourceRegion`.
  2. `VideoWriter.stop()` skipped framerate adjustment when `screenshots == 0 || testDuration <= 0` without deleting the empty/partial video file initialized during `start()`.
  3. `AuraReportViewController` hardcoded `Paths.get("storage/runs")` rather than injecting `@Value("${aura.report.storage.runs.base-dir:storage/runs}")`.
  4. `VideoWriter.stop()` caught restoration move exceptions without deleting the temporary file in the exception handler.
- **Detection Gap ("What did we miss?"):**
  Unit tests only validated HTTP 200 responses on simple class names without verifying HTTP `Range: bytes=...` request headers and `206 Partial Content` responses, and tests did not simulate zero-screenshot early test termination or test failure in `VideoWriter`.
- **Resolution:**
  1. Added HTTP byte-range support in `AuraReportViewController.serveRunFile` using `ResourceRegion` and `HttpRange`, returning `206 Partial Content` with `Accept-Ranges: bytes` when `Range` headers are requested.
  2. Injected configurable `@Value("${aura.report.storage.runs.base-dir:storage/runs}")` in `AuraReportViewController`.
  3. Added `getSimpleClassName()` to `TestExecutionDto` and updated `run-report.html` to supply `simpleClassName` directly to `data-test-class`, avoiding client-side parsing.
  4. Updated `VideoWriter.stop()` in both `neodymium-core` and `neodymium-classic` to delete empty video files when `screenshots == 0 || testDuration <= 0` and delete `tempFile` if restoration throws an exception.
  5. Added faststart and zero-screenshot assertions to `VideoWriterTest` in both `core` and `classic`.
- **Safety Net Added:**
  Added unit tests `AuraReportViewControllerTest.testServeRunFileWithByteRangeReturns206PartialContent`, `testServeRunFileAcceptRangesHeaderPresentOnFullContent`, `testServeRunFileInvalidByteRangeReturns416`, `TestExecutionDtoTest.testSimpleClassNameExtraction`, and `VideoWriterTest.testStopWithZeroScreenshotsDeletesEmptyVideoFile`.
