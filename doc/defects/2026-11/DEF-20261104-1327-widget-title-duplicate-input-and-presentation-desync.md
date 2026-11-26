# [DEF-20261104-1327] Duplicate Widget Title Form Input Appending Text on Save and Static Name Suggestion Desync

- **Status:** `Resolved`
- **Opened:** 2026-11-04 13:27
- **Closed:** 2026-11-04 13:46
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Every time a dashboard widget is edited, "Status Pie Chart" (or its prior title) is appended with a comma to the widget title. Furthermore, when configuring a widget, the suggested name is always stuck on "Status Pie Chart" regardless of the selected metric (Costs, Bugs, Tags, Flakiness) or presentation type (Trend Graph, Ratio Bar, String of Pearls, KPI, List).
- **Root Cause:** 
  1. `widget-configurator-modal.html` contained two `<input name="title" id="widgetTitleInput">` fields within the same `<form>` (one in the top "Optional Custom Title" section and an unmaintained duplicate in section 8 "Title & Sizing"). Spring MVC binds multi-value query/form parameters into comma-joined strings, appending the section 8 value to the edited title on every form submit.
  2. For new widgets, `getWidgetConfigModal` set `targetWidget = new DashboardWidgetConfigDto()`, pre-populating `widget.title` with "Status Pie Chart" in `th:value`, preventing dynamic placeholder and auto-suggestion behavior.
  3. Client-side change handlers (`onWidgetMetricChanged`, `onWidgetPresentationChanged`) lacked dynamic name suggestion logic responding to presentation, metric, tag dimension, and trend mode changes.
- **Detection Gap ("What did we miss?"):** Existing static resource and controller unit tests verified that `widgetConfiguratorModal` fragment renders and that `saveWidget` accepts a single `title` parameter, but never simulated full form submissions containing duplicate input names or client-side metric/presentation switching events.
- **Resolution:** 
  1. Removed duplicate `<input name="title">` from section 8 of `widget-configurator-modal.html` (renaming section 8 to "8. WIDGET SIZING") and added server-side title sanitization in `AuraReportViewController.saveWidget()` to strip duplicate comma-joined tokens.
  2. Kept `widgetTitleInput.value` empty on new widget creation while dynamically showing the suggested title in `placeholder` and updating `value` reactively until the user manually types a custom title.
  3. Implemented `updateSuggestedWidgetTitle()` and `computeSuggestedWidgetTitle()` in `widget-configurator-modal.html` and `report-manager.js` to compute intelligent, presentation-aware default names (e.g. `Status KPI`, `Cost Trend Graph`, `Browser Ratio Bar`, `Bug Breakdown List`).
  4. Refined `DashboardWidgetConfigDto.generateDefaultTitle(...)` to match the enhanced presentation-dependent naming conventions.
- **Safety Net Added:** 
  - `StaticResourceSyntaxTest.testWidgetConfiguratorModalNoDuplicateTitleInputAndHasNameSuggestion`: Asserts exactly one `widgetTitleInput` exists and dynamic suggestion hooks are wired.
  - `DashboardWidgetConfigDtoTest.testGenerateDefaultTitlePresentationAwareness`: Asserts default title generation across all metric and presentation combinations.
  - `AuraReportViewControllerTest.testSaveWidgetSanitizesCommaAppendedTitles`: Verifies comma-joined duplicate titles from legacy submissions are cleaned up.
