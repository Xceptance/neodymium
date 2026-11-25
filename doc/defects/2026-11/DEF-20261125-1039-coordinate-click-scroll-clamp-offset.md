# [DEF-20261125-1039] Coordinate and Visual Marker Click Offset When window.scrollBy Clamps at Page Boundaries

- **Status:** `Resolved`
- **Opened:** 2026-11-25 09:01
- **Closed:** 2026-11-25 10:39
- **Component:** `neodymium-core` (`BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:**
  In Jenkins build #304, `VisualMarkersSandboxLiveTest#testVisualMarkersLive` clicked `marker:4` (Quick View eye icon on the Cyberpunk Neon Hoodie card in `visual-markers.html`) ~`111 px` above the actual button and reported `SUCCESS`, leaving `#action-feedback` at `"No action taken yet"`.
- **Root Cause:**
  1. `BrowserToolProvider.performSafeCoordinateClick()` computed `deltaY = y - (vpHeight / 2)` (~`318 px` for a `1024x625` viewport on an `832 px` page with max vertical scroll `207 px`), called `window.scrollBy(0, deltaY)`, and blindly subtracted the *requested* `deltaY` from `y` without measuring the *actual* `window.scrollY` delta applied by the browser (`207 px`).
  2. `clickBadgeScript` (`marker:N`) read `el.getBoundingClientRect()` without first scrolling off-screen marked elements into view.
  3. `executeCoordinateClick` (`coord:<anchor>@x,y`) only scrolled the anchor container into view and did not re-read the anchor's bounding rect when an offset inside a tall anchor remained outside the viewport.
- **Detection Gap ("What did we miss?"):**
  `BrowserToolProviderStabilityTest#testPureCoordinateClickAutoScrollsOutOfBoundsY` only verified that `window.scrollBy` was invoked and returned `null` from its mock, without asserting the post-scroll click coordinates under clamped scroll boundaries.
- **Resolution:**
  1. Updated `BrowserToolProvider.performSafeCoordinateClick()` to measure `window.scrollX` / `window.scrollY` before and after `window.scrollBy({ left, top, behavior: 'instant' })` (with fallback to `window.scrollBy(dx, dy)`) and subtract only the actual rounded scroll delta (`dx`, `dy`) from `(x, y)`.
  2. Updated `clickBadgeScript` (`marker:N`) to call `el.scrollIntoView({ behavior: 'instant', block: 'center', inline: 'nearest' })` and re-read `getBoundingClientRect()` when the element's center is outside the viewport (guarding against badge overlay divs).
  3. Updated `executeCoordinateClick` (`coord:<anchor>@x,y`) to scroll the window with `behavior: 'instant'` and re-read `anchor.getBoundingClientRect()` if `anchor.left + rawX` or `anchor.top + rawY` remains outside the viewport.
- **Safety Net Added:**
  - Added unit tests in `BrowserToolProviderStabilityTest` covering bottom-clamped vertical scroll (`1024x625` / `207 px` max scroll), unclamped scroll, top/left edge clamping, unscrollable pages, anchored coordinate post-scroll rect re-read, and visual marker `scrollIntoView` behavior.
