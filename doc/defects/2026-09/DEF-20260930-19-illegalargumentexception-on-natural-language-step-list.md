# [DEF-20260930-19] IllegalArgumentException on natural language step list items containing colons or hints

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** `org.neodymium.ai.playbook.YamlPlaybookParser`
- **Scope:** `Framework`
- **Symptom:** Parsing playbooks with natural language YAML list steps containing colons within step text (such as `- Generate random email address (hint: use java method)`) fails with `java.lang.IllegalArgumentException: Invalid playbook step format in file: ... Expected string step, 'include' map, or 'instruction' map, but found map keys: [Generate random email address (hint]`.
- **Root Cause:** SnakeYAML parses list items containing `: ` (`- key: value`) into single-entry `Map` objects. `YamlPlaybookParser.parseStepBlock` rejected single-entry maps whose scalar values did not contain an `include` keyword, failing to recognize natural language step instructions containing colons (such as parenthetical hints `(hint: ...)` or formatted text `Label: text`).
- **Detection Gap ("What did we miss?"):** Unit tests only tested simple string list steps without inline colons or parenthetical hints containing `: `.
- **Resolution:** In `YamlPlaybookParser.java`, updated single-entry map scalar handling in `parseStepBlock` to reconstruct `key + ": " + value` into a full step instruction for any single-entry map in a step list, while maintaining strict `IllegalArgumentException` validation for invalid multi-key step maps.
- **Safety Net Added:** Added unit test `testParseYamlListStepWithParentheticalHintColon()` in `YamlPlaybookParserTest.java` validating natural language steps with colons in parenthetical hints or step text.
