# [DEF-20261117-1245] Broken YAML Editor Undo/Redo Buttons Across Visual, Raw, Typing, and Side Panel

- **Status:** `Resolved`
- **Opened:** 2026-11-17 12:45
- **Closed:** 2026-11-17 12:58
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Clicking `#undoBtn` or `#redoBtn` in the YAML editor failed to revert or restore changes:
  1. In visual mode, clicking `#undoBtn` did not revert changes made via typing or the right sidebar palette.
  2. In raw mode, `#undoBtn` and `#redoBtn` were permanently disabled (`disabled = true`), and keyboard shortcuts were bypassed.
  3. Side panel insertions did not insert into the raw editor and could not be undone via `#undoBtn`.
  4. Steps containing variable pills or control tags did not capture typing mutations into undo snapshots.
  5. Undoing a state with playbook properties triggered an asynchronous snapshot that erased the redo stack.
- **Root Cause:**
  1. `editorUndo()` detected uncommitted changes by checking `JSON.stringify(lastSnap) !== JSON.stringify(currentSnap)`. Because snapshots included `activeLine` and `activeCaret` based on `document.activeElement`, clicking the `#undoBtn` button shifted focus away from `.step-content` (`activeLine: null`), triggering a false uncommitted change branch that pushed current state to `redoStack` and restored `lastSnap` (the same current text) without popping `undoStack`.
  2. Raw mode unconditionally disabled `#undoBtn` / `#redoBtn` in `setEditorMode('raw')`, and `getEditorSnapshot()` / `restoreEditorSnapshot()` lacked support for raw YAML text and textarea caret positions.
  3. Right sidebar palette insertion handlers (`insertSnippetWithCaret`, `insertIncludePill`) only manipulated visual `.step-row` DOM elements and failed in raw mode.
  4. `getStepRawText()` ignored `data-raw` when `content.innerText` was non-empty and not actively focused, causing formatStepToTokens to overwrite `data-raw` and drop side-panel insertions.
  5. `initVisualPlaybookEditor` input handler checked `!hasPills` before syncing `data-raw`, ignoring input in steps with pill tokens.
  6. `restoreEditorSnapshot` invoked `addPropertyRow()`, which triggered `debouncedPushSnapshot()`, asynchronously clearing `redoStack`.
- **Detection Gap ("What did we miss?"):** The only pre-existing test in `AuraManagerEditorUiTest.java` verified `Ctrl+Z` via synthetic `KeyboardEvent` on a step element with no UI button interaction, no raw mode testing, and no side panel testing.
- **Resolution:**
  1. Extracted content-state comparison into `isSnapshotContentEqual(snapA, snapB)`, decoupling snapshot content equality from transient DOM focus/caret metadata (`activeLine`, `activeCaret`, selection ranges).
  2. Added mode awareness (`mode: 'raw' | 'visual'`) to snapshot models and restoration handlers, ensuring full snapshot capture and restoration for raw YAML textarea value and cursor position.
  3. Replaced raw mode button disabling with dynamic `updateUndoRedoUI()` invocations and removed raw-mode bypass in global keyboard shortcuts (`Ctrl+Z`, `Ctrl+Y`, `Cmd+Shift+Z`).
  4. Updated `insertSnippetWithCaret` and `insertIncludePill` to detect raw editor mode and insert directly into `rawYamlTextarea` at cursor position, followed by model compile and undo snapshot creation.
  5. Guarded `debouncedPushSnapshot()` and `pushEditorSnapshot()` during restoration via `isRestoringSnapshot` to prevent properties restoration from destroying the redo stack.
  6. Fixed `getStepRawText()` to prioritize `data-raw` when not actively focused, and set `content.innerText` in `insertSnippetWithCaret`.
  7. Nested `rawEditorPanel` alongside `visualEditorMain` inside `#visualEditorContainer` so that the right sidebar Quick Insert Palette is persistently available across both Visual and Raw YAML editing modes.
- **Safety Net Added:** Added permanent automated integration test `AuraManagerEditorUiTest#testYamlEditorUndoRedoFullWorkflow` covering visual typing undo/redo, side-panel palette insertion undo/redo, raw YAML mode typing undo/redo, and raw mode side-panel insertion undo/redo.
