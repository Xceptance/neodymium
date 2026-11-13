# [DEF-20261021-1336] Raw YAML Area Tag Format Violates Standard YAML Syntax Specification

- **Status:** `Resolved`
- **Opened:** 2026-10-21 13:36
- **Closed:** 2026-10-21 13:38
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `Framework & Doc/Spec`
- **Symptom:** YAML test playbooks serialized with `@Tag` at column 0 trigger syntax errors in external YAML validators and IDE linters because `@` is a reserved indicator in YAML 1.2.
- **Root Cause:** `compilePlaybookToYaml()` in `dashboard-editor.js` serialized the area tag as an unescaped, unquoted root token `@TagName`. While internal SnakeYAML parsing had a preprocessor workaround (`normalizeBlockScalars`), standard external tooling rejected the files as malformed YAML.
- **Detection Gap ("What did we miss?"):** Unit tests verified that Neodymium's internal preprocessor bypassed the SnakeYAML scanner error, but did not assert compatibility against standard external YAML 1.2 syntax validators where preprocessing is not applied.
- **Resolution:** Changed canonical raw YAML serialization to `#@Tag` (e.g. `#@Checkout`), updated raw mode modal insertions and hints to use `#@Tag`, ensured `YamlPlaybookParser.parseString()` recognizes `#@`-prefixed inline playbooks, and retained backward-compatible parsing for existing `# @Tag` and `@Tag` files.
- **Safety Net Added:** Added unit tests in `AuraFileServiceTest.testExtractAreaTagFromContent` and `YamlPlaybookParserTest.testParseYamlPlaybookWithHashAtAreaTag` and `testParseStringWithHashAtAreaTag` explicitly validating the `#@Tag` format.
