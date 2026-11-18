# [DEF-20261117-1338] YAML Editor Undo/Redo DOM Corruption and Cross-Mode Reset

- **Status:** `Resolved`
- **Opened:** 2026-11-17 13:38
- **Closed:** 2026-11-17 13:44
- **Component:** `aura-manager`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** In the visual editor, undoing a character edit empties the edited line, empties the line below, or clears the first two lines of main steps if before: steps exist. In raw mode, pressing Undo flips the editor back into visual mode. Undoing the first action is noticeably more corrupting than subsequent actions.
- **Root Cause:**
  1. `restoreEditorSnapshot` completely wiped container DOM (`innerHTML = ''`), firing unhandled `blur` events on detached rows. Because `handleStepBlur` lacked an `isRestoringSnapshot` guard, it targeted newly inserted blank rows (`<div></div>`), read empty `innerText`, and overwrote `data-raw` with empty string.
  2. `rebuildBlock` used `idx + 1` independently for `#beforeStepsList`, `#stepsList`, and `#afterStepsList`, causing duplicate `data-line` values during reconstruction. `formatStepToTokens(lineNum)` used `document.querySelector` and formatted `#beforeStepsList` instead of `#stepsList`.
  3. `rebuildBlock` generated inline event listeners with hardcoded numbers (`handleStepBlur(${lineNum})`) rather than passing `this`.
  4. `setEditorMode('raw')` did not push a snapshot on mode entry, leaving `snap0` (visual mode) as the only prior state; undoing raw edits restored `snap0.mode = 'visual'`.
- **Detection Gap ("What did we miss?"):** Previous test `testYamlEditorUndoRedoFullWorkflow` created a brand new, empty file where line 1 started blank (`""`), so asserting that undo produced an empty string passed by coincidence and never verified modifying existing playbook steps or multi-section files with `before:` blocks.
- **Resolution:**
  1. Guard `handleStepBlur`, `handleStepFocus`, and `checkAndUpdateCommentRow` with `if (isRestoringSnapshot) return;`.
  2. Perform in-place DOM updates when step count matches rather than wiping out DOM containers, preventing blur leaks and focus loss.
  3. In `rebuildBlock`, seed `<div class="step-content">${escapeHtml(stepText)}</div>` with initial content, pass `row` directly to `formatStepToTokens(row)`, use `this` in listeners, and execute global `reindexSteps()` and `refreshAllSubstepToggleBars()`.
  4. In `setEditorMode`, push a mode snapshot so undoing in raw mode stays within raw mode.
- **Safety Net Added:** Added regression test `testYamlEditorUndoRedoPreservesUnrelatedSectionsAndSteps` in `AuraManagerEditorUiTest` asserting full file text integrity before, during, and after undo/redo across visual mode, multi-section (`before:` + `steps:`), and raw mode.
