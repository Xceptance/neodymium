# [DEF-20261012-1358] Comment Text Typography and Background Inside Step Line Not Immediately Styled Upon Typing '#'

- **Status:** `Resolved`
- **Opened:** 2026-10-12 13:58
- **Closed:** 2026-10-12 14:02
- **Component:** `aura-manager` / `neodymium-core`
- **Scope:** `UI / Visual Editor`
- **Symptom:** Typing '#' at the beginning of a step line toggled the gutter badge to '#' and cleared row borders, but the text inside the step line retained regular step styling (normal font-style, primary text color, and a solid card focus background) while typing.
- **Root Cause:**
  1. `.step-content:focus` applied `background: var(--bg-card)` during active editing, creating a solid card background box that conflicted with transparent comment styling.
  2. `.step-row.step-row-comment .step-content` lacked explicit focus rules (`:focus`) and child element inheritance (`*`) with `!important`.
  3. `checkAndUpdateCommentRow` toggled classes on the row without directly ensuring the `content` element's style properties (`color`, `fontStyle`) were synchronously updated on the `input` event.
- **Detection Gap ("What did we miss?"):** Previous UI tests asserted `.step-row-comment` class presence on `.step-row` and `#` badge in the gutter, but omitted assertions on computed `font-style: italic` and `background-color` on `.step-content`.
- **Resolution:**
  1. Updated `checkAndUpdateCommentRow` in `dashboard-editor.js` (both `aura-manager` and `neodymium-core`) to synchronously apply `content.style.color = 'var(--text-secondary, #64748b)'` and `content.style.fontStyle = 'italic'` when `isComment` is detected, and revert to empty strings when `#` is removed.
  2. Updated `dashboard-styles.css` (both `aura-manager` and `neodymium-core`) so `.step-row.step-row-comment .step-content, .step-row.step-row-comment .step-content:focus` have `background: transparent !important`, `box-shadow: none !important`, `outline: none !important`, `caret-color: var(--text-secondary, #64748b)`, and `.step-row.step-row-comment .step-content *` inherits comment styling.
- **Safety Net Added:**
  - Extended `com.xceptance.aura.test.ui.AuraManagerEditorUiTest#testTypeHashAtBeginningOfLineStylesRowAsCommentImmediately` to verify that directly upon typing `#`, `content1.getCssValue("font-style")` is `"italic"`, `background-color` is transparent, and upon removing `#`, `font-style` reverts to `"normal"`.
