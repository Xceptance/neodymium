# [DEF-20261116-1145] Diagnostics Copy Button Stuck in Loading State Due to Fetch ID Race

- **Status:** `Resolved`
- **Opened:** 2026-11-16 11:45
- **Closed:** 2026-11-16 11:54
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In Aura Manager's reporting side-panel drawer, the "Copy Full Test Diagnostics" button (`#copyFullTestDiagnosticsBtn`) remains permanently disabled with the title "Loading diagnostics..." and an endlessly spinning sync icon, preventing users from copying test diagnostics.
- **Root Cause:** In `report-manager.js`, `renderStepsForExecution` guarded button re-enablement inside `.finally()` with `if (fetchId === activeStepFetchId)`. However, the `.then()` handler immediately preceding `.finally()` called `startLiveStepRefreshTimer(activeRow)`, which unconditionally executed `stopLiveStepRefreshTimer()` and bumped `activeStepFetchId++`. Consequently, `fetchId === activeStepFetchId` always evaluated to `false` when `.finally()` executed, bypassing button restoration and leaving it permanently disabled and spinning. Furthermore, non-fetch execution paths (when steps were already cached on the row) and drawer dismissal did not manage button state.
- **Detection Gap ("What did we miss?"):** `StaticResourceSyntaxTest.java` verified the markdown formatting generator (`generateFullTestDiagnosticsMarkdown`) inside Node VM, but did not test the asynchronous step-fetch completion lifecycle alongside timer restarts and DOM button state transitions.
- **Resolution:**
  1. Introduced a centralized `setCopyDiagnosticsBtnLoading(isLoading)` helper in `report-manager.js` to coordinate button disabled state, title, and icon animation.
  2. Updated `renderStepsForExecution.finally()` to re-enable the button whenever `fetchId === activeStepFetchId || currentActiveRowId === rowId`, preventing `stopLiveStepRefreshTimer`'s `activeStepFetchId++` from swallowing button restoration.
  3. Ensured the cached/non-fetch branch of `renderStepsForExecution` actively calls `setCopyDiagnosticsBtnLoading(false)` to guarantee the button is enabled when inspecting cached executions.
  4. Reset button state in `closeTestSidePagePanel()` and toggled button visibility in Test Base cross-batch mode.
- **Safety Net Added:** Added Node VM test in `StaticResourceSyntaxTest#testCopyDiagnosticsButtonLoadingLifecycle` verifying button state across initial fetch, asynchronous completion with live timer invalidation, and cached step rendering.
