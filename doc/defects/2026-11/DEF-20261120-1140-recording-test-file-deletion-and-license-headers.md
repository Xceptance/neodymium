# Defect Post-Mortem: DEF-20261120-1140-recording-test-file-deletion-and-license-headers

- **ID:** DEF-20261120-1140
- **Status:** Resolved
- **Date Diagnosed:** 2026-11-20 11:40
- **Scope:** Neodymium Classic & Neodymium Core (Recording Engine & Test Harness)
- **Component:** `TakeScreenshotsThread`, `VideoRecordingConfigurations`, `GifRecordingConfigurations`, `FilmTestExecutionSafetyNetTest`

## Summary
Jenkins CI build failed on two fronts:
1. License compliance check failures due to missing standard headers in newly introduced safety net test files.
2. Multiple recording tests failed with `the recording file doesn't exist` because changing the default `appendAllRecordingsToAllureReport` to `false` coupled with unconditional file deletion in `TakeScreenshotsThread` caused temporary recordings to be deleted on passing test runs before assertions could verify them.

## Symptoms & Trigger
- `ClassicLicenseComplianceTest.testZeroCopyleftHeadersAndFullMitCoverage` failed:
  `Found Java files in neodymium-classic without standard MIT license header: ./src/test/java/com/xceptance/neodymium/common/recording/FilmTestExecutionSafetyNetTest.java ==> expected: <true> but was: <false>`
- `LicenseComplianceTest.testCoreJavaFilesHaveDualLicenseHeader` failed:
  `Found Java files in neodymium-core missing canonical dual-license header: .../FilmTestExecutionSafetyNetTest.java ==> expected: <true> but was: <false>`
- 10 unit test cases across `AutomaticGifRecordingTest`, `AutomaticVideoRecordingTest`, `RecordingDurationTest`, `RecordingDurationWithAlertTest`, `ManualGifRecordingTest`, and `ManualVideoRecordingTest` (both JUnit 4 and JUnit 5) failed with `AssertionError: the recording file doesn't exist`.

## Root Cause Analysis (5 Whys)
1. **Why did the recording tests fail?** Because `recordingFile.exists()` returned `false` in `assertRecordingFileExists()`.
2. **Why didn't the recording file exist?** Because `TakeScreenshotsThread` deleted `tempRecording` upon test completion.
3. **Why did it delete the recording?** Because the test was a passing test and `TakeScreenshotsThread` entered an unconditional `else` block intended to discard unattached videos.
4. **Why was the recording unattached?** Because `appendAllRecordingsToAllureReport` was changed from default `true` to `false`, causing passing tests without explicit configuration to be considered unattached.
5. **Why was deletion unconditional?** Because the new `else` block bypassed the existing `deleteTempRecordings` configuration setting, which explicitly defaults to `false` to preserve unattached files on disk for verification.
6. **Why did the license tests fail?** Because `FilmTestExecutionSafetyNetTest.java` was created without top-of-file license headers, failing automated compliance audits introduced in PR #565.

## Detection Gap
- Local tests previously targeted Aura Manager syntax and controller tests instead of running the full classic/core recording test suites with headless browsers.

## Safety Net
1. Restored `appendAllRecordingsToAllureReport` default to `true` in `VideoRecordingConfigurations`, `GifRecordingConfigurations`, and all corresponding `.properties` files across both `neodymium-classic` and `neodymium-core`.
2. Removed unconditional `else` deletion block in `TakeScreenshotsThread.java` (in both modules); unattached files are only cleaned up when `deleteTempRecordings()` is configured.
3. Added canonical MIT header in `neodymium-classic` and canonical dual-license header (`GNU AGPLv3 + Commercial`) in `neodymium-core` to `FilmTestExecutionSafetyNetTest.java`.
4. Updated `FilmTestExecutionSafetyNetTest.java` to test both default retention and failure-only attachment gating (`attachOnlyOnFailure = true`).
