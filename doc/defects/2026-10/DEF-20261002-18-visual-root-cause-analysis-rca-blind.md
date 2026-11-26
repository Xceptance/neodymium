# [DEF-20261002-18] Visual Root Cause Analysis (RCA) Blind Spot on Browser Form Validation and Missing Rubrics Architecture

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`VisualRcaPrompt`, `VisualRcaResult`, `VisualRcaStep`, `StateMachineRunner`, `HtmlReportGenerator`, `MarkdownReportGenerator`, `visual-rca-prompt.md`)
- **Scope:** `Framework`
- **Symptom:**
  When checkout halted due to a missing mandatory Province dropdown on Verla Canadian French store, Visual RCA failed to identify the root cause. It produced a generic macro-level diagnosis (*"application remains on checkout page... order submission either was not triggered or has not yet finished"*), completely overlooking the visible, browser-native HTML5 validation bubble (`! Please fill out this field.` / `! Please select an item in the list.`) pointing directly to the unselected Province select box, and confabulated the button text as English `"Complete Order"` instead of French `"ACHETER"`.
- **Root Cause:**
  1. `visual-rca-prompt.md` lacked a structured rubric evaluation framework. An open-ended instruction ("What is the root cause?") allowed vision models to stop at macro symptoms ("still on checkout page") rather than scrutinizing form fields.
  2. `VisualRcaPrompt` provided only the immediate failed step's tool call (`ASSERT_TEXT`), depriving the vision model of recent preceding execution actions (e.g. clicking the submit button `ACHETER` in the immediately preceding step).
  3. The prompt lacked explicit instructions to scrutinize native HTML5 validation bubbles/tooltips, input border highlights, or required field indicators in multi-field forms, and lacked multilingual transcription constraints to prevent confabulating English button names.
  4. Both HTML and Markdown report generators lacked structured rubric visualization for visual RCA diagnoses.
- **Detection Gap ("What did we miss?"):**
  Visual RCA unit tests tested text string pass-through with mock responses rather than validating end-to-end diagnosis of form validation failures on live SUT flows with native validation popups.
- **Resolution:**
  1. Created structured `VisualRcaResult` DTO with 4 diagnostic rubrics (`targetPresence`, `formValidation`, `flowState`, `obstruction`) and synthesized `rootCause`, with backward-compatible `toFormattedDiagnosis()`.
  2. Upgraded `visual-rca-prompt.md` with explicit Form & Validation Bubble Scrutiny rules, multilingual transcription rules, and enforced JSON schema.
  3. Upgraded `VisualRcaPrompt` to `AiPrompt<VisualRcaResult>`, `ResponseSchema.ASSERTION`, injected the last 5 executed playbook steps from `execution.stepStatsList`, and used `ResponseRepairService.deserialize` with fallback.
  4. Updated `StateMachineRunner` and `VisualRcaStep` to invoke `rcaPrompt.getResponseSchema()` and store `VisualRcaResult` into `ExecutionContext`.
  5. Enhanced `HtmlReportGenerator` and `MarkdownReportGenerator` to render structured RCA cards with rubric status pills (`pill-fail`, `pill-pass`).
- **Safety Net Added:** Added `FrenchMissingProvinceCheckoutTest.java` verifying that an omitted required checkout field halts execution in the SUT sense, triggers Visual RCA, reports `formValidation` rubric as `ERROR_PRESENT`, and explicitly identifies the missing `Province` field in the diagnosis. Added unit tests in `VisualRcaPromptTest.java`.
