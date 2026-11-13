# [DEF-20261030-1012] Ambiguous Trend Chart X-Axis "M" Milestone Markers and Missing Date/Time Progression

- **Status:** `Resolved`
- **Opened:** 2026-10-30 10:12
- **Closed:** 2026-10-30 10:20
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In trend charts (such as `Costs Trend Graph (Daily)` or dense execution series), the X-axis displayed single-letter milestone labels like `M1`, `M2`, ..., `M7`, `Latest`. Users were confused as to what the single "M" stood for and could not discern the actual calendar dates or execution timeframes represented by each checkpoint.
- **Root Cause:** 
  1. Dense milestone sampling unconditionally assigned synthetic prefixes `M1`, `M2`, ..., `Latest` to sampled ticks without attempting to extract or format the underlying point timestamp, calendar date, or execution progression.
  2. Even when points had explicit dates (e.g. `Oct 17`, `Oct 20`, `Oct 22`), the milestone sampling loop wiped them out in favor of `M(m+1)`.
  3. When runs lacked timestamps or were on the same day, `M` was used rather than explicit, human-readable run markers (`Run 1`, `Run 4`, ..., `Latest`).
- **Detection Gap ("What did we miss?"):** Previous tests only checked that labels were compact and non-empty, and explicitly asserted `M1`/`M2` as expected output rather than testing that human-readable temporal labels (calendar dates or execution progression) were displayed to users.
- **Resolution:**
  1. Replaced ambiguous `M1..M10` markers with automatic extraction and formatting of human-readable calendar dates (`Oct 17`, `Oct 20`, `Oct 22`) when points carry date information or run IDs embed date timestamps.
  2. For series where all points occur on the same day, format checkpoints as execution time (`HH:mm`) to show intra-day progression.
  3. For non-dated or identical duplicate checkpoints, fall back to explicit `Run 1`, `Run 4`, ..., `Latest` labels instead of cryptic `M` abbreviations.
  4. Preserved concise direct identifiers (`#run-101`) for small unsampled series ($\le 8$ or $\le 10$ runs).
  5. Retained full run identifiers, timestamps, and metric values in the SVG `<title>` tooltip on hover.
- **Safety Net Added:** 
  - Updated `WidgetAggregationServiceTest.testTrendGraph_DenseRuns_GroupsXAxisInMilestones` to assert `Run 1`, `Run 4`, `Latest`, and explicitly verify that no labels start with ambiguous `M` markers.
  - Added `WidgetAggregationServiceTest.testTrendGraph_DenseRunsAcrossDays_ProducesHumanReadableDateMilestones` to verify that dense daily runs produce clean calendar date milestones (`Oct 10` .. `Oct 25`).
