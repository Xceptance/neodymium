# [DEF-20261004-1335] Claude finding P0-4 claiming upfront recording deletion in FORCE_RECORDING violates documentation

- **Status:** `WontFix`
- **Opened:** 2026-10-04 13:00
- **Closed:** 2026-10-04 13:35
- **Component:** `neodymium-core`
- **Scope:** `Doc/Spec` & `Framework`
- **Symptom:** Claude review (2026-10-01, P0-4) flagged that `NeodymiumAiRunner` deletes existing recording files upfront in `beforeEach` before the test completes, claiming this violated documentation stating that recordings are written upon test success.
- **Root Cause:** Adversarial stress-testing and architectural analysis confirmed that upfront deletion in `FORCE_RECORDING` is an intentional safety guarantee. `FORCE_RECORDING` explicitly signals the user's intent to discard previous recordings and establish a new baseline. If a test running in `FORCE_RECORDING` fails partway through, leaving an old recording on disk would cause subsequent replay runs to falsely trust a stale or conflicting baseline.
- **Detection Gap ("What did we miss?"):** The documentation and code comments did not explicitly distinguish between standard `LLM_RECORDING` (which overwrites on success and preserves on failure) and `FORCE_RECORDING` (which wipes the baseline upfront so failure never leaves stale traces).
- **Resolution:** Marked as `WontFix` regarding code deletion removal. Clarified code comments in `NeodymiumAiRunner.java` and scoped upfront deletion strictly to `FORCE_RECORDING` while ensuring `LLM_RECORDING` preserves existing files upfront (resolved in DEF-20261004-1330).
- **Safety Net Added:** Verified by `NeodymiumAiRunnerTest.testForceRecordingDeletesUpfrontWhileLlmRecordingPreserves`.
