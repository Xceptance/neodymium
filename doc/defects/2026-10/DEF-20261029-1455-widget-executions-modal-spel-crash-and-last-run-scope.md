# [DEF-20261029-1455] SpEL Property Resolution Crash in Widget Executions Modal Freezing Stale Multi-Run View

- **Status:** `Resolved`
- **Opened:** 2026-10-29 14:55
- **Closed:** 2026-10-29 15:05
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Clicking on a widget card (specifically one configured with "Last Run", e.g., "Status Pie Chart (Last Run)") displays a stuck modal showing an unrelated previous widget ("Tag: Browser Ratio Bar" with 3 contributing runs and 215 executions). Inspection of the HTTP response shows `transfer closed with outstanding read data remaining` (HTTP chunked abort).
- **Root Cause:** 
  1. `widget-executions-modal.html` referenced non-existent properties on `TestExecutionDto`: `e.methodName`, `e.className`, `e.duration`, and `e.rowId`. Thymeleaf's SpEL engine threw `SpelEvaluationException` on the first execution row, aborting the chunked HTTP response.
  2. `report-manager.js#openWidgetExecutionsModal` opened the dialog immediately before HTMX AJAX completed. When the chunked response failed, HTMX aborted swapping, leaving the modal displaying stale DOM contents.
  3. `WidgetAggregationService#sortRunsChronologically` sorted ascending with `nullsLast`, which caused any run with a null `startTimeMs` to be placed at the end and erroneously selected by `WidgetPointScope.LAST_RUN`.
  4. `AuraReportViewController#getWidgetExecutions` queried all historical runs without respecting active batch/filter parameters in `HX-Current-URL` or `Referer`.
- **Detection Gap ("What did we miss?"):** `WidgetAggregationServiceTest` tested `resolveContributingExecutions` with unit test mocks without rendering the actual Thymeleaf HTML fragment, allowing broken SpEL property names to slip into production templates undetected.
- **Resolution:**
  1. Fixed SpEL expressions in `widget-executions-modal.html` to use `e.testMethod`, `e.testClass`, `e.durationFormatted`, and `e.id`.
  2. Added alias getters in `TestExecutionDto` (`getMethodName()`, `getClassName()`, `getDuration()`, `getRowId()`) as an additional compatibility buffer.
  3. Updated `sortRunsChronologically` to use `Comparator.nullsFirst(...)` and tie-break by run ID.
  4. Updated `getWidgetExecutions` to parse batch and filter parameters from `HX-Current-URL` / `Referer`.
  5. Updated `report-manager.js` to render a clean loading spinner before swapping, preventing stale modal reuse on network/render failures.
- **Safety Net Added:** Added Spring MockMvc integration test in `AuraReportViewControllerSpringContextTest` rendering `/widgets/executions` with live `TestExecutionDto` instances to verify full 200 OK HTML generation without template evaluation errors, and unit test in `WidgetAggregationServiceTest` verifying null-timestamp handling in chronological sorting.
