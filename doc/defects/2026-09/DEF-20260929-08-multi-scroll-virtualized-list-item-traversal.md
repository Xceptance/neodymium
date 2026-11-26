# [DEF-20260929-08] Multi-Scroll Virtualized List Item Traversal Exceeds Default Step Token Budget

- **Status:** Resolved
- **Opened:** 2026-09-29
- **Closed:** 2026-09-29
- **Component:** `neodymium-core` (`sandbox-tests` / `live-integration` / `VirtualizedListSandboxLiveTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Running `VirtualizedListSandboxLiveTest.testVirtualizedListLive` aborted with `TokenBudgetExceeded: Total tokens consumed (103894) exceeded configured step token budget (100000)`.
- **Root Cause:** Locating dynamically unmounted items in a virtualized DOM feed required multiple scroll-and-inspect cycles, accumulating 103,894 prompt tokens across intermediate DOM snapshots and exceeding the default 100k safety budget.
- **Detection Gap ("What did we miss?"):** The default 100k token guardrail was calibrated for typical forms and standard pages, without configuring an elevated limit for heavy virtualized feed traversal tests.
- **Resolution:** Configured `neodymium.ai.step.maxTokens` to 250,000 via session test data in `setupProperties`.
- **Safety Net Added:** Verified `VirtualizedListSandboxLiveTest` passes cleanly across all modes (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`).
