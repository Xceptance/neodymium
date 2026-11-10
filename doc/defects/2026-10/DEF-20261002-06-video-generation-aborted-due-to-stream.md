# [DEF-20261002-06] Video Generation Aborted Due to Stream Flush After Close Exception in VideoWriter

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`VideoWriter.java`, `TakeScreenshotsThread.java`)
- **Scope:** `Framework`
- **Symptom:** Tests executed with video filming enabled failed to produce any video files in run storage or execution logs.
- **Root Cause:** `VideoWriter.stop()` invoked `ffmpegInput.close()` before `ffmpegInput.flush()`. Calling `flush()` on a closed `OutputStream` threw an `IOException: Stream closed`, which was wrapped in a `RuntimeException`, terminating `VideoWriter.stop()` prematurely and aborting `TakeScreenshotsThread` video relocation.
- **Detection Gap ("What did we miss?"):** Unit tests used mock streams without testing real FFmpeg output stream teardown sequence.
- **Resolution:** Reordered stream closing in `VideoWriter.stop()` to `flush()` before `close()` with safe exception handling. Wrapped `writer.stop()` in `TakeScreenshotsThread` with try-catch to ensure run folder file relocation and execution log video attachment always execute.
- **Safety Net Added:** Added unit test verifying `VideoWriter.stop()` stream teardown without throwing exceptions.
