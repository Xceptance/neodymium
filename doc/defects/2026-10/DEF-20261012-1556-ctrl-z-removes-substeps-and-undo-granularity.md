# [DEF-20261012-1556] Ctrl+Z in Test Editor Strips Sub-steps and Reverts Prior Actions

- **Status:** `Resolved`
- **Opened:** 2026-10-12 15:56
- **Closed:** 2026-10-12 16:03
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Pressing Ctrl+Z in the visual test editor stripped sub-steps (converting them to regular top-level steps, deleting toggle bars, and re-flattening line numbers). Furthermore, pressing Ctrl+Z within the debounce interval reverted multiple actions at once or missed recent edits.
- **Root Cause:** 
  1. `getEditorSnapshot()` serialized steps as an array of plain strings without saving `isSubstep`, `isManualSubstep`, or `isComment` metadata. Rebuilding the DOM in `restoreEditorSnapshot()` reconstructed all rows as generic `.step-row` elements.
  2. `editorUndo()` popped from `undoStack` without checking or comparing uncommitted DOM changes in the 350ms `debouncedPushSnapshot()` window, causing it to skip the active state and pop earlier history states.
  3. `handleKeyDown` used debounced snapshots for structural operations (`Enter`, `Backspace` at col 0) instead of immediate snapshots, causing preceding typing and structural line additions/removals to coalesce into a single pending debounce cycle.
- **Detection Gap ("What did we miss?"):** Existing UI tests in `AuraManagerEditorUiTest` covered keyboard navigation, step reordering, and sub-step typing/persistence across reloads, but lacked tests validating `Ctrl+Z` / `editorUndo()` with sub-steps and granular single-action reversion.
- **Resolution:** 
  - Enhanced `getEditorSnapshot()` in `aura-manager` and `neodymium-core` (`dashboard-editor.js`) to capture step objects `{ text, isSubstep, isManualSubstep, isComment }`, along with active focused line and caret.
  - Enhanced `restoreEditorSnapshot()` in both files to rebuild `.step-row-substep`, `data-manual-substep`, and `.step-row-comment` attributes, and restore focus to the edited line.
  - Updated `editorUndo()` and `editorRedo()` to clear pending debounce timers, compare live DOM snapshot against `undoStack[top]`, and push uncommitted deltas to `redoStack` before restoring.
  - Updated `handleKeyDown` to flush pending typing debounces before executing structural changes (`Enter`, `Backspace` at offset 0) and commit immediately via `pushEditorSnapshot(false)`.
- **Safety Net Added:** `AuraManagerEditorUiTest.testCtrlZUndoesLastActionAndPreservesSubsteps`
