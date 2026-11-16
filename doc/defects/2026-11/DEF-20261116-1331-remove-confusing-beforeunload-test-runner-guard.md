# [DEF-20261116-1331] Remove Confusing beforeunload Test Runner Guard Triggering False Unsaved Changes Browser Dialog

- **Status:** `Resolved`
- **Opened:** 2026-11-16 13:31
- **Closed:** 2026-11-16 13:34
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Browsing the Aura Manager dashboard, reloading the page, or closing the tab while a test queue is executing triggers the browser's native confirmation dialog ("Leave site? Changes that you made may not be saved"), creating the false impression that an editor has unsaved changes even when the editor was never opened.
- **Root Cause:** `dashboard-runner.js` registered a `window.addEventListener('beforeunload', ...)` listener that invoked `event.preventDefault()` and assigned a custom warning to `event.returnValue` whenever `isRunning` was true. Modern browsers (Chrome, Edge, Firefox, Safari) deliberately ignore custom strings assigned in `beforeunload` and substitute hardcoded generic messages warning about unsaved changes.
- **Detection Gap ("What did we miss?"):** Headless and automated browser execution does not invoke interactive `beforeunload` dialogs, and previous static asset syntax tests did not verify the presence or absence of `beforeunload` event listeners.
- **Resolution:** Removed the `beforeunload` event listener from `dashboard-runner.js` in both `aura-manager` and `neodymium-core`.
- **Safety Net Added:** Added unit test `StaticResourceSyntaxTest#testDashboardRunnerDoesNotRegisterBeforeunload` in `aura-manager` asserting that `dashboard-runner.js` does not register `beforeunload`.
