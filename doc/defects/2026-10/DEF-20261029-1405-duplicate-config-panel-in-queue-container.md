# [DEF-20261029-1405] Duplicate Configuration Panel Rendered in Queue Container

- **Status:** `Resolved`
- **Opened:** 2026-10-29 14:05
- **Closed:** 2026-10-29 14:08
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** On initial load of `/playbooks`, the Browser Profiles & Run Configuration panel was visually duplicated inside the middle "Run Queue" column below the empty queue card.
- **Root Cause:** In `aura-manager/src/main/resources/templates/fragments/queue.html`, lines 96–99 nested an out-of-band `<div id="configPanel" hx-swap-oob="true">` inside the `queueListContainerContent` fragment. On initial server-side template rendering, Thymeleaf statically expanded `queueListContainerContent` into `#queueListContainer`, rendering duplicate `#configPanel` containers across both the middle queue area and the right configuration area.
- **Detection Gap:** Previous UI tests validated the existence and functionality of the Add Tag button and modal, but did not assert that `#queueListContainer` contains zero instances of `#configPanel` or `.config-panel` and that exactly one `#configPanel` exists in the DOM.
- **Resolution:** Removed the nested `#configPanel` block from `queueListContainerContent` in `queue.html`. Added `@GetMapping({"/api/config/panel", "/api/config/refresh"})` in `AuraTestQueueController.java` to return `"fragments/queue :: configPanel"`, and updated `submitQueueRunTag` and `removeQueueRunTag` in `dashboard-runner.js` to refresh `#configPanel` alongside `#queueListContainer` in a decoupled manner.
- **Safety Net Added:** Added assertions in `AuraTestQueueControllerTest.java` (`testQueueAndSelectionTemplatesDoNotContainSaveQueueAsBatchButtons`, `testGetConfigPanelReturnsConfigPanelFragment`) and `AuraManagerEditorUiTest.java` (`testPlaybooksQueuePageHasNoSaveAsBatchButtonAndProvidesRunTagging`) ensuring that `#queueListContainer` contains zero `.config-panel` elements and exactly one `#configPanel` exists in the document.
