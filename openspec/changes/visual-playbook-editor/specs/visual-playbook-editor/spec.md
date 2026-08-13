# Visual Playbook Editor Specification

## Source of Truth Reference
The canonical source of truth for all visual components, styling rules, DOM structure, and interactive behaviors is:
[playbook-editor-mockup.html](file:///home/weigel/tmp/neos/reworkBranch/src/main/resources/com/xceptance/neodymium/aura/playbook-editor-mockup.html)

---

## Requirements

### Requirement: Modular Section Code Panels (`before:`, `steps:`, `after:`)
The editor SHALL provide distinct section panels for `before:`, `steps:`, and `after:` blocks. The `steps:` panel SHALL always be visible. The `before:` and `after:` panels SHALL be optional and toggled via subtle `+ Add 'before:' Block` and `+ Add 'after:' Block` buttons. When created, optional section blocks SHALL start with a clean, empty step row. If an optional section panel (`before:` or `after:`) is empty (contains no text content across step rows) and focus/cursor leaves the panel (`blur`), the editor SHALL automatically collapse/hide the empty panel and restore the subtle `+ Add ...` button.

#### Scenario: Auto-collapsing an empty before block on blur
- **Given** an empty `before:` panel is currently visible
- **When** the focus/cursor leaves the `before:` panel (`blur` event)
- **Then** the `before:` panel automatically collapses (`display: none`) and the `#addBeforeBtnContainer` button is restored.

---

### Requirement: Continuous Global Line Indexing
The editor SHALL maintain continuous global line numbers ($1 \dots N$) across all active section panels in sequence (`before` $\rightarrow$ `steps` $\rightarrow$ `after`) and through any expanded `_include:` step trees without resetting.

#### Scenario: Line numbering across active blocks
- **Given** a `before:` block with 2 steps, a `steps:` block with 4 steps, and an `after:` block with 2 steps
- **When** line numbers are calculated
- **Then** the `before:` steps are numbered 1 to 2, the `steps:` steps are numbered 3 to 6, and the `after:` steps are numbered 7 to 8.

---

### Requirement: Focus / Blur Line Editing & Color Scheme Alignment
When a step row is focused, the editor SHALL display raw text from `data-raw`. When blurred, the editor SHALL parse syntax tokens (`.var-pill`, `.unified-include-pill`, `.hint-badge`) while preserving `data-raw` intact. All token pills and badges SHALL strictly conform to Aura Manager's existing UI color scheme:
- AI & Variable Items: Purple (`var(--accent-purple)` / `#8b5cf6`).
- Core Accents & Navigation: Blue (`var(--accent)` / `#3b82f6`).
- Success & Saved States: Green (`var(--success)` / `#10b981`).
- Warnings & Unsaved Badges: Amber (`var(--warning)` / `#f59e0b`).
- Danger & Delete Actions: Red (`var(--danger)` / `#ef4444`).

---

### Requirement: Keyboard Navigation
- Pressing `ArrowLeft` or `ArrowRight` SHALL perform standard character-by-character text navigation within the active step line.
- Pressing `ArrowUp` or `ArrowDown` SHALL focus the preceding or succeeding step row while preserving the cursor's column index (clamped to the length of the destination line if shorter).
- Pressing `Enter` SHALL insert a new step row below the current line.

---

### Requirement: Quick Insert Palette & File Inclusion Path Insertion
The editor SHALL provide a right sidebar Quick Insert Palette containing 4 categorized groups:
1. **Actions & Templates**: `Open Base URL`, `Type Text`, `Wait for Visible`, `Assert Visible`, `Assert Text`, `Select Option`, `Click Element`.
2. **Control Tags & Flags**: `(optional)`, `(continue-on-error)`, `(no-replay)`, `(bug: ID)`, `(hint: #)`, `(timeout: 10s)`, `(visual)`, `(layout)`.
3. **Java Extensions & Variables**: `Call Java Method`, `Insert Variable ${}`.
4. **Available Included Files**: List of available `.yaml` playbook files dynamically populated from Aura Manager's file service folder tree model (relative file paths from base test directory).

Clicking an included file card SHALL insert `_include: <relative-file-path.yaml>` on a new step line. Palette action shortcuts SHALL insert snippets into the active line at `lastCaretOffset` and position the cursor inside quotes `""`, tags `(bug: |)`, or hints `(hint: #)`.

---

### Requirement: Detailed Unified Recursive Inclusion Tree
When a step contains `_include: <relative-path>`, the line SHALL render a formatted include pill (`.unified-include-pill`) containing an extension icon, file path, and a ▾ hover toggle arrow.

#### Scenario: Toggling, editing, saving, and discarding included files
- **Toggle**: Clicking ▾ expands an embedded tree card (`.include-tree-card`) displaying inner included steps.
- **Editing**: Clicking `Edit File` sets `contenteditable="true"` on inner steps, hides `Edit File`, and displays `Save` and `Discard`.
- **Unsaved State**: Modifying any inner step text displays a yellow warning badge (`.unsaved-badge`) and highlights the card border.
- **Save Action**: Clicking `Save` persists the modified inner steps to the target file on disk and updates all include pill references across the editor.
- **Discard Action**: Clicking `Discard` reverts all inner step text to `data-original` attribute values and clears the unsaved warning badge.

---

### Requirement: Transposed Test Data Matrix
The editor SHALL render the `data:` block as a transposed matrix table where variable keys are rows and test iterations are columns. Iteration headers SHALL derive dynamically from `testId` values. Clicking `[+ Insert]` on any variable row SHALL insert `${varName}` at the exact cursor position in the active step line.
