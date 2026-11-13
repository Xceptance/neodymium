# [DEF-20261105-0935] String of Pearls Long Run ID Label Smear and Bugs Metric Pass Rate Leak

- **Status:** `Resolved`
- **Opened:** 2026-11-05 09:35
- **Closed:** 2026-11-05 09:50
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. Top milestone pill labels in String of Pearls widgets rendered as an illegible, overlapping black smudge when run IDs exceeded ~10 characters (e.g. `#run_dev_na_20261026_104200`). Bottom timestamps similarly crowded the card with raw 19-character ISO strings (`2026-10-28 10:39:06`).
  2. String of Pearls widgets tracking BUGS or COSTS metrics displayed the test suite passRate (e.g. `97%`) inside the pearls when "Percents (%)" format was selected or defaulted, completely hiding bug counts and cost values.
- **Root Cause:**
  1. `buildPerlenketteWidget` passed raw run IDs (`"#" + r.getId()`) directly as `label` without length gating or milestone indexing. In an 800px card with 6 pearls, each pearl has ~136px spacing, but a 30-character run ID consumes ~220px, causing adjacent labels to overlap by ~80px.
  2. `buildPerlenketteWidget` contained hardcoded fallback expressions `(format == WidgetValueFormat.PERCENT) ? String.format("%.0f%%", passRate) : ...` under `WidgetMetric.BUGS` and `WidgetMetric.COSTS`, substituting the suite pass rate instead of calculating metric values. Furthermore, `DashboardWidgetConfigDto` and `widget-configurator-modal.html` defaulted `valueFormat` to `PERCENT` for all metrics.
- **Detection Gap ("What did we miss?"):**
  Unit tests in `WidgetAggregationServiceTest` only tested short synthetic run IDs like `run-1` through `run-5` (length 5), masking the text collision that occurs with production run IDs like `run_dev_na_20261026_104200`. In addition, existing tests asserted `valFormatted` on `STATUS` widgets, but never verified that a `BUGS` widget with `PERCENT` format actually represented bug data rather than test pass rates.
- **Resolution:**
  1. Added smart milestone label formatting in `buildPerlenketteWidget`:
     - If run ID is short (<= 8 chars, e.g. `#104`, `r1`), retain it directly.
     - If run ID is long (> 8 chars, e.g. `run_dev_na_...`), format as sequential milestone markers `Run 1`, `Run 2`, ..., `Run N`, matching the widget's milestone badge. Full raw ID is preserved in the SVG `<title>` tooltip and link URL.
     - Formatted bottom timestamps to intra-day times (`HH:mm:ss` or `HH:mm`) when runs occur on the same day, or compact dates (`MMM dd`) when spanning multiple days, keeping the full ISO timestamp in the tooltip.
  2. Fixed metric value evaluation for `BUGS` and `COSTS`:
     - Under `WidgetMetric.BUGS`: if format is `PERCENT`, compute actual bug rate `(totalTests > 0) ? ((double) bugCount / totalTests * 100.0) : 0.0`, completely eliminating the pass rate leak.
     - Under `WidgetMetric.COSTS`: format as currency `$%.4f`, eliminating the pass rate leak.
     - In `widget-configurator-modal.html`, default `valueFormat` to `ABSOLUTE` when metric is `BUGS` or `COSTS`.
- **Safety Net Added:**
  - Added unit test `WidgetAggregationServiceTest.testPerlenkette_LongRunIds_ProducesCompactMilestoneLabels` verifying that 30-char production run IDs format as compact milestone labels (`Run 1` .. `Run 6`) without horizontal text overlap.
  - Added unit test `WidgetAggregationServiceTest.testPerlenkette_BugsMetricWithPercentFormat_DoesNotLeakPassRate` asserting that a Bugs widget under percent format calculates bug rate rather than leaking test pass rates.
