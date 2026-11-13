# [DEF-20261030-1605] Trend Graph Hover Popover Clipped by Card Overflow and Stacking Context

- **Status:** `Resolved`
- **Opened:** 2026-10-30 16:05
- **Closed:** 2026-10-30 16:15
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Hovering high-pass-rate data point dots near the top of the trend graph truncates the upper half of the state popover (hiding Run ID, timestamp, and Total Tests) behind the card header and border.
- **Root Cause:** `.widget-card` enforced `overflow: hidden;` and lacked dynamic z-index elevation on hover. The ~160px hover popover positioned at `bottom: 12px` exceeded the available ~70px clearance above the dot, causing its upper half to be clipped by the card boundary and obscured by other elements.
- **Detection Gap ("What did we miss?"):** Previous static resource syntax tests verified CSS rule validity and Thymeleaf expression resolution, but did not assert viewport overflow escaping or stacking contexts on interactive card overlays.
- **Resolution:** Configured `.widget-trend-card` with `overflow: visible;`, elevated `:hover` card stacking to `z-index: 1000 !important;`, elevated `.widget-trend-dot-popover` to `z-index: 1050;` with enhanced elevation shadows, and introduced adaptive vertical orientation (`popover-below` for upper-boundary points).
- **Safety Net Added:** `StaticResourceSyntaxTest.testTrendGraphDotsPopoverElevationAndOverflowStyling` verifying `overflow: visible`, `z-index: 1000`, `z-index: 1050`, and `.popover-below` dynamic template logic.
