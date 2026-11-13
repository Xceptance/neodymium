# [DEF-20261102-1355] United Widget Container Boxed Cards Layout and Thymeleaf 3 Precedence Bug

- **Status:** `Resolved`
- **Opened:** 2026-11-02 13:40
- **Closed:** 2026-11-02 13:55
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When two widgets (e.g. Status Donut Chart and Browser Ratio Bar) were united into a single container, the child widgets rendered as nested rectangular boxed cards with redundant inner headers, heavy border outlines, and wasted space instead of a seamless side-by-side overview layout. Furthermore, all presentation bodies were evaluated into each child, producing phantom "No data available" placeholders and empty grey circles inside Child 2.
- **Root Cause:**
  1. Container template in `dashboard-widgets.html` utilized `.widget-container-children-grid` and `.widget-child-card` components styled as full standalone cards.
  2. Declaring `th:if` and `th:replace` on the exact same `<th:block>` tag triggered the Thymeleaf 3 attribute precedence conflict (`th:replace` has precedence 100 while `th:if` has precedence 500), causing `th:replace` to inject all 7 presentation fragments before evaluating `th:if`.
- **Detection Gap ("What did we miss?"):** Previous unit tests verified widget aggregation DTO hierarchy without inspecting Thymeleaf fragment attribute precedence or container child CSS classes.
- **Resolution:**
  1. Redesigned container layout into a unified row (`.widget-container-unified-row`, `.widget-container-section`, `.widget-container-pie-section`, `.widget-container-ratios-section`, `.widget-container-divider`) matching the seamless layout of the overview panel without nested inner card chrome.
  2. Separated each presentation body into an outer `<th:block th:if="...">` wrapping an inner `<div th:replace="...">` to ensure strict conditional execution order in Thymeleaf 3.
  3. Added CSS rules in `dashboard-styles.css` removing inner padding and min-height on container sections.
- **Safety Net Added:** Added `@Test testUnitedWidgetContainerSeamlessLayoutAndThymeleafPrecedenceSafety()` to `StaticResourceSyntaxTest.java`, asserting absence of co-located `th:if` and `th:replace` on the same `<th:block>` tag and presence of `.widget-container-unified-row` and `.widget-container-divider`.
