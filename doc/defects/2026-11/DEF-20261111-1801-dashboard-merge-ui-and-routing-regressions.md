# [DEF-20261111-1801] Dashboard Merge UI Routing and Interaction Regressions

- **Status:** `Resolved`
- **Opened:** 2026-11-11 16:30
- **Closed:** 2026-11-11 19:05
- **Component:** `aura-manager`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** After merging `origin/develop` into `feat/draft-of-new-dashboard`, `AuraManagerEditorUiTest` failed with 19 errors across root URL routing (`#pageCustomDashboard` missing), legacy batch buttons (`#saveQueueAsBatchSelectionBtn` existing), missing element hooks (`#editorTitle`, `#addStepBtn`, `#addVarBtn`, `#openModalFragmentEditorBtn`), modal interception, substep toggle propagation, and include tree action clipping/visibility.
- **Root Cause:**
  1. `ReportDashboardSettingService.getCustomPages` returned an empty list when JSON settings were `"[]"` instead of falling back to `getDefaultCustomPages()`, causing the root view to fall back to `batchOverview` rather than `#pageCustomDashboard`.
  2. Root `@GetMapping("/")` did not redirect requests with `?file=` query parameters to `/playbooks`, breaking test file deep links.
  3. `test-selection.html` retained legacy `#saveQueueAsBatchSelectionBtn` removed on develop.
  4. `editor.html` was missing `id="editorTitle"`, `id="addStepBtn"`, `id="addVarBtn"`, and `id="openModalFragmentEditorBtn"` on the header/palette hooks relied upon by UI tests.
  5. `dashboard-editor.js` stopped click propagation unconditionally in `handleSubstepTitleClick` on `.substeps-title`, causing Selenide center clicks on `.substeps-toggle-bar` to be absorbed without toggling sub-step visibility, and preferred stale `data-raw` over updated `innerText` in `getStepRawText`.
  6. In `dashboard-editor.js`, `enableIncludeEdit` force-focused step 0 unconditionally, which replaced token pills with raw text mode and broke assertions expecting un-focused pills.
  7. `.include-tree-header` and `.include-tree-actions` lacked `flex-wrap: wrap`, causing action buttons (`btnSave`, `btnDiscard`) to overflow horizontally beyond the card boundary where an ancestor's `overflow-x: hidden` clipped them, rendering them `displayed:false` in Selenium.
  8. `AuraManagerEditorUiTest` ran without explicit `browserSize`, defaulting to smaller viewports that aggravated horizontal clipping in multi-column layouts.
- **Detection Gap ("What did we miss?"):** Unit tests ran against mock controllers and services without end-to-end browser DOM interaction between dashboard root routing, editor URL forwarding, responsive layout overflow boundaries, and UI click event bubbling.
- **Resolution:**
  1. Updated `ReportDashboardSettingService.getCustomPages()` to fall back to `getDefaultCustomPages()` when parsed list is empty.
  2. Added `@RequestParam(value = "file", required = false)` handling to `rootIndex` in `AuraReportViewController.java` to redirect to `/playbooks?file=...`.
  3. Removed `#saveQueueAsBatchSelectionBtn` from `test-selection.html`.
  4. Added `id="editorTitle"`, `id="addStepBtn"`, `id="addVarBtn"`, and `id="openModalFragmentEditorBtn"` to `editor.html`.
  5. Updated `getStepRawText` to prioritize live `innerText` when focused or when no pills exist, and updated `handleSubstepTitleClick` to only stop propagation when the title is actively focused.
  6. Added `ensureIncludeTreeCardLoaded(cardId, cleanFilePath, targetRow)` to auto-load top-level include cards without toggling visibility to `none`.
  7. Updated `enableIncludeEdit` to only call `addNestedStep` when the fragment has 0 steps, keeping existing steps un-focused so their token pills remain visible until clicked.
  8. Added `flex-wrap: wrap; gap: 8px;` to `.include-tree-header` and `.include-tree-actions` in `dashboard-styles.css` to prevent horizontal clipping.
  9. Configured `Configuration.browserSize = "1920x1080"` in `AuraManagerEditorUiTest#setup`.
- **Safety Net Added:**
  - `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testRootUrlLoadsFirstDashboardByDefaultAndNavigatesToPlaybooks`
  - `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testPlaybooksQueuePageHasNoSaveAsBatchButtonAndProvidesRunTagging`
  - `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testOpenFoldedScalarStepsRendersDiscreteRowsInVisualEditor`
  - `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testTypeHashInsideStepLineStylesInlineCommentImmediatelyAndPersistsOnSave`
  - `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testRemoveDataSetColumnWithConfirmation`
  - `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testRemoveVariableRowFromDataMatrix`
  - `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testHierarchicalStepFragmentsDisplayOnlyFileNames`
  - `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testDiscardIncludeFileRestoresOriginalSteps`
  - `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testPressEscapeWithCreateModalOpenDoesNotCloseEditor`
  - `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testInsertVariableFromTestDataIntoInlineIncludeFragment`
  - `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testBackspaceOnNewLineInIncludeMovesCursorToEndWithoutLineNumberText`
  - `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testHighlightVariablesAndIncludesOnIncludeEdit`
