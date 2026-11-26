# [DEF-20261105-1407] Run-Level Tag Trend Graph Shows Only "Default" While Pie Chart Shows Values

- **Status:** `Resolved`
- **Opened:** 2026-11-05 14:07
- **Closed:** 2026-11-05 14:16
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the dashboard, widgets configured with `WidgetMetric.TAG` (such as Release, Build, Branch) show accurate tag distributions in the Pie Chart widget (e.g. showing "2026.10" and "Default"), but the corresponding Trend Graphs in both "Suite Evolution" and "By Runs" modes display only a single "Default" layer with 100% of tests.
- **Root Cause:** 
  1. `WidgetAggregationService.extractExecutionTagValue()` only checked `TestExecutionDto.getTags()`. If missing on the execution, it immediately returned `"Default"` without inspecting parent run tags on `TestRunEntity` or querying `dataService.getRunTagValues(run, tagKey)`.
  2. `AuraReportDataService.getLatestExecutions()` populated `varTagsMap` solely from `exec.getTags()` during suite evolution compilation without falling back to run tags, causing `BatchSuiteEvolutionPointDto.tagCountsByType` to be empty for tags defined on runs.
- **Detection Gap ("What did we miss?"):** Existing unit tests for `WidgetAggregationServiceTest` verified tag trends using built-in dimensions (browser/area/locale) or mock executions with execution-level tags, but never tested custom tags attached exclusively to `TestRunEntity.getTags()` or `TestRunTagEntity`.
- **Resolution:** Updated `WidgetAggregationService.extractExecutionTagValue()` to inspect parent `run` tags and `dataService.getRunTagValues(run, normKey)` before defaulting to `"Default"`. Updated `AuraReportDataService.getLatestExecutions()` to merge run-level tags into `varTagsMap` and cumulative variation tags during suite evolution calculation.
- **Safety Net Added:** Added unit tests `testTrendGraph_TagMetric_RunLevelTags_ByRunsMode`, `testTrendGraph_TagMetric_RunLevelTags_WithDataServiceExecutionReport`, and `testTrendGraph_TagMetric_SuiteEvolution_WithRunLevelTags` in `WidgetAggregationServiceTest`.
