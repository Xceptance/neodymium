# [DEF-20261104-1525] String of Pearls Hidden Labels, Rollercoaster Wave Disconnect, and Anchor Underline Glitch

- **Status:** `Resolved`
- **Opened:** 2026-11-04 15:25
- **Closed:** 2026-11-04 15:45
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** The String of Pearls (`PERLENKETTE`) milestone progression widget presented multiple severe usability and aesthetic flaws:
  1. Milestone labels were completely hidden by default (`opacity: 0; visibility: hidden;`), requiring users to manually hover over each individual pearl to know its run ID, timestamp, or temporal order.
  2. Numbers inside the pearls had an unsightly text underline slicing through them due to unstyled SVG `<a>` anchor tags defaulting to browser link decoration.
  3. Pearls sat in a rigid horizontal row while the connecting thread oscillated in a dramatic sine wave that dipped below the centerline, directly intersecting bottom label coordinate space and creating visual dissonance.
  4. Inflection accent dots were mathematically placed within the pearl radius (`xa + (midX - xa) * 0.42`), resulting in partially occluded crescent "pimple" blemish artifacts peeking out from behind pearl borders.
- **Root Cause:**
  1. `dashboard-styles.css` restricted `.perlenkette-sub-label` visibility strictly to `:hover` and `:focus-within` states.
  2. `.perlenkette-pearl-anchor` lacked `text-decoration: none !important;`, causing browser user-agent stylesheets to apply underlines to child `<text class="perlenkette-pearl-value">`.
  3. `WidgetAggregationService.buildPerlenketteWidget` generated sine-wave Bézier paths oscillating between `cy - waveAmp` and `cy + waveAmp` around horizontal pearls and calculated minor dot coordinates (`infX`, `infY`) that fell inside the 36px/24px pearl radius.
- **Detection Gap ("What did we miss?"):**
  Existing unit tests in `WidgetAggregationServiceTest` validated that `svgWavePath` was non-empty and that `minorDots` count met threshold conditions, but never checked for mathematical separation between pearl boundaries and accent dots, nor did they assert permanent label visibility or link style normalization.
- **Resolution:**
  1. Transitioned the String of Pearls visualization to the **Precision DevOps Timeline Rail** paradigm:
     - Straight horizontal pipeline track rail running along `cy = height / 2.0` with right-pointing directional progress chevrons (`>`) at segment midpoints.
     - Relocated minor accent dots to clean intermediate positions along the rail strictly outside pearl boundaries (`xa + radius + 12.0` and `xb - radius - 12.0`).
  2. Implemented permanent dual-tier milestone typography in `dashboard-widgets.html`:
     - Top Tier: Compact monospace milestone pill badge (`bead.label`) with rounded background capsule.
     - Latest Badge: Added glowing `LATEST` badge indicator over the most recent pearl node.
     - Bottom Tier: Monospace timestamp label (`bead.timestampLabel`) centered beneath each pearl.
  3. Redesigned pearl nodes into modern concentric ring gauges with high-contrast centered values and zero text-decoration underlines.
  4. Updated `dashboard-styles.css` with dark/light mode token support for rails, chevrons, pills, and hover elevations.
- **Safety Net Added:**
  - Added unit test `WidgetAggregationServiceTest.testPerlenketteTimelineRailAndNoOccludedDots` asserting that track rail path contains straight linear segments and directional chevrons, that all minor accent dots lie strictly outside pearl radii, and that permanent labels are populated.
  - Added CSS contract assertions in `StaticResourceSyntaxTest` ensuring `.perlenkette-pearl-anchor` enforces `text-decoration: none` and `.perlenkette-top-pill-bg` exists.
