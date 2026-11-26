# [DEF-20261030-0948] Trend Chart Layout Enforcing Full-Width Unconditionally and Overcrowded X-Axis Labels

- **Status:** `Resolved`
- **Opened:** 2026-10-30 09:48
- **Closed:** 2026-10-30 09:52
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** User observed that trend graph widgets were being forced to full width (12 columns) regardless of user configuration, and in datasets with many test runs or chronological points, every individual run ID was displayed on the X-axis causing severe visual label overlapping and crowding.
- **Root Cause:** 
  1. `DashboardWidgetViewDto.isFullWidth()` returned `true` unconditionally whenever `config.getPresentation() == WidgetPresentation.TREND_GRAPH`, ignoring `config.getWidth() == "half"`.
  2. JavaScript modal configurator (`report-manager.js`) and HTML fragment (`widget-configurator-modal.html`) forced the full-width radio button checked and disabled half-width selection when trend graph presentation was selected.
  3. `WidgetAggregationService.computeXAxisTicks()` produced a tick for every single data point regardless of series length `n`, leading to text collisions when `n > 7`.
- **Detection Gap ("What did we miss?"):** Previous tests in `WidgetAggregationServiceTest` tested trend graph full-width enforcement as an intentional override rather than testing that half-width configuration remains respected. Dense execution runs (> 7 points) had not been tested for X-axis label legibility and milestone grouping.
- **Resolution:**
  1. Updated `DashboardWidgetViewDto.isFullWidth()` to strictly reflect `config.isFullWidth()`, allowing user choice between half-width (6 columns) and full-width (12 columns).
  2. Maintained the lengthened chart aspect ratio (viewBox `0 0 800 200`, container height `220px`) with `preserveAspectRatio="none"` so that trend charts fit crisply in both 6-column and 12-column grid cards without vertical squashing.
  3. Removed forced `full` width override from modal UI configurator and JavaScript listeners, re-enabling user selection of half vs full width.
  4. Updated `WidgetAggregationService.computeXAxisTicks()`: when `points.size() > 7`, groups the timeline into 5 evenly distributed chronological milestones (`M1`, `M2`, `M3`, `M4`, `Latest` with `(#runId)`), ensuring clean spacing and readability even with dense test run histories. When `points.size() <= 7`, displays individual run points directly.
- **Safety Net Added:** Added automated regression test `WidgetAggregationServiceTest.testTrendGraph_HonorsSelectedWidthAndGeneratesAxes` asserting half-width preservation and full-width support, and `WidgetAggregationServiceTest.testTrendGraph_DenseRuns_GroupsXAxisInMilestones` asserting 5-milestone grouping on 25 test runs.
