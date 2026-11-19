# [DEF-20261119-1402] Phantom 0:00 Video Attachments and Missing Failure-Only Gating

- **Status:** `Resolved`
- **Opened:** 2026-11-19 14:02
- **Closed:** 2026-11-19 14:26
- **Component:** `neodymium-core`, `neodymium-classic`, `aura-manager`
- **Scope:** `Framework & Infra/Build`
- **Symptom:**
  1. In test reports (Allure and Aura Manager), tests that do not have video recording enabled display video attachments with duration 0:00 seconds that cannot be played. When a test explicitly enables video recording (e.g. via `@video` tag), its video records and attaches normally, but all subsequent tests running in the suite get corrupted 0:00 video attachments.
  2. Videos are currently attached to every test execution (passing or failing) by default in Allure, and unconditionally in Aura Manager, consuming significant disk space and CPU.
- **Root Cause:**
  1. `FilmTestExecution.clearThreadContexts()` was never called during test teardown or lifecycle (`BrowserRunner.teardown()`, `Neodymium.clearThreadContext()`). When a test mutated `video.enableFilming = true` on the worker thread (e.g. Cucumber `@video` hook), all subsequent tests running on the same thread inherited `enableFilming = true`.
  2. `FilmTestExecution.java` line 118 checked `if (!config.enableFilming() && !Neodymium.hasDriver()) return null;`. Because of `&&`, if `Neodymium.hasDriver()` was true, a recording thread was created even when filming was disabled.
  3. `InteractiveConsoleEngine.saveLocalExecutionLog()` blindly scanned `target/videos/` and re-attached leftover video recordings to unrelated subsequent test logs.
  4. In `TakeScreenshotsThread.java`, `InteractiveConsoleEngine.attachVideoToLatestExecutionLog()` was invoked unconditionally without respecting `video.appendAllRecordingsToAllureReport` or test failure state.
  5. `report-manager.js` displayed the `<video>` player based on the DOM row's `data-video` attribute even when no valid video attachment existed on disk, triggering a 404 request that rendered an unplayable `0:00 / 0:00` player.
- **Detection Gap ("What did we miss?"):**
  Previous unit tests executed in isolation or manually invoked `FilmTestExecution.clearThreadContexts()` in test fixtures, masking thread-context leaks and directory spillage across sequential test runs on shared worker threads. Furthermore, tests did not assert failure-only video attachment gating in Aura Manager logs.
- **Resolution:**
  1. In `BrowserRunner.teardown()`, `Neodymium.clearThreadContext()`, and Cucumber listeners, invoked `FilmTestExecution.clearThreadContexts()` in `finally` blocks to guarantee thread isolation.
  2. Fixed inverted boolean condition in `FilmTestExecution.java` (`!config.enableFilming() || !Neodymium.hasDriver()`) across both `neodymium-core` and `neodymium-classic`.
  3. Gated Aura Manager and Allure video attachment in `TakeScreenshotsThread` behind `recordingConfigurations.appendAllRecordingsToAllureReport() || testFailed`.
  4. When a test passes and `appendAllRecordingsToAllureReport` is `false`, immediately deleted the temporary video file from disk.
  5. Removed naive `target/videos/` leftover directory scanning in `InteractiveConsoleEngine.saveLocalExecutionLog()`.
  6. In `report-manager.js`, only rendered the video player container if a valid video attachment actually exists in `_execDto.attachments`, and attached an `onerror` handler to hide player on 404/corruption.
  7. Updated default configuration `video.appendAllRecordingsToAllureReport` and `gif.appendAllRecordingsToAllureReport` to `false`.
- **Safety Net Added:**
  Created `FilmTestExecutionSafetyNetTest` in both `neodymium-core` and `neodymium-classic` testing thread-context clearance after teardown, boolean guard behavior when filming is disabled, default configuration values, and automatic cleanup of temporary files on passing test executions. Verified syntax with `StaticResourceSyntaxTest`.
