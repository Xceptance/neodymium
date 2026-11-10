# [DEF-20261002-10] Dummy Video Player Displayed in Report Side Panel when No Video Attached

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `aura-manager` (`side-panel-step-list.html`, `report-manager.js`)
- **Scope:** `Framework`
- **Symptom:** In Aura report UI execution side panel, a dummy video player (`execution_replay.mp4`) was displayed even when no video attachment was attached to the test execution.
- **Root Cause:** `side-panel-step-list.html` hardcoded a static video attachment preview box mockup, and `report-manager.js` did not hide the box when `data-video-url` or `data-video` attribute was missing/empty on the active execution row.
- **Detection Gap ("What did we miss?"):** UI HTML template mockups were not hidden by default, and frontend JS logic lacked fallback hiding for empty video attributes.
- **Resolution:** Set default `style="display: none;"` on `#attachmentVideoBox` in `side-panel-step-list.html` and updated `report-manager.js` to dynamically show/hide `#attachmentVideoBox` and update `#attachmentSectionContainer` visibility based on actual attachment presence.
- **Safety Net Added:** Frontend assertion/verification in report manager JS and UI component tests.
