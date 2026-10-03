# [DEF-20260929-09] Static Includes Create Synthetic Wrapper Step Nodes and Duplicate Substeps in Console Execution Reports

- **Status:** Resolved
- **Opened:** 2026-09-29
- **Closed:** 2026-09-29
- **Component:** `neodymium-core` (`playbook-parser` / `console-reporting`)
- **Scope:** `Framework`
- **Symptom:** Unconditional static includes (`_include: file.steps`) generated synthetic container step nodes with duplicated substeps in `console-execution.json` instead of clean inlined top-level steps.
- **Root Cause:** `YamlPlaybookParser` wrapped static include steps inside a synthetic `PlaybookStep("_include: ...")` container and assigned `subStep.setParent(containerStep)`. Even after `flattenSteps()` flattened `flatSteps`, child steps retained `parent != null`, causing `PreliminaryReportListener` to reconstruct parent-child step hierarchies in `TestExecutionReport`.
- **Detection Gap ("What did we miss?"):** `YamlPlaybookParserTest` verified step counts after flattening but did not assert `parent == null` or check step hierarchy rendering for static includes.
- **Resolution:** Updated `YamlPlaybookParser` to inline static include steps directly into the playbook step list without synthetic wrapper nodes or parent links, keeping their origin `sourceFile` and `lineNumber` intact.
- **Safety Net Added:** Added unit tests in `YamlPlaybookParserTest` asserting `steps.size() == 5`, `parent == null`, `subSteps.isEmpty()`, and correct origin source file metadata for inlined static include steps.
