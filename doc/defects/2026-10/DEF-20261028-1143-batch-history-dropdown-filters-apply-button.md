# [DEF-20261028-1143] Run History Dropdown Filters Premature HTMX Reload and Missing Apply Action

- **Status:** `Resolved`
- **Opened:** 2026-10-28 11:43
- **Closed:** 2026-10-28 11:48
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** On the `/batch-history` run history view, clicking any checkbox inside the browser or tag filter dropdowns immediately reloaded the entire page and closed the dropdown. It was impossible to check multiple options in one interaction, and selecting options when already filtered frequently reset all checkboxes back to "Select All".
- **Root Cause:** In `report-manager.js`, `toggleBatchRunScopeFilter` was directly bound to the `onchange` event of individual checkboxes and unconditionally issued `htmx.ajax` requests on every click rather than maintaining client-side draft selection state until explicitly confirmed.
- **Detection Gap ("What did we miss?"):** Previous static syntax and controller tests verified that `toggleBatchRunScopeFilter` and `selectAllBatchRunScopeFilter` were present in the template and JavaScript, but did not test multi-checkbox selection flows or assert the existence of an explicit apply confirmation button.
- **Resolution:**
  - Added an "Apply" button (`btn-apply-scope-filter`) inside the footer of each run scope dropdown menu in `batch-history.html`.
  - Updated `toggleBatchRunScopeFilter` and `selectAllBatchRunScopeFilter` in `report-manager.js` to modify checkbox states client-side without firing network requests.
  - Added `applyBatchRunScopeFilter` to evaluate checked vs. unchecked values (inclusion, exclusion, none, or all) and trigger the HTMX reload only when the button is pressed (or on Enter key).
  - Added cancellation rollback on menu close if the user closes the dropdown without applying (`revertScopeMenuCheckboxes`).
  - Updated "Browsers: None" and "Tags: None" active filter chip removal to use `removeBatchRunScopeFilter`.
- **Safety Net Added:**
  - `AuraReportViewControllerTest.testBatchHistoryRunScopeExclusionAndInclusionDisplay`: Asserts "Apply" buttons and footer markup are rendered in browser and tag dropdowns.
  - `StaticResourceSyntaxTest.testBatchRunScopeFilterDeselectAllAndExclusionSyntax`: Asserts `applyBatchRunScopeFilter`, `btn-apply-scope-filter`, and `revertScopeMenuCheckboxes` exist and verifies `toggleBatchRunScopeFilter` does not dispatch premature network requests.
