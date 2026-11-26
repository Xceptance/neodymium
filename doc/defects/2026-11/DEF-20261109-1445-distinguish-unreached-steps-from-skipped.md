# [DEF-20261109-1445] Distinguish unreached downstream steps from intentionally skipped steps in test reports

- **Status:** `Resolved`
- **Opened:** 2026-11-09 14:45
- **Closed:** 2026-11-09 15:20
- **Component:** `neodymium-core` / `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In test execution reports (HTML report, index overview, Markdown summary) and the Aura Manager interactive console, downstream playbook steps and sub-steps unreached due to an earlier step failure were labeled identically as `SKIPPED` alongside intentionally skipped steps (e.g. conditional skips or optional steps), and displayed confusing `PENDING` context badges. In addition, unreached downstream steps showed no explanation banner, lacked visual distinction from intentionally skipped steps, and were either lumped into `skippedSteps` or miscalculated as passed steps in index summaries.
- **Root Cause:** In `PreliminaryReportListener.resolveUnfinishedSteps()`, `StateMachineRunner.markRemainingStepsAsSkipped()`, `ExecuteActionsStep`, and `InteractiveStateBuilder.java`, all unexecuted leaf steps occurring after a failing step were uniformly marked `SKIPPED` without assigning an explanation message or distinguishing intentional skips from downstream abortion. Furthermore, `PlaybookStepStatus` lacked a dedicated `NOT_RUN` status, and Aura Manager's `report-manager.js` displayed a redundant `PENDING` context badge when subsequent steps were not reached.
- **Detection Gap ("What did we miss?"):** Previous reporting and runner tests focused on clean passing runs or verified that failing sub-steps were not reported as `SUCCESS`, asserting `assertEquals("SKIPPED", ...)` indiscriminately without testing reports and interactive console views containing both intentional skips and aborted downstream steps.
- **Resolution:**
  1. Added `PlaybookStepStatus.NOT_RUN` enum constant and extended `PlaybookStep` with `notRunReason` field, getter/setter, and `isNotRun()` helper.
  2. Extended `TestExecutionReport.ReportStepEntry` with `notRunReason` field, `getNotRunReason()`, `setNotRunReason()`, and helper `isNotRun()`.
  3. Extended `ReportMetrics` and `HtmlIndexReportGenerator.IndexEntry` with `notRunSteps` field, tracking unexecuted downstream steps independently from `skippedSteps`.
  4. Updated `PreliminaryReportListener.resolveUnfinishedSteps()` to set downstream unexecuted steps to `NOT RUN` with an informative banner reason referencing the failing step label (e.g., *"Not executed because step #2 failed."* or composite label *"Not executed because step #1.2 failed."*).
  5. Updated `StateMachineRunner` and `ExecuteActionsStep` to mark unreached downstream steps and trailing sub-steps as `NOT_RUN` with detailed `notRunReason` and dispatch `StepFinishedEvent(step, PlaybookStepStatus.NOT_RUN)`.
  6. Updated `InteractiveStateBuilder` to serialize unexecuted downstream steps and sub-steps as `"status": "not_run"`, enrich with `notRunReason`, record `notRunSteps` in metrics, and exclude non-executed steps from start timestamps/durations.
  7. Updated `HtmlReportGenerator` to render subdued step cards (`.step-card-not-run`, `opacity: 0.65; hover: 0.95`), gray status pills (`.pill-not-run`), and informative banners (`.step-card-not-run-banner`), and updated inspector JavaScript to handle `NOT RUN` pills and display `notRunReason`.
  8. Updated `HtmlIndexReportGenerator` to correctly calculate `passedSteps` subtracting both `skippedSteps` and `notRunSteps`, display `⏹️ X not run` in the steps column, and map `NOT RUN` status to `.pill-not-run`.
  9. Updated `MarkdownReportGenerator` to include `Not Run` counts in total steps breakdown, render the `⏹️` emoji for `NOT RUN` steps, and output a `- **Note:** *...*` block with the `notRunReason`.
  10. Updated `AuraReportDataService.java`, `report-manager.js`, `report-manager.css`, and `side-panel-step-list.html` in `aura-manager` to render gray `.badge-not-run` (`NOT RUN`), suppress redundant `PENDING` context badges, display `.step-not-run-banner` and `.sub-step-not-run-banner`, and apply `.step-not-run` subdued styling.
- **Safety Net Added:** Added comprehensive automated regression tests:
  - `PreliminaryReportListenerTest`:
    - `testResolveUnfinishedStepsDistinguishesNotRunFromSkipped`: validates step-level separation between `SUCCESS`, intentional `SKIPPED`, `FAILED`, and aborted `NOT RUN` steps, and verifies `ReportMetrics` separation (`notRunSteps` vs `skippedSteps`).
    - `testCompoundStepFailureMarksDownstreamSubStepsNotRun`: validates nested sub-step failure marking downstream sub-steps and root steps as `NOT RUN` with parent-relative label `#1.2`.
    - `testHtmlAndMarkdownReportGeneratorsRenderNotRunPillsAndBanners`: validates HTML, Markdown, and Index reports rendering `.step-card-not-run`, `.pill-not-run`, `.step-card-not-run-banner`, and `⏹️` indicators.
  - `StateMachineRunnerTest`: validates `markRemainingStepsAsSkipped` sets `PlaybookStepStatus.NOT_RUN` with informative `notRunReason` on downstream steps and sub-steps, and dispatches `StepFinishedEvent(..., NOT_RUN)`.
  - `PlaybookLifecycleExecutionTest`: validates that unexecuted main playbook steps are set to `NOT_RUN` when a setup step fails.
  - `InteractiveConsoleEngineTest`: validates `InteractiveStateBuilder` serialization of `not_run` steps, sub-steps, `notRunReason`, and `notRunSteps` metrics.
