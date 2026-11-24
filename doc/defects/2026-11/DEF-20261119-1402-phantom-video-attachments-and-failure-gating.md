# [DEF-20261119-1402] Phantom 0:00 Video Attachments and Missing Failure-Only Gating

- **Status:** `Resolved`
- **Opened:** 2026-11-19 14:02
- **Closed:** 2026-11-19 15:46
- **Component:** `neodymium-core`, `neodymium-classic`, `aura-manager`
- **Scope:** `Framework & Infra/Build`
- **Symptom:**
  1. In test reports (Allure and Aura Manager), tests that do not have video recording enabled display video attachments with duration 0:00 seconds that cannot be played. When a test explicitly enables video recording (e.g. via `@video` tag), its video records and attaches normally, but all subsequent tests running in the suite get corrupted 0:00 video attachments.
  2. Videos are currently attached to every test execution (passing or failing) by default in Allure, and unconditionally in Aura Manager, consuming significant disk space and CPU.
  3. *Secondary Regression:* Initial fix inverted the initialization guard in `FilmTestExecution.createTakeScreenshotsThread` to `|| !Neodymium.hasDriver()`, which prevented video recording from starting during `setUpTest()` (when the driver has not yet been opened), and defaulted `appendAllRecordingsToAllureReport` to `false`, discarding videos of passing tagged tests.
- **Root Cause:**
  1. `FilmTestExecution.clearThreadContexts()` was never called during test teardown or lifecycle (`BrowserRunner.teardown()`, `Neodymium.clearThreadContext()`). When a test mutated `video.enableFilming = true` on the worker thread (e.g. Cucumber `@video` hook), all subsequent tests running on the same thread inherited `enableFilming = true`.
  2. In `InteractiveConsoleEngine.saveStructuredTestExecutionLogs()`, the engine blindly scanned `target/videos/` and re-attached leftover video recordings to unrelated subsequent test logs.
  3. In `TakeScreenshotsThread.java`, `InteractiveConsoleEngine.attachVideoToLatestExecutionLog()` was invoked unconditionally without respecting `video.appendAllRecordingsToAllureReport` or test failure state.
  4. `report-manager.js` displayed the `<video>` player based on the DOM row's `data-video` attribute even when no valid video attachment existed on disk, triggering a 404 request that rendered an unplayable `0:00 / 0:00` player.
  5. In `FilmTestExecution.createTakeScreenshotsThread()`, checking `!Neodymium.hasDriver()` during thread creation caused `BrowserRunner.setUpTest()` and `NeodymiumAiRunner.executeBeforeEach()` to abort thread creation because the WebDriver is only opened lazily during test execution.
- **Detection Gap ("What did we miss?"):**
  Previous unit tests executed in isolation or manually invoked `FilmTestExecution.clearThreadContexts()` in test fixtures, masking thread-context leaks and directory spillage across sequential test runs on shared worker threads. Furthermore, unit tests checked thread suppression when filming is disabled, but missed verifying thread initialization in the specific state where `enableFilming == true` and `hasDriver() == false`.
- **Resolution:**
  1. In `BrowserRunner.teardown()`, `Neodymium.clearThreadContext()`, Cucumber listeners, and AI runner, invoked `FilmTestExecution.clearThreadContexts()` in `finally` blocks to guarantee thread isolation across test runs.
  2. Maintained `if (!config.enableFilming() && !Neodymium.hasDriver()) return null;` in `FilmTestExecution.java` (both `neodymium-core` and `neodymium-classic`) allowing background recording threads to start in `setUpTest()` prior to driver initialization.
  3. Gated Aura Manager and Allure video attachment in `TakeScreenshotsThread` behind `recordingConfigurations.appendAllRecordingsToAllureReport() || testFailed`.
  4. When a test passes and `appendAllRecordingsToAllureReport` is explicitly set to `false`, immediately deleted the temporary video file from disk to support failure-only attachment.
  5. Kept default `appendAllRecordingsToAllureReport = true` across all configuration files and interfaces so tagged tests retain and attach their videos by default.
  6. Removed naive `target/videos/` leftover directory scanning in `InteractiveConsoleEngine.saveStructuredTestExecutionLogs()`.
  7. In `report-manager.js`, only rendered the video player container if a valid video attachment actually exists in `_execDto.attachments`, and attached an `onerror` handler to hide player on 404/corruption.
- **Safety Net Added:**
  Created and expanded `FilmTestExecutionSafetyNetTest` in both `neodymium-core` and `neodymium-classic` testing:
  - Default `appendAllRecordingsToAllureReport` is `true`.
  - Recording thread successfully starts when `enableFilming == true` before driver initialization (`hasDriver() == false`).
  - Thread does NOT start when `enableFilming == false`.
  - Thread context is reset after `BrowserRunner.teardown()` and `Neodymium.clearThreadContext()`.
  - Passing tests retain recordings under default configuration.
  - Passing tests delete recordings when `appendAllRecordingsToAllureReport == false` (failure-only mode).
