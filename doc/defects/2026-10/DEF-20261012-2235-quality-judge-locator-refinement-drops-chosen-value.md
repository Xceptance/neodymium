# [DEF-20261012-2235] Quality Judge Locator Refinement Drops Chosen Value and Regex Flag

- **Status:** `Resolved`
- **Opened:** 2026-10-12 22:35
- **Closed:** 2026-10-12 23:11
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** `HomepageTest` live runs failed at Step 1 ("Validate that United States as country is selected") with error:
  `Expected text/pattern "United States ($)" was not found on selector "#country-trigger-btn" within 3000ms. (Actual element text: "🇺🇸")`.
- **Root Cause:** In interactive discussion mode (and single-shot mode), the LLM Quality Judge correctly identified that the primary generator's selector (`li.country-item.selected`) was invisible inside a closed modal and refined the target to navbar button `#country-trigger-btn`, returning `"chosenLocator": "#country-trigger-btn"`, `"chosenValue": "🇺🇸"`, and `"isRegex": false`. However, `QualityJudgeToolInterceptor` only wrote `winningLocator` into `newArgs.put("selector", winningLocator)`. It completely discarded `judgeResult.getChosenValue()` and `judgeResult.isRegex()`, leaving the original `expectedText` ("United States ($)") intact. This resulted in executing an invalid assertion expecting `"United States ($)"` directly on `#country-trigger-btn` whose actual text is `"🇺🇸"`, immediately triggering `Stop Criterion 2: Immediate fail on real defects!` in `AgentToolLoopStep`.
- **Detection Gap ("What did we miss?"):** Existing `QualityJudgeToolInterceptorTest` cases only asserted the resulting `selector` key on the adjusted `ToolCall`, and only tested click actions (`browser_click`) that had no value or regex arguments. No tests verified that assertion tools (`assert_text`, `assert_attribute`, `assert_value`) or value-bearing actions had their expected value/text and regex flags updated when the Judge adjusted both locator and value.
- **Resolution:**
  1. Added `applyLocator(final ObjectNode args, final String newLocator)` to safely update `selector` or `target`.
  2. Added `extractActionValue(final JsonNode args)` to extract existing expected texts/values (`expectedText`, `expectedValue`, `text`, `value`) so the Judge receives complete action details during interactive discussion and single-shot evaluation.
  3. Added `applyJudgeValueAndRegex(final ObjectNode args, final QualityJudgeResult judgeResult, final String toolName)` to update the corresponding value property (`expectedText`, `expectedValue`, `text`, `value`) when `judgeResult.getChosenValue()` is provided, and set `regex: true` when `judgeResult.isRegex()` is true.
  4. Updated discussion mode consensus (`APPROVED` and `REFINED`) and single-shot mode to apply `applyJudgeValueAndRegex`, and dispatch `deliberated(adjusted)` even when the locator was already matching but the value or regex was refined.
- **Safety Net Added:** Added 4 permanent automated regression unit tests in `QualityJudgeToolInterceptorTest`:
  - `testDiscussionModeJudgeApprovalUpdatesBothSelectorAndExpectedText`
  - `testDiscussionModeJudgeApprovalPropagatesRegexFlag`
  - `testDiscussionModeJudgeApprovalUpdatesExpectedTextWhenSelectorUnchanged`
  - `testSingleShotJudgeRefinementUpdatesBothSelectorAndExpectedValue`
