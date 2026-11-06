# [DEF-20261030-1335] Widget Filter Value Duplicate Display in Card Header and Filter Strip

- **Status:** `Resolved`
- **Opened:** 2026-10-30 13:35
- **Closed:** 2026-10-30 13:36
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** Active filter values (such as `@Account`, `@Browsing (default)`, and `@Analytics`) appeared twice on widget cards: once as a title badge (`FILTERED: @ACCOUNT`) and again in the chip strip directly below (`Filtered: [@Account]`). On half- and quarter-width cards, the title badge also caused title bar wrapping and displaced card action buttons (`list`, `edit`, `close`).
- **Root Cause:** Both the card title badge (`<span class="badge-status" th:if="${w.hasOwnRunFilters()}"><span th:text="${w.filterBadgeLabel}">`) and the dedicated `.widget-filter-strip` container were rendered simultaneously for all 4 widget presentation types (`PIE_CHART`, `RATIO_BAR`, `PERLENKETTE`, `TREND_GRAPH`) in `dashboard-widgets.html`.
- **Detection Gap ("What did we miss?"):** Unit tests verified DTO filter chip generation and badge string labels independently, but did not assert template HTML output to ensure the card header omits duplicate filter badge pills when the chip strip is rendered.
- **Resolution:** Removed the redundant header filter badge from all 4 widget presentation headers in `dashboard-widgets.html`, delegating active filter representation entirely to the dedicated `.widget-filter-strip` below the title. Retained the filter badge in `widget-executions-modal.html` where no strip exists.
- **Safety Net Added:** Added `@Test` `testDashboardWidgetsRendersFilterStripWithoutDuplicateHeaderFilterBadge` in `AuraReportViewControllerSpringContextTest` asserting that the rendered HTML contains the `.widget-filter-strip` with semantic tag classes and filter labels, while strictly omitting duplicate `Filtered: ...` and `Filtered (N)` badges in the header bar.
