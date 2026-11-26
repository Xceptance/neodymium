# [DEF-20261013-1105] Playbook Steps Without Hyphen Following Arrow Folded Scalar Logged as Independent Steps in Console Execution Log

- **Status:** `Resolved`
- **Opened:** 2026-10-13 11:05
- **Closed:** 2026-10-13 12:05
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** In interactive execution runs (e.g. `console-execution.json`), steps following a `->-\n` or `->-` marker without hyphens were flattened and displayed as independent top-level steps rather than grouped as `subSteps` under a single parent step. Furthermore, when preceding comments or block titles were present, the composite parent step was either missing or lacked appropriate descriptive naming (e.g., failing to name unnamed blocks `"Substeps begining with <first substep prompt>"`).
- **Root Cause:**
  1. `YamlPlaybookParser.parseStepBlock` parsed multi-line block strings (`rawSteps instanceof String`) without maintaining state across un-hyphenated lines under a `->-` header, causing un-hyphenated lines to be either treated as top-level steps or discarded.
  2. `AuraFileService.normalizeBlockScalars` contained a regex pattern `([ \t]*(?:-\s*|...))>(-\s*...)` where `-\s*` matched `-` followed immediately by `>-` (zero whitespace), unintentionally replacing `->-` with `-|-` (literal block scalar) and bypassing folded scalar detection in subsequent parser logic.
  3. `InteractiveStateBuilder.buildStateJson` iterated directly over `flatSteps` (where composite steps were flattened into leaf steps) rather than preserving root parent steps (`playbook.steps`), causing all substeps to be emitted as top-level items in `blocks.steps`.
  4. `InteractiveConsoleListener` resolved current step indices against `flatSteps` without mapping composite sub-steps to their root parent step index, leading to index mismatches when composite step hierarchies were executed.
- **Detection Gap ("What did we miss?"):** Previous tests in `InteractiveConsoleEngineTest` and `YamlPlaybookParserTest` primarily verified standard hyphenated YAML step lists and conditional `_include` directives. Arrow folded scalar substeps (`->-`) had not been tested end-to-end through `InteractiveStateBuilder`'s `console-execution.json` serialization.
- **Resolution:**
  1. Updated `YamlPlaybookParser.java` to track `inArrowSubsteps` state during multi-line string parsing, creating a composite parent `PlaybookStep` containing each un-hyphenated step as a substep.
  2. Implemented `resolvePrecedingComment` and `resolveParentInstruction` in `YamlPlaybookParser.java` to derive the parent step's instruction from preceding comments (e.g. `# Step comment`), or fallback to `"Substeps begining with " + firstSubstepPrompt` when no comment exists.
  3. Corrected `FOLDED_BLOCK_SCALAR_PATTERN` in `AuraFileService.java` to require whitespace after hyphen (`-\s+`), preventing false-positive normalization of `->-` markers.
  4. Updated `InteractiveStateBuilder.java` to serialize root steps (`playbook.steps`) into `blocks.steps`, rendering substeps under `subSteps: [...]` and preserving the hierarchical execution tree in `console-execution.json`.
  5. Updated `InteractiveConsoleListener.java` to map `step.getRootStep()` against `playbook.steps` for accurate step index resolution.
- **Safety Net Added:**
  - `YamlPlaybookParserTest.testParsePlaybookWithArrowSubstepsAndComment`
  - `YamlPlaybookParserTest.testParsePlaybookWithArrowSubstepsWithoutComment`
  - `YamlPlaybookParserTest.testParsePlaybookWithArrowSubstepsInlineAndTermination`
  - `InteractiveConsoleEngineTest.testArrowSubstepsLoggedAsSubstepsInConsoleExecutionJson`
