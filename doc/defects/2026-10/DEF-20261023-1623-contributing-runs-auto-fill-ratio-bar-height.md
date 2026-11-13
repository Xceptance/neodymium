# [DEF-20261023-1623] Batch History Contributing Runs List Height and Ratio Stack Matching

- **Status:** `Resolved`
- **Opened:** 2026-10-23 16:23
- **Closed:** 2026-10-23 16:28
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the Batch History Overview (`/batch-history`), `.contributing-runs-list` was clamped to an arbitrary `max-height: 275px;`, creating a large empty white space above the footer. Additionally, the left container's height should strictly match and be driven by the size of the stacked ratio bar containers on the right.
- **Root Cause:** `.contributing-runs-list` had static `max-height: 275px;` without `flex: 1; min-height: 0;`. In CSS Grid `auto` row tracks, giving an element unconstrained flex causes it to contribute to the row height, meaning 15+ runs would expand the dashboard vertically. Setting `height: 0; min-height: 100%;` on `.batch-latest-status-card` in 2-column desktop mode ensures the right column of ratio cards acts as the sole driver of row height, while the left card stretches to match and the runs list fills the available vertical space.
- **Detection Gap ("What did we miss?"):** Previous adjustment in `DEF-20261023-1142` increased `max-height` from 155px to 275px statically without tying the container to dynamic flex sizing against `.batch-ratios-stack-column`.
- **Resolution:**
  1. Updated `report-manager.css` to configure `.batch-latest-status-card` with `height: 0; min-height: 100%;`.
  2. Changed `.contributing-runs-list` to `flex: 1; min-height: 0; overflow-y: auto;` without rigid desktop `max-height`.
  3. Added responsive overrides in `@media (max-width: 1100px)` so `.batch-latest-status-card` uses `height: auto; min-height: auto;` and `.contributing-runs-list` uses `max-height: 280px;` when stacked in a single column.
  4. Bumped `report-manager.css` cache buster version in `index.html` to `?v=1.1.8`.
- **Safety Net Added:** Added `testBatchLatestStatusCardMatchesRatioColumnHeightAndFillsRunsList` in `StaticResourceSyntaxTest.java` verifying the CSS rules and responsive overrides.
