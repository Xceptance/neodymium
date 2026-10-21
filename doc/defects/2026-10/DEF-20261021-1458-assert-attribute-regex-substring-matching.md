# [DEF-20261021-1458] Support Substring Regex Matching in assert_attribute

- **Status:** `Resolved`
- **Opened:** 2026-10-21 14:45
- **Closed:** 2026-10-21 14:58
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In live test executions (e.g. `TGifts`), asserting partial attribute values using `assert_attribute` with `regex=true` failed with a 3,000ms condition timeout:
  ```text
  Error: Element should have match attribute aria-label~/Hochgeladenes Foto/ {PlaywrightBy: [role="img"]}
  Element: '<div ... aria-label="Hochgeladenes Foto mit dem Namen ifolor-image-1792586054022-1846296935174937703.jpg" ... role="img" ...></div>'
  Actual value: aria-label~/Hochgeladenes Foto mit dem Namen ifolor-image-1792586054022-1846296935174937703.jpg/
  (Timeout: 3s)
  ```
- **Root Cause:** In `BrowserToolProvider.java`, `assert_attribute` with `regex=true` directly delegated to Selenide's built-in `Condition.attributeMatching(attrName, expectedValue)`. Selenide's `MatchAttributeWithValue` evaluates `attributeRegex.matcher(attributeValue).matches()`. In Java's regex engine, `Matcher.matches()` checks for a complete, whole-string match against the attribute, whereas `Matcher.find()` tests for a substring match. As a result, unanchored regexes failed whenever the attribute contained prefix, suffix, or surrounding text unless explicitly wrapped in `.*`. This also caused an architectural discrepancy with `PlaybookToolReplayer.java` and `AssertAction.java`, which already utilized `Matcher.find()`.
- **Detection Gap ("What did we miss?"):** `BrowserToolProviderStabilityTest` tested `assert_attribute` with substring matching for `exact=false` and full value equality, but lacked a test asserting partial unanchored regex patterns against compound attribute strings.
- **Resolution:** Replaced the direct delegation to Selenide's `Condition.attributeMatching` in `BrowserToolProvider.java` with a dedicated `AttributePatternCondition` implementing `WebElementCondition`. It compiles the pattern with `Pattern.DOTALL | Pattern.CASE_INSENSITIVE` and evaluates `pattern.matcher(val).find()`, permitting substring matching when unanchored while continuing to enforce strict whole-string matching when anchored (`^...$`).
- **Safety Net Added:** Added unit test `testAssertAttributeRegexSubstringAndAnchoredMatching` in `BrowserToolProviderStabilityTest.java` verifying unanchored regex matching against compound attributes, anchored full-string match failures/successes, and negated pattern assertions.
