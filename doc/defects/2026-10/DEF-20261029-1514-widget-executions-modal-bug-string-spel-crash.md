# [DEF-20261029-1514] Widget Executions Modal SpEL Crash on String Bug Tickets

- **Status:** `Resolved`
- **Opened:** 2026-10-29 15:14
- **Closed:** 2026-10-29 15:26
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Clicking dashboard cost or status widgets containing test executions with linked bugs triggers `SpelEvaluationException: EL1008E: Property or field 'ticket' cannot be found on object of type 'java.lang.String'` on line 135 of `widget-executions-modal.html`, leaving the execution modal empty.
- **Root Cause:** In `widget-executions-modal.html`, the bug chip iteration iterated over `e.bugs` (`List<String>`) while dereferencing `b.ticket` and `b.severity`, instead of inspecting `e.bugDetails` (`List<BugDetailDto>`) or rendering `b` directly as a string identifier.
- **Detection Gap ("What did we miss?"):** Previous integration tests for `widget-executions-modal` only rendered an empty execution list or executions without linked bugs, failing to exercise the bug chip DOM rendering branch.
- **Resolution:** Updated `widget-executions-modal.html` to prioritize `e.bugDetails` (displaying ticket ID and severity pill) and fall back to `e.bugs` (displaying ticket ID string directly). Added regression test cases in `AuraReportViewControllerSpringContextTest` asserting both bug string lists and `BugDetailDto` structures render without SpEL exceptions.
- **Safety Net Added:** `AuraReportViewControllerSpringContextTest#testWidgetExecutionsModalRendersWithBugDetailsAndRawBugStrings`
