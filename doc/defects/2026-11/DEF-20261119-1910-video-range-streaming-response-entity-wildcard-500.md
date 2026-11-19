# [DEF-20261119-1910] Video Range Streaming HTTP 500 Caused by Wildcard ResponseEntity Return Type

- **Status:** `Resolved`
- **Opened:** 2026-11-19 19:10
- **Closed:** 2026-11-19 19:15
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  In test reports (e.g. `run_20261119_184334`), video recordings were attached on disk with full durations (1m25s) but displayed in the report UI with duration `0:00` seconds and could not be played.
- **Root Cause:**
  When an HTML5 `<video>` element loads in a browser, it issues an HTTP Range request (`Range: bytes=0-...`) to parse video metadata (such as duration, dimensions, and codecs). In Spring MVC, `AuraReportViewController.serveRunFile` was declared with a wildcard generic return type: `public ResponseEntity<?> serveRunFile(...)`.
  Spring MVC's `StreamingResponseBodyReturnValueHandler.supportsReturnType` executes:
  ```java
  Class<?> bodyType = ResolvableType.forMethodParameter(returnType).getGeneric(0).resolve();
  return (bodyType != null && StreamingResponseBody.class.isAssignableFrom(bodyType));
  ```
  Because the method returned `ResponseEntity<?>` with wildcard `?`, `resolve()` returned `null`/`Object.class` instead of `StreamingResponseBody.class`. Consequently, `StreamingResponseBodyReturnValueHandler` rejected the method, and Spring MVC fell back to `HttpEntityMethodProcessor`. Since no `HttpMessageConverter` exists to serialize a `StreamingResponseBody` lambda into `video/mp4`, Spring threw `HttpMessageNotWritableException`, resulting in **HTTP 500 Internal Server Error** on all range requests. Without valid header slices, the browser was unable to read video duration metadata, freezing the player at `0:00`.
- **Detection Gap ("What did we miss?"):**
  Unit tests invoked `controller.serveRunFile(...)` directly in plain Java without routing through Spring MVC's dispatcher servlet and return value handler pipeline, missing the wildcard type inspection failure.
- **Resolution:**
  Refactored `serveRunFile` in `AuraReportViewController` to accept `HttpServletResponse response` and write directly to `response.getOutputStream()`:
  1. For range requests, sets status 206 `SC_PARTIAL_CONTENT` and streams the exact byte range using `StreamUtils.copyRange(in, out, start, end)` with RFC 7233 headers (`Content-Range`, `Content-Length`, `Accept-Ranges: bytes`, `X-Content-Type-Options: nosniff`).
  2. For invalid or out-of-bounds ranges, sets status 416 `SC_REQUESTED_RANGE_NOT_SATISFIABLE` with `Content-Range: bytes */totalLength`.
  3. For full file requests, sets status 200 `SC_OK` and streams the entire file using `in.transferTo(out)`.
  4. For missing files or directory traversals, sends status 404 `SC_NOT_FOUND`.
  This completely removes Spring MVC's return value handler reflection, generic type inspection, and async dispatch dependencies.
- **Safety Net Added:**
  Updated `AuraReportViewControllerTest` with `MockHttpServletResponse` covering:
  - `testServeRunFileReturnsVideoFile`: Full video streaming (status 200).
  - `testServeRunFileWithByteRangeReturns206PartialContent`: Byte-range slicing (`bytes=0-4` returns status 206, `Content-Range: bytes 0-4/20`, and exact bytes).
  - `testServeRunFileWithSuffixByteRangeReturns206PartialContent`: Suffix byte ranges (`bytes=-5` returns status 206, `Content-Range: bytes 15-19/20`).
  - `testServeRunFileOutOfBoundsByteRangeReturns416`: Out-of-bounds range (`bytes=100-200` returns status 416).
  - `testServeRunFileInvalidByteRangeReturns416`: Malformed range returns status 416.
  - `testServeRunFileNotFoundReturns404`, `testServeRunFilePathTraversalReturns404`, `testServeRunFileDirectoryReturns404`.
