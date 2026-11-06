# [DEF-20261103-1248] List Widget Subitems Default to Uncollapsed State on Every Render

- **Status:** `Resolved`
- **Opened:** 2026-11-03 12:48
- **Closed:** 2026-11-03 12:48
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** List widgets in dashboard containers always rendered with all sub-item trees completely uncollapsed whenever the dashboard page was loaded, refreshed via HTMX, or re-rendered on any filter change, rather than remaining collapsed until clicked.
- **Root Cause:** In `dashboard-widgets.html`, the Thymeleaf fragment `listBody` defined `.widget-list-group` without the default `collapsed` CSS class, initialized `.widget-list-subitems` without `is-collapsed-hidden` and without `style="display: none;"`, and the chevron lacked an initial rotation (`transform: rotate(-90deg)`). Consequently, every server-side template evaluation rendered all child items expanded.
- **Detection Gap ("What did we miss?"):** Previous static resource syntax tests verified that click event handlers properly toggled the collapse classes, but did not assert the default initial markup state in `dashboard-widgets.html`.
- **Resolution:** Updated `dashboard-widgets.html` to render `.widget-list-group.collapsed` by default, set the chevron's initial style to `transform: rotate(-90deg);`, and initialized `.widget-list-subitems` with `class="widget-list-subitems is-collapsed-hidden ..."` and `style="display: none;"`.
- **Safety Net Added:** Added assertions in `StaticResourceSyntaxTest.testUnitedContainerListWidgetAdaptiveFullSpanAndPanoramicRow` verifying that `dashboard-widgets.html` renders `.widget-list-group.collapsed`, rotated chevron, and `.widget-list-subitems.is-collapsed-hidden` with `display: none;` by default.
