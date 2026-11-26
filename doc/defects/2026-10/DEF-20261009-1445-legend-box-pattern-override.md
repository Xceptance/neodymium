# [DEF-20261009-1445] Legend Box Pattern Clashing with Inline Shorthand Background

- **Status:** `Resolved`
- **Opened:** 2026-10-09 14:45
- **Closed:** 2026-10-09 14:47
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In the History Trend legend of `batch-history.html`, swatches for "Passed Healed", "Passed AI-Driven", "Succeeded Fixed Healed", and "Failed AI-Driven" render as solid flat colors without diagonal pinstripes or sparkle dots.
- **Root Cause:** Inline `style="background: var(--status-pass);"` shorthand on `.legend-box` resets `background-image: none` with higher inline specificity than the stylesheet rules `.legend-box.seg-ai-print` and `.legend-box.seg-healed-print`. Furthermore, `.legend-box.seg-healed-print` used a 16px background size inside a 14px box, making the sparkle dots ill-fitted for small swatches.
- **Detection Gap ("What did we miss?"):** Existing test suite checked DTO math and JS asset syntax, but did not assert template HTML style properties or CSS specificity conflicts on legend swatches.
- **Resolution:** 
  1. In `batch-history.html`, replaced all inline `style="background: ..."` on `.legend-box` with `style="background-color: ..."` to preserve layered background images.
  2. In `report-manager.css`, added `!important` to `background-image`, `background-size: 8px 8px`, and `background-repeat: repeat` on `.seg-ai-print` and `.seg-healed-print` to guarantee pattern rendering across all resolutions and containers.
- **Safety Net Added:** Added regression test `StaticResourceSyntaxTest.testLegendBoxPatternStylesNotOverriddenByBackgroundShorthand` verifying template legend elements use `background-color` and stylesheet pattern rules enforce `!important`.
