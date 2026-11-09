# [DEF-20261109-1016] AI Test Fails When Trying to Verify That an Input Field Is Empty

- **Status:** `Resolved`
- **Opened:** 2026-11-09 10:16
- **Closed:** 2026-11-09 10:40
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** During AI test execution, asserting that an input field or element is empty (e.g. `assert_text(selector='#input', expectedText='')` or `ASSERT_TEXT` with `""`) failed with `IllegalArgumentException: Argument must not be null or empty string. Use $.shouldBe(empty) or $.shouldHave(exactText(""))` from Selenide's `PartialText` condition constructor. Furthermore, swapping to `exactText("")` was ineffective for `<input>` and `<textarea>` elements because WebDriver's `WebElement.getText()` returns an empty string even when the input element is populated with text.
- **Root Cause:**
  1. In `AssertAction.java` (`executeTextAssertion`), `Condition.partialText(matchText)` was invoked directly for non-regex assertions without intercepting empty strings (`matchText.isEmpty()`), violating Selenide's precondition that partial text arguments cannot be null or empty.
  2. While Selenide's `exactText("")` accepts empty strings, WebDriver's `WebElement.getText()` returns `""` on `<input>` and `<textarea>` elements, which caused `exactText("")` to falsely pass on populated inputs. Selenide's `Condition.empty` executes `Condition.and("empty", exactValue(""), exactText(""))`, correctly testing both `value` (for inputs and textareas) and text content (for divs, spans, and other containers).
  3. Neither `AssertAction.java` nor `BrowserToolProvider.java` provided first-class element state support for `"empty"` and `"not_empty"` in `ASSERT_ELEMENT_STATE` and `assert_element_state`.
  4. In `BrowserToolProvider.java`, `matchesCandidate(...)` used `candidate.toLowerCase().contains(unescaped.toLowerCase())`, which evaluates to `true` for any non-empty candidate when `unescaped` is `""`, causing populated elements to falsely match empty text queries during locator reanchoring and visual element verification.
- **Detection Gap ("What did we miss?"):** Existing assertion test cases in `AssertActionTest` and `BrowserToolsTest` only verified non-empty string literals (e.g. `"Welcome"`, `"CAD $"`, `"50 €"`). No tests exercised `assert_text` or `ASSERT_TEXT` with an empty string `""` on input or textarea elements.
- **Resolution:**
  1. In `AssertAction.java` (`executeTextAssertion`), intercepted `matchText.isEmpty()` to delegate directly to `element.shouldBe(Condition.empty)` (or `element.shouldNotBe(Condition.empty)` when negated).
  2. In `AssertAction.java` (`executeCompoundStateAssertion`), added first-class `"empty"` and `"not_empty"` states mapping to `element.shouldBe(Condition.empty)` / `element.shouldNotBe(Condition.empty)` with symmetric negation.
  3. In `BrowserToolProvider.java`, added `"empty"` and `"not_empty"` to the `assert_element_state` tool parameter schemas, state enum lists, negation mapping, and evaluation switch.
  4. In `BrowserToolProvider.java` (`normalizeElementState`), normalized `"blank"` and `"empty"` to `"empty"`, and `"not_empty"`, `"non_empty"`, `"not-empty"`, `"non-empty"` to `"not_empty"`.
  5. In `BrowserToolProvider.java` (`matchesCandidate`), returned `candidate.isBlank()` when `expectedText.isEmpty()`, preventing substring `.contains("")` false positive matches on populated candidates.
  6. In `BrowserToolProvider.java` (`matchesElementText`), delegated to `se.is(Condition.empty)` when `expectedText == null || expectedText.isEmpty()`.
  7. In `BrowserToolProvider.java` (`matchesElementOrAssociatedLabel` and `isTextPresentOnPage`), prevented falling back to element labels or entire page body text when `expectedText` is empty.
  8. In `BrowserToolProvider.java` (`createAssertTextTool`), skipped searching container/parent elements when `expectedText.isEmpty()`.
  9. In `neodymium-test-server`, enhanced `testAssertHappyPath.html` with `#empty-input`, `#empty-div`, `#empty-textarea`, and `#populated-textarea`.
- **Safety Net Added:**
  - `AssertActionTest#testAssertActionEmptyTextAssertions` asserting `ASSERT_TEXT` with `""` passes on empty inputs, divs, and textareas, fails on populated inputs/textareas, and handles negation properly.
  - `AssertActionTest#testAssertActionEmptyElementState` asserting `ASSERT_ELEMENT_STATE` with `"empty"` and `"not_empty"` across empty and populated inputs, textareas, and containers.
  - `BrowserToolsTest#testAssertionToolsParameterSchema` verifying `"empty"` and `"not_empty"` are declared in the `assert_element_state` parameter schema enum.
  - `BrowserToolsTest#testNormalizeElementStateEmpty` and `BrowserToolProviderStabilityTest#testNormalizeElementStateUtility` verifying normalization of `"empty"`, `"blank"`, `"[empty]"`, `"EMPTY"`, `"not_empty"`, `"non_empty"`, `"not-empty"`, `"non-empty"`, etc.
  - `BrowserToolsTest#testMatchesCandidateWithEmptyExpectedText` and `BrowserToolProviderStabilityTest#testMatchesCandidateWithEmptyExpectedText` verifying candidate matching returns true only for blank candidates and false for populated candidates when expected text is empty.
