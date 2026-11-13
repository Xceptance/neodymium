# [DEF-20261102-1342] Dashboard Widget Drag and Drop Missing Auto-Scroll and Wheel Scrolling

- **Status:** `Resolved`
- **Opened:** 2026-11-02 13:20
- **Closed:** 2026-11-02 13:42
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When dragging a dashboard widget card vertically to reorder or drop it over another widget across long dashboards, the viewport did not automatically scroll near top or bottom edges, and trackpad/mouse-wheel scrolling was ignored or blocked by the browser drag session, preventing users from moving widgets across off-screen dashboard boundaries.
- **Root Cause:** The HTML5 Drag and Drop API implementation on `#dashboardWidgetsGrid` lacked boundary proximity scroll detection (`requestAnimationFrame` velocity loop) and wheel event propagation listening during active dragging (`activeDraggedWidgetId`).
- **Detection Gap ("What did we miss?"):** Previous drag-and-drop tests verified DOM drop target IDs and reorder endpoints via mock events, but did not assert edge-distance viewport auto-scrolling or wheel event handling during dragging.
- **Resolution:**
  1. Implemented `getDashboardScrollContainer()` in `report-manager.js` to accurately locate the scrollable container (`#mainViewContainer`, document element, or window).
  2. Implemented smooth edge auto-scrolling with `startDragAutoScrollLoop()`, `stopDragAutoScroll()`, and `updateDragAutoScrollOnPointer(clientY)`, calculating dynamic acceleration within 90px of top and bottom viewport boundaries using `requestAnimationFrame`.
  3. Added non-passive `window.addEventListener('wheel', ...)` listener active only while dragging widgets, directly dispatching delta offsets to the dashboard scroll container.
  4. Protected all top-level listeners with `typeof window !== 'undefined' && typeof window.addEventListener === 'function'` to ensure Node.js VM test runner compatibility.
- **Safety Net Added:** Added `@Test testDashboardWidgetDragAutoScrollEnginePresent()` to `StaticResourceSyntaxTest.java`, asserting presence of `getDashboardScrollContainer`, `startDragAutoScrollLoop`, `stopDragAutoScroll`, `requestAnimationFrame`, and wheel listeners.
