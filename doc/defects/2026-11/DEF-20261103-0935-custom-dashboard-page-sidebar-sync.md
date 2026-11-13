# [DEF-20261103-0935] Custom Dashboard Page Missing in Side Panel Without Browser Refresh

- **Status:** `Resolved`
- **Opened:** 2026-11-03 09:35
- **Closed:** 2026-11-03 09:47
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** After creating a new dashboard page via the modal dialog, the main view switched to the empty page state with an "+ Add Widget" button, but the side panel navigation was not updated with the newly created page button and "Run History" remained highlighted as active until a manual full browser page refresh.
- **Root Cause:** Technical explanation:
  `submitCreatePage()` in `report-manager.js` invoked `window.htmx.ajax('POST', '/dashboard-pages/create', { target: '#mainViewContainer', swap: 'innerHTML' })`. The backend controller responded with an HTTP 302 redirect to `/dashboard-pages/{id}`. Per the W3C XMLHttpRequest specification, the browser's underlying XHR layer silently follows HTTP 302 redirects automatically, preserving the incoming `HX-Request: true` header.
  When the followed request reached `AuraReportViewController.getCustomDashboardPage(...)`, it saw `HX-Request: true` and returned solely the partial fragment `fragments/custom-dashboard-page :: customDashboardPage`. HTMX then swapped this fragment into `#mainViewContainer`. Consequently:
  1. The browser address bar URL remained `localhost:8080/`.
  2. The sidebar navigation `#customPagesNav` was never re-rendered or swapped, leaving "Run History" active and omitting the new page button until a manual browser refresh.
- **Detection Gap ("What did we miss?"):**
  Existing automated tests in `AuraReportViewControllerTest` exercised the Spring MVC controller endpoints with MockMvc in isolation, checking that `/dashboard-pages/create` yielded a redirect status and `/dashboard-pages/{id}` returned the view fragment name. No test validated the client-side JavaScript modal submission path, the browser XHR redirect behavior, or the out-of-band DOM synchronization of the sidebar navigation.
- **Resolution:**
  1. Updated `submitCreatePage` in `report-manager.js` to send an asynchronous `fetch('/dashboard-pages/create', { headers: { 'Accept': 'application/json' }, ... })`, receive `{ id: data.id }`, close the modal, and navigate via `window.location.href = '/dashboard-pages/' + data.id`. This guarantees an address bar update and a full layout render with the newly created page active.
  2. Updated `deleteCustomPage` in `report-manager.js` to also use `fetch` with `Accept: application/json` and navigate via `window.location.href = '/'`.
  3. Added an out-of-band HTMX synchronization safety net: in `sidebar.html`, `<nav id="customPagesNav">` was given `th:attr="hx-swap-oob=${isHxRequest == true ? 'true' : null}"`. In `custom-dashboard-page.html`, an out-of-band block `<th:block th:if="${isHxRequest == true}"><th:block th:replace="~{fragments/sidebar :: customPagesNav}"></th:block></th:block>` was added so any partial HTMX swap also immediately refreshes `#customPagesNav`.
  4. Updated `getCustomDashboardPage` in `AuraReportViewController.java` to set the `HX-Push-Url` header when `HX-Request` is present and explicitly add `isHxRequest` and `customPages` to the Spring MVC model.
  5. Updated `deleteCustomDashboardPage` in `AuraReportViewController.java` to return a JSON response `{"deleted": true, "id": ...}` when `Accept: application/json` is requested.
  6. Provided `setActiveNavBtn(btn)` in `report-manager.js` to handle sidebar active state switching cleanly.
- **Safety Net Added:**
  Verified with automated regression tests in `com.xceptance.aura.report.controller.AuraReportViewControllerTest`:
  - `testCreateCustomDashboardPageJson()`
  - `testGetCustomDashboardPage()`
  - `testDeleteCustomDashboardPage()`
  and static resource verification in `com.xceptance.aura.report.StaticResourceSyntaxTest`.
