# OpenSpec Design Document: Visual Playbook Editor Integration

## Source of Truth
All component layouts, DOM hierarchies, CSS classes, typography tokens, interaction behaviors, and color schemes are strictly derived from the standalone mockup:
[playbook-editor-mockup.html](file:///home/weigel/tmp/neos/reworkBranch/src/main/resources/com/xceptance/neodymium/aura/playbook-editor-mockup.html)

---

## Architectural Layout & Component Placement

The editor consists of a central visual editor area (`.editor-main-area`) and a right sidebar palette (`.sidebar-palette-right`).

```
+-----------------------------------------------------------------------------------+
| Aura Manager Editor Panel (templates/fragments/editor.html)                       |
|                                                                                   |
|  +-------------------------------------+  +------------------------------------+  |
|  | Central Main Area (#visualEditorMain|  | Right Sidebar (.sidebar-palette..)|  |
|  |                                     |  |                                    |  |
|  | [#addBeforeBtnContainer]            |  | [ Actions & Templates ]            |  |
|  | - Subtle + Add 'before:' Block btn  |  | - Open Base URL, Type Text, etc.   |  |
|  |                                     |  |                                    |  |
|  | [#beforeCodePanel]                  |  | [ Control Tags & Flags ]           |  |
|  | - Before Steps (#beforeStepsList)   |  | - (optional), (continue-on-error)  |  |
|  |                                     |  | - (no-replay), (bug: ID), etc.     |  |
|  | [#stepsCodePanel]                   |  |                                    |  |
|  | - Header: Playbook Steps (steps:)   |  | [ Java & Variables ]               |  |
|  | - Step rows (#stepsList)            |  | - Call Java, Insert Variable ${}   |  |
|  |   |-> Step number (.step-number)    |  |                                    |  |
|  |   |-> Content (.step-content)       |  | [ Available Included Files ]       |  |
|  |   |-> Embedded Include Tree Cards   |  | - List of files from fileService   |  |
|  |                                     |  |   (relative paths from base dir)   |  |
|  | [#afterCodePanel]                   |  |                                    |  |
|  | - After Steps (#afterStepsList)     |  +------------------------------------+  |
|  |                                     |                                          |
|  | [#addAfterBtnContainer]             |                                          |
|  | - Subtle + Add 'after:' Block btn   |                                          |
|  |                                     |                                          |
|  | [ Transposed Test Data Matrix ]     |                                          |
|  | - Key rows x Iteration columns      |                                          |
|  +-------------------------------------+                                          |
+-----------------------------------------------------------------------------------+
```

---

## Detailed Component Specifications

### 1. Central Main Editor Area (`#visualEditorMain`)
- **Container**: `<main class="editor-main-area" id="visualEditorMain">`
  - Style: `flex: 1 1 0%; min-height: 0; overflow-y: auto; display: flex; flex-direction: column; gap: 16px; padding: 16px;`
- **Subtle Block Add Buttons**:
  - Rendered in `#addBeforeBtnContainer` (above steps) and `#addAfterBtnContainer` (below steps).
  - Class: `.btn-add-section-block`
  - Appearance: Dashed border (`border: 1px dashed var(--border-color)`), background transparent, hover accent highlight.
- **Section Code Panels (`.editor-code-panel`)**:
  - Panels: `#beforeCodePanel` (hidden by default), `#stepsCodePanel` (always visible), `#afterCodePanel` (hidden by default).
  - Header: `.panel-header` containing title icon, title text, subtitle description.
  - **Auto-Collapse Behavior**: No delete button on headers. If focus leaves an optional panel (`blur` event on `#beforeStepsList` or `#afterStepsList`) and the container has 0 step rows (or all content is blank), the panel automatically hides (`display: none`) and restores the `#addBeforeBtnContainer` / `#addAfterBtnContainer` button.
- **Step Rows (`.step-row`)**:
  - Layout: Flex row (`display: flex; align-items: center; gap: 10px; padding: 4px 8px; border-radius: 6px;`).
  - Active Line: `.step-row.active-line` applies subtle accent border/background highlight.
  - Number: `.step-number` displays continuous line number ($1 \dots N$).
  - Content: `.step-content` (`contenteditable="true"`, `spellcheck="false"`, `data-raw="..."`).

### 2. Token Pills & Color Scheme
- All colors strictly conform to existing `dashboard-styles.css` variables:
  - **Variable Pills (`.var-pill`)**: `color: var(--accent-purple); background: rgba(139, 92, 246, 0.12); border: 1px solid rgba(139, 92, 246, 0.3); font-weight: 600; padding: 1px 6px; border-radius: 4px;`
  - **Include Pills (`.unified-include-pill`)**: Extension icon (`extension`), relative filename, and hover toggle arrow (`expand_more`). `background: rgba(59, 130, 246, 0.12); border: 1px solid rgba(59, 130, 246, 0.3); color: var(--accent);`
  - **Hint Badges (`.hint-badge`)**: Selector badges `#id` or `.class`. `color: var(--warning); background: rgba(245, 158, 11, 0.12);`
  - **Unsaved Badges (`.unsaved-badge`)**: Amber warning badge (`color: var(--warning);`).

### 3. Detailed Inclusion Tree Mechanism (`.include-tree-card`)
- **Structure**: Embedded card inserted directly below the host step row.
- **Header**: Contains title (`Included File: <filename>`), unsaved warning badge, and action buttons (`Edit File`, `Save`, `Discard`).
- **Inner Steps (`[id^="includeInnerSteps_"]`)**:
  - Renders inner steps (`.nested-editable-step`).
  - **Edit Mode**: Clicking `Edit File` sets `contenteditable="true"` on inner steps, hides `Edit File`, and shows `Save` and `Discard`.
  - **Unsaved State**: Editing inner steps triggers `markIncludeUnsaved()`, displaying `.unsaved-badge` and highlighting the card border.
  - **Save Action**: Saves inner step modifications to disk via HTMX `hx-post="/api/editor/save-include"` and updates all instances of that include pill across the playbook.
  - **Discard Action**: Reverts inner step text to `data-original` attribute and clears the unsaved badge.
- **Recursive Nesting**: Supports arbitrary inclusion nesting depth using `.nested-level-1`, `.nested-level-2`, etc.

### 4. Right Sidebar Palette (`.sidebar-palette-right`)
- **Container**: `<aside class="sidebar-palette-right">`
  - Style: `width: 280px; flex-shrink: 0; display: flex; flex-direction: column; gap: 16px; overflow-y: auto;`
- **4 Categorized Groups**:
  1. **Actions & Templates**: `Open Base URL`, `Type Text`, `Wait for Visible`, `Assert Visible`, `Assert Text`, `Select Option`, `Click Element`.
  2. **Control Tags & Flags**: `(optional)`, `(continue-on-error)`, `(no-replay)`, `(bug: ID)`, `(hint: #)`, `(timeout: 10s)`, `(visual)`, `(layout)`.
  3. **Java Extensions & Variables**: `Call Java Method`, `Insert Variable ${}`.
  4. **Available Included Files**: Dynamically populated from Aura Manager's `AuraFileService` folder tree logic. Displays relative file paths from the base test resource directory. Clicking a card inserts `_include: <relative-path>` on a new step line.

### 5. Integration Architecture (Zero New Dependencies)
- **Thymeleaf Template**: `templates/fragments/editor.html` renders the visual editor structure inside Aura Manager's layout (`dashboard.html`).
- **External CSS**: Styles appended to `dashboard-styles.css`.
- **External JS**: Interactivity modularized into `dashboard-editor.js`.
- **Font & Assets**: Reuses local Material Symbols font files (`material-symbols-outlined.woff2` and `material-symbols.css`).
- **Java Controller (`AuraManagerEditorController.java`)**:
  - `/api/editor/panel`: Serves `editor.html` fragment.
  - `/api/editor/include-tree`: HTMX endpoint serving included step fragments.
  - `/api/save`: Persists playbook YAML content to disk.
