# [DEF-20261021-1438] Misplaced "Save as Batch" button in test editor toolbar

- **Status:** `Resolved`
- **Opened:** 2026-10-21 14:38
- **Closed:** 2026-10-21 14:41
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** The test playbook editor toolbar displayed a "Save as Batch" button (`#saveQueueAsBatchEditorBtn`), which is out of place for editing individual test playbooks and duplicates queue-level batch management controls.
- **Root Cause:** During batch management UI implementation, `<button id="saveQueueAsBatchEditorBtn">` was erroneously placed inside the `.editor-actions` toolbar in `editor.html`, duplicating queue-level batch actions instead of remaining solely in the queue and test selection panels.
- **Detection Gap ("What did we miss?"):** Existing UI tests in `AuraManagerEditorUiTest` exercised valid editor actions (Save, Undo, Redo, Delete, Rename) but lacked a negative DOM assertion verifying that queue-specific actions like `#saveQueueAsBatchEditorBtn` are not exposed in the editor toolbar.
- **Resolution:** Removed `<button id="saveQueueAsBatchEditorBtn">` from `aura-manager/src/main/resources/templates/fragments/editor.html`.
- **Safety Net Added:** Added `@Test` `AuraManagerEditorUiTest.testEditorToolbarDoesNotContainSaveAsBatchButton` verifying `#saveQueueAsBatchEditorBtn` and button text "Save as Batch" do not exist in `#editorPanel`.
