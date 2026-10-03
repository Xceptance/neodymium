# [DEF-20261001-14] Warning Banner Remains Visible in Include Card After Saving Non-Existent Include File

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `aura-manager` (`Visual Playbook Editor`, `dashboard-editor.js`)
- **Scope:** `Test/Harness`
- **Symptom:** The non-existent include file warning banner ("Included file does not exist on disk. Edit steps below and save to create it.") stays visible inside the include card even after saving the created steps to disk.
- **Root Cause:** `saveIncludeInline(cardId)` posted step content to `/api/save` and updated unsaved badges, but did not remove the `.include-warning-banner` DOM node rendered when the card was initially loaded.
- **Detection Gap ("What did we miss?"):** The initial UI test verified toast notification and unsaved badge removal, but did not assert that `.include-warning-banner` was removed after save.
- **Resolution:** Updated `saveIncludeInline(cardId)` in `dashboard-editor.js` to locate and remove `.include-warning-banner` from `treeCard` when the save request succeeds.
- **Safety Net Added:** Added assertion `includeCard.$(".include-warning-banner").shouldNotBe(Condition.visible)` in `AuraManagerEditorUiTest.java`.
