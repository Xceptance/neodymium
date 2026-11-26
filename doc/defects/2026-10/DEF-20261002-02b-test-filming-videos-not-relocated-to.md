# [DEF-20261002-02b] Test Filming Videos Not Relocated to Corresponding Run Folder On Test Teardown

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`TakeScreenshotsThread.java`, `InteractiveConsoleEngine.java`)
- **Scope:** `Framework`
- **Symptom:** Videos and GIFs created during tests with filming enabled remained in root `target/videos/` or were deleted by temp cleanup instead of being stored in the active run folder.
- **Root Cause:** `TakeScreenshotsThread` wrote recordings directly into `tempFolderToStoreRecording()` root without moving the resulting file into `InteractiveConsoleEngine.getRunFolder()` on test finish.
- **Detection Gap ("What did we miss?"):** Early recording tests checked for files directly in `target/videos/` or `target/gifs/` without verifying run folder organization.
- **Resolution:** Updated `TakeScreenshotsThread` teardown to resolve the active run folder via `InteractiveConsoleEngine.getRunFolder()`, create `<tempFolderToStoreRecording>/<runFolder>/`, and move the test recording file into the run folder upon test completion.
- **Safety Net Added:** Added unit tests in `AutomaticVideoRecordingTest`, `AutomaticGifRecordingTest`, and `InteractiveConsoleEngineTest` verifying video relocation to the corresponding run folder on test finish.
