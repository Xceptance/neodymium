# [DEF-20261104-1526] Sidebar Fragment Scope Placement and SpEL Null Pointer Crash

- **Status:** `Resolved`
- **Opened:** 2026-11-04 15:26
- **Closed:** 2026-11-04 15:29
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  Loading the root page (`GET /`, `index.html`) threw `org.thymeleaf.exceptions.TemplateInputException` caused by `SpelEvaluationException: EL1007E: Property or field 'isGroupParent' cannot be found on null` at `fragments/sidebar.html`.
- **Root Cause:**
  The `renderCustomPageNav(page, level)` recursive fragment was nested within `<aside th:fragment="sidebar" id="mainSidebar">` instead of being declared as a sibling fragment under `<body>`. When Thymeleaf replaced `<aside th:replace="~{fragments/sidebar :: sidebar}">`, it parsed and executed all child elements of `sidebar` in top-level context where `page` is unassigned (`null`). Evaluating `!page.isGroupParent` resulted in an unhandled SpEL null evaluation exception.
- **Detection Gap ("What did we miss?"):**
  Targeted unit tests (`StaticResourceSyntaxTest`, `AuraReportViewControllerTest`, `ReportDashboardSettingServiceTest`) did not include `AuraReportViewControllerSpringContextTest`, which initializes the full Spring MVC `MockMvc` context and executes live template rendering on `/`.
- **Resolution:**
  1. Relocated `</aside>` in `fragments/sidebar.html` directly after the `.sidebar-scrollable-content` container.
  2. Declared `<th:block th:fragment="renderCustomPageNav(page, level)">` outside `<aside>` as a top-level fragment sibling under `<body>`.
  3. Added `<th:block th:if="${page != null}">` as a defensive top-level guard in `renderCustomPageNav` to prevent null dereferences.
- **Safety Net Added:**
  Verified via `AuraReportViewControllerSpringContextTest.testIndexPageContainsConvertToGroupModal`, which dispatches a live `MockMvc` `GET /` request and executes full Thymeleaf template engine parsing of `index.html` and its included fragments.
