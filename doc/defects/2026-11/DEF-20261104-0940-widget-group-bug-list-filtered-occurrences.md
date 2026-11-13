# [DEF-20261104-0940] United Widget Group Bug List Displays Non-Matching Bug Occurrences and Inaccurate Severity Counts Under Cross-Filtering

- **Status:** `Resolved`
- **Opened:** 2026-11-04 09:40
- **Closed:** 2026-11-04 09:55
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In a united container (widget group) containing a List widget configured for `metric === 'BUGS'`, filtering by any dimension (e.g. browser, status, severity, env, tag) failed to adjust the bug list to only show bug occurrences that are present in the filtered slice. For instance, filtering by Chrome displayed bugs that only occurred on Firefox because the test variation existed on Chrome (even when running cleanly with 0 bugs). Furthermore, the severity group count collapsed to 1 due to reading `data-subitem-badge` (the severity label) rather than the ticket name, and severity filters matched non-matching severities via overly broad fallback logic.
- **Root Cause:**
  1. In `report-manager.js` (`recalculateContainerList`), bug subitems only checked `subVarId && matchedVids.has(subVarId)` without verifying whether that specific variation in `matchedVariations` actually had that bug in `v.bugs`.
  2. `recalculateContainerList` extracted `uniqueTickets` via `(sub.getAttribute('data-subitem-badge') || sub.getAttribute('data-subitem-label'))`. Since `data-subitem-badge` stores the severity ("MAJOR", "CRITICAL"), the Set collapsed all distinct tickets in that group down to a single element (`1`).
  3. In `matchesVariationFilter`, when `dim === 'severity'`, a fallback `val.includes('critical') return s.includes('known')` returned `true` for any known failure even when `v.bsev` explicitly contained only other severities (e.g. `MAJOR`).
  4. In `WidgetAggregationService.java` (`buildListWidget`), `WidgetListItemDto` for bugs was passed an empty string `""` for `variationKey`, preventing key-based slice alignment.
  5. In `dashboard-widgets.html`, `.widget-list-subitem-row` omitted `data-subitem-tags`, preventing tag-based occurrence validation.
- **Detection Gap ("What did we miss?"):** Previous tests in `StaticResourceSyntaxTest.java` and `WidgetAggregationServiceTest.java` verified that `matchedVids.has(subVarId)` was present and that `uniqueTickets` was initialized, but did not assert that `matchedBugOccurrences` indexed per-variation bug occurrences from `matchedVariations` or that `data-subitem-tags` was exposed on list subitems.
- **Resolution:**
  1. Updated `recalculateContainerList` in `report-manager.js` to build `matchedBugOccurrences` map from `matchedVariations` indexing `normTicket -> [{ vid, k, browser, env, locale, sev }]`.
  2. Subitem matching for `metric === 'BUGS'` now verifies both that the ticket occurs in `matchedBugOccurrences` and that the subitem's variation identity (`vid`, `key`, or `tags`) aligns with the occurrence in the filtered slice.
  3. Fixed group header and section header counts in `recalculateContainerList` to extract `data-subitem-label` before `data-subitem-badge`, properly counting distinct bug tickets.
  4. Updated `matchesVariationFilter` in `report-manager.js` to reject non-matching severities when `v.bsev` is non-empty before evaluating the generic known-failure fallback.
  5. Updated `WidgetAggregationService.java` to resolve `variationKey` from `latestVariations` and pass it to `WidgetListItemDto`.
  6. Added `data-subitem-tags="${sub.hasTags() ? #strings.listJoin(sub.tags, ',') : ''}"` to `dashboard-widgets.html`.
- **Safety Net Added:** Added automated regression assertions to `StaticResourceSyntaxTest.java` (`testUnitedContainerListWidgetFiltering` and `testUnitedContainerBugDeDuplicationAndSeverityRanking`) verifying `const matchedBugOccurrences = new Map();`, `matchedBugOccurrences.get(normSubTicket)`, `uniqueVisibleTickets = new Set();`, and `data-subitem-tags` template binding.
