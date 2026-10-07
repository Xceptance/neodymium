# [DEF-20261007-1540] Playbook Drawer Crashes with SpEL EL1008E on Missing Healed/Failed Map Properties

- **Status:** `Resolved`
- **Opened:** 2026-10-07 15:40
- **Closed:** 2026-10-07 15:45
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Opening the playbook view drawer in Aura Manager fails with `TemplateProcessingException: Exception evaluating SpringEL expression: "step.status == 'HEALED' or step.healed == true ? 'badge-healed' : ..."` and `SpelEvaluationException: EL1008E: Property or field 'healed' cannot be found on object of type 'java.util.LinkedHashMap'` (as well as subsequent errors for `step.failed` or `playbook.testFileLastEdited`).
- **Root Cause:** Playbook JSON files and step objects are deserialized by Jackson into generic `Map<String, Object>` (e.g. `LinkedHashMap`). In Thymeleaf with SpringEL, accessing `map.property` triggers reflection-based property resolution. When a key is absent from the map, SpringEL throws `EL1008E` rather than returning null. In `side-panel-playbook.html`, expressions accessed `step.healed`, `step.failed`, and `playbook.testFileLastEdited` via dot syntax instead of map bracket index syntax (`step['healed']`, `step['failed']`, `playbook['testFileLastEdited']`). Furthermore, `AuraFileService.loadPlaybookDetails` omitted explicit initialization of `"healed"` and `"failed"` keys in step maps.
- **Detection Gap ("What did we miss?"):** Prior controller tests mocked or stubbed service calls without triggering template fragment evaluation against realistic deserialized JSON maps containing steps without an explicit `healed` or `failed` boolean entry.
- **Resolution:**
  1. Updated `side-panel-playbook.html` to convert all dot-notation property accesses on `step`, `act`, and `playbook` to safe bracket syntax (`step['healed']`, `step['failed']`, `playbook['testFileLastEdited']`, `playbook['steps']`, etc.), allowing safe null handling in SpEL expressions.
  2. Updated `AuraFileService.loadPlaybookDetails` to explicitly compute and inject `stepMap.put("healed", isStepActuallyHealed)` and `stepMap.put("failed", isFailed)` during step map processing.
- **Safety Net Added:** Added unit test `AuraTestQueueControllerTest.testSidePanelPlaybookTemplateRendersWithoutHealedOrFailedProperties`, which parses `fragments/side-panel-playbook.html` using `SpringTemplateEngine` with a realistic step map lacking `healed` and `failed` keys, asserting that the template renders without SpEL exceptions.
