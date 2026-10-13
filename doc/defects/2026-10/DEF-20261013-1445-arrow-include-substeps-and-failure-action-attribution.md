# [DEF-20261013-1445] Arrow Folded Include Substeps and Substep Action Attribution on Failure

- **Status:** `Resolved`
- **Opened:** 2026-10-13 14:45
- **Closed:** 2026-10-13 14:55
- **Component:** `neodymium-core`, `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. `_include:` directives nested inside folded scalar blocks (`- >-`) or indented under parent steps were parsed into top-level steps rather than nested child sub-steps under the composite step.
  2. When a composite step timed out or encountered an exception, tool calls and actions were not partitioned across sub-steps, `subStepIndex` and `subStepInstruction` remained empty, and all child sub-steps were unconditionally marked `FAILED`.
  3. Parent step actions in Aura Manager report did not display an attribution pill or badge indicating which sub-step an executed action belonged to.
- **Root Cause:**
  - `YamlPlaybookParser.java` checked for `trimmed.startsWith("_include:")` inside `inArrowSubsteps` and immediately terminated the arrow block, routing the included steps directly to `outSteps`.
  - `StateMachineRunner.java`'s `finally` failure block marked all pending/running sub-steps as `FAILED` without invoking `AgentToolLoopStep.partitionToolCallsAndActions(...)`.
  - `PreliminaryReportListener.java` did not backfill `subStepIndex` and `subStepInstruction` from partitioned actions to `ReportActionEntry` list on `targetStep`, and converted aborted sub-steps to `PENDING` instead of preserving `SKIPPED`.
  - `report-manager.js` omitted substep badge rendering in the parent step's `Actions Performed` pill row.
- **Detection Gap ("What did we miss?"):**
  - Existing folded scalar tests checked multiline text continuation but did not test `_include:` directives nested within `- >-` blocks.
  - Sub-step action attribution tests previously only exercised happy-path pipeline completions through `ExecuteActionsStep`, without simulating abortive timeout/exception pathways in `StateMachineRunner`.
- **Resolution:**
  - Updated `YamlPlaybookParser.java` to recursively parse `_include:` lines indented under arrow parents (`indent > parentIndent`) directly into `arrowParentStep.getSubSteps()`.
  - Updated `StateMachineRunner.java` failure handler to invoke `AgentToolLoopStep.partitionToolCallsAndActions(...)`, determine the active failing sub-step, and set prior executed sub-steps to `SUCCESS`, the failing sub-step to `FAILED`, and subsequent unreached sub-steps to `SKIPPED`.
  - Updated `PreliminaryReportListener.java` to synchronize `subStepIndex` and `subStepInstruction` onto parent report action entries, and set unreached sub-steps to `SKIPPED`.
  - Updated `report-manager.js` to render a `#Step.SubStep` badge on parent action pills.
- **Safety Net Added:**
  - `YamlPlaybookParserTest.testParsePlaybookWithArrowSubstepsIncludingFragments`
  - `StateMachineRunnerTest.testCompositeStepFailurePartitionsActionsAndSetsAccurateSubStepStatuses`
