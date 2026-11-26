# [DEF-20261028-1325] Bug Linking Ignored Environment Dimension and Lacked Completion Feedback

- **Status:** `Resolved`
- **Opened:** 2026-10-28 12:35
- **Closed:** 2026-10-28 13:25
- **Component:** `neodymium-core` / `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When clicking "Save Bug" on the test execution side panel with criteria involving run environment (e.g., `location: DE, environment: STG`), the form drawer remained open with inputs unmodified and the status badge displayed `Clean / No Linked Bugs`. No error was thrown, but the bug ticket was not associated with the execution on the report page.
- **Root Cause:**
  1. `TagUtils.matchesTagCriteria` only matched dynamic YAML tags, `browser`, and `location`/`locale`. It had no handling for `environment` (`environment`, `env`, `instance`) or `batch` (`batch`, `batchname`, `suite`).
  2. `AuraReportDataService.matchesBugTags(TestExecutionDto, String)` only passed `exec.getTags()`, `exec.getJunitTags()`, `exec.getBrowser()`, and `exec.getLocale()`, omitting the run's environment (`STG`) and batch name (`stg-emea`). When evaluating `environment: STG`, it looked inside execution YAML tags, failed to match, and discarded the bug association (`appliesToTarget = false`).
  3. The HTMX form inside `#bugTicketFormBox` lacked a post-request completion callback (`hx-on::after-request`). Even upon request completion, `#bugTicketFormBox` remained open without clearing the inputs or resetting selected tags, resulting in zero visible user feedback.
- **Detection Gap ("What did we miss?"):**
  Existing test `AuraReportTagBoundBugSeverityTest` only exercised criteria containing `browser` (`browser: Safari`) and custom execution YAML tags (`os: iOS, mobile`). No test asserted criteria with `environment: STG` or `batch: ...`, nor verified that the UI form drawer automatically resets upon saving.
- **Resolution:**
  1. Overloaded `TagUtils.matchesTagCriteria` to evaluate `environment` (matching `environment`, `env`, `instance`) and `batchName` (matching `batch`, `batchname`, `suite`) for both key-value pairs and standalone flag tags.
  2. Extended `TestExecutionDto` and `RunReportDto` with `environment` and `batchName` fields, populated during run ingestion and report construction.
  3. Updated `AuraReportDataService.matchesBugTags` and `applyBugsToExecution` to propagate and evaluate the run's `environment` and `batchName`.
  4. Added `getAllKnownTagsList()` to `AuraReportDataService` exposing all distinct environments, batches, browsers, and tags to the side panel model.
  5. Refactored the bug ticket form in `side-panel-step-list.html` and `report-manager.js`:
     - Replaced the free-form text input with a hidden `boundTags` input and an active chip container.
     - Added quick-pick chips for the run itself (`environment: <env>`, `batch: <batch>`) and execution (`location`, `browser`, execution tags).
     - Added an "Add other tag" dropdown populated dynamically with all known tags.
     - Added `onBugSavedSuccessfully()` invoked on `hx-on::after-request` and on the `bugUpdated` event to close the form drawer, clear inputs, and reset selected chips.
- **Safety Net Added:**
  - `TagUtilsTest.testMatchesTagCriteriaWithEnvironmentAndBatch`: verifies matching for `environment: STG`, `env: stg`, `instance: STG`, `batch: stg-emea`, `suite: stg-emea`, and composite `location: DE, environment: STG`.
  - `AuraReportTagBoundBugSeverityTest.testLocationAndEnvironmentCriteriaBugBinding`: verifies end-to-end that linking a bug with `location: DE, environment: STG` attaches only to runs matching both `location: DE` and `environment: STG`, while excluding runs on `US` or `PROD`, and verifies `getAllKnownTagsList()`.
