# [DEF-20261028-1048] Dashboard Navigation Targets Lack Semantic Anchor Href Attributes for New Tab, Window, and Context Menu Operations

- **Status:** `Resolved`
- **Opened:** 2026-10-28 10:48
- **Closed:** 2026-10-28 10:55
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Interactive navigation targets across the Aura Manager UI (such as "Back to Runs Overview", "Group By" pills, batch cards "View History" / "Open Last Report", contributing runs ID and "Open Latest Report", history table run IDs, test suite chips, "Inspect" buttons, test title rows, and side-panel "View History") could not be opened in a new browser tab (Ctrl/Cmd-click, middle-click), opened in a new window, or copied via right-click context menu "Copy link address". Some anchor tags used dead pseudo-hrefs such as `href="javascript:void(0)"` while others were non-semantic `<button>` or `<span>` tags with `hx-get` or inline JS handlers.
- **Root Cause:** Navigation elements were implemented using HTMX attributes (`hx-get`, `hx-push-url`) directly on `<button>` elements, or using `onclick` JavaScript callbacks without standard HTML `<a>` tags and `href` / `th:href` URL attributes. Furthermore, `.test-title-link` in `run-report.html` was rendered as `<a href="javascript:void(0)" ...>` which broke native right-click context menu "Copy link address" and new tab navigation.
- **Detection Gap ("What did we miss?"):** Previous static asset tests in `StaticResourceSyntaxTest` and integration tests in `AuraReportViewControllerTest` only checked HTTP status codes and Thymeleaf model attribute bindings, but did not assert semantic HTML anchor tags and valid URL attributes on interactive dashboard links.
- **Resolution:**
  1. Converted navigation buttons across `batch-history.html`, `batch-details.html`, `test-base.html`, and `side-panel-step-list.html` to semantic `<a>` tags with `th:href` and valid fallback `href`.
  2. Maintained HTMX smooth SPA transitions (`hx-get`, `hx-target="#mainViewContainer"`, `hx-push-url="true"`) for standard left-clicks, allowing native browser modifier clicks (middle-click, Ctrl/Cmd-click, Shift-click) and right-click context menus to utilize `href`.
  3. Replaced `href="javascript:void(0)"` on `.test-title-link` with permalink `th:href="@{/run-report(runId=${report?.runId ?: runId}, executionId=${exec.id})}"` and modifier-aware `onclick` that prevents default navigation only on unmodified primary left-clicks.
  4. Wrapped Run IDs in the history table and contributing runs list in styled anchor links (`.run-id-link` and `.contributing-run-id`) pointing to `/run-report?runId=...`.
  5. Updated side-panel step list script `openTestSidePagePanel()` to dynamically assign `href` to `#btnViewHistoryInTestBase` with current variation query parameters.
- **Safety Net Added:** Added regression test `testAllDashboardNavigationLinksHaveAnchorHrefSemantics()` in [StaticResourceSyntaxTest.java](file:///home/olha/git/neodymium/aura-manager/src/test/java/com/xceptance/aura/report/staticassets/StaticResourceSyntaxTest.java) asserting presence of `th:href` and absence of `href="javascript:void(0)"` across all dashboard navigation fragments.
