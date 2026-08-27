## 1. PESAP Prompt & Model Extension

- [ ] 1.1 Update `src/main/resources/ai-prompts/pesap-pre-step-prompt.md` to include rule for predicting semantic intent `i` (`ASSERT_TEXT`, `ASSERT_STATE`, `ASSERT_URL`, `ASSERT_TITLE`, `INTERACT`, `NAVIGATE`, `BRANCH`, `LOOP`, `WAIT`, `STORE`, `JAVA_METHOD`) and verify prompt schema format
- [ ] 1.2 Update `PesapPrompt.java` to parse `intent` (`i`) from JSON response into `PesapResult` and verify unit tests in `PesapPromptTest`
- [ ] 1.3 Update `PesapPreStep.java` to store `KEY_PESAP_INTENT` in `ExecutionContext` transient data and log intent classification

## 2. Action Extractor Prompt Injection & Java Guardrails

- [ ] 2.1 Update `ActionExtractionPrompt.java` to inject `Semantic Intent: [INTENT]` into user prompt when available
- [ ] 2.2 In `ActionExtractionPrompt.java`, enforce Java-level invariant rejecting mutating actions (`CLICK`, `TYPE`, `CLEAR`, `SELECT`) when `intent` is `ASSERT_*`, overriding or converting to pure assertion/visual check
- [ ] 2.3 In `ExecuteActionsStep.java`, add guard verifying that assertion intent steps do not execute mutating DOM interactions

## 3. Fast-Path & Title/URL Assertion Optimizations

- [ ] 3.1 Optimize context level and state capture for `ASSERT_URL` and `ASSERT_TITLE` to use `MINIMAL` context
- [ ] 3.2 Add native evaluation fallback for URL and Title assertions to skip heavy DOM serialization

## 4. Diagnostics & Reporting

- [ ] 4.1 Update `TestExecutionReport` and `StepExecutionData` to capture and record `pesapIntent`
- [ ] 4.2 Update `MarkdownReportGenerator` and `HtmlReportGenerator` to display PESAP intent badge in step execution details

## 5. Verification & Tests

- [ ] 5.1 Add unit tests in `PesapPromptTest` validating intent classification across multilingual instructions (English, French, German)
- [ ] 5.2 Add unit tests in `ActionExtractionPromptTest` verifying that mutating actions emitted on assertion intents are strictly rejected
- [ ] 5.3 Run integration test suite (`VerlaGuestCheckout_CaFr_French_MissingProvinceBug` and `AddToCartJudgeAndVerificationsTest`) to verify end-to-end execution
