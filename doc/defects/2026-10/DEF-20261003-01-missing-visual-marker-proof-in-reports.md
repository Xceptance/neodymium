# [DEF-20261003-01] Missing Visual Marker Proof in Reports and Text-Only Context Demotion for (marker) Steps

- **Status:** Resolved
- **Opened:** 2026-10-03
- **Closed:** 2026-10-03
- **Component:** `neodymium-core` (`ExecuteActionsStep`, `AgentToolLoopStep`, `PreliminaryReportListener`, `HtmlReportGenerator`, `MarkdownReportGenerator`, `PlaybookStep`, `TestExecutionReport`)
- **Scope:** `Framework`
- **Symptom:**
  Executing a playbook step with `(marker)` hint (e.g. `(marker) Click the quick view button with the eye icon on the Cyberpunk Neon Hoodie card`) failed to show any visual marker badge (`🎯 MARKER`) or marked screenshot overlay in the native HTML/Markdown execution reports. Furthermore, the step ran in `TEXT_ONLY` mode at `LEAN` context level over multiple turns instead of executing as a visual marker step with coordinate clicks.
- **Root Cause:**
  1. `ExecuteActionsStep.java` ignored `step.isMarker()`, defaulting `initialLevel` to `LEAN` and stamping `step.setContextLevel("LEAN")`.
  2. `AgentToolLoopStep.java` checked `if (step.getContextLevel() == null)` before escalating `(marker)` steps to `VISUAL_LEAN`, preventing escalation when `LEAN` was already set.
  3. Because the context remained `LEAN`, `captureState()` captured no screenshot, the marked screenshot `StateCapturedEvent` was never emitted, and the LLM was forced into text-only DOM querying.
  4. `PlaybookStep.setInstruction()` stripped `(marker)` immediately without preserving `rawInstruction`.
  5. `ReportStepEntry` and report generators lacked `marker` metadata, stripping marker indicators from the report UI.
- **Detection Gap ("What did we miss?"):**
  Unit tests mocked `LlmResponse` and `StepStartedEvent` in isolation without verifying full pipeline context level elevation and HTML/Markdown report artifact generation for `(marker)` steps.
- **Resolution:**
  1. Preserved `rawInstruction` in `PlaybookStep` and `ReportStepEntry`.
  2. Elevated `initialLevel` to `ContextLevel.VISUAL_LEAN` in `ExecuteActionsStep` when `step.isMarker()` is true.
  3. Relaxed escalation condition in `AgentToolLoopStep` to upgrade `LEAN` to `VISUAL_LEAN` for marker steps.
  4. Labeled marker screenshots as `(Visual Markers Overlay)` and propagated `marker` flag to `ReportStepEntry`.
  5. Rendered `🎯 MARKER` badge and CSS styling in HTML report card, inspector, and Markdown reports.
- **Safety Net Added:**
  `PreliminaryReportListenerTest.testMarkerStepScreenshotCaptureAndReporting`, `PlaybookStepTest.testIsMarkerStep`, `PlaybookStepTest.testMarkerSerialization`, and `VisualMarkersSandboxMockTest.testVisualMarkersDynamicFallbackWithoutHint`.
