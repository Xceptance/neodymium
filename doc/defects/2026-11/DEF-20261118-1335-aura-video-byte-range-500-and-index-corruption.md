# [DEF-20261118-1335] Aura Video Playback Stuck at 0:00 Due to Spring MVC ResourceRegion Wildcard Rejection & Index Order

- **Status:** `Resolved`
- **Opened:** 2026-11-18 13:35
- **Closed:** 2026-11-18 13:41
- **Component:** `aura-manager` (`AuraReportViewController.java`), `neodymium-core` (`InteractiveConsoleEngine.java`)
- **Scope:** `Framework & Infra/Build`
- **Symptom:** All test execution videos in Aura Manager display 0:00 duration in browser and cannot be started or scrubbed. In multi-test suites, some storage recordings (e.g. video-3.mp4) remain 48-byte stubs while previous indices are overwritten.
- **Root Cause:**
  1. `AuraReportViewController.serveRunFile` declared a return type of `ResponseEntity<?>`. Spring's `ResourceRegionHttpMessageConverter.canWrite` inspects the generic reflection type. Because `WildcardType` is neither a `ParameterizedType` of Collection nor a `Class` assignable to `ResourceRegion`, the converter rejects writing the body. Spring MVC throws `HttpMessageNotWritableException` and responds with HTTP 500 when browsers issue standard `Range: bytes=0-` requests.
  2. `InteractiveConsoleEngine.attachVideoToLatestExecutionLog` looped over `targetTestDir.listFiles()` and assigned `index = parsedIndex` without `Math.max()`, causing the latest test recording index to depend on arbitrary directory traversal order and overwriting previous video indices.
  3. `InteractiveConsoleEngine` eagerly copied newest videos from `target/videos` during intermediate JSON saves without checking if the file is an initial 48-byte empty FFmpeg stub.
- **Detection Gap ("What did we miss?"):** Previous unit tests only invoked `controller.serveRunFile(...)` in direct Java isolation and never tested Spring MVC message conversion via MockMvc or real HTTP dispatch. Index calculation tests never tested unordered file listings.
- **Resolution:**
  1. Split `AuraReportViewController` file serving: added `@GetMapping(value = "/api/runs/{runId}/{testClass}/{filename:.+}", headers = "Range")` returning `ResponseEntity<ResourceRegion>` for byte-range streaming, and kept `@GetMapping("/api/runs/{runId}/{testClass}/{filename:.+}")` returning `ResponseEntity<Resource>` for full downloads.
  2. Updated `InteractiveConsoleEngine.attachVideoToLatestExecutionLog` to compute `index = Math.max(index, parsed)` across all matching execution JSON files.
  3. Guarded intermediate video copying in `InteractiveConsoleEngine` to filter out files with length <= 1024 bytes.
- **Safety Net Added:**
  1. Added `AuraReportViewControllerSpringContextTest.testServeRunFileWithByteRangeHeaderStreamsVideoThroughSpringMvc` verifying HTTP 206 Partial Content, `Accept-Ranges`, `Content-Range`, and video stream bytes through Spring MVC.
  2. Added `AuraReportViewControllerSpringContextTest.testServeRunFileWithoutRangeHeaderStreamsFullVideoThroughSpringMvc` verifying standard HTTP 200 OK file downloads through Spring MVC.
  3. Added `InteractiveConsoleEngineTest.testAttachVideoToLatestExecutionLogWithUnorderedJsonFiles` asserting monotonic index resolution when directory listings return files out of order.
