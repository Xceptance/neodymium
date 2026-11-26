# [DEF-20261007-1505] Playbook Drawer Fails to Display "Healed From" Original Target for Self-Healed Steps

- **Status:** `Resolved`
- **Opened:** 2026-10-07 15:05
- **Closed:** 2026-10-07 15:05
- **Component:** `neodymium-core`, `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In Aura Manager's Playbook View drawer, steps with status `HEALED` display the new target (e.g. `#searchLanguage`), but do not render the `🧬 Healed from: [#selectLanguage]` badge showing the original pre-healing locator.
- **Root Cause:** Playbook JSON files on disk preserve the original locator in `step.toolCalls[i].arguments.selector` while storing the healed locator in `step.actions[i].target`, leaving `actions[i].expectedTarget` null. In `AuraFileService.loadPlaybookDetails`, this discrepancy was initially not reconciled. Furthermore, in Thymeleaf template rendering, `th:if` has higher precedence (order 4) than `th:with` (order 6); when both were placed on the same `<div>`, `th:if` evaluated before the local variable was assigned, causing the element to be omitted.
- **Detection Gap ("What did we miss?"):** Prior unit tests in `AuraFileServiceTest` checked presence of steps and timestamps, but did not assert action-level target reconciliation or healing badges on deserialized steps.
- **Resolution:** Updated `AuraFileService.loadPlaybookDetails` to detect and populate `expectedTarget`/`healed` from `step.toolCalls` when locators differ, enhanced `side-panel-playbook.html` by wrapping variable scoping in `<th:block th:with="...">` to respect Thymeleaf attribute precedence, and updated `PlaybookToolReplayer` to synchronize `step.getActions()` upon self-healing.
- **Safety Net Added:** Unit test `AuraFileServiceTest.testLoadPlaybookDetailsWithHealedStepReconcilesOriginalTarget` verifying `loadPlaybookDetails` populates `expectedTarget` and `healed` from `toolCalls`.
