# [DEF-20261029-1542] Cost Trend Graph Collapses Same-Day Runs and Distorts Point Markers into Ovals

- **Status:** `Resolved`
- **Opened:** 2026-10-29 15:42
- **Closed:** 2026-10-29 15:46
- **Component:** `aura-manager`
- **Scope:** `Framework & UI`
- **Symptom:** In dashboard trend widgets configured with Metric: COSTS and Trend Mode: LATEST_APPROACH:
  1. A batch with multiple same-day runs collapsed all runs into a single isolated dot with no connecting trend line.
  2. The point marker rendered as an elongated horizontal pill/ellipse rather than a round circle.
  3. The delta badge read `+$0.0000 vs start` when displaying a single baseline point.
- **Root Cause:**
  1. `WidgetAggregationService` routed `LATEST_APPROACH` for `COSTS` to calendar-day `tokenCostTrend.getPointGroups()`, collapsing multiple same-day runs into a single day point.
  2. When `n == 1`, `WidgetAggregationService` generated `M 150 y` which emits no visible line strokes in SVG.
  3. `dashboard-widgets.html` placed `<circle>` directly inside an SVG with `preserveAspectRatio="none"`, non-uniformly stretching the circle horizontally across wide cards.
  4. Delta calculation compared the single point to itself, outputting `+$0.0000 vs start` instead of displaying a baseline indicator.
- **Detection Gap ("What did we miss?"):** Previous unit tests only verified multi-day synthetic fixtures and tested `DAILY` fallback to `BY_RUNS`, without testing `LATEST_APPROACH` with same-day runs or visual marker geometry.
- **Resolution:**
  1. In `WidgetAggregationService.java`, when `runs.size() > 1`, populated `LATEST_APPROACH` for costs using chronological run progression (`BY_RUNS`) so same-day runs produce a real multi-point trend curve.
  2. When `n == 1`, generated a horizontal reference baseline path (`M 15 y L 285 y`) and soft area fill so a single run has clear visual context.
  3. Updated delta badge for `n <= 1` to display `Baseline` instead of `+$0.0000 vs start`.
  4. In `dashboard-widgets.html`, replaced stretching SVG `<circle>` elements with aspect-ratio protected HTML overlay point marker dots (`border-radius: 50%`) ensuring dots remain perfectly circular across any card width.
- **Safety Net Added:** Added unit tests `WidgetAggregationServiceTest.testLatestApproachCostTrendWithMultipleSameDayRuns_GeneratesRunSeries` and `WidgetAggregationServiceTest.testSinglePointTrend_GeneratesBaselineLineAndBaselineLabel`.
