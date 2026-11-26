# [DEF-20261012-1145] Missing YAML Comments and Flattened Sub-steps for Folded Block Scalars in Test Editor and Reports

- **Status:** `Resolved`
- **Opened:** 2026-10-12 11:45
- **Closed:** 2026-10-12 12:15
- **Component:** `neodymium-core` / `aura-manager`
- **Scope:** `Framework`
- **Symptom:** YAML comments (e.g. `# Add product to cart`, `# Open cart`) were omitted from the Visual Test Editor, and verification steps written as folded block scalars (`- >-`) were rendered as flat, unindented top-level steps rather than grouped sub-steps. During test execution and HTML reporting, these steps lacked parent-child hierarchy and were not logged as sub-steps by `InteractiveConsoleListener`.
- **Root Cause:**
  1. `AuraFileService.parsePlaybookSections` delegated solely to SnakeYaml, which strips comment nodes during parsing, preventing YAML comments from reaching the editor DOM.
  2. `YamlPlaybookParser` parsed multi-line scalar strings into standalone sequential steps without establishing parent-child relationships (`step.getSubSteps()` and `child.setParent(parent)`).
  3. The Visual Editor template (`editor.html`), stylesheet (`dashboard-styles.css`), and JavaScript serialization (`dashboard-editor.js`) lacked styling and gutter representations for comments (`#`) and sub-steps (`↳`).
  4. `InteractiveConsoleListener` restricted sub-step progress logging exclusively to include fragments (`_include`), ignoring composite parent steps containing nested sub-steps.
- **Detection Gap ("What did we miss?"):** Existing parser and UI tests asserted total step count without validating comment preservation, parent step hierarchy in execution models, or visual indicators (`.step-row-comment`, `.step-row-substep`, `↳`) in the browser editor.
- **Resolution:**
  1. Added `AuraFileService.extractSectionStepsWithComments` to retain `#` comments and strictly match the `- >-.*\n` sequence pattern to identify and prefix folded scalar sub-steps with `"  - "` across `before`, `steps`, and `after` playbook sections. All other steps—including literal block scalars (`- |`, `- |-`), plain folded scalars (`- >`), and standard bullet steps—are preserved and displayed as normal steps.
  2. Enhanced `YamlPlaybookParser` to detect folded block scalars (`isFoldedScalarStep`) strictly matching the `- >-` pattern and construct composite parent steps with children (`setParent`, `getSubSteps`), extracting parent instructions from preceding/inline YAML comments or descriptive defaults (`"Detailed verification"`). Plain folded scalars (`- >`) and literal block scalars (`- |`) decompose into normal sequential steps.
  3. Updated `editor.html`, `dashboard-styles.css`, and `dashboard-editor.js` to render `.step-row-comment` (transparent background, muted slate text, dedicated `[#]` hash badge chip) and `.step-row-substep` (enclosed in a card container with rounded borders, tree branch guide lines `|--`, hierarchical numbering `parent.child`, and hover actions), complete with an interactive collapsible `.substeps-group-header` toggle bar with expand/collapse chevron, dynamic title extracted from preceding comments, and count badge. Lossless round-trip YAML serialization compiles contiguous sub-step groups with `  - >-\n`.
  4. Updated `InteractiveConsoleListener` to log sub-step start and finish events for any parent step with children (`Executing sub-step #X/Y ("...") of parent "..."`).
  5. Implemented immediate `#` typing detection (`checkAndUpdateCommentRow`) via top-level input listeners and inline `oninput` handlers on all step rows. Typing `#` at the beginning of a step instantly toggles `.step-row-comment`, renders the `#` badge in the gutter, re-indexes subsequent step numbers sequentially, and updates any adjacent sub-step group header title; removing `#` immediately restores normal step styling and integer numbering.
- **Safety Net Added:**
  - `com.xceptance.neodymium.aura.AuraFileServiceTest#testParsePlaybookSectionsPreservesCommentsAndSubSteps`
  - `com.xceptance.neodymium.aura.AuraFileServiceTest#testParsePlaybookSectionsWithFoldedBlockScalarChompedSteps`
  - `com.xceptance.neodymium.aura.AuraFileServiceTest#testParsePlaybookSectionsWithLiteralBlockScalarSteps` (verifying `- |` renders as normal steps without `"  - "`)
  - `com.xceptance.neodymium.aura.AuraFileServiceTest#testParsePlaybookSectionsWithHierarchicalMapSubSteps`
  - `org.neodymium.ai.playbook.YamlPlaybookParserTest#testParsePlaybookWithCommentPrecedingFoldedBlockScalar`
  - `org.neodymium.ai.playbook.YamlPlaybookParserTest#testParsePlaybookWithInlineCommentOnFoldedBlockScalar`
  - `org.neodymium.ai.playbook.YamlPlaybookParserTest#testParsePlaybookWithLiteralBlockScalarSteps` (verifying `- |` decomposes to normal steps without sub-steps)
  - `org.neodymium.ai.playbook.YamlPlaybookParserTest#testParsePlaybookWithPlainFoldedScalarWithoutDashIsNormalSteps` (verifying `- >` decomposes to normal steps without sub-steps)
  - `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testOpenFoldedScalarStepsRendersDiscreteRowsInVisualEditor` (asserting rows, collapse/expand toggle interactions, group title, badge count, hierarchical numbering `1.1..1.3`, and transparent comment background)
  - `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testTypeHashAtBeginningOfLineStylesRowAsCommentImmediately` (asserting immediate `.step-row-comment` class application, `#` gutter badge, subsequent row renumbering on `#` insertion, and immediate restoration upon deletion)
