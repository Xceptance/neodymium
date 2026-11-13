# [DEF-20261021-1458] Delay and Empty File Flash on Opening Test File in Editor

- **Status:** `Resolved`
- **Opened:** 2026-10-21 14:50
- **Closed:** 2026-10-21 14:58
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** When clicking the "Edit" button next to any test file in the sidebar list, there was a noticeable delay before the file opened, and during this delay a blank/empty editor panel with zero steps was displayed to the user.
- **Root Cause:** 
  1. Frontend: In `dashboard-editor.js`, `openYamlEditor(filename)` immediately emptied `stepsList.innerHTML = ''` to prevent stale DOM compilation, updated `activeEditingFile = filename`, and invoked `updateCenterLayout()` before dispatching the asynchronous `htmx.ajax('GET', '/api/editor?file=...')` request. `updateCenterLayout()` immediately triggered `switchState('editor')`, switching `#state-editor` into active view before the network response arrived, presenting an empty steps container with no loading state or indicator.
  2. Backend: In `AuraTestEditorController.java`, `getEditorFragment(...)` invoked `fileService.getYamlFilesList()` and added `yamlFiles` to the Spring MVC model. `getYamlFilesList()` recursively traversed the entire filesystem, read every `.yaml` test file on disk, parsed them with SnakeYaml to extract datasets, and checked for companion playbooks — even though `yamlFiles` was never referenced or rendered in `fragments/editor.html`. Furthermore, `getFragmentRequiredVariablesMap()` parsed every `.steps` file on every request without caching.
- **Detection Gap ("What did we miss?"):** UI tests (`AuraManagerEditorUiTest.java`) used Selenide's fluent `.shouldBe(Condition.visible)` and `.shouldHave(Condition.text("..."))` which wait for the final DOM state, masking the intermediate empty view state and the unnecessary backend disk I/O.
- **Resolution:**
  1. In `AuraTestEditorController.java`, removed redundant `model.addAttribute("yamlFiles", fileService.getYamlFilesList())` calls, eliminating unnecessary full-workspace disk scans and SnakeYaml parsing on every editor opening request.
  2. In `AuraFileService.java`, added `fragmentVarsCache` backed by file `lastModified` timestamps to prevent repetitive disk I/O and re-parsing of unchanged step fragments.
  3. In `dashboard-editor.js`, updated `openYamlEditor(filename)` to immediately set `#editorFileName` and render a centered loading spinner placeholder (`.editor-loading-placeholder` with Material icon `progress_activity`) inside `stepsList` and `# Loading ${filename}...` in `rawYamlTextarea`, eliminating the blank/empty file flash while the server response is in flight.
- **Safety Net Added:** 
  - `AuraTestEditorControllerTest.testGetEditorFragmentWithFile()`: explicitly verifies via `Mockito.verify(fileService, Mockito.never()).getYamlFilesList();` that `getYamlFilesList()` is never called when retrieving an editor fragment.
  - `AuraFileServiceTest.testCreateYamlFileWithStepsExtension()`: verifies fragment required variable resolution and cache invalidation.
