# [DEF-20261103-1340] Known Test Variations Widget Horizontal Area Group Layout and Expanded Default

- **Status:** `Resolved`
- **Opened:** 2026-11-03 13:40
- **Closed:** 2026-11-03 13:45
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the dashboard widget for "Known Test Base Variations", all test area categories were rendered horizontally side-by-side across a single row, squishing content into narrow columns. Furthermore, all area groups and test class rows were expanded by default upon page load, overwhelming the card space.
- **Root Cause:** In `dashboard-styles.css`, `.widget-card-body` defaults to `display: flex; flex-direction: row; align-items: stretch;`. Because the test variations widget card did not override the flex direction to `column`, its `.area-group` child containers flowed horizontally side-by-side. Additionally, the Thymeleaf template `fragments/dashboard-widgets.html` hardcoded the `expanded` CSS class on both `.area-group` and `.test-class-container` elements along with the `folder_open` icon, causing full tree expansion on initial render instead of starting collapsed.
- **Detection Gap ("What did we miss?"):** The initial unit test `AuraReportViewControllerTest.testDashboardWidgets_TestVariationsRendering` only asserted string containment of the test class name and playbook execution mode in the rendered HTML output without verifying layout flex classes or checking that default container state was collapsed (`expanded` class absent).
- **Resolution:**
  1. Updated `dashboard-styles.css` to enforce `display: flex !important; flex-direction: column !important; width: 100% !important;` on `.widget-test-variations-card .widget-card-body` and `.widget-test-variations-body`, and set `width: 100%` on `.area-group`.
  2. Updated `fragments/dashboard-widgets.html` to add `d-flex flex-column gap-2 w-100` to `.widget-test-variations-body`, removed default `expanded` class from `.area-group` and `.test-class-container`, and set initial folder icons to `folder`.
  3. Updated `fragments/widget-configurator-modal.html` to auto-suggest `"Known Test Base Variations"` when switching the metric dropdown to `TEST_VARIATIONS` if an existing default title was present.
- **Safety Net Added:** Reference to `AuraReportViewControllerTest.testDashboardWidgets_TestVariationsRendering` asserting `flex-column`, presence of unexpanded classes (`class="area-group test-row-group"` and `class="test-class-container tb-class-item"`), and absence of `class="area-group test-row-group expanded"`.
