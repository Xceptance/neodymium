# [DEF-20261116-0545] Redundant Preliminary Report Serialization Cascade in Aura / Console Runners

- **Status:** `Resolved`
- **Opened:** 2026-11-16 05:45
- **Closed:** 2026-11-16 06:42
- **Component:** `neodymium-core` (`PreliminaryReportListener`, `InteractiveConsoleListener`, `NeodymiumAiRunner`)
- **Scope:** `Framework`
- **Symptom:** During test completion in Aura / Interactive Console executions, preliminary HTML and JSON report files are written and logged up to 5 consecutive times for a single test run within a few seconds.
- **Root Cause:**
  1. `InteractiveConsoleListener` instantiated a second, independent `PreliminaryReportListener` instance rather than referencing or reusing the instance already registered on the active session's event bus (`AiSession`).
  2. `InteractiveConsoleListener.onEvent` forwarded all execution events to this private duplicate listener, causing duplicate state tracking.
  3. `InteractiveConsoleListener.onEvent(SessionFinishedEvent)` contained an explicit trailing `this.reportListener.flushReport()` call, duplicate to the flush executed inside `reportListener.onEvent(SessionFinishedEvent)`.
  4. During JUnit test teardown, `NeodymiumAiRunner.afterEach` iterated over all event bus listeners and invoked `reconcileTestOutcome` on both `InteractiveConsoleListener` (which delegated to its report listener) and the bus-registered `PreliminaryReportListener`.
  5. `PreliminaryReportListener.reconcileTestOutcome` unconditionally invoked `flushReport()` even when the test outcome and status did not differ from the existing report state.
- **Detection Gap ("What did we miss?"):** Existing listener unit tests verified report generation using standalone mock event buses with only a single listener attached, without asserting the composite listener topology and multi-stage lifecycle created by `NeodymiumAiRunner` in full integration.
- **Resolution:**
  1. Updated `InteractiveConsoleListener` to discover and reuse any existing `PreliminaryReportListener` already attached to the session's event bus; avoid duplicating event dispatches if the listener is already subscribed to the bus.
  2. Removed the redundant explicit `this.reportListener.flushReport()` inside `InteractiveConsoleListener.onEvent(SessionFinishedEvent)`.
  3. In `InteractiveConsoleListener.reconcileTestOutcome`, only delegate reconciliation to `this.reportListener` if it was owned by the console listener, preventing duplicate reconciliation when the report listener is already a registered event bus listener.
  4. Updated `PreliminaryReportListener.reconcileTestOutcome` to only trigger disk re-flush if the status or failure details actually mutated (or if the report had not yet been flushed).
  5. Added `flushCount` tracking to `PreliminaryReportListener` for deterministic verification.
- **Safety Net Added:**
  - `PreliminaryReportListenerTest.testReconcileTestOutcome_doesNotReflushWhenPassedReportRemainsPassed`
  - `PreliminaryReportListenerTest.testReconcileTestOutcome_flushesWhenStatusChangesToExpectedFailure`
  - `InteractiveConsoleListenerTest.testReusesPreliminaryReportListenerFromSessionEventBus`
  - `InteractiveConsoleListenerTest.testNoDuplicateDispatchOrFlushWhenReusingReportListener`
