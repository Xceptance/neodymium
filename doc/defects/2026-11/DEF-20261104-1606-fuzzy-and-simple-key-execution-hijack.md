# [DEF-20261104-1606] Fuzzy Folder Fallback and Simple-Class Keys Can Return the Wrong Execution

- **Status:** `Resolved`
- **Opened:** 2026-11-04 16:06
- **Closed:** 2026-11-04 16:12
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** The step-details panel can show the steps of a different execution: (a) during a live run, a pending dataset B of a class whose only written file is dataset A shows dataset A's steps; (b) two classes with the same simple name in different packages are mixed up when queried by a simple-class key.
- **Root Cause:** `AuraReportDataService.getExecutionDetails()` (a) accepted the single-file folder fallback ("Pass 2") for any id starting with `<folder>#`, without checking that the file's own identity (id/title/dataset) fits the requested id; (b) treated simple-class-name keys as exact matches in Pass 1, in the `run.json` metric/array fallbacks and in the cache/live-buffer lookups, so the first file/entry in scan order won.
- **Detection Gap ("What did we miss?"):** Existing tests only covered single-file folders where the fuzzy match was correct, and uniquely named classes. No test had a pending dataset or two same-named classes.
- **Resolution:**
  1. Pass 2 now requires `isIdentityCompatible(...)`: if the file carries non-placeholder identity values they must occur in the requested id.
  2. Simple-class-key hits are collected and used only if unambiguous (Pass 1, `run.json` metrics, and via the new `findUniqueExecution` for run.json executions, cache and live buffer); ambiguity logs a warning and returns no guess.
  3. Extracted `buildExecutionDetailsDto`; logged previously swallowed exceptions at debug level.
  4. `report-manager.js`: fixed a temporal-dead-zone `ReferenceError` (`rowId` read before its `const` declaration) in the in-flight guard of `renderStepsForExecution`; the guard now also makes `isLiveStepFetchInFlight` effective instead of dead state. Placeholder run-id fallback to the latest run now logs a warning.
- **Safety Net Added:** `RunStorageSyncServiceTest.testFuzzyFolderFallbackDoesNotHijackOtherDatasetOfSameClass` and `testSimpleClassKeyCollisionAcrossPackagesIsNotGuessed`.
