# [DEF-20261029-1406] Widget Configurator Edit Modal Closes Instantly Due to HTMX Attribute Settling Conflict

- **Status:** `Resolved`
- **Opened:** 2026-10-29 14:06
- **Closed:** 2026-10-29 14:09
- **Component:** `aura-manager`
- **Scope:** `Framework & UI`
- **Symptom:** Clicking "Edit widget" on any dashboard widget card causes the configurator modal dialog to flash open and immediately close without user interaction.
- **Root Cause:** In `report-manager.js`, `openWidgetConfiguratorModal` invoked `window.htmx.ajax` to swap `#widgetConfiguratorModalContainer` innerHTML and set `modal.style.display = 'flex'` inside the resolved promise `.then()` callback. However, the root element in `widget-configurator-modal.html` was hardcoded with `style="display: none;"` and `AuraReportViewController.getWidgetConfigModal` did not bind an open flag. HTMX has a built-in attribute settling pipeline (`defaultSettleDelay: 20ms`) configured for `attributesToSettle: ["class", "style", "width", "height"]`. When the swap settled, HTMX executed `we(e, a)` 20ms later, restoring the server response's attribute `style="display: none;"` onto `#widgetConfiguratorModal` and overriding the client-side `display: flex`.
- **Detection Gap ("What did we miss?"):** Existing controller tests only asserted that the modal view fragment name was returned and that widget attributes were non-null, but never asserted modal visibility model binding or evaluated HTMX client attribute settling lifecycle behavior.
- **Resolution:**
  1. In `AuraReportViewController.getWidgetConfigModal`, added `model.addAttribute("isOpen", true)`.
  2. In `widget-configurator-modal.html`, updated the root element to `th:style="${isOpen == true} ? 'display: flex;' : 'display: none;'"` so the server response returns `style="display: flex;"`, ensuring HTMX attribute settling settles into visible state.
  3. In `widget-configurator-modal.html`, added backdrop click-to-dismiss handler `onclick="if (event.target === this) closeWidgetConfiguratorModal();"`.
  4. In `report-manager.js`, refactored `openWidgetConfiguratorModal` to immediately show the modal overlay, fetch clean state via HTMX for both edit and add actions, and settle reliably.
- **Safety Net Added:** Added assertions in `AuraReportViewControllerTest.testGetWidgetConfigModal_NewWidget` and `testGetWidgetConfigModal_ExistingWidget` verifying that `Boolean.TRUE.equals(model.getAttribute("isOpen"))`.
