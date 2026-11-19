# [DEF-20261119-1736] Video Streaming Range Request HTTP 500 and Multi-Dataset Video Overwrite

- **Status:** `Resolved`
- **Opened:** 2026-11-19 17:36
- **Closed:** 2026-11-19 18:05
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `Framework & Infra/Build`
- **Symptom:**
  1. Test runs triggered with video filming enabled (e.g. `mvn test -Dtest=...Aura_tests_examples_wikipedia_search_yml_Test -Dvideo.enableFilming=true ...`) showed no video attachments in the Aura Manager web report UI.
  2. Direct browser and curl requests for video files with standard byte-range headers (`Range: bytes=0-100`) failed with `HTTP/1.1 500 Internal Server Error: No converter for [class ResourceRegion] with preset Content-Type 'video/mp4'`.
  3. In multi-dataset tests (e.g. `wikipedia_search_example_1` and `wikipedia_search_example_2`), only one video file was retained because subsequent datasets overwrote `video-1.mp4` and updated all execution JSONs across the directory.
  4. In `report-manager.js`, `videoPlayer.onerror` caught the HTTP 500 range failure and concealed the video card (`videoContainer.style.display = 'none'`), hiding the video preview and misleading users into believing filming had failed.
- **Root Cause:**
  1. In `AuraReportViewController.servePath`, byte-range requests converted the file resource into a Spring `ResourceRegion`. However, because `serveRunFile` returns `ResponseEntity<?>` (a wildcard type), Spring MVC's `ResourceRegionHttpMessageConverter.canWrite(Type type, Class<?> clazz, MediaType mediaType)` evaluated `type instanceof Class<?>` as `false`, threw `HttpMessageNotWritableException`, and returned HTTP 500.
  2. `InteractiveConsoleEngine.attachVideoToLatestExecutionLog()` looped blindly across all `.json` files in the directory and set `videoName = "video-" + index + ext`, clobbering earlier dataset recordings and metadata.
  3. Writing to secondary mirror directories (`reportsStructuredDir`, `auraStructuredDir`) without directory safety (`tryWrite`, `tryWriteAtomic`) caused unhandled I/O exceptions when paths were misconfigured.
  4. `report-manager.js` lacked graceful error notification, hiding the video entirely on playback or range failure.
- **Detection Gap ("What did we miss?"):**
  Unit tests for `AuraReportViewController` asserted that `response.getBody()` was an instance of `ResourceRegion`, but never invoked Spring MVC's full message converter pipeline or an HTTP client sending actual `Range` headers, missing the wildcard type conversion crash. Similarly, `InteractiveConsoleEngineTest` only tested single-execution video attachments, missing multi-dataset sequential execution overwrite.
- **Resolution:**
  1. Replaced `ResourceRegion` with Spring MVC's `StreamingResponseBody` and `StreamUtils.copyRange(in, outputStream, start, end)` in `AuraReportViewController.servePath`, returning HTTP 206 `PARTIAL_CONTENT` with RFC 7233 headers (`Content-Range`, `Content-Length`, `Accept-Ranges: bytes`, `X-Content-Type-Options: nosniff`) and HTTP 416 on out-of-bounds ranges.
  2. Updated `report-manager.js` to preserve the video container and display an informative `.side-panel-video-error-notice` with a direct download link if the browser encounters a decoding or network error.
  3. Refactored `InteractiveConsoleEngine.attachVideoToLatestExecutionLog()` to isolate the target execution JSON for the finished test, generate distinct video names (`video-1.mp4`, `video-2.mp4`), and embed video metadata into the `attachments` array.
  4. Added `tryWrite` and `tryWriteAtomic` in `InteractiveConsoleEngine` so mirror directory writes cannot crash the primary execution logging.
- **Safety Net Added:**
  - `AuraReportViewControllerTest.testServeRunFileWithByteRangeReturns206PartialContent`: Asserts status 206, `Content-Range`, `StreamingResponseBody`, and streams exact bytes.
  - `AuraReportViewControllerTest.testServeRunFileWithSuffixByteRangeReturns206PartialContent`: Asserts status 206 and suffix range streaming (`bytes=-5`).
  - `AuraReportViewControllerTest.testServeRunFileOutOfBoundsByteRangeReturns416`: Asserts status 416 `REQUESTED_RANGE_NOT_SATISFIABLE`.
  - `InteractiveConsoleEngineTest.testAttachVideoToMultipleExecutionsMaintainsDistinctIndices`: Asserts multi-dataset execution maintains distinct `video-1.mp4` and `video-2.mp4` files and distinct execution JSONs without overwriting.
