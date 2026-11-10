# [DEF-20261008-1705] Visual RCA in Error Messages Rendered as Unparsed Raw Markdown in Aura Manager

- **Status:** Resolved
- **Opened:** 2026-10-08
- **Closed:** 2026-10-08
- **Component:** `aura-manager` (`report-manager.js`, `report-manager.css`), `neodymium-core` (`HtmlReportGenerator.java`, `VisualRcaResult.java`)
- **Scope:** `Framework` / `Reporting`
- **Symptom:**
  When a test execution encounters an error with Visual RCA or has RCA embedded within its failure message, Aura Manager's execution error box displays raw Markdown syntax (`**Root Cause Analysis:**`, `### Diagnostic Rubrics`, `- **Target Presence Check:** [FOUND]`) in a monospace text dump rather than rendering formatted Markdown and structured rubric scorecard cards.
- **Root Cause:**
  `report-manager.js` joined `failureReason` and `visualRcaExplanation` into a plain string and inserted it via `escapeHtml` into a `<pre-wrap>` monospace container. It lacked parsing for Markdown and rubric cards, and lacked extraction logic when RCA text was embedded directly within `failureReason`. In addition, `HtmlReportGenerator.java` lacked a fallback parser to reconstruct rubric cards when only the Markdown string was supplied.
- **Detection Gap ("What did we miss?"):**
  Existing UI tests in Aura Manager only checked for element presence and string length rather than validating HTML rendering of RCA Markdown and rubric badge elements.
- **Resolution:**
  1. Added RCA detection, extraction, and Markdown/Rubrics parsing in `report-manager.js`.
  2. Added styles in `report-manager.css` for `.visual-rca-box`, `.rubric-item-card`, `.rubrics-grid`, and score badges (`pill-pass`, `pill-fail`, `pill-pending`).
  3. Added `VisualRcaResult.fromFormattedDiagnosis(markdown)` in `neodymium-core` so `HtmlReportGenerator.java` can parse Markdown text into structured rubric cards if `visualRcaResult` is null or if RCA is embedded in `failureReason`.
- **Safety Net Added:**
  Unit tests in `neodymium-core` (`VisualRcaPromptTest.java` and `PreliminaryReportListenerTest.java`) validating that error messages containing RCA (or explicit `visualRcaExplanation` Markdown) are parsed and rendered into styled RCA boxes with rubric cards.
