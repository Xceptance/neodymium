# [DEF-20261110-1205] Known Test Variations Widget Row Click Fails to Display Variation History

- **Status:** `Resolved`
- **Opened:** 2026-11-10 12:05
- **Closed:** 2026-11-10 12:36
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Clicking a test variation entry in the "Known Test Base Variations" widget opened the side panel with blank single-run execution controls (showing 0 duration/steps/cost and "Select a test execution row to view steps") rather than loading and displaying the cross-batch test variation execution history table.
- **Root Cause:** In `dashboard-widgets.html`, variation rows prefixed their HTML `id` attribute with `'widget-' + ${w.id} + '-row-var-' + ${var.id}`, while leaving `data-row-id` as `'row-var-' + ${var.id}`. `handleRowClick` preferred `data-row-id`, passing `"row-var-X"` to `openTestSidePagePanel`. `document.getElementById("row-var-X")` returned `null`, which caused `isTestBaseRow` to fall back to `document.getElementById('pageTestBase') !== null` (which evaluates to false on dashboard pages). As a result, the side panel routed to single execution step rendering instead of `loadTestBaseVariationHistory()`.
- **Detection Gap ("What did we miss?"):** Existing UI tests verified widget configurator settings and Test Base page variation clicks, but lacked an automated check validating that row click events originating from within the Known Test Base Variations widget successfully route to variation history in the unified side panel drawer.
- **Resolution:** Aligned `th:data-row-id` with `th:id` in `dashboard-widgets.html`, updated `handleRowClick` and `focusAndInspectExecution` to prioritize `row.id` over `data-row-id`, added defensive multi-selector fallbacks and `.widget-test-variations-body` checking in `openTestSidePagePanel`, and bound `tbSideBatchBadge` to the active variation's batch.
- **Safety Net Added:** Added `@Test` `StaticResourceSyntaxTest.testKnownTestVariationsWidgetRowClickOpensVariationHistorySafetyNet` verifying template markup contract, JavaScript event handling, and mock DOM behavior.
