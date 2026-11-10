# [DEF-20260927-03] False Negative in Temporal Flow Anomaly Test and Non-Agnostic Linter Prompt Examples

- **Status:** Resolved
- **Opened:** 2026-09-27
- **Closed:** 2026-09-27
- **Component:** `neodymium-core` (`ai-prompts` / `live-integration`)
- **Scope:** `Test/Harness` & `Framework`
- **Symptom:** `PrelinterRuleMatrixLiveTest.testTemporalFlowAnomaly_English_CloudIam` failed with `AssertionFailedError: Expected at least one linter finding ==> expected: <false> but was: <true>` due to zero linter findings; `playbook-linter-prompt.md` contained hardcoded language token lists (`and, und, et`, `If, Falls, Wenn`).
- **Root Cause:** In `testTemporalFlowAnomaly_English_CloudIam`, Step 2 clicked "Configure Role" to open general settings rather than opening the Revocation modal referenced in Step 1, so the linter correctly evaluated the sequence as non-inverted. Furthermore, the prompt relied on language-specific keyword lists rather than universal semantic definitions.
- **Detection Gap ("What did we miss?"):** Test fixture steps were written without verifying that the second step unambiguously targeted the opening of the dialog in the first step; prompt review failed to catch non-agnostic keyword enumerations.
- **Resolution:** Updated `testTemporalFlowAnomaly_English_CloudIam` so Step 2 explicitly triggers the confirmation modal dialog (`Click "Revoke Role" to open the confirmation modal dialog`). Refactored `playbook-linter-prompt.md` Rules 1 and 9 to excise language-specific keywords in favor of universal syntactic/semantic definitions.
- **Safety Net Added:** Deterministic unit tests in `PlaybookLinterPromptTest` and verified live isolation tests in `PrelinterRuleMatrixLiveTest`.
