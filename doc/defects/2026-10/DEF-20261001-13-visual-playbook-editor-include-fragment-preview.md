# [DEF-20261001-13] Visual Playbook Editor Include Fragment Preview Lacks Step Lines and Add-Step Capability for Non-Existent Includes

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `aura-manager` (`Visual Playbook Editor`, `editor.html`, `dashboard-editor.js`)
- **Scope:** `Test/Harness`
- **Symptom:** When an include file does not exist on disk, the visual editor preview displays a warning ("Included file does not exist on disk. Edit steps below and save to create it."), but clicking "Edit File" provides no step line or UI mechanism to add a step line, preventing users from creating/editing the missing include.
- **Root Cause:** Non-existent includes render 0 `.nested-editable-step` DOM elements (`includeSteps` is empty). `enableIncludeEdit(cardId)` only attempted to focus `steps[0]`. With 0 steps, no line was focused, and no empty placeholder or add step handler was provided.
- **Detection Gap ("What did we miss?"):** Existing UI tests verified editing existing step fragments (`Child.steps`, `MultiChild.steps`), but lacked test coverage for editing missing/non-existent step fragment files.
- **Resolution:** Added `addNestedStep(cardId)` helper, auto-insertion of an initial step row on `enableIncludeEdit` when steps are empty, an empty steps placeholder in Thymeleaf template, and an "+ Add Step" button to include cards.
- **Safety Net Added:** Added Selenide UI test `testEditNonExistentIncludeFileCreatesStepAndSavesFile` in `AuraManagerEditorUiTest.java`.
