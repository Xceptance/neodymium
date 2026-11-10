# [DEF-20261012-1615] Raw YAML Content in Test Editor Updated Only After Save and Page Refresh

- **Status:** `Resolved`
- **Opened:** 2026-10-12 16:11
- **Closed:** 2026-10-12 16:15
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Visual edits made in the test editor (typing steps, adding/deleting steps, reordering, data matrix changes, and undo/redo) are not displayed in the Raw YAML code panel (`#rawYamlTextarea`) or upon switching to "Raw YAML" mode until the user saves the test file and reloads the browser page.
- **Root Cause:** `compilePlaybookToYaml()` only assigned compiled YAML to the hidden form input `#editorContent`, omitting `#rawYamlTextarea`. Furthermore, `setEditorMode('raw')` guarded the textarea assignment behind `if (rawTextarea && (!rawTextarea.value || rawTextarea.value.trim() === ''))`, which was always false due to the initial server-side Thymeleaf rendering of `th:text="${fileContent}"`.
- **Detection Gap ("What did we miss?"):** Existing UI tests tested direct paste operations into the raw editor in isolation, but did not assert bidirectional synchronization between visual step mutations and raw textarea content prior to save.
- **Resolution:** Updated `compilePlaybookToYaml()` to update `rawYamlTextarea.value` directly when in visual mode, and removed the empty check guard in `setEditorMode('raw')` so switching to raw YAML mode always reflects current visual changes.
- **Safety Net Added:** Added Selenide UI test `AuraManagerEditorUiTest.testVisualEditorChangesDirectlyUpdateRawYamlContent`.
