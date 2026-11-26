# [DEF-20261030-1005] Trend Chart X-Axis Low Milestone Density, Label Collision, and Alphabetical Sorting Fallback

- **Status:** `Resolved`
- **Opened:** 2026-10-30 10:05
- **Closed:** 2026-10-30 10:09
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** User observed that trend chart X-axes showed too few milestone groups (hardcoded to 5 intervals) and appended full 35-character raw run IDs (e.g. `M2 (#run_custom_stg_20261022_173500)`), causing unreadable horizontal label collisions across the chart and clipping of `M1` on the left edge. Furthermore, when `TestRunEntity.startTimeMs` was null, runs sorted alphabetically by run ID string rather than chronologically, and `DAILY` trend mode collapsed all runs into today's bucket.
- **Root Cause:** 
  1. Milestone sampling was hardcoded to only 5 checkpoints (`milestoneCount = 5`) regardless of chart width.
  2. Suffix generation unconditionally appended full raw run IDs up to 40 characters wide, resulting in text overlapping across the 800px SVG timeline.
  3. Axis labels used uniform middle anchoring (`text-anchor="middle"`) at the left boundary (`padLeft = 55.0`), causing leftmost labels like `M1` to be clipped off-screen to the left.
  4. Chronological sorting and time bucketing relied solely on `TestRunEntity.getStartTimeMs()`. When null, sorting fell back to string ID comparison (e.g. `run_20261028` sorted before `run_custom_stg_20261022`), and daily bucketing collapsed into a single bucket and fell back to raw runs.
- **Detection Gap ("What did we miss?"):** Previous unit tests used short synthetic IDs (`run-01`) rather than realistic test runner IDs, did not assert label character width bounds, and did not test fallback timestamp extraction when `startTimeMs` was null.
- **Resolution:**
  1. Increased milestone checkpoints to 8 on half-width cards and 10 on full-width cards.
  2. Formatted milestone labels compactly (`M1..M10` or short date buckets `MMM dd` $\le 10$ chars), moving the full run ID, timestamp, and formatted metric value into the SVG `<title>` tooltip.
  3. Implemented responsive SVG `text-anchor`: `start` on the first tick, `end` on the last tick, and `middle` on intermediate ticks, eliminating boundary clipping.
  4. Added `TestRunEntity.getEffectiveStartTimeMs()` to robustly extract timestamps from `startTimeMs`, `timestampLabel`, or regex pattern `\d{8}[_-]\d{6}` in `id`, ensuring correct chronological sorting and distinct daily bucket aggregation.
- **Safety Net Added:** Added automated tests `WidgetAggregationServiceTest.testTrendGraph_DenseRuns_GroupsXAxisInMilestones` (asserting 8- and 10-milestone density, compact label lengths $\le 10$, and hover tooltips) and `WidgetAggregationServiceTest.testTrendGraph_ChronologicalOrderingFromRunIdTimestamp` (asserting chronological ordering and daily bucketing from composite run IDs with null `startTimeMs`).
