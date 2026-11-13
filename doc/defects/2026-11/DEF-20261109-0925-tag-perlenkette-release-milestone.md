# [DEF-20261109-0925] Tag Metric Perlenkette Truncation to Single Latest Release and Mutual Exclusivity UI Leak

- **Status:** `Resolved`
- **Opened:** 2026-11-09 09:25
- **Closed:** 2026-11-09 09:38
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. In the Widget Configurator modal for String of Pearls (`PERLENKETTE`), both "Target Point / Time Scope" (`pointScope`) and "Trend Time Grouping / Mode" (`trendMode`) were simultaneously rendered and selectable in the UI.
  2. Configuring a String of Pearls widget with `Metric: TAG` (e.g. Tag Dimension `Release`) displayed only a single latest release pearl (`2026.10`) and `Default` sorted by test count descending, completely hiding historical release milestones (`2026.09`).
- **Root Cause:**
  1. In `widget-configurator-modal.html`, `widgetTrendModeSection` server-side condition did not exclude `metric == 'TAG'`, causing both `pointScope` and `trendMode` sections to render simultaneously when configuring a String of Pearls.
  2. In `WidgetAggregationService#populateTagPerlenketteBeads`, tag telemetry was queried from `latestVariations`, which by design deduplicates tests to only the single latest execution across history, dropping earlier release executions. Furthermore, beads were sorted by `s.total` descending rather than chronologically by release timestamp.
- **Detection Gap ("What did we miss?"):**
  Existing unit tests in `WidgetAggregationServiceTest#testAggregateWidgetTagMetricPerlenketteProducesBeadsPerTagValue` tested only browser tag beads in a single run (`run-1`) with mock `latestVariations` and asserted sorting by test volume descending. Multi-run historical release milestone progression across distinct runs was never exercised in automated tests.
- **Resolution:**
  1. Updated `widget-configurator-modal.html` to hide `widgetTrendModeSection` when `widget.metric != null && widget.metric.name() == 'TAG'` and synchronized presentation visibility in `report-manager.js` upon HTMX modal load.
  2. In `WidgetAggregationService#populateTagPerlenketteBeads`, aggregated historical runs chronologically across `scopedRuns`, deduplicating test executions per milestone, recording earliest/latest timestamps and `latestRunId`, and sorting beads chronologically (or via `compareNaturalVersion` for milestone tags like `Release` / `Sprint`).
  3. Added bead navigation links pointing to the latest run report for each milestone bead (`/run-report?runId=...`).
- **Safety Net Added:**
  `WidgetAggregationServiceTest#testTagPerlenketteChronologicalMilestonesAcrossRuns` explicitly verifying that multiple runs tagged with distinct release milestones are correctly aggregated, ordered chronologically, and display accurate pass rates and test counts.
