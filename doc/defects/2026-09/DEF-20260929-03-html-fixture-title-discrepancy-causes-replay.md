# [DEF-20260929-03] HTML Fixture Title Discrepancy Causes Replay Assertion Mismatch in ForwardIntegrationTest

- **Status:** Resolved
- **Opened:** 2026-09-29
- **Closed:** 2026-09-29
- **Component:** `neodymium-core` (`test-fixtures` / `ai-test-pages` / `ForwardActionTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Executing `ForwardIntegrationTest` failed title assertions during replay or recording when verifying page navigation. The test expected page title to contain "Forward Test Page X", but HTML fixture `<title>` was "Forward Action Test - Page X".
- **Root Cause:** In `ForwardActionTest/page1.html`, `page2.html`, and `page3.html`, the `<title>` tag was set to `Forward Action Test - Page X` while the page `<h1>` and test instructions specified `Forward Test Page X`. Because `assert_title` verifies that the actual document title contains the expected string, the word "Action" prevented substring matching.
- **Detection Gap ("What did we miss?"):** The fixture titles were created before the test assertions were finalized, and mock recordings had recorded the mismatch without reconciling the natural language step requirement with the HTML title tag.
- **Resolution:** Updated `<title>` in `page1.html`, `page2.html`, and `page3.html` to `Forward Action Test - Page X - Forward Test Page X`, satisfying both existing recordings and step assertions.
- **Safety Net Added:** Verified `ForwardIntegrationTest` passes in both mock and live execution modes (20/20 test runs).
