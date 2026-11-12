# [DEF-20261111-2315] Mid-Run Duplicate Execution Rows on Report and Missing Suite Tag for Console Runs

- **Status:** `Resolved`
- **Opened:** 2026-11-11 23:15
- **Closed:** 2026-11-11 23:25
- **Component:** `aura-manager` / `neodymium-core`
- **Scope:** `Framework & Test/Harness`
- **Symptom:**
  1. During active test runs executed from Aura Manager, opening or viewing the `/run-report` page temporarily displays duplicate test execution rows (e.g. 2 test rows instead of 1), which vanishes as soon as the run finishes and is finalized.
  2. When executing tests from the command line / console (e.g. `mvn test -Dneodymium.tag.Suite=Checkout` or `-DbatchName=Checkout`), the `Suite` tag is not displayed on the active context card in the sidebar or breadcrumbs, showing an empty batch name or falling back to "Default".
- **Root Cause:**
  1. In `AuraQueueService.ingestBatchExecutionFiles()`, execution payloads were constructed without setting the execution `id`. In `AuraReportDataService.getRunReport()` and `ingestExecution()`, live in-buffer executions were merged against disk-loaded executions using strict `idMatch` and strict SHA-256 variation ID matching (`generateVariationId(testClass, testMethod, title, location, browser)`). If `testClass` differed between simple class name and fully qualified class name, or if `testMethod` was null/empty in one payload and present in the other, `generateVariationId` produced mismatched hashes and `idMatch` failed, treating the active live execution as an additional, un-merged test row until `finishRun()` cleared `liveRunBuffer`.
  2. For console runs, `TestRunEntity.tags` is `@Transient` and not populated by default JPA entity queries. In `AuraGlobalModelAdvice` and `AuraReportDataService.getAllRuns()`, runs were returned without calling `populateRunTags()`, leaving `tags` empty. Furthermore, `LocalRunJsonStorageService.resolveOrCreateBatchJson()` and `buildRunJsonContent()` only extracted `environment` from execution JSON files while omitting execution tags and CLI property overrides, writing `{ "name": "Default" }` into `batch.json` and omitting `Suite` from `run.json` tags.
- **Detection Gap ("What did we miss?"):**
  1. Existing report tests verified finalized runs after `finishRun()` cleared `liveRunBuffer`, never asserting report contents concurrently while a run was active in `liveRunBuffer` with differing class naming conventions between live buffer DTOs and disk JSON files.
  2. Active context card and status tests did not simulate console runs where tags originate exclusively from execution JSON files or JVM system properties rather than the Web UI queue submission payload.
- **Resolution:**
  1. In `AuraQueueService.ingestBatchExecutionFiles()`, populated execution `id` from `testId`, `datasetId`, or JSON filename.
  2. In `AuraReportDataService.getRunReport()` and `ingestExecution()`, enhanced execution deduplication and merging with `isSameTestClass(c1, c2)` (class name comparison ignoring package prefixes), `varIdNoMethod` fallback, and component-level matching (class, browser, title/location).
  3. In `AuraReportDataService.getAllRuns()`, invoked `populateRunTags(runs)` before returning. Added disk fallback to `getRunTagsMap(runId)` to read tags from `batch.json` and `run.json` when relational database tags are not yet persisted.
  4. In `AuraGlobalModelAdvice` and `AuraTestQueueController.getStatusStream()`, added `populateRunTags()`, disk-active run detection via `isRunCurrentlyActiveOnDisk()`, and tag map fallback via `getRunTagsMap()` to resolve effective batch names.
  5. In `LocalRunJsonStorageService.resolveOrCreateBatchJson()` and `buildRunJsonContent()`, extracted tags from execution JSON files, resolved effective batch name from tags, and persisted tags in `batch.json` and `run.json`.
  6. In `neodymium-core`: `InteractiveStateBuilder.java` and `NeodymiumAiRunner.java`, merged CLI system properties (`-DbatchName`, `-Dneodymium.tag.Suite`, etc.) into state and variation tags.
- **Safety Net Added:**
  - Automated tests verifying:
    - Mid-run live execution merging without duplicate rows when class names differ by package in `AuraReportDataServiceTest` / `AuraReportFinishedStatusMappingTest`.
    - Batch tag detection from execution JSON files and system properties in `LocalRunJsonStorageService` and `AuraGlobalModelAdvice`.
