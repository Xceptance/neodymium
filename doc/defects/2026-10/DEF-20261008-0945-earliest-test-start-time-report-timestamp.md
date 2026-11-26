# [DEF-20261008-0945] Report Timestamp on Run History and Batch History Not Defined by Earliest Test Start Time

- **Status:** `Resolved`
- **Opened:** 2026-10-08 09:45
- **Closed:** 2026-10-08 10:00
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** On the Run History page (`/report`, `/batch-overview`) and Batch History page (`/batch-history`), the timestamp displayed for a test run reflects the run creation/startRun invocation time or top-level run metadata rather than the startTime of the earliest test executed within the run.
- **Root Cause:** 
  1. `AuraReportDataService.ingestExecution` and `finishRun` updated execution counts and pass rates but never recalculated or updated `TestRunEntity.startTimeMs` and `timestampLabel` from the ingested test executions in `liveRunBuffer`.
  2. `RunStorageSyncService.syncRunJsonReport` pre-seeded the earliest timestamp tracker with top-level container metadata timestamps (`root.path("timestamp")`/`root.path("startTime")`). If the container was created or queued before the tests started, `minMs` never updated to the earliest test's start time because the test start times were chronologically later than the pre-seeded metadata value. Furthermore, only `executionMetrics` was checked in the timestamp resolution block, ignoring reports with tests located in the `executions` array.
  3. `LocalRunJsonStorageService.buildRunJsonContent` omitted `startTimeMs` from generated `run.json` manifests.
- **Detection Gap ("What did we miss?"):** Existing test `RunStorageSyncServiceTest.testEarliestTime` only tested the case where a test started earlier than the run container timestamp, masking situations where the run container was initialized prior to test execution or where runs were populated live via `AuraReportDataService`.
- **Resolution:** 
  1. In `AuraReportDataService.ingestExecution` and `finishRun`, calculate the minimum start time across all buffered executions and update `TestRunEntity.startTimeMs` and `timestampLabel`.
  2. In `RunStorageSyncService.syncRunJsonReport`, determine `minStartTimeMs` and the corresponding earliest timestamp string directly from the earliest test execution in `execList` (supporting both `executionMetrics` and `executions`), falling back to container metadata only if no test executions contain start time data.
  3. In `LocalRunJsonStorageService.buildRunJsonContent`, persist `startTimeMs` into `run.json` when `earliestMs != Long.MAX_VALUE`.
- **Safety Net Added:** Added automated regression test `testEarliestTestStartTimeDefinesRunTimestampEvenWhenContainerHasEarlierTimestamp` in `RunStorageSyncServiceTest` and `testLiveIngestionUpdatesRunTimestampToEarliestTest` in `AuraReportDataServiceTest`.
