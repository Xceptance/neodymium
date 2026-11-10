# [DEF-20261002-09] Separate Window Attachment Button Used Unresolved Relative Filename Path Resulting in 404 Error Page

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `aura-manager` (`side-panel-step-list.html`, `report-manager.js`)
- **Scope:** `Framework`
- **Symptom:** Clicking "Open in Separate Window" on test video attachments produced a Spring MVC 404 Whitelabel Error Page for `/execution_replay.mp4`.
- **Root Cause:** `openAttachmentWindow` received bare filenames (e.g. `execution_replay.mp4`) from fallback HTML template attributes instead of resolving the target URL to `/api/runs/{runId}/{testClass}/{filename}`.
- **Detection Gap ("What did we miss?"):** UI tests verified inline `<video>` element rendering without triggering the popup window action button.
- **Resolution:** Updated `renderStepsForExecution` in `report-manager.js` to dynamically set the popup button's `onclick` to the resolved API endpoint URL (`/api/runs/...`). Enhanced `openAttachmentWindow` with auto-resolution logic for relative filenames.
- **Safety Net Added:** Added frontend unit check verifying popup window URL construction.
