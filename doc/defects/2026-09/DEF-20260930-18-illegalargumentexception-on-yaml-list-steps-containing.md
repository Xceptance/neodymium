# [DEF-20260930-18] IllegalArgumentException on YAML list steps containing colons or inline includes

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `org.neodymium.ai.playbook.YamlPlaybookParser`
- **Scope:** `Framework`
- **Symptom:** Parsing playbooks containing list steps with colons in step text (such as `- ... and _include: fragment.steps`) fails with `java.lang.IllegalArgumentException: Invalid playbook step format in file: ... Expected string step, 'include' map, or 'instruction' map, but found map keys: [...]`.
- **Root Cause:** SnakeYAML parses list items containing `: ` into single-entry `Map` objects. In `YamlPlaybookParser.parseStepBlock`, single-entry maps were only handled if the value was a `List` or `Map` (sub-steps) or if the key was exact `_include`/`instruction`. Single-entry maps with string/primitive values (resulting from natural language step lines containing colons or inline `_include:`) threw an `IllegalArgumentException`.
- **Detection Gap ("What did we miss?"):** Unit tests in `YamlPlaybookParserTest` did not cover YAML list items containing inline colons (`:`) or inline `_include:` parameters within step text.
- **Resolution:** In `YamlPlaybookParser.java`, extended single-entry map handling in `parseStepBlock` to reconstruct `key + ": " + value` into full step instruction text when the value is scalar (`String`, primitive, or `null`), correctly creating `PlaybookStep` instances or resolving inline include directives.
- **Safety Net Added:** Added unit test `testParseYamlListStepWithInlineColonAndInclude()` in `YamlPlaybookParserTest.java` validating list steps containing inline colons and `_include:` targets.
