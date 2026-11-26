# [DEF-20261014-1115] In-flight Step Context Level Renders as PENDING on Live Execution View

- **Status:** `Resolved`
- **Opened:** 2026-10-14 11:15
- **Closed:** 2026-10-14 11:38
- **Component:** `neodymium-core` / `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In Neodymium Aura Manager, passed execution steps for currently running tests display `PENDING` instead of their recorded context level (e.g. `LEAN`, `MINIMAL`, `STANDARD`), and the AI LLM responsibility context tier counters remain at 0 until the test run finishes.
- **Root Cause:** 
  1. `PreliminaryReportListener` handled `StepFinishedEvent` without merging `StepStats` into `ReportStepEntry`; merging and context level aggregation only occurred inside `recalculateMetrics()` upon `SessionFinishedEvent`.
  2. `InteractiveStateBuilder.serializeStep` did not fall back to `step.getContextLevel()` or `context.getTransientData("KEY_CURRENT_STEP_STATS")` / `execution.stepStatsMap`, emitting empty step stats during active test execution.
  3. `report-manager.js:3006` omitted checking `s.contextLevel` (singular), falling back unconditionally to `'PENDING'`, and used unparsed escalation strings for CSS classes.
- **Detection Gap ("What did we miss?"):** Unit tests in `PreliminaryReportListenerTest` evaluated final completed report flushes after session completion rather than asserting intermediate report entry state immediately following a `StepFinishedEvent`.
- **Resolution:** 
  1. In `PreliminaryReportListener.java`, merged step stats from `stepStatsMap` / `KEY_CURRENT_STEP_STATS` into `targetStep` upon receiving `StepFinishedEvent`, falling back to `pbStep.getContextLevel()`, and refreshed report metrics via `recalculateMetrics()`.
  2. In `AgentToolLoopStep.java`, invoked `step.setContextLevel(activeContextLevel.name())` whenever context level is updated or escalated.
  3. In `InteractiveStateBuilder.java`, updated `serializeStep` and `serializeStepStats` to propagate `contextLevel` and `contextLevels` directly from `pbStep.getContextLevel()` or active `StepStats` when `reportStep` stats are not yet populated.
  4. In `report-manager.js`, aligned line 3006 with line 3813 to check `s.contextLevel || s.contextLevels || s.stats?.contextLevels || s.stats?.contextLevel`, and updated badge class calculation to use `contextLevelClass(...)` with escalation and `PENDING` support.
- **Safety Net Added:** 
  - `PreliminaryReportListenerTest.testStepFinishedEventPopulatesContextLevelsBeforeSessionFinish`: asserts `ReportStepEntry.getContextLevels()` is populated immediately after `StepFinishedEvent` before session finish.
  - `InteractiveConsoleEngineTest.testBuildStateJsonIncludesContextLevelsForInFlightStep`: asserts in-flight state JSON includes `contextLevels` and `contextLevel` for completed and escalated steps while runner status is `"running"`.
  - `StaticResourceSyntaxTest`: verified JavaScript static assets in `aura-manager` pass syntax validation.
