# [DEF-20261112-1355] Playbook Drawer Fails to Deserialize Object-Rooted Recorded Playbook JSON

- **Status:** `Resolved`
- **Opened:** 2026-11-12 13:55
- **Closed:** 2026-11-12 14:02
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Opening the recorded playbook drawer for a test (such as `tests/examples/wikipedia_search.yml`) in Aura Manager displays "No Recorded Playbook JSON Found" with error message: `Failed to parse playbook JSON: Cannot deserialize value of type java.util.ArrayList<java.lang.Object> from Object value (token JsonToken.START_OBJECT) at [Source: REDACTED; line: 1, column: 1]`.
- **Root Cause:** `AuraFileService.loadPlaybookDetails` and `acceptPlaybookHealing` explicitly invoked `mapper.readValue(rawContent, List.class)`, strictly expecting a top-level JSON array `[...]`. Live test runs executed via `AuraQueueService` and `NeodymiumAiRunner` persist companion recordings wrapped in a top-level JSON object (`{ "id": "...", "steps": [...] }`). When Jackson encountered `JsonToken.START_OBJECT`, it threw `MismatchedInputException`. Furthermore, `PlaybookStep` lacked `@JsonAlias` for `"promptLine"`, causing steps in recorded playbooks to lose their instruction label when loaded into the drawer.
- **Detection Gap ("What did we miss?"):** Previous unit tests in `AuraFileServiceTest` exclusively tested array-rooted JSON fixtures (`[ { "instruction": ... } ]`), never exercising the object-rooted structure (`{ "id": "...", "steps": [...] }`) generated during actual test runner execution.
- **Resolution:**
  1. Updated `AuraFileService.loadPlaybookDetails` to parse the root JSON generically as `Object.class`, handling both `List<?>` and `Map<?, ?>` (extracting steps from `"steps"`, `"_steps"`, or `"recordedSteps"` and capturing `"id"` into `"playbookId"`).
  2. Enhanced `loadPlaybookDetails` step normalization to fallback to `promptLine`, `step`, or `description` when `instruction` is null, and compute `durationMs` from action durations when missing at step level.
  3. Updated `AuraFileService.acceptPlaybookHealing` to support both `List` and `Map` roots, preserving root metadata (e.g. `"id"`, `"sourceYamlHash"`) when re-serializing updated healing to disk.
  4. Updated `YamlPlaybookParser.parse` and `parseAndMerge` to handle object-rooted companion JSON files.
  5. Added `@JsonProperty("instruction")` and `@JsonAlias({"promptLine", "step", "prompt"})` to `PlaybookStep.instruction`.
- **Safety Net Added:** Added automated regression unit tests:
  - `AuraFileServiceTest.testLoadPlaybookDetailsWithObjectRootedPlaybookJson`
  - `AuraFileServiceTest.testAcceptPlaybookHealingWithObjectRootedPlaybookJson`
  - `YamlPlaybookParserTest.testParseObjectRootedJsonPlaybook`
