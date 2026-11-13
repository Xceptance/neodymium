# [DEF-20261027-1412] Allow Grouping Inside Group and Scoped Inner Group Pinning

- **Status:** `Resolved`
- **Opened:** 2026-10-27 14:12
- **Closed:** 2026-10-27 14:15
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When browsing within a grouped view (such as `/batch-history?groupType=environment&groupValue=Production`), the grouping toolbar was previously hidden or non-functional, preventing users from sub-grouping the scoped test runs by another criterion (e.g. browser or locale), viewing sub-group overview cards above the page content, or pinning default inner grouping preferences per parent group.
- **Root Cause:** `batchHistory` previously only supported flat top-level grouping; the grouping toolbar was either omitted or lacked sub-group scoping, `BatchSummaryDto` lacked compound `historyUrl` navigation capabilities, and `ReportDashboardSettingService` stored only a single global `pinned_group_by` preference without context-aware parent group keying (`pinned_inner_group_by:<groupType>`).
- **Detection Gap ("What did we miss?"):** Previous tests only asserted top-level grouping toolbar behavior and flat un-grouped batch history pages, lacking tests verifying hierarchical compound navigation, sub-group card calculation and positioning, and scoped inner group pinning persistence.
- **Resolution:**
  1. Updated `BatchSummaryDto` with `historyUrl` field, constructor overloads, and getter for compound sub-group card navigation.
  2. Extended `ReportDashboardSettingService` with inner group pinning APIs (`pinned_inner_group_by:<groupType>`), file fallback synchronization, and context-aware overloads (`getPinnedGrouping`, `setPinnedInnerGrouping`, `togglePinnedGrouping`, `isGroupingPinned`).
  3. Added `getGroupOverviewData` overload in `AuraReportDataService` supporting scoping by parent group criteria and compound `historyUrl` generation.
  4. Updated `AuraReportViewController` `pinGrouping` and `batchHistory` to accept inner `groupBy`, exclude defining parent group dimensions from inner grouping pills, compute sub-group overview cards, and resolve scoped inner pinning.
  5. Updated `batch-history.html` to render `#batchGroupingToolbar` with Back button, exclude parent group pills, pass `groupType` in `#btnPinGrouping`, and render `#batchOverviewCardsContainer` directly under the grouping bar and above the rest of the page.
- **Safety Net Added:** Added unit and template integration tests in `ReportDashboardSettingServiceTest` (`testGetPinnedInnerGroupingFromDatabase`, `testGetPinnedInnerGroupingFallbackToFile`, `testSetPinnedInnerGroupingPersistsToDbAndFile`, `testClearPinnedInnerGroupingRemovesFromDbAndFile`, `testTogglePinnedInnerGrouping`, `testIsInnerGroupingPinned`, `testContextAwareIsGroupingPinnedAndToggle`) and `AuraReportViewControllerTest` (`testBatchHistoryInnerGroupingCardsAndScoping`, `testBatchHistoryHierarchicalBackUrl`, `testPinInnerGroupingViaEndpoint`, `testBatchHistoryInnerGroupingTemplateRendersCardsAboveMetrics`).
