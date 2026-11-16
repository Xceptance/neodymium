# DEF-20261116-0720: False Dirty State Prompts on Closing Unmodified YAML Test Files

- **Status**: Implemented
- **Date**: 2026-11-16 07:20
- **Scope**: Aura Manager (YAML Editor Frontend)
- **Component**: `dashboard-editor.js`, `editor.html`, `AuraManagerEditorUiTest.java`

## Symptom
Closing an open YAML test file in the Aura Manager without making any edits displays the `#unsavedChangesModal` dialog warning that unsaved changes will be lost.

## Root Cause
1. **Baseline Serialization & Mode Switching Drift**: In `dashboard-editor.js`, `initialEditorContent` was previously initialized to the compiled visual representation, whereas the raw text loaded from the backend was placed in `rawYamlTextarea.value`. When switching modes or comparing raw textarea contents, line endings (`\r\n` vs `\n`) or synthetic headers produced false mismatches (`rawTextarea.value !== initialEditorContent`), falsely marking clean files as dirty. Furthermore, when opening a subsequent file, `initVisualPlaybookEditor` did not re-initialize the baseline content if `initialEditorContent` was already non-empty.
2. **Close Button Interceptor Collision**: In `fragments/editor.html`, the Close button was defined with `hx-post="/api/editor/close"`. The global navigation click capture listener in `dashboard-editor.js` intercepted all elements matching `[hx-post]`, executing dirty state checks concurrently with `closeEditor()`.

## Fix
1. **Normalized Dirty Comparison**: Introduced `normalizeYamlForDirtyComparison()` in `dashboard-editor.js` to normalize line endings and trim whitespace before evaluating dirty diffs.
2. **File Baseline Synchronization**: Ensured `initVisualPlaybookEditor()` establishes a fresh initial snapshot whenever a new file is loaded (`window.lastInitializedEditorFile !== activeEditingFile`).
3. **Synchronize Raw Mode on Switch**: Updated `setEditorMode('raw')` to unconditionally compile and synchronize current visual content to the raw textarea before displaying the raw panel.
4. **Close Button Cleanup & Interceptor Bypass**: Changed `#closeEditorBtn` in `fragments/editor.html` to `type="button"` and removed redundant `hx-post="/api/editor/close"` (since `closeEditor()` already issues the POST request). In `dashboard-editor.js`, explicitly bypassed `#closeEditorBtn` in the global navigation interceptor.

## Detection Gap
Existing UI tests bypassed the modal by invoking `closeEditor(true)` directly via JavaScript rather than simulating a user click on the UI Close button.

## Safety Net
Added automated UI test `AuraManagerEditorUiTest#testCleanFileCloseDoesNotShowUnsavedPrompt` verifying that clicking `#closeEditorBtn` on an unmodified file closes the editor cleanly without triggering `#unsavedChangesModal`.
