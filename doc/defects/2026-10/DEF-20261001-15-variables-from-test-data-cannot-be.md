# [DEF-20261001-15] Variables from Test Data Cannot Be Inserted into Fragment Steps Being Edited Inline

- **Status:** Resolved
- **Opened:** 2026-10-01
- **Closed:** 2026-10-01
- **Component:** `aura-manager` (`Visual Playbook Editor`, `dashboard-editor.js`, `editor.html`)
- **Scope:** `Framework`
- **Symptom:** Variables from test file test data could not be inserted into steps of fragment include cards being edited inside a test file, and inserted variables were not dynamically updated in the include card's variable dropdown with the flag "required from using file".
- **Root Cause:** `insertVariableFromInput` in `dashboard-editor.js` lacked active nested step detection (`nestedStep`), and the editor did not trigger dynamic variable dropdown list refresh (`updateIncludeVarsDropdown`) when editing fragment step lines.
- **Detection Gap ("What did we miss?"):** Existing UI tests focused on main playbook step insertion and pre-existing include tree variables, missing dynamic variable insertion into nested inline fragment steps.
- **Resolution:** Updated `insertVariableFromInput` to check for active nested fragment steps and insert `${varName}` cleanly; introduced `updateIncludeVarsDropdown` to dynamically refresh the "Variables in Include" dropdown with flag "required from using file" (`scope-required active-required`).
- **Safety Net Added:** Added UI test `testInsertVariableFromTestDataIntoInlineIncludeFragment()` in `AuraManagerEditorUiTest.java`.
