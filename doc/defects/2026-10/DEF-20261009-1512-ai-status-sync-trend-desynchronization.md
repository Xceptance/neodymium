# [DEF-20261009-1512] AI Status Sync and History Trend Pattern Desynchronization

- **Status:** `Resolved`
- **Opened:** 2026-10-09 15:12
- **Closed:** 2026-10-09 15:20
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** AI-driven tests with linked bug tickets (`failed-known` or `succeeded-fixed`) rendered with solid flat colors rather than diagonal AI stripes (`.seg-ai-print`, `trendLayerKnownAiPattern`, `trendLayerFixedAiPattern`) in the History Trend SVG and status breakdown bars. Concurrently, a phantom 5th segment (`seg-unknown seg-ai-print`, 20% width) appeared in the status breakdown bar, causing the bar's total width to sum to 120%. In area trend SVGs (`batch-history.html`), `<path>` layer elements for `area-layer-known-ai`, `area-layer-known-ai-pat`, `area-layer-fixed-ai`, and `area-layer-fixed-ai-pat` were missing from the SVG DOM.
- **Root Cause:** 
  1. When bug tickets are linked or unlinked, `AuraReportDataService.recalculateRunEntityStats()` and `updateCachedReportBugsAndStats()` updated `failedKnownCount` and `failedUnknownCount` on `TestRunEntity`, but omitted recalculating or updating `failedKnownAiCount` and `failedUnknownAiCount`. Because `TestRunEntity` retained `failedUnknownAiCount=1` while `failedKnownAiCount=0`, `TestRunEntity.getKnownCleanCount()` computed `Math.max(0, 1 - 0 - 0) = 1` (solid orange segment), while `failedUnknownAiCount=1` rendered a phantom 20% striped red segment.
  2. In `AuraReportDataService.getBatchOverviewData()` and `AuraReportViewController.batchHistory()`, entity synchronization only triggered if entity count fields were `null`, failing to detect divergence when underlying execution bug states changed.
  3. In `batch-history.html`, the `.area-trend-svg` SVG definition omitted `<path>` elements for `area-layer-known-ai`, `area-layer-known-ai-pat`, `area-layer-fixed-ai`, and `area-layer-fixed-ai-pat`, even though `report-manager.js` computed and populated their `d` attributes.
- **Detection Gap ("What did we miss?"):** Prior bug link/unlink tests (`AuraReportBugUnlinkTest`) only asserted standard counts (`knownCount`, `unknownCount`, `getStatus()`) on non-AI test fixtures and never asserted granular AI counter updates on `TestRunEntity`. Area trend SVG tests only validated JSON and JS syntax without asserting SVG path layer presence in Thymeleaf templates.
- **Resolution:**
  1. Updated `AuraReportDataService.updateCachedReportBugsAndStats()` and `recalculateRunEntityStats()` to compute and persist all 4 AI counters (`passedAiCount`, `succeededFixedAiCount`, `failedKnownAiCount`, `failedUnknownAiCount`) to `TestRunEntity`.
  2. Updated synchronization loops in `AuraReportDataService.getBatchOverviewData()` and `AuraReportViewController.batchHistory()` to detect divergence between `TestRunEntity` and `RunReportDto` and save updated counts.
  3. Added the 4 missing `<path>` elements for known AI and fixed AI area layers to `.area-trend-svg` in `batch-history.html`.
- **Safety Net Added:** 
  - `AuraReportBugUnlinkTest.testAiDrivenExecutionBugLinkAndUnlinkUpdatesRunEntity` explicitly verifying that linking and unlinking bug tickets on AI-driven executions properly transitions `failedKnownAiCountSafe` and `failedUnknownAiCountSafe` on `TestRunEntity`.
  - `StaticResourceSyntaxTest.testAreaTrendSvgLayersIncludeKnownAndFixedAiPaths` asserting `area-layer-known-ai`, `area-layer-known-ai-pat`, `area-layer-fixed-ai`, and `area-layer-fixed-ai-pat` exist in `batch-history.html`.
