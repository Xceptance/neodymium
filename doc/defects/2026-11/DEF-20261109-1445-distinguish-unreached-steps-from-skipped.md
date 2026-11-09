# [DEF-20261109-1445] Distinguish unreached downstream steps from intentionally skipped steps in test reports

- **Status:** `Resolved`
- **Opened:** 2026-11-09 14:45
- **Closed:** 2026-11-09 15:20
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In test execution reports (HTML report, index overview, Markdown summary), downstream playbook steps unreached due to an earlier step failure were labeled identically as `SKIPPED` alongside intentionally skipped steps (e.g. conditional skips or optional steps). In addition, unreached downstream steps showed no explanation banner, lacked visual distinction from intentionally skipped steps, and were either lumped into `skippedSteps` or miscalculated as passed steps in index summaries.
- **Root Cause:** In `PreliminaryReportListener.resolveUnfinishedSteps()`, all unexecuted leaf steps occurring after the failing step index were uniformly marked `leaf.setStatus("SKIPPED")` without assigning any explanation message or distinguishing intentional skips from downstream abortion. Furthermore, `TestExecutionReport.ReportMetrics` and `HtmlIndexReportGenerator.IndexEntry` only tracked `skippedSteps`, and index pass calculations used `passedSteps = Math.max(0, total - healed - failed - skipped)`, erroneously counting unreached steps as passed.
- **Detection Gap ("What did we miss?"):** Previous reporting tests focused on clean passing runs or verified that failing sub-steps were not reported as `SUCCESS`, asserting `assertEquals("SKIPPED", sub.getStatus())` indiscriminately without testing reports containing both intentional skips and aborted downstream steps.
- **Resolution:**
  1. Extended `TestExecutionReport.ReportStepEntry` with `notRunReason` field, `getNotRunReason()`, `setNotRunReason()`, and helper `isNotRun()`.
  2. Extended `ReportMetrics` and `HtmlIndexReportGenerator.IndexEntry` with `notRunSteps` field, tracking unexecuted downstream steps independently from `skippedSteps`.
  3. Updated `PreliminaryReportListener.resolveUnfinishedSteps()` to set downstream unexecuted steps to `NOT RUN` with an informative banner reason referencing the failing step label (e.g., *"Not executed because step #2 failed."* or composite label *"Not executed because step #1.2 failed."*).
  4. Updated `HtmlReportGenerator` to render subdued step cards (`.step-card-not-run`, `opacity: 0.65; hover: 0.95`), gray status pills (`.pill-not-run`), and informative banners (`.step-card-not-run-banner`), and updated inspector JavaScript to handle `NOT RUN` pills and display `notRunReason`.
  5. Updated `HtmlIndexReportGenerator` to correctly calculate `passedSteps` subtracting both `skippedSteps` and `notRunSteps`, display `⏹️ X not run` in the steps column, and map `NOT RUN` status to `.pill-not-run`.
  6. Updated `MarkdownReportGenerator` to include `Not Run` counts in total steps breakdown, render the `⏹️` emoji for `NOT RUN` steps, and output a `- **Note:** *...*` block with the `notRunReason`.
- **Safety Net Added:** Added comprehensive automated regression tests in `PreliminaryReportListenerTest`:
  - `testResolveUnfinishedStepsDistinguishesNotRunFromSkipped`: validates step-level separation between `SUCCESS`, intentional `SKIPPED`, `FAILED`, and aborted `NOT RUN` steps, and verifies `ReportMetrics` separation (`notRunSteps` vs `skippedSteps`).
  - `testCompoundStepFailureMarksDownstreamSubStepsNotRun`: validates nested sub-step failure marking downstream sub-steps and root steps as `NOT RUN` with parent-relative label `#1.2`.
  - `testHtmlAndMarkdownReportGeneratorsRenderNotRunPillsAndBanners`: validates HTML, Markdown, and Index reports rendering `.step-card-not-run`, `.pill-not-run`, `.step-card-not-run-banner`, and `⏹️` indicators.
