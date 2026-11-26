# [DEF-20261106-1415] Lock Execution Identity by Test and Browser in Dashboard and Widget Dialogs

- **Status:** `Resolved`
- **Opened:** 2026-11-06 14:15
- **Closed:** 2026-11-06 14:27
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In dashboard creation (`createPageModal`), convert to group (`convertToGroupModal`), and widget configurator (`widgetConfiguratorModal`) dialogs, "Browser" was rendered as an interactive checkbox while "Test" was locked. Users could uncheck "Browser", falsely implying that execution identity could be defined solely by "Test". In reality, Neodymium Aura requires Browser as a mandatory base coordinate alongside Test (Class + Method + DataSet), and unchecking Browser led to ambiguous state or silent fallback to default coordinates.
- **Root Cause:** The Thymeleaf templates rendered `Browser` as `<input type="checkbox" ... value="browser">` instead of a locked pill (`<span class="run-scope-pill active"><span class="material-symbols-outlined pill-icon">lock</span>Browser</span>`). JavaScript update functions and backend controllers did not unconditionally enforce `browser` inclusion when custom execution identities were specified.
- **Detection Gap ("What did we miss?"):** Previous syntax tests in `StaticResourceSyntaxTest` only checked for generic input presence (`createPageIdentityDimCheckbox`), and integration tests used pre-built parameter sets with `browser` already included, missing the UI inconsistency where Browser could be unchecked.
- **Resolution:**
  1. Updated `modals.html` (`createPageModal` and `convertToGroupModal`) and `widget-configurator-modal.html` to lock `Browser` alongside `Test` with a lock icon and mandatory coordinate tooltip, removing the uncheckable checkbox.
  2. Updated `report-manager.js` (`updateExecutionIdentityDimensionsInput`, `updateCreatePageIdentityDimensionsInput`, `updateConvertToGroupIdentityDimensionsInput`, `openCreatePageModal`, and `openConvertToGroupModal`) to always prepend/include `'browser'` as the immutable base coordinate.
  3. Hardened backend controllers (`AuraReportViewController.saveWidget`) and services (`ReportDashboardSettingService.createCustomPage`, `createGroupDashboard`, `convertToGroupDashboard`, `setExecutionIdentityDimensions`, and `toggleExecutionIdentityDimension`) to guarantee `browser` is always preserved and cannot be toggled off.
- **Safety Net Added:**
  - `StaticResourceSyntaxTest#testExecutionIdentityLockedByTestAndBrowserInAllDialogs`: verifies locked Browser pills exist in all three dialogs and checkboxes for `browser` are absent.
  - `ReportDashboardSettingServiceTest#testToggleExecutionIdentityDimension`: verifies toggling `browser` is a protected no-op.
  - `ReportDashboardSettingServiceTest#testCreateCustomPageEnforcesBrowserInExecutionIdentity`: verifies `browser` is enforced when omitted from page creation parameters.
  - `ReportDashboardSettingServiceTest#testCreateGroupDashboardEnforcesBrowserInExecutionIdentity`: verifies `browser` is enforced in group dashboard creation.
  - `AuraReportViewControllerTest#testSaveWidget_EnforcesBrowserInExecutionIdentityDimensions`: verifies `saveWidget` prepends `browser` when saving custom widget execution identities.
