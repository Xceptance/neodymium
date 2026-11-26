# [DEF-20261012-2240] Unwanted Hyphen Prepended to Playbook Steps on Save

- **Status:** `Resolved`
- **Opened:** 2026-10-12 22:30
- **Closed:** 2026-10-12 22:42
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Saving a test playbook in the Aura Visual Test Editor caused normal top-level steps to be serialized with prepended bullet hyphens (`- Step description`) unless they followed folded block scalars (`>-`). When steps contained colons (e.g. `http://...`, `https://...`, `include:...`, or conditional phrases like `If condition: then action`), SnakeYAML attempted to interpret them as key-value mappings rather than list items, causing parse errors or corrupted step text.
- **Root Cause:** In `dashboard-editor.js`, `serializeStepRows()` defaulted to prefixing non-substep rows with `  - ${cleanText}\n`, and section headers were formatted as standard sequence mappings (`steps:\n`). Furthermore, when returning from folded scalar blocks (`>-`), `AuraFileService.extractSectionStepsWithComments()` lacked indentation depth comparison against the folded scalar anchor, which caused subsequent normal steps to be misclassified or forced into bulleted formatting.
- **Detection Gap ("What did we miss?"):** Visual test editor UI tests only verified saving simple single-word or basic sentence steps without colons or URLs, and did not test round-trip YAML persistence preserving multiline literal block scalars (`steps: |`) across top-level steps and folded scalar sub-steps.
- **Resolution:**
  1. Updated `dashboard-editor.js` (`aura-manager` and `neodymium-core` resource copies) to compile section headers (`before: |`, `steps: |`, `after: |`) as multiline block scalars and serialize top-level steps cleanly as `  ${cleanText}\n` without prepending `- `.
  2. Updated `AuraFileService.extractSectionStepsWithComments()` to track `foldedScalarIndent` and cleanly exit folded block scalar context when encountering lines with indentation less than or equal to the scalar indent.
  3. Enhanced `YamlPlaybookParser.java` multiline block scalar parsing to support folded scalar indicators (`- >-`, `- |-`, `>-`, `|-`) when resolving parent instructions and nesting indented sub-steps.
- **Safety Net Added:**
  - `AuraFileServiceTest.testParsePlaybookSectionsWithMultilineStepsAndFoldedSubsteps` in `neodymium-core`.
  - `YamlPlaybookParserTest.testParsePlaybookWithMultilineBlockScalarAndFoldedSubsteps` in `neodymium-core`.
  - `AuraManagerEditorUiTest.testSavePlaybookDoesNotPrependHyphenToNormalSteps` in `aura-manager`.
