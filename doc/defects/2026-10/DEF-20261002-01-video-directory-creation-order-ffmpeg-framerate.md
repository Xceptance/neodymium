# [DEF-20261002-01] Video Directory Creation Order & FFmpeg Framerate Adjustment Failures Prevent Video File Generation

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`TakeScreenshotsThread.java`, `VideoWriter.java`)
- **Scope:** `Framework`
- **Symptom:** Tests executed with video recording enabled produced no video recordings in target/videos/ or run storage folders.
- **Root Cause:** TakeScreenshotsThread instantiated VideoWriter before calling directory.mkdir(), causing FFmpeg to attempt writing to non-existent target/videos/ directory. In addition, VideoWriter.stop() attempted to rename and re-encode videos without verifying screenshots > 0 or file existence.
- **Detection Gap ("What did we miss?"):** Unit tests created target/videos/ manually during test setup, masking the missing directory initialization order during real test runs.
- **Resolution:** Moved target directory creation using directory.mkdirs() prior to Writer.instantiate in TakeScreenshotsThread constructor. Added defensive checks for file existence, non-zero screenshots, and fallback file copying in VideoWriter.stop().
- **Safety Net Added:** Added unit test coverage verifying automatic directory creation and zero-screenshot video handling.
