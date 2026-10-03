# [DEF-20260916-05] Complete Removal of PESAP Tracing & Reporting and Prompt Cleanliness

- **Status:** Resolved
- **Opened:** 2026-09-16
- **Closed:** 2026-09-16
- **Component:** `neodymium-core` (`ai-report`, `ai-runner`, `ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** Reports (HTML, Markdown, JSON) continued to render "PESAP (Pre-Execution Semantic Anchor)" category rows and badge cards with zero calls. StateMachineRunner and AiSession logged empty PESAP metrics in console banners, and AgentToolLoopStep appended conversational "What is your next tool call?" trailing prompts to user turns.
- **Root Cause:**
  1. HTML, Markdown, and JSON report generators retained hardcoded category rows, badges, and JavaScript/CSS handlers for PESAP metrics.
  2. StateMachineRunner, AiSession, and InteractiveStateBuilder retained legacy PESAP token accumulators and banner debug statements.
  3. AgentToolLoopStep retained conversational scaffolding ("What is your next tool call?") originally meant for text-completion agents rather than native function-calling APIs.
- **Detection Gap ("What did we miss?"):** Report unit tests asserted the presence of the PESAP row rather than validating that decoupled/deprecated phases are omitted from user-facing accounting.
- **Resolution:**
  1. Removed "PESAP (Pre-Execution Semantic Anchor)" row and all phase badge/JS logic from HtmlReportGenerator and MarkdownReportGenerator.
  2. Removed PESAP accumulation and console tracing from StateMachineRunner, AiSession, and InteractiveStateBuilder.
  3. Removed "What is your next tool call?" prompts from AgentToolLoopStep turns and updated DOM pruning to be content-boundary-based.
  4. Updated PreliminaryReportListenerTest to assert that PESAP is absent from report tables and JSON metrics.
- **Safety Net Added:** Automated assertions in `PreliminaryReportListenerTest#testReportTokenAccountingAndCategoryBreakdown` verifying absence of "PESAP" in HTML, Markdown, and JSON reports.
