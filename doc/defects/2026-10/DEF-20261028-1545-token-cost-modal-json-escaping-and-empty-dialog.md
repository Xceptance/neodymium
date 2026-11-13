# [DEF-20261028-1545] Token Cost Breakdown Modal JSON Escaping and Empty Dialog

- **Status:** `Resolved`
- **Opened:** 2026-10-28 15:45
- **Closed:** 2026-10-28 15:47
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Clicking the token cost summary widget opens the breakdown modal dialog, but all fields display default empty placeholders ($0.0000, 0 tokens, 0 Contributing Runs, empty runs table) despite the background widget displaying non-zero costs ($0.3620).
- **Root Cause:** In `batch-history.html`, `<script id="tokenCostTrendGroupsJson">` used `th:text` instead of `th:utext`. In Thymeleaf, `th:text` on non-inlined script elements converts double quotes to `&quot;`. In `report-manager.js`, `JSON.parse(jsonEl.textContent.trim())` failed with `SyntaxError: Unexpected token & in JSON at position 2`, causing `getTokenCostPointGroupsData()` to return `[]`. In `openTokenCostBreakdownModal()`, an empty groups check triggered an early return before `renderTokenCostBreakdown()` was called, leaving default static HTML placeholders on screen.
- **Detection Gap ("What did we miss?"):** Unit tests in `TokenCostTrendDtoTest` verified Java-side JSON serialization, but template fragment tests did not assert that Thymeleaf renders unescaped raw JSON inside the `<script id="tokenCostTrendGroupsJson">` tag.
- **Resolution:**
  1. In `batch-history.html`, updated `<script id="tokenCostTrendGroupsJson" type="application/json">` to use `th:utext` instead of `th:text`.
  2. In `report-manager.js`, enhanced `getTokenCostPointGroupsData()` with HTML entity decoding (e.g. `&quot;`, `&#34;`, `&amp;`) prior to `JSON.parse()`.
  3. In `report-manager.js`, implemented `extractFallbackTokenCostGroupFromDom()` to extract current run contributions from `#batchLatestVariationsData` or `#batchContributingRunsList` and `#batchTokenCostValue` if the historical points JSON is empty, ensuring the dialog always renders meaningful data.
- **Safety Net Added:** Added automated regression test `testBatchHistoryTokenCostTrendGroupsJsonUnescaped()` in `AuraReportViewControllerTest` executing Thymeleaf template processing on `fragments/batch-history` and verifying that the rendered `<script id="tokenCostTrendGroupsJson">` contains raw unescaped JSON parsable directly by `ObjectMapper`.
