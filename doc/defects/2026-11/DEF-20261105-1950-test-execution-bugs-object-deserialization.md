# [DEF-20261105-1950] Test Execution Metric Deserialization Failure on Object-Structured Bugs

- **Status:** `Resolved`
- **Opened:** 2026-11-05 19:50
- **Closed:** 2026-11-05 20:02
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** `AuraReportDataService` logged `WARN ... Failed to deserialize execution metric for key <key>: Cannot deserialize value of type java.lang.String from Object value (token JsonToken.START_OBJECT)` when reading `run.json` or variation execution JSON files where bugs were stored as structured objects (`[ { "id": "...", ... } ]`). The affected test variation execution was silently dropped from `rawExecutions`, vanishing from the report execution table, count metrics, and side panel listings.
- **Root Cause:**
  1. In `TestExecutionDto.java`, the `@JsonCreator` constructor declared `@JsonProperty("bugs") final List<String> bugs` without a flexible deserializer. When execution files originated with structured bug objects (e.g. `{ "id": "HUG-85323", "title": "...", "url": "...", "status": "Open" }`) and were copied into `run.json`'s `executionMetrics`, Jackson failed with `MismatchedInputException`.
  2. `BugDetailDto` only aliased `{"ticket", "bugTicket"}`, omitting `"id"`, `"ticketId"`, and `"key"`, which prevented mapping Jira / issue tracker object payloads to `BugDetailDto`.
  3. `LocalRunJsonStorageService.buildRunJsonContent` copied raw `objNode.path("bugs")` into `run.json` without normalizing object bug representations into string ticket arrays.
  4. `RunStorageSyncService.java:546-555` and `AuraReportDataService.java:5747-5756, 5887-5893` called `.asText()` on bug nodes. In Jackson, calling `.asText()` on an `ObjectNode` returns an empty string `""`, silently dropping existing linked bug tickets during synchronization, bug linking, or unlinking.
- **Detection Gap ("What did we miss?"):** Existing unit tests (`TestExecutionDtoTest`, `AuraReportBugUnlinkTest`) only exercised clean executions without bugs or executions where `bugs` was an array of plain strings (e.g., `["BUG-101"]`), never providing structured JSON objects in the `bugs` array.
- **Resolution:**
  1. Implemented `FlexibleBugListDeserializer` in `TestExecutionDto` to safely parse `bugs` from arrays of strings, arrays of bug objects (extracting `ticket`, `bugTicket`, `id`, `key`, `ticketId`, `name`), single strings, and single objects.
  2. Annotated `bugs` parameter in `TestExecutionDto` constructors, setter, and field with `@JsonDeserialize(using = FlexibleBugListDeserializer.class)`.
  3. Added `@JsonAlias({"bugTicket", "id", "ticketId", "key"})` to `BugDetailDto(ticket, ...)`.
  4. Added `extractTicket(final JsonNode node)` in `AuraReportDataService` and `hasBugsNode(final JsonNode bugsNode)` in `LocalRunJsonStorageService`.
  5. Updated `RunStorageSyncService`, `AuraReportDataService`, and `LocalRunJsonStorageService` to use `extractTicket` and normalize bugs into clean ticket string arrays in `executionMetrics`.
- **Safety Net Added:**
  - `TestExecutionDtoTest.testDeserializationWithObjectStructuredBugs`: verifies deserialization of `TestExecutionDto` from JSON with bug objects containing `id`, `title`, `url`, `status`.
  - `TestExecutionDtoTest.testDeserializationWithMixedBugsAndBugDetails`: verifies deserialization of mixed arrays, single strings, single objects, and `bugDetails` with `id` alias.
  - `RunStorageSyncServiceTest.testExecutionMetricsDeserializationWithObjectBugs`: end-to-end integration test creating execution files with object-structured bugs, generating `run.json`, asserting that `dataService.getRunReport()` deserializes without warnings and returns the execution with `failed-known` status and normalized bug tickets in `executionMetrics`.
