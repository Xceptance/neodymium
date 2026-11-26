# OpenSpec Proposal: Visual Playbook Editor

## Source of Truth
The canonical source of truth for the Visual Playbook Editor layout, styling, interaction models, DOM structure, and component behavior is the standalone mockup file:
[playbook-editor-mockup.html](file:///home/weigel/tmp/neos/reworkBranch/src/main/resources/com/xceptance/neodymium/aura/playbook-editor-mockup.html)

## Intent & Objective
Replace the simple, plain-text YAML `<textarea id="editorContent">` in Neodymium Aura Manager (`src/main/resources/com/xceptance/neodymium/aura/templates/fragments/editor.html`) with an interactive, rich **Visual Playbook Editor**.

The new editor integrates seamlessly into the existing Aura Manager UI, reusing all existing styling, fonts, Thymeleaf template engine, and HTMX fragment mechanisms with **zero new third-party dependencies**.

---

## Detailed Specifications & Refined Features

### 1. Modular Section Code Panels (`before:`, `steps:`, `after:`)
- **`steps:` Panel**: Permanent central panel for core playbook steps.
- **`before:` Panel**: Optional setup steps panel toggled via a subtle `+ Add 'before:' Block` dashed button above `steps:`.
- **`after:` Panel**: Optional teardown steps panel toggled via a subtle `+ Add 'after:' Block` dashed button below `steps:`.
- **Auto-Collapse Empty Panels**: There are no explicit delete buttons on optional section headers. Instead, if an optional panel (`before:` or `after:`) contains no remaining step lines and focus/cursor leaves the panel (`blur`), the panel automatically collapses/hides itself and restores the subtle `+ Add ...` button.

### 2. Continuous Global Line Indexing
- Line numbers sequence continuously ($1 \dots N$) across active section panels in execution order:
  $$\text{before steps} \longrightarrow \text{main steps} \longrightarrow \text{after steps}$$
- Expanded `_include:` step trees increment global line numbers sequentially without resetting.

### 3. Focus / Blur Line Editing & Token Protection
- **Focus**: Clicking or navigating to a step line reveals raw text from `data-raw`.
- **Blur**: Unfocusing parses syntax tokens (`.var-pill`, `.unified-include-pill`, `.hint-badge`) without corrupting `data-raw`.
- **Color Scheme Alignment**: Uses existing Aura Manager color variables from `dashboard-styles.css`:
  - AI & Variable items: Purple (`var(--accent-purple)` / `#8b5cf6`).
  - Core Accents & Navigation: Blue (`var(--accent)` / `#3b82f6`).
  - Success & Saved States: Green (`var(--success)` / `#10b981`).
  - Warnings & Unsaved Badges: Amber (`var(--warning)` / `#f59e0b`).
  - Danger & Delete Actions: Red (`var(--danger)` / `#ef4444`).

### 4. Keyboard Navigation
- **`ArrowUp` / `ArrowDown`**: Move between step lines while preserving the cursor's column index (clamped to the length of destination line if shorter).
- **`ArrowLeft` / `ArrowRight`**: Perform standard text editing character-by-character navigation inside the current line.
- **`Enter`**: Creates a new step row below the current line.

### 5. Caret Offset Tracking & Data Variable Insertion
- Caret offset tracked continuously via `selectionchange`.
- Clicking `[+ Insert]` in the Test Data Matrix splices `${varName}` at the exact recorded cursor position.

### 6. Redesigned Quick Insert Palette
Located in the right sidebar (`.sidebar-palette-right`) and organized into 4 categorized groups:
- **Actions & Templates**:
  - `Open Base URL` $\rightarrow$ Inserts `Open ${neodymium.url}`.
  - `Type Text` $\rightarrow$ Inserts `Type ""` with caret **placed inside quotes `""`**.
  - `Wait for Visible` $\rightarrow$ Inserts `Wait for element to be visible`.
  - `Assert Visible` $\rightarrow$ Inserts `Assert element is visible`.
  - `Assert Text` $\rightarrow$ Inserts `Assert page contains ""` with caret **inside quotes `""`**.
  - `Select Option` $\rightarrow$ Inserts `Select option ""` with caret **inside quotes `""`**.
  - `Click Element` $\rightarrow$ Inserts `Click button`.
- **Control Tags & Flags**:
  - `(optional)`, `(continue-on-error)`, `(no-replay)`, `(bug: ID)` (caret inside `(bug: |)`), `(hint: #)` (caret after `#`), `(timeout: 10s)`, `(visual)`, `(layout)`.
- **Java Extensions & Variables**:
  - `Call Java Method` $\rightarrow$ Inserts `Execute Java method ""` (caret inside `""`).
  - `Insert Variable ${}` $\rightarrow$ Inserts `${}` (caret inside `${|}`).
- **Available Included Files**:
  - Reuses Aura Manager's file service folder tree logic. Displays available playbook files in the workspace (relative paths from base directory). Clicking a card appends `_include: <relative-path-to-file.yaml>` on a new step line.

### 7. Detailed Inclusion Tree Mechanism
- Includes are rendered as unified pills (`.unified-include-pill`) containing an extension icon, file path, and a ▾ hover arrow.
- **Toggle**: Clicking ▾ expands an embedded tree card showing inner included steps.
- **Editing**: Clicking `Edit File` sets `contenteditable="true"` on inner steps.
- **Unsaved Indicator**: Modifying inner steps displays a yellow warning badge (`.unsaved-badge`) and highlights the card border.
- **Save**: Saves inner steps to the target file on disk and updates all include pill references across the editor.
- **Discard**: Reverts inner steps to original disk state (`data-original`) and clears the unsaved badge.

### 8. Zero New Dependencies & Manager Integration
- Strictly reuses existing Aura Manager stack:
  - Java 21 & HttpServer (`AuraManagerEditorController.java`, `AuraFileService.java`).
  - Thymeleaf template engine (`templates/fragments/editor.html`).
  - HTMX (`hx-get`, `hx-post`, `hx-swap`).
  - External CSS (`dashboard-styles.css`) and JS (`dashboard-editor.js`).
  - Local font handling (`material-symbols-outlined.woff2` and `material-symbols.css`).
- No automated Maven test execution during development (manual verification in browser first).
