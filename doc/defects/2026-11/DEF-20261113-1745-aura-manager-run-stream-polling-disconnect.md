# [DEF-20261113-1745] Aura Manager Run Stream Polling Disconnect on Queue Execution

- **Status:** `Resolved`
- **Opened:** 2026-11-13 17:40
- **Closed:** 2026-11-13 17:45
- **Component:** `aura-manager` / `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** When clicking "Run Queue" or "Run Open Test" in the Aura Manager UI, the terminal console remained permanently stuck on `"Connecting to run stream..."`. Backend tests executed in the background without issue, but no live logs or test status reached the browser unless the user manually refreshed the browser tab.
- **Root Cause:** In `dashboard-runner.js`, `prepareClientForExecution()` incremented `currentPollSession++` and reset indices (`lastLogIndex = 0; lastEventIndex = 0;`) to supersede poll results from previous runs. However, `prepareClientForExecution()` never invoked `startPolling()`. Inside `pollStatus()`, the in-flight poll request evaluated `thisSession !== currentPollSession` upon response and in its `finally` block, exiting without scheduling the next `setTimeout(pollStatus, 2000)`. Because no new poll loop was initiated, client polling terminated completely.
- **Detection Gap ("What did we miss?"):** Backend Spring MVC and controller tests verified `/api/run` and `/api/status` independently via MockMvc and service mocks without validating continuous client-side browser polling session handoff across queue triggers.
- **Resolution:** Updated `prepareClientForExecution()` in both `aura-manager` and `neodymium-core` to defensively invoke `startPolling()` (clearing prior timeouts and immediately initiating the new poll session).
- **Safety Net Added:** Added automated regression test `StaticResourceSyntaxTest.testPrepareClientForExecutionInitiatesPolling` in `aura-manager`, verifying that `prepareClientForExecution()` initiates polling on the live run stream.
