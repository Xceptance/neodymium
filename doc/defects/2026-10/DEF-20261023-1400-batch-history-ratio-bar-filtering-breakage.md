# [DEF-20261023-1400] Batch History Ratio Bar Filtering Breakage and Missing Tag Filter Dimension

- **Status:** `Resolved`
- **Opened:** 2026-10-23 14:00
- **Closed:** 2026-10-23 14:05
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Clicking segments or legend chips in the Browser, Locale, Environment, Area, or Tag ratio bars on the Batch History / All Runs page did not filter the test variations, run history rows, or update the donut chart and ratio visual states. Clicking Clear buttons had no effect.
- **Root Cause:** Ratio bar event handlers (`filterFromRatioBar`, `resetBatchRatioFilter`, `applyBatchDirectoryFilters`, `syncBatchRatioBarsVisualState`, and `recalculateBatchLatestStatusPie`) in `report-manager.js` were tightly coupled to the legacy multiselect dropdown menus (`batchEnvMultiselectMenu`, etc.). When those dropdown menus were removed in favor of the Group By toolbar, all ratio bar operations failed with null checks and early returns. In addition, the new Tag: Batch ratio dimension was not handled in JavaScript, and run history rows lacked `data-batch` attributes.
- **Detection Gap ("What did we miss?"):** Existing controller and Thymeleaf integration tests verified server-side template generation and HTML element presence, but did not exercise client-side JavaScript DOM event delegation for ratio bar interaction.
- **Resolution:** Decoupled ratio bar filtering into an autonomous state manager (`batchActiveRatioFilters`), added complete support for all 5 dimensions (`browser`, `locale`, `env`, `area`, `tag`), dynamically updating row visibility, donut charts, cross-ratio distributions, and Clear button states, and injected `data-batch` into run rows and test base variation rows.
- **Safety Net Added:** Added assertions in `AuraReportViewControllerTest` (`testBatchHistoryTemplateRendersWithoutSpelErrorsWhenAiPassedTestsPresent`) validating `data-batch` attributes across run rows and variation rows.
