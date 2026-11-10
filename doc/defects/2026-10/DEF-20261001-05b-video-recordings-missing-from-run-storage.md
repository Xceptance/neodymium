# [DEF-20261001-05b] Video Recordings Missing from Run Storage Folder and Execution Logs

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `neodymium-core` (`InteractiveConsoleEngine.java`, `TakeScreenshotsThread.java`)
- **Scope:** `Framework`
- **Symptom:** Test video recordings generated during test runs (such as run_20261001_164918) were missing from the run storage directory and not referenced in console-execution-*.json.
- **Root Cause:** InteractiveConsoleEngine checked for videos in target/videos/ during live step dispatches before FFmpeg finished video assembly at test teardown. Upon test teardown, TakeScreenshotsThread finalized the video and immediately deleted target/videos/*.mp4 before InteractiveConsoleEngine had a chance to attach it.
- **Detection Gap ("What did we miss?"):** Previous video relocation logic relied solely on live step dispatches and did not hook into TakeScreenshotsThread teardown before temp file cleanup.
- **Resolution:** Added InteractiveConsoleEngine.attachVideoToLatestExecutionLog helper and called it from TakeScreenshotsThread immediately after writer.stop() completes, copying the video to run storage and updating console-execution-*.json with videoUrl before temp deletion.
- **Safety Net Added:** Added unit test coverage in InteractiveConsoleEngineTest verifying video attachment and JSON tagging upon test teardown.
