# [DEF-20261027-1335] Batch Latest Status Card Sticky Top Offset Overlaps Sticky Global Filters

- **Status:** `Resolved`
- **Opened:** 2026-10-27 13:30
- **Closed:** 2026-10-27 13:35
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When scrolling down the `/batch-history` page, the Latest Test Status card (`.batch-latest-status-card`) pinned at `top: 0.5rem`, sliding underneath the sticky global filters toolbar (`#batchStickyFiltersContainer`). The card header, test count badge, and top third of the donut pie chart were obscured.
- **Root Cause:** `.batch-latest-status-card` was given a hardcoded `top: 0.5rem;` sticky offset. Because the sibling `#batchStickyFiltersContainer` is also sticky at `top: 0` with `z-index: 1020` and dynamic height (~100px to 150px depending on active filter chips and screen width), the card pinned behind the filter toolbar instead of stacking below it.
- **Detection Gap ("What did we miss?"):** Previous static asset tests verified the presence of `position: sticky; top: 0.5rem;` on `.batch-latest-status-card` without verifying coordinate stacking relative to the sibling sticky filter toolbar (`#batchStickyFiltersContainer`).
- **Resolution:**
  1. Added `--batch-sticky-filters-height: 135px;` fallback variable in `:root` in `report-manager.css`.
  2. Updated `.batch-latest-status-card` to use `top: calc(var(--batch-sticky-filters-height, 135px) + 0.5rem);` and `max-height: calc(100vh - var(--batch-sticky-filters-height, 135px) - 1.5rem);`.
  3. Added `updateBatchStickyFiltersHeight()` and `initBatchStickyFiltersObserver()` in `report-manager.js` using `ResizeObserver` on `#batchStickyFiltersContainer` (with window resize listener) to dynamically sync `--batch-sticky-filters-height` to its exact pixel height.
  4. Bumped cache-busting version parameter in `index.html` to `?v=1.1.14`.
- **Safety Net Added:** `com.xceptance.aura.report.staticassets.StaticResourceSyntaxTest.testBatchLatestStatusCardStickyPinnedAndFillsRunsList` asserting fallback CSS variable, dynamic sticky top offset, and JS `ResizeObserver` setup.
