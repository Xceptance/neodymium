# OpenSpec Tasks: Visual Playbook Editor Integration

## Source of Truth Reference
Implementation MUST mirror the DOM structure, styling rules, and JS behaviors defined in:
[playbook-editor-mockup.html](file:///home/weigel/tmp/neos/reworkBranch/src/main/resources/com/xceptance/neodymium/aura/playbook-editor-mockup.html)

---

- [ ] 1. **CSS Integration in `dashboard-styles.css`**
  - [ ] Extract layout styles for `.editor-main-area`, `.editor-code-panel`, `.sidebar-palette-right`, `.btn-add-section-block`, and `.btn-remove-section-block` from mockup HTML into `dashboard-styles.css`.
  - [ ] Extract component styles for step rows (`.step-row`), token pills (`.var-pill`, `.unified-include-pill`, `.hint-badge`), and include tree cards (`.include-tree-card`).
  - [ ] Ensure full dark/light theme support using existing Manager CSS variables and local Material Symbols font.

- [ ] 2. **JavaScript Modularization in `dashboard-editor.js`**
  - [ ] Extract line focus/blur handlers (`handleStepFocus`, `handleStepBlur`).
  - [ ] Extract token parser (`formatStepToTokens`).
  - [ ] Extract caret offset tracking (`getCaretOffset`, `setCaretOffset`, `insertSnippetWithCaret`).
  - [ ] Extract line numbering and reindexing (`updateAllLineNumbers`, `reindexSteps`).
  - [ ] Extract block management (`addBeforeBlock`, `removeBeforeBlock`, `addAfterBlock`, `removeAfterBlock`).
  - [ ] Implement auto-collapse of empty optional section panels (`before:`, `after:`) when cursor/focus leaves.
  - [ ] Extract include tree expansion and inline editing (`handleArrowMouseDown`, `renderIncludeTreeCard`, `enableIncludeEdit`, `saveIncludeInline`, `discardIncludeInline`).
  - [ ] Extract Data Matrix synchronization and live YAML compiler (`compilePlaybookToYaml`).

- [ ] 3. **Thymeleaf Template Integration in `templates/fragments/editor.html`**
  - [ ] Replace static `<textarea id="editorContent">` with visual editor DOM structure.
  - [ ] Add `#addBeforeBtnContainer` and `#addAfterBtnContainer` toggle buttons.
  - [ ] Integrate transposed Test Data Matrix table `#transposedGrid`.
  - [ ] Add Quick Insert Palette sidebar with 4 categorized section groups (`Actions`, `Control Tags`, `Java & Variables`, `Available Included Files`).
  - [ ] Populate `Available Included Files` dynamically using Aura Manager's file service folder tree model (relative file paths).

- [ ] 4. **Java Backend & Controller Updates**
  - [ ] Update `AuraManagerEditorController.java` with `/api/editor/include-tree` HTMX endpoint for include tree fragments.
  - [ ] Update `/api/save` endpoint to process and persist compiled YAML from the visual editor.
  - [ ] Update `AuraFileService.java` with playbook section parsing and serialization helpers.

- [ ] 5. **Manual Browser Verification**
  - [ ] Verify editor rendering, section panel toggling, auto-collapse of empty panels, step line focus/blur, token pills formatting, include tree expansion/editing, palette shortcut insertion, and YAML file saving directly in browser.
  - [ ] Note: Automated JUnit/integration tests will NOT be executed during implementation; manual browser verification is required prior to developer test updates.
