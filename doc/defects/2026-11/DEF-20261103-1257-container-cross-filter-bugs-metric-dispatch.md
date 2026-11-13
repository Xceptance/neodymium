# [DEF-20261103-1257] United Container Cross-Filtering Clobbers Bugs Pie Chart, Ratio Bar, and Text Widgets

- **Status:** `Resolved`
- **Opened:** 2026-11-03 12:57
- **Closed:** 2026-11-03 13:05
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In united widget containers containing BUGS metric widgets (Text, Pie Chart, or Ratio Bar):
  - Applying or clearing any cross-filter in the container caused the Bugs Text widget to display the total number of test variations (e.g. `114` and `114 Items`) instead of the active bug count (`1 Bug`).
  - The Bugs Pie Chart displayed `100% PASS` with `MAJOR 0 (0.0%)` and an empty 0% donut ring instead of active bug counts by severity.
  - The Bugs Ratio Bar collapsed to 0% width with `MAJOR 0 (0%)`.
- **Root Cause:**
  1. In `report-manager.js`:
     - `recalculateContainerPieChart`: Did not check for `metric === 'BUGS'`, falling through to `recalculateStatusPieChart`. This overwrote the center rate with test variation pass rate (`100% PASS`) and zeroed out severity slices (`MAJOR 0 (0.0%)`) because severity keys did not match test execution statuses (`Passed`, `Known Fail`, etc.).
     - `recalculateContainerRatioBar`: Did not handle `metric === 'BUGS'`, defaulting to `'browser'` dimension evaluation. The severity segments failed to match browser names, resulting in 0% widths and 0 count.
     - `recalculateContainerText`: Handled `STATUS` and `AI_USAGE` metrics but lacked a branch for `BUGS`, falling through to generic total count `vTotal` (number of test variations).
     - `matchesVariationFilter`: Did not evaluate bug severities (`dim === 'severity'`) against variation data.
  2. In `dashboard-widgets.html`:
     - In `pieChartBody` and `ratioBarBody`, `data-filter-dim` mapped `COSTS` to `'costs'` but defaulted `BUGS` to `'generic'`, preventing severity-based cross-filtering.
  3. In Backend DTO & Serialization:
     - `BatchLatestVariationDto` and `DashboardWidgetViewDto.serializeVariationsToJson` serialized bug ticket IDs (`v.bugs`) but omitted bug severities (`v.bsev`), preventing client-side recalculation from knowing each bug's severity when only summary, pie, or ratio widgets were present.
- **Detection Gap ("What did we miss?"):** Existing cross-filtering tests verified container pie chart, ratio bar, and text recalculation solely for `STATUS`, `COSTS`, and `AI_USAGE` metrics. Heterogeneous widget containers linking status filters with `BUGS` metric widgets were not covered.
- **Resolution:**
  1. Updated `BatchLatestVariationDto` to store `bugSeverities` and populated it in `AuraReportDataService` from active `TestBaseBugEntity.getSeverity()` values.
  2. Updated `DashboardWidgetViewDto.serializeVariationsToJson` to serialize `bsev` list when variations have bug severities.
  3. Updated `dashboard-widgets.html` to bind `data-filter-dim` to `'severity'` for `w.metric.name() == 'BUGS'` in both pie charts and ratio bars.
  4. Updated `report-manager.js`:
     - Added `extractVariationBugSeverities(variations, container)` helper resolving bug counts and severity buckets (`CRITICAL`, `MAJOR`, `MINOR`, `TRIVIAL`) from `v.bsev` (with DOM fallback).
     - Added `recalculateBugsPieChart(sec, variations, isFiltered)` updating center rate (`totalBugs`), center label (`'BUGS'`), SVG donut circle stroke dasharrays, breakdown percentage labels, and header summary.
     - Dispatched `metric === 'BUGS'` in `recalculateContainerPieChart`, `recalculateContainerRatioBar`, and `recalculateContainerText`.
     - Added support in `matchesVariationFilter` for `dim === 'severity'` checking `v.bsev`.
- **Safety Net Added:**
  - Added unit test `@Test public void testUnitedContainerBugsWidgetsCrossFilteringSupport()` in `StaticResourceSyntaxTest.java` verifying JavaScript bug severity extraction, pie chart and ratio bar recalculation, text widget bug counting, and HTML template dimension bindings.
  - Verified with `mvn test -pl aura-manager -Dtest=StaticResourceSyntaxTest,WidgetAggregationServiceTest`.
