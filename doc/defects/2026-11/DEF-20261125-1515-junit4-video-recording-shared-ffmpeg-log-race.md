# [DEF-20261125-1515] Parallel JUnit 4 Video Recording Tests Race on Shared Default FFmpeg Log File

- **Status:** `Resolved`
- **Opened:** 2026-11-25 14:37
- **Closed:** 2026-11-25 15:15
- **Component:** `neodymium-classic`, `neodymium-core` (`AbstractRecordingDeletionTest`, `AbstractRecordingTest`, `DeleteVideoRecordingTest`, `AutomaticVideoRecordingTest`, `ManualVideoRecordingTest`)
- **Scope:** `Test/Harness` & `Concurrency`
- **Issue:** GitLab #175
- **Symptom:**
  Running JUnit 4 tests in `neodymium-classic` (`-Pjunit-4` / `-Pneo-classic-junit4` with `<forkCount>4</forkCount>`) intermittently fails with:
  - `DeleteVideoRecordingTest.assertLogFileExists:53 the logfile for the automatic video recording test exists`
  - `AutomaticVideoRecordingTest.assertLogFileExists:54 the logfile for the automatic video recording test exists`
- **Root Cause:**
  1. Commit `fe6f69985` (`[#420] fix concurrency issues in recording tests`) isolated `video.ffmpegLogFile` per test run via UUID-suffixed paths and `logFilePath` in the JUnit 5 recording test hierarchy, but left `logFilePath` unused and omitted `video.ffmpegLogFile` from `beforeClass(...)` in the JUnit 4 `AbstractRecordingDeletionTest` and `AbstractRecordingTest` base classes.
  2. All four JUnit 4 video recording test classes in `neodymium-classic` (`config.DeleteVideoRecordingTest`, `config.AutomaticVideoRecordingTest`, `automatic.AutomaticVideoRecordingTest`, `manual.ManualVideoRecordingTest`) therefore wrote to and deleted the same default `target/ffmpeg_output_msg.txt` across Surefire's 4 parallel forks, causing whichever test finished first to unlink the shared log file before concurrent forks reached `@AfterClass assertLogFileExists()`.
  3. Additionally, `neodymium-classic`'s `AbstractRecordingDeletionTest.assertRecordingFileWasDeleted()` (both JUnit 4 and JUnit 5) concatenated the `uuid` `Map` object (`+ uuid +`) instead of `uuid.get(Thread.currentThread())`, making the recording-file deletion check vacuous.
- **Detection Gap ("What did we miss?"):**
  Each JUnit 4 video recording test passes when run in isolation or serially (`forkCount=1`), and the JUnit 5 fix in `fe6f69985` was not symmetrically verified against parallel JUnit 4 runs of the four `*VideoRecordingTest` classes together.
- **Resolution:**
  1. Switched `uuid`, `logFilePath`, and `configurationsClass` to `ConcurrentHashMap` in the JUnit 4 recording base classes (`neodymium-classic` and `neodymium-core`) and JUnit 5 `AbstractRecordingDeletionTest` (`neodymium-classic`).
  2. Generated unique `target/ffmpeg_output_msg_<UUID>.txt` paths when `"video".equals(format)` in JUnit 4 `beforeClass(...)`, storing them in `logFilePath` and configuring `video.ffmpegLogFile` in the per-run temporary properties file.
  3. Updated `@AfterClass assertLogFileExists()` in all four JUnit 4 video recording test subclasses in both `neodymium-classic` and `neodymium-core` to resolve `logFilePath.get(Thread.currentThread())` directly instead of calling `FilmTestExecution.getContextVideo().ffmpegLogFile()` after teardown context clearing.
  4. Fixed `assertRecordingFileWasDeleted()` in both JUnit 4 and JUnit 5 `neodymium-classic` `AbstractRecordingDeletionTest` to resolve `uuid.get(Thread.currentThread())` (asserting non-null) rather than stringifying the `Map`.
- **Safety Net Added:**
  - Verified two consecutive parallel (`<forkCount>4</forkCount>`) runs of `com.xceptance.neodymium.junit4.tests.recording.**.*RecordingTest` in `neodymium-classic`, JUnit 5 `Delete*RecordingTest` in `neodymium-classic`, and `mvn test-compile -pl neodymium-core`.
