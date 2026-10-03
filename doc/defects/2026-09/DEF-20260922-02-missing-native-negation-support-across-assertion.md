# [DEF-20260922-02] Missing Native Negation Support across Assertion Tools Causing Flakiness on Negative Assertions

- **Status:** Resolved
- **Opened:** 2026-09-22
- **Closed:** 2026-09-22
- **Component:** `neodymium-core` (`ai-tool`, `browser-tool-provider`, `ai-executor`, `action-model`)
- **Scope:** `Framework`
- **Symptom:** In `AssertIntegrationTest.testAssertUrl`, step 9 ("There is no '#' in the url") exhibited non-deterministic flakiness: in some runs, the LLM invoked `assert_url({"expectedUrl": "#"})` without negation support, timing out waiting for '#' to appear and failing the step; in other runs, the LLM guessed a complex regex workaround (`^[^#]*$`), passing the step.
- **Root Cause:**
  1. None of Neodymium's browser assertion tools (`assert_url`, `assert_title`, `assert_text`, `assert_attribute`, `assert_count`, `assert_element_state`) exposed a first-class `negated` / `not` property in their schema or runtime execution loops.
  2. The `Action` domain model lacked a `negated` property and corresponding tool serialization/deserialization logic, preventing offline replays and action plugins from preserving or enforcing negative assertions.
  3. `AssertAction` lacked handling for negative assertions on URL, title, text, attribute, and count (`!=`), as well as `ASSERT_UNFOCUSED`.
- **Detection Gap ("What did we miss?"):**
  Assertion tool tests previously verified positive existence or exact matches, but lacked negative asserting suites testing that absence of characters, absent attributes, not-equal counts, and element state inverters (e.g. visible <-> hidden, focused <-> unfocused) work reliably without prompt engineering regexes.
- **Resolution:**
  1. Added `@JsonProperty("negated") private boolean negated = false;` to `Action` with full constructor overloads, with-methods, and JSON serialization/deserialization.
  2. In `Action.toToolCall()`, emit `"negated": true` if set, and map `ASSERT_UNFOCUSED`.
  3. In `Action.fromToolCall()`, parse `negated` (with aliases `not`, `invert`, `inverted`), invert element states, and map `NOT_EQUALS` count assertions to `!=`.
  4. Added `negated` schema property and inverted condition loops across `assert_url`, `assert_title`, `assert_text`, `assert_attribute`, `assert_count`, and `assert_element_state` (including `unfocused`).
  5. Updated `AssertAction` to honor `action.isNegated()` on URLs, titles, text, attributes, count (`!=`), and element focus.
- **Safety Net Added:**
  - `ActionTest#testNegatedJsonRoundTrip`, `ActionTest#testFromToolCallWithNegatedFlag`, `ActionTest#testFromToolCallAssertElementStateInversion`, `ActionTest#testFromToolCallAssertElementStateUnfocused`, `ActionTest#testFromToolCallAssertCountNotEquals`.
  - `BrowserToolsTest#testAssertToolsNegationSchemaProperties`, `BrowserToolsTest#testNormalizeElementStateUnfocused`.
  - `BrowserToolProviderStabilityTest` async polling regression coverage.
  - `AssertActionTest#testNegatedUrlAssertions`, `AssertActionTest#testNegatedTitleAssertions`, `AssertActionTest#testNegatedTextAssertions`, `AssertActionTest#testNegatedAttributeAssertions`, `AssertActionTest#testUnfocusedAndNegatedCountAssertions`.
