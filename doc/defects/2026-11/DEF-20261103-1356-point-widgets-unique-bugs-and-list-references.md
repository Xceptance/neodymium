# [DEF-20261103-1356] Point-in-Time Widgets Inflate Bug Counts by Counting Assignments Instead of Unique Defects

- **Status:** `Resolved`
- **Opened:** 2026-11-03 13:56
- **Closed:** 2026-11-03 14:02
- **Component:** `aura-manager`
- **Scope:** `Framework & UI`
- **Symptom:** In point-in-time widgets (Pie Chart donut, Ratio Bar segments, Text KPI, Text with Small Trend, and List widget severity headers) configured for the `BUGS` metric, the displayed bug count reflected the total number of test variation assignments rather than distinct defects. When a single defect ticket (e.g. `BUG-12345`) was assigned to multiple test variations, point widgets inflated the count (e.g., displaying `2 Active Bugs` when only 1 unique defect existed), creating a visual contradiction with single-run failure trends.
- **Root Cause:**
  1. In `WidgetAggregationService#buildPieChartWidget`, `totalBugs` and `sevCounts` were incremented for every `TestBaseBugEntity` record without de-duplicating by normalized bug ticket (`b.getBugTicket()`). This inflated center rate, summary labels, and slice counts, which in turn cascaded into Ratio Bar and Text KPI widgets.
  2. In `WidgetAggregationService#buildListWidget`, severity group counts were taken directly from `slice.getCount()`, displaying assignment totals instead of unique defects.
  3. In `report-manager.js`:
     - `extractVariationBugSeverities` iterated `v.bugs` without tracking seen tickets, causing client-side container cross-filtering to re-inflate unique counts.
     - `recalculateContainerList` set `countSpan` to the total visible DOM subitems (`visibleCount`) rather than the number of unique bug tickets among visible subitems.
- **Detection Gap ("What did we miss?"):** Prior unit tests (`testBugsPieChartAggregation`, `testListWidgetPresentationBugsWithSubItemsAndTags`) only tested datasets where each bug ticket had exactly one test variation link, masking the assignment duplication behavior.
- **Resolution:**
  1. Added `getBugSeverityRank(sev)` and `resolveUniqueBugs(bugs, scope, scopedRunIds)` in `WidgetAggregationService.java` to de-duplicate defects by normalized ticket key while resolving conflicting severities to the highest priority (`CRITICAL > MAJOR > MINOR > TRIVIAL`).
  2. Updated `WidgetAggregationService#buildPieChartWidget` to compute `sevCounts` and `totalBugs` from `resolveUniqueBugs`, ensuring Pie Chart, Ratio Bar, and Text widgets display unique bug counts.
  3. Updated `WidgetAggregationService#buildListWidget` to filter and group `subItems` by the unique bug's effective severity, ensuring the severity group header reflects the unique bug count while all individual occurrence references remain visible in `subItems`.
  4. Updated `report-manager.js`:
     - Added `getBugSeverityRank` and updated `extractVariationBugSeverities` to de-duplicate unique bug tickets.
     - Updated `recalculateContainerList` to count unique tickets among visible subitems when `metric === 'BUGS'`.
- **Safety Net Added:**
  - Added unit test `testBugsPointWidgetsDeduplicateByTicketWithAllReferencesInList` in `WidgetAggregationServiceTest.java` verifying that multi-occurrence bugs count as 1 in Pie Chart, Ratio Bar, Text, and List severity group counts, while preserving all occurrence references in `subItems`.
  - Added unit test `testUniqueBugsPointWidgetsDeduplicationInJavaScript` in `StaticResourceSyntaxTest.java` verifying JavaScript de-duplication in `extractVariationBugSeverities` and `recalculateContainerList`.
  - Verified with `mvn test -pl aura-manager -Dtest=StaticResourceSyntaxTest,WidgetAggregationServiceTest` (128 passed, 0 failures).
