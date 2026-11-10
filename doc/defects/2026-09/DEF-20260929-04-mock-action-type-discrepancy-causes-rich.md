# [DEF-20260929-04] Mock Action Type Discrepancy Causes Rich Editor Initial Content Retention

- **Status:** Resolved
- **Opened:** 2026-09-29
- **Closed:** 2026-09-29
- **Component:** `neodymium-core` (`sandbox-tests` / `mock-integration` / `RichEditorSandboxMockTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Running `RichEditorSandboxMockTest.testRichEditorAutonomousTypingAndSave` failed with `Element should have text "Document saved: Autonomous release notes for Q3 2026." {#saved-message}` because the actual text was `Document saved: Initial draft notes for product release.Autonomous release notes for Q3 2026.`.
- **Root Cause:** In `RichEditorSandboxMockTest.java`, the mock LLM action response specified `"action": "TYPE"`, which maps to `browser_type` with append semantics (`clearFirst = false`). In the live agent implementation, typing into an input/editor defaults to `fill` with replace semantics (`clearFirst = true`). Consequently, the mock test retained the initial HTML placeholder text inside `<div id="rich-editor" contenteditable="true">`.
- **Detection Gap ("What did we miss?"):** The mock response was crafted using legacy `TYPE` action terminology without reflecting the modern `fill` default tool behavior executed by live models on rich text inputs.
- **Resolution:** Updated mock action responses in `RichEditorSandboxMockTest.java` from `"action": "TYPE"` to `"action": "FILL"`.
- **Safety Net Added:** Verified `RichEditorSandboxMockTest` passes all 5 tests (100%).
