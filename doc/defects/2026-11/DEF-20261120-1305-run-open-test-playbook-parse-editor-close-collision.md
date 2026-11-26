# [DEF-20261120-1305] Run Open Test Fails With Playbook Parsing Error Due To Editor Close Collision

- **Status:** `Resolved`
- **Opened:** 2026-11-20 12:50
- **Closed:** 2026-11-20 13:05
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Triggering a test from the Test Editor via the "Run Open Test" button (`#runCurrentTestBtn`) failed immediately with:
  `java.lang.RuntimeException: Failed to parse playbook: com/xceptance/neodymium/aura/sandbox/Aura__Test_executeYamlTest.yaml`
  `Caused by: java.io.FileNotFoundException: Classpath resource not found: com/xceptance/neodymium/aura/sandbox/Aura__Test_executeYamlTest.yaml`
  Meanwhile, the same test started via the "Run Queue" button (`#runQueueBtn`) executed normally.
- **Root Cause:**
  1. In `dashboard-editor.js`, the global click interceptor for detecting navigation away from an open file intercepted clicks on any element with `[hx-get]` or `[hx-post]` outside `#editorPanel`. Because `#runCurrentTestBtn` is located in the right sidebar (`#configPanel`) and has `hx-post="/api/run?runCurrent=true"`, the listener treated clicking the run button as navigating away. When the file was clean, it immediately triggered `closeEditor(true)`.
  2. `closeEditor()` dispatched `POST /api/editor/close`, setting `fileService.setActiveEditingFile("")` on the backend.
  3. In `AuraTestQueueController.java`, `/api/run?runCurrent=true` only checked `if (activeFile != null)` rather than checking for non-blank (`!activeFile.isBlank()`). Because `activeFile` was `""`, it constructed a `DatasetSelection` with `sel.file = ""`. Additionally, `#runCurrentTestBtn` never submitted the filename parameter to `/api/run`.
  4. In `AuraQueueService.java`, an empty file string produced `safeName = ""` and `className = "Aura__Test"`, which lacked `@AiPlaybook` and caused `NeodymiumAiRunner` to fall back to the default convention `Aura__Test_executeYamlTest.yaml`, triggering a `FileNotFoundException`.
  5. In contrast, "Run Queue" was immune because `selectedQueue` preserved explicit test paths selected from the test tree.
- **Detection Gap ("What Did We Miss?"):**
  1. Existing controller unit tests (`AuraTestQueueControllerTest`) mocked `fileService.getActiveEditingFile()` with non-empty filenames and never asserted behavior when `activeEditingFile` was `""` or null.
  2. Frontend browser tests did not simulate the exact DOM click chain on `#runCurrentTestBtn` in combination with the global navigation interceptor.
- **Resolution:**
  1. In `dashboard-editor.js`, explicitly excluded `#runControls, #configPanel, .run-controls-container, #runCurrentTestBtn, #runQueueBtn, #stopQueueBtn` from the global navigation interceptor.
  2. In `dashboard-runner.js`, updated `triggerRunCurrentTest()` to pass `values: { file: fileToRun }` on `POST /api/run?runCurrent=true`.
  3. In `queue.html`, bound current active file dynamically via `th:attr="hx-vals=..."` and updated the conditional to check `(activeEditingFile != null and !activeEditingFile.isBlank())`.
  4. In `AuraTestQueueController.java`, prioritized explicit `file` request parameters over session state, verified `activeFile != null && !activeFile.isBlank()`, and rejected blank requests safely without launching empty executions.
  5. In `AuraQueueService.java`, added a defensive check `if (selection.file == null || selection.file.isBlank()) continue;`.
- **Safety Net Added:**
  - `AuraTestQueueControllerTest.testStartExecutionRunCurrentWithActiveFileInFileService`: verifies standard active file execution.
  - `AuraTestQueueControllerTest.testStartExecutionRunCurrentWithExplicitFileParameter`: verifies request parameter takes precedence.
  - `AuraTestQueueControllerTest.testStartExecutionRunCurrentWithBlankOrNullFileFailsSafely`: verifies blank/empty active file does not launch a queue execution.
  - `AuraTestQueueControllerTest.testQueueTemplateRunCurrentButtonConditionalHidesWhenBlank`: verifies `#runCurrentTestBtn` carries `d-none` when `activeEditingFile` is empty.
