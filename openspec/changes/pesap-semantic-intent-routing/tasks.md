## 1. Semantic Intent Model & PESAP Prompt Extension

- [x] 1.1 Create `SemanticIntent` enum in `org.neodymium.ai.model` (`ASSERT`, `ASSERT_METADATA`, `CLICK`, `TYPE`, `SELECT`, `HOVER_SCROLL`, `NAVIGATE`, `WAIT`, `STORE`, `BRANCH`) with helper methods (`isAssertion()`, `fromCode()`)
- [x] 1.2 Update `src/main/resources/ai-prompts/pesap-pre-step-prompt.md` to define semantic intent classification rules for `i` and minified JSON schema format
- [x] 1.3 Update `PesapPrompt.java` to parse `intent` (`i`) into `PesapResult` and add unit tests in `PesapPromptTest`
- [x] 1.4 Update `PesapPreStep.java` to store `KEY_PESAP_INTENT` in `ExecutionContext` transient data, set `semanticIntent` on `PlaybookStep`, and log intent classification

## 2. Action Extractor Prompt Injection & Java Invariant Guardrails

- [x] 2.1 Update `ActionExtractionPrompt.java` to inject `[SEMANTIC_INTENT]  <INTENT>` into the user message header when available
- [x] 2.2 In `ActionExtractionPrompt.java`, enforce Java-level invariant rejecting mutating actions (`CLICK`, `TYPE`, `CLEAR`, `SELECT`) when intent is `ASSERT` or `ASSERT_METADATA`, logging a warning and treating the step strictly as an assertion
- [x] 2.3 In `ExecuteActionsStep.java`, add execution guard ensuring assertion intent steps do not execute mutating DOM actions

## 3. Metadata Fast-Path & Minimal Context Optimizations

- [x] 3.1 Optimize context level and state capture for `ASSERT_METADATA` to clamp to `MINIMAL` context
- [x] 3.2 Add native evaluation fallback for page title and URL assertions to skip heavy DOM serialization and vision calls

## 4. Diagnostics, Reporting & Documentation

- [x] 4.1 Update `TestExecutionReport.ReportStepEntry` and `StepStats` to capture and record `semanticIntent`
- [x] 4.2 Update `MarkdownReportGenerator` and `HtmlReportGenerator` to display semantic intent badges in step execution details
- [x] 4.3 Update `doc/DOCUMENTATION.md` with Section 4.4 on the **360° LLM Taming & Safety Lifecycle** (Pre-Execution Intent Routing & Volatile ID Stripping, In-Flight Mutating Action Guards, Post-Execution Semantic Outcome Verification & Quality Judge Auditing)

## 5. Verification & Tests

- [x] 5.1 Add comprehensive unit tests in `PesapPromptTest` validating intent classification across multilingual instructions (English, French, German, Japanese)
- [x] 5.2 Add unit tests in `ActionExtractionPromptTest` verifying that mutating actions emitted on assertion intents are strictly rejected
- [x] 5.3 Run integration test suite (`VerlaGuestCheckout_CaFr_French_MissingProvinceBug` and `AddToCartJudgeAndVerificationsTest`) to verify end-to-end execution
