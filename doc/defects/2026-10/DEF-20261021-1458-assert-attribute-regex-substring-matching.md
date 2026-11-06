# [DEF-20261021-1458] Support Substring and Regex Matching in assert_attribute (Default exact to false)

- **Status:** `Resolved`
- **Opened:** 2026-10-21 14:45
- **Closed:** 2026-10-21 15:50
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In live test executions (e.g. `TGifts`), asserting partial attribute values using `assert_attribute` failed with condition timeouts in two distinct scenarios:
  1. With `regex=true`:
     ```text
     Error: Element should have match attribute aria-label~/Hochgeladenes Foto/ {PlaywrightBy: [role="img"]}
     Element: '<div ... aria-label="Hochgeladenes Foto mit dem Namen ifolor-image-1792586054022-1846296935174937703.jpg" ... role="img" ...></div>'
     Actual value: aria-label~/Hochgeladenes Foto mit dem Namen ifolor-image-1792586054022-1846296935174937703.jpg/
     (Timeout: 3s)
     ```
  2. With `regex=false` (or omitted):
     ```text
     Error: Element should have attribute aria-label="Hochgeladenes Foto" {PlaywrightBy: [role="img"]}
     Element: '<div ... aria-label="Hochgeladenes Foto mit dem Namen ifolor-image-1792587957645-7204938647068727335.jpg" ... role="img" ...></div>'
     Actual value: aria-label="Hochgeladenes Foto mit dem Namen ifolor-image-1792587957645-7204938647068727335.jpg"
     (Timeout: 3s)
     ```
- **Root Cause:**
  1. In `BrowserToolProvider.java`, `assert_attribute` with `regex=true` directly delegated to Selenide's built-in `Condition.attributeMatching(attrName, expectedValue)`. Selenide's `MatchAttributeWithValue` evaluates `attributeRegex.matcher(attributeValue).matches()`. In Java's regex engine, `Matcher.matches()` checks for a complete, whole-string match against the attribute, whereas `Matcher.find()` tests for a substring match. As a result, unanchored regexes failed whenever the attribute contained prefix, suffix, or surrounding text unless explicitly wrapped in `.*`.
  2. Additionally, `BrowserToolProvider.assert_attribute` defaulted `exact` to `true` (`asBoolean(true)`), enforcing strict full-string equality (`actual.equals(expected)`) whenever `exact` was not explicitly supplied. In contrast, all other assertion tools (`assert_text`, `assert_url`, `assert_title`, and `PlaybookToolReplayer`) default `exact` to `false` (substring match).
- **Detection Gap ("What did we miss?"):** `BrowserToolProviderStabilityTest` tested `assert_attribute` with substring matching for `exact=false` and full value equality, but lacked tests asserting partial unanchored regex patterns against compound attribute strings and verifying the default behavior when `exact` is omitted.
- **Resolution:**
  1. Replaced the direct delegation to Selenide's `Condition.attributeMatching` in `BrowserToolProvider.java` with a dedicated `AttributePatternCondition` implementing `WebElementCondition`. It compiles the pattern with `Pattern.DOTALL | Pattern.CASE_INSENSITIVE` and evaluates `pattern.matcher(val).find()`, permitting substring matching when unanchored while continuing to enforce strict whole-string matching when anchored (`^...$`).
  2. Changed `exact` default in `assert_attribute` from `true` to `false` in both the tool schema definition and execution logic (`call.arguments().path("exact").asBoolean(false)`), making substring matching the consistent default across all Neodymium assertion tools while preserving strict exact matching when `exact: true` is explicitly provided.
- **Safety Net Added:** Added unit tests `testAssertAttributeRegexSubstringAndAnchoredMatching` and `testAssertAttributeDefaultsToSubstringMatch` in `BrowserToolProviderStabilityTest.java` verifying:
  - Unanchored regex matching against compound attributes
  - Anchored full-string match failures/successes
  - Negated pattern assertions
  - Default substring matching when `exact` is omitted, and strict equality when `exact: true` is provided
