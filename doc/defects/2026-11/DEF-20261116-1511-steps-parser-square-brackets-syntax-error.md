# [DEF-20261116-1511] Parsing Error When Using Square Brackets [...] in Included Step Files or Playbooks

- **Status:** `Resolved`
- **Opened:** 2026-11-16 15:11
- **Closed:** 2026-11-16 15:25
- **Component:** `neodymium-core` (`AuraFileService`, `YamlFileReader`, `YamlPlaybookParser`)
- **Scope:** `Framework & Doc/Spec`
- **Symptom:** Steps containing CSS attribute selectors (e.g. `[data-cse='encryptedCardNumber']`) or selector hints containing square brackets (e.g. `(hint selector: [data-cse='...'])`) in `.steps` files or YAML playbooks fail to parse with SnakeYAML `ParserException`:
  `ParserException: while parsing a block mapping in 'string', line 1, column 5: expected <block end>, but found '<scalar>'`
  or
  `ParserException: while parsing a block collection in 'string', line 1, column 5: expected <block end>, but found '<scalar>'`
- **Root Cause:**
  1. In the YAML 1.2 specification, square brackets `[...]` are reserved delimiters indicating flow sequences (inline arrays).
  2. When an unquoted step line in a list starts with `- [` (e.g. `- [data-test=submit] click`), SnakeYAML begins parsing an inline flow sequence. Any trailing text after `]` on the same line violates block collection syntax, throwing `ParserException: while parsing a block collection`.
  3. When an unquoted step line contains a colon followed by a space `: ` (e.g. `(hint selector: [data-cse='...'])`), SnakeYAML interprets the preceding text as a block mapping key and the `[...]` as a flow sequence value. Any subsequent trailing scalar on the same line (e.g. `${creditCardNumber}.`) violates block mapping syntax, throwing `ParserException: while parsing a block mapping`.
  4. Neither `AuraFileService`, `YamlFileReader`, nor `YamlPlaybookParser` preprocessed step lines to auto-quote these patterns or provided diagnostic error messages pointing users to quoting solutions.
- **Detection Gap ("What did we miss?"):**
  Existing test suites (`YamlFileReaderIncludesTest` and `YamlPlaybookParserTest`) only tested standard step phrasing without attribute selectors or bracketed hints in list format, missing real-world test cases utilizing CSS attribute selectors like `[data-test=...]` or Adyen/Stripe iframe selectors.
- **Resolution:**
  1. Added `preprocessYamlSteps(content)` and `shouldQuoteStepLine(trimmed)` to `AuraFileService.java`:
     - Identifies unquoted step list items (`^(\s*-\s+)(.*)$`).
     - Detects items containing square brackets `[...]` or conditional include statements while excluding structural YAML keys (`_include:`, `steps:`, `data:`, `properties:`, etc.).
     - Automatically wraps matched step instructions in escaped double quotes (`"..."`) prior to parsing by SnakeYAML.
  2. Integrated `preprocessYamlSteps` into `YamlFileReader.java` (`preprocessYamlString`) and `YamlPlaybookParser.java` (`parseRecursive` and `parseString`).
  3. Hardened `loadYaml` error handling in both `YamlFileReader.java` and `YamlPlaybookParser.java` with diagnostic guidance on YAML reserved characters (`[...]`, `:`) and quoting recommendations when SnakeYAML mapping/collection exceptions occur.
  4. Updated documentation in `doc/DOCUMENTATION.md` and `doc/conditional-includes.md` detailing YAML special characters, automatic preprocessing, and explicit quoting best practices.
- **Safety Net Added:**
  - `YamlFileReaderIncludesTest#testIncludedStepsWithSquareBracketSelectors` verifying unquoted steps containing square bracket selectors and hints in included step fragments parse and expand without error.
  - `YamlPlaybookParserTest#testParsePlaybookStepsWithSquareBracketsAndColonHints` asserting unquoted steps with attribute selectors, bracketed hints, and colons in YAML playbooks parse into discrete executable `PlaybookStep` instances with exact instructions intact.
