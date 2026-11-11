# [DEF-20261110-1422] Missing Healed and AI Pattern Overlays on Grouping Card Pie Charts

- **Status:** `Resolved`
- **Opened:** 2026-11-10 14:22
- **Closed:** 2026-11-10 14:31
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** On grouping cards in Aura Manager (`custom-dashboard-page.html`), donut chart slices do not display the healed sparkle pattern or AI pinstripe pattern overlays. Legend values and base colors render correctly, but slices appear as flat solid colors.
- **Root Cause:** 
  1. `custom-dashboard-page.html` defined `<pattern th:id="'sparkle-pat-group-' + ${b.id}">` without `patternTransform="rotate(25)"` using a dense rigid polygon rather than the rotated quadratic bezier multi-star pattern from `dashboard-widgets.html`. In thin arc segments (1–3% of circumference), the sparkle coordinates fell outside the stroked band.
  2. The SVG element used `viewBox="0 0 36 36"` instead of `viewBox="-2 -2 40 40" style="overflow: visible;"`, causing 5px stroke bounds to clip at outer edges.
  3. Overlay circles lacked `pointer-events="none"` and standard CSS hook classes (`pie-slice-sparkle`, `pie-slice-stripes`).
  4. In `AuraReportDataService.java`, `cardId` generation sanitized `groupName` but did not sanitize `activeDimensions`, risking invalid characters in SVG element IDs.
- **Detection Gap ("What did we miss?"):** Existing integration tests in `CustomDashboardViewControllerIntegrationTest` checked for the presence of the container header and grouping labels without asserting SVG `<defs>` pattern attributes or overlay circle `stroke="url(#...)"` references on rendered cards.
- **Resolution:** 
  1. Synchronized the `<pattern>` definitions and SVG `viewBox` in `custom-dashboard-page.html` with `dashboard-widgets.html` (`patternTransform="rotate(25)"`, quadratic curves, yellow accent star, and rotated pinstripe).
  2. Added `class="pie-slice-sparkle"` and `class="pie-slice-stripes"` with `pointer-events="none"` to all healed and AI overlay circles.
  3. Sanitized `activeDimensions` in `AuraReportDataService.java` when building `cardId`.
  4. Added automated integration assertions verifying `<defs>`, `<pattern>`, and matching `stroke="url(#...)"` attributes in rendered grouping cards.
- **Safety Net Added:** `CustomDashboardViewControllerIntegrationTest#testGroupDashboardCardsRenderHealedAndAiPatternSvg`
