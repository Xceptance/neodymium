# [DEF-20261002-08] Missing `data-test-class` Attribute in Run Report Template Caused HTTP 404 Video URL Resolution Failures

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `aura-manager` (`run-report.html`, `report-manager.js`)
- **Scope:** `Framework`
- **Symptom:** Video playback in Aura report UI failed with HTTP 404. Console log showed request URL containing a double slash `/api/runs/<runId>//video-1.mp4`.
- **Root Cause:** `run-report.html` set `th:data-test-name="${exec.testClass}"` on `<tr>` elements instead of `th:data-test-class`. `report-manager.js` looked only for `data-test-class`, resolving `testClass` to an empty string `""` and forming invalid 2-segment URLs.
- **Detection Gap ("What did we miss?"):** UI tests verified test class names in table text cells without inspecting `data-*` HTML row attributes used by media endpoints.
- **Resolution:** Added `th:data-test-class="${exec.testClass}"` to `run-report.html` and updated `report-manager.js` to fallback gracefully to `data-test-name`. Declared `window.openAttachmentWindow` in `report-manager.js`.
- **Safety Net Added:** Added frontend JS attribute fallback check and unit test verifying valid `/api/runs/{runId}/{testClass}/{filename}` URL construction.
