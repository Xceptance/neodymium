# [DEF-20261003-04] Premature Blank String Validation in assert_url and assert_title Rejecting Negated Non-Empty Assertions

- **Status:** Resolved
- **Opened:** 2026-10-03
- **Closed:** 2026-10-03
- **Component:** `neodymium-core` (`BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:**
  In `AssertIntegrationTest.testAssertUrl`, Step 8 ("Url is not empty") caused the LLM to emit `{ name: "assert_url", arguments: { expectedUrl: "", exact: true, negated: true } }`. The step failed with `AssertionError: assert_url requires an 'expectedUrl' argument`. Because the initial recording failed, subsequent replay execution modes also failed with `FileNotFoundException: No recorded companion JSON file found`.
- **Root Cause:**
  In `BrowserToolProvider.java`, `createAssertUrlTool` and `createAssertTitleTool` checked `rawExpectedUrl.isBlank()` before inspecting boolean modifiers like `negated`, `notEmpty`, or `empty`. Because the tool schema marked `expectedUrl` as required, models passed an empty string `""` together with `negated: true` to assert that the URL/title is not empty. The tool prematurely rejected the argument as missing without checking if a non-empty assertion was intended.
- **Detection Gap ("What did we miss?"):**
  Unit tests for `assert_url` and `assert_title` only tested assertions with non-empty expected strings (matching or not matching explicit text/regex). There were no unit tests verifying empty or non-empty assertions where `expectedUrl` was blank with `negated: true` or `notEmpty: true`.
- **Resolution:**
  Refactored argument parsing in `createAssertUrlTool` and `createAssertTitleTool` to evaluate `notEmpty` and `negated` before rejecting blank values. When asserting not-empty (`(negated && rawExpected.isBlank()) || notEmpty`), the tool waits until `url != null && !url.trim().isEmpty()`. When asserting empty (`(!negated && exact && rawExpected.isBlank()) || empty`), the tool waits until `url == null || url.trim().isEmpty()`.
- **Safety Net Added:**
  Added unit test cases 4 & 5 to `BrowserToolsTest.testBrowserAssertUrlExecutionAndSchema` and `BrowserToolsTest.testBrowserAssertTitleExecutionAndSchema` explicitly asserting non-empty URL and title behavior with `{ expectedUrl: "", negated: true }` and `{ notEmpty: true }`.
