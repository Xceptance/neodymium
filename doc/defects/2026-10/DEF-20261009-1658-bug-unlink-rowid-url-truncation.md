# [DEF-20261009-1658] Bug Unlinking Fails to Persist Across Page Reload Due to Unescaped RowId Hash in HTMX Delete URL

- **Status:** `Resolved`
- **Opened:** 2026-10-09 16:58
- **Closed:** 2026-10-09 17:09
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In Aura Report UI, unlinking a bug ticket from a test execution displays "Clean / No Linked Bugs" immediately in the side panel, but upon page reload (F5) or report refresh, the unlinked bug ticket returns and the test status reverts to failed-known / succeeded-fixed.
- **Root Cause:** 
  1. In `side-panel-step-list.html`, `th:hx-delete` interpolated `rowId=${exec?.id ?: rowId}` without `#uris.escapeQueryParam(...)`. When `exec.id` was a composite execution key containing `#` delimiters (`TestClass#method#title#browser`), the browser truncated the request URL at the first `#` as a client-side fragment identifier (RFC 3986), dropping `#method#title#browser&bugTicket=...` entirely.
  2. The server received `DELETE /fragments/test-side-panel/bugs` with `rowId` truncated to `TestClass` and `bugTicket == null`.
  3. `AuraReportDataService.removeBugFromExecution` saw `bugTicket == null`, skipped all database/disk mutation logic, and returned an empty `new TestExecutionDto()`.
  4. `side-panel-step-list.html` rendered the empty DTO as `Clean / No Linked Bugs`, giving false client-side confirmation while disk (`console-execution-*.json`, `run.json`) and database (`test_base_bug`) records remained unchanged.
  5. In `AuraReportDataService.addBugToExecution` and `removeBugFromExecution`, the final return DTO lookup only checked `rowId.equalsIgnoreCase(e.getId())` without composite key resolution.
  6. In `AuraReportViewController.removeBugFromExecution`, the controller passed hollow DTOs (`id == ""`) directly to the model without falling back to existing execution details.
- **Detection Gap ("What did we miss?"):** Existing controller and data service unit tests passed synthetic IDs like `row-unlink-1` without `#` characters and asserted immediate in-memory return values without validating URI percent-encoding or truncated query parameters in HTMX action attributes.
- **Resolution:** 
  1. In `side-panel-step-list.html`, wrapped both `rowId` and `runId` in `${#uris.escapeQueryParam(...)}` within `th:hx-delete`, ensuring `#` is encoded as `%23`.
  2. In `AuraReportDataService.removeBugFromExecution` and `addBugToExecution`, return `getExecutionDetails(effectiveRunId, rowId)` when `bugTicket` is null/blank, and resolve the final updated DTO using `matchesExecution(e, rowId)` with fallback.
  3. In `AuraReportViewController.removeBugFromExecution`, fallback to `dataService.getExecutionDetails(effectiveRunId, effectiveRowId)` if the return DTO is hollow (`id == null || id.isBlank()`).
- **Safety Net Added:** 
  - `AuraReportBugUnlinkTest.testBugLinkAndUnlinkPersistenceWithCompositeKeyContainingHashes`: verifies linking and unlinking with composite keys containing `#`, asserts non-hollow fallback on null ticket, and validates persistence across cache clear and disk reload.
  - `AuraReportViewControllerTest.testRemoveBugFromExecutionWithCompositeKeyContainingHashes`: validates controller handles composite keys with `#`.
  - `AuraReportViewControllerTest.testRemoveBugFromExecutionFallsBackToExecutionDetailsWhenRemovalYieldsHollowDto`: asserts controller falls back to existing execution details when unlinking yields a hollow DTO.
