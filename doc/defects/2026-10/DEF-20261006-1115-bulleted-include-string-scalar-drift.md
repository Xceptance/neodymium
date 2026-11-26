# [DEF-20261006-1115] Bulleted String Includes Treated as LLM Prompts Causing Token Exhaustion

- **Status:** `Resolved`
- **Opened:** 2026-10-06 11:15
- **Closed:** 2026-10-06 11:20
- **Component:** `neodymium-core` / `eminence`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** Eminence `SearchSingleProductTest` failed with token budget exhaustion (`Total tokens consumed (202826) exceeded configured step token budget (200000)`). The test execution report showed Step #1 executing the literal text `- _include: fragments/open-pdp.steps` as a browser instruction via the LLM agent for 20 futile turns.
- **Root Cause:** 
  1. In `eminence`, `SearchSingleProductTest.yaml` specified `steps: |` declaring steps as a multiline string scalar rather than a YAML sequence, with a stale reference to `fragments/open-pdp.steps` instead of `fragments/open-pdp.yaml`.
  2. In `neodymium-core`, `YamlPlaybookParser.parseStepBlock` for string blocks only checked `trimmed.startsWith("_include:")` or `trimmed.startsWith("include:")`. Because the string block contained list bullets (`- _include:`), the prefix prevented include detection. Consequently, the raw include directive fell through and was instantiated as a regular `PlaybookStep`, which was then dispatched to the LLM agent as a browser instruction.
  3. Single-line quoted string includes in sequence lists (`- "_include: ..."`) were similarly parsed as plain steps rather than expanding includes.
- **Detection Gap ("What did we miss?"):** No parser unit tests covered multiline string scalars (`steps: |`) containing bulleted `- _include:` directives, and there was no validation guard preventing unresolved `_include:` text from escaping parse time as an executable browser step.
- **Resolution:** 
  1. In `eminence`, updated `SearchSingleProductTest.yaml` to use standard `steps:` sequence syntax and point to `fragments/open-pdp.yaml`.
  2. In `neodymium-core`, updated `YamlPlaybookParser` to strip bullet prefixes (`- `, `* `) before detecting includes and unbullet top-level parent steps in multiline string blocks.
  3. Added support for single-line string include list items (`stepItem instanceof String`).
  4. Added `validateNoUnresolvedIncludeDirectives` to reject any playbook at parse time if an `_include:` or `include:` directive escaped resolution, failing immediately with an actionable error.
  5. Updated `PlaybookParserTest.testParseNestedIncludes` assertions to match the recursive inlining contract (3 inlined steps).
- **Safety Net Added:** Added unit tests `testParseMultilineStringWithBulletedInclude` and `testUnresolvedIncludeDirectiveThrowsException` in `YamlPlaybookParserTest`, and verified all 37 tests in `YamlPlaybookParserTest` and `PlaybookParserTest` pass.
