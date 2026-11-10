# [DEF-20261014-1425] Interactive Mode Sub-Steps Omit Passed Status Marker During Live Turns

- **Status:** `Resolved`
- **Opened:** 2026-10-14 14:25
- **Closed:** 2026-10-14 14:35
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In interactive console execution, after a sub-step executes and the console pauses before the next sub-step action, the executed sub-step remains marked as pending (empty circle `○`) and the sub-step counter badge remains at `0/N Sub-steps` instead of indicating that the sub-step passed (`check_circle`).
- **Root Cause:** `InteractiveStateBuilder.serializeStep` prioritized pre-populated unexecuted `ReportStepEntry.getSubSteps()` (which had status PENDING) over the live `step.getSubSteps()` managed by `AgentToolLoopStep`. This prevented live turn partitioned sub-step statuses and actions from being serialized into the SSE state JSON.
- **Detection Gap ("What did we miss?"):** Previous tests verified live status partitioning on `AgentToolLoopStep` model instances, but did not assert JSON serialization output of `InteractiveStateBuilder.buildStateJson` when both a live `PlaybookStep` and an active `TestExecutionReport` co-exist.
- **Resolution:** Prioritize live `step.getSubSteps()` in `InteractiveStateBuilder`, normalize sub-step statuses to standard lowercase tokens (`passed`, `running`, `failed`, `pending`), enrich live entries with report attachments, and update `PreliminaryReportListener` to reflect sub-step actions on `ActionExecutedEvent`.
- **Safety Net Added:** Automated unit test in `InteractiveConsoleListenerTest.testSubStepStatusesSerializedDuringLiveExecution` verifying live sub-step status serialization in state JSON during active pauses with report entries.
