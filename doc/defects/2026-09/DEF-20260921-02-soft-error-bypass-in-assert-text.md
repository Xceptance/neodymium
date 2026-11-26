# [DEF-20260921-02] Soft Error Bypass in assert_text Suppressing AssertionError on Mismatched Selectors

- **Status:** Resolved
- **Opened:** 2026-09-21
- **Closed:** 2026-09-21
- **Component:** `neodymium-core` (`ai-tool`, `browser-tool-provider`, `ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `WikipediaProgrammaticTestDataTest`, Step #5 (`Verify the main heading contains '${searchPhrase}' (bug)`) failed with `ExpectedBugNotReproducedException: Expected bug but step succeeded` instead of catching the headline mismatch defect ("Neodym" vs "Neodymium"). The HTML report displayed an uncommanded second assertion on `#mw-content-subtitle`.
- **Root Cause:**
  1. In `BrowserToolProvider.createAssertTextTool`, when an element selector failed to match the expected text within `Configuration.timeout`, the tool checked `if (!isTextPresentOnPage(...))`. If the expected text existed anywhere else on the page (e.g. in a redirect subtitle, breadcrumb, or footer), it returned `ToolResult.error` instead of throwing `AssertionError`.
  2. Returning `ToolResult.error` bypassed Stop Criterion 2 (immediate termination on assertion failures) in `AgentToolLoopStep`.
  3. The agent loop fed the error back to the LLM in Turn 2, which searched for the text elsewhere in the DOM, asserted on `#mw-content-subtitle` instead of the commanded `#firstHeading`, and called `complete_step`.
  4. This false step success caused `ExecuteActionsStep` to throw `ExpectedBugNotReproducedException` on the expected defect step.
- **Detection Gap ("What did we miss?"):** `BrowserToolProviderStabilityTest` tested page-wide fallbacks and asynchronous polling, but did not assert that an element-specific `assert_text` failure throws `AssertionError` when the text is present in another element on the page.
- **Resolution:**
  1. Removed `isTextPresentOnPage` bypass from element-targeted `assert_text` in `BrowserToolProvider`, throwing `AssertionError` directly when the specified selector does not match the expected text within timeout.
  2. Maintained page-wide text assertions (`isTextPresentOnPage`) only when `selector` is null, blank, `"body"`, or `"html"`.
- **Safety Net Added:** Unit regression test in `BrowserToolProviderStabilityTest#testAssertTextThrowsAssertionErrorWhenSelectorDoesNotMatchEvenIfTextExistsElsewhereOnPage` verifying that `assert_text` on a specific selector throws `AssertionError` when the element text does not match, even if the text exists elsewhere in the document body.
