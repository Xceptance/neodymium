# [DEF-20261103-1115] United Container List Widget Item Hiding and Healed/AI Cross-Filtering Support

- **Status:** `Resolved`
- **Opened:** 2026-11-03 11:00
- **Closed:** 2026-11-03 11:15
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:**
  1. When filtering inside united widget containers, non-matching list items in `LIST` presentation widgets were not hidden (they remained displayed in the UI, or only group counts updated while non-matching rows stayed visible; additionally, if filtering by status, list cards with status metric skipped status filtering due to facet exclusion).
  2. The healed (`.badge-healed-inline`) and AI (`.badge-ai-inline`) labels on pie slices, ratio chips, text KPI cards, and list groups/subitems were static badges rather than interactive filters. It was impossible to filter and view exclusively healed-passed tests without clean-passed tests or healed-failed tests.
- **Root Cause:**
  1. In `updateContainerCardsVisualState` in `report-manager.js`, `matchedVariations` was computed using `excludeDim = cardDim`. For a `LIST` widget with `data-metric="STATUS"`, filtering by `Passed` caused `status` to be excluded from filtering for that list card, leaving failed variations in `matchedVariations`. Furthermore, `recalculateContainerList` previously did not toggle `display = 'none'` on individual non-matching `widget-list-subitem-row` elements or on `widget-list-group` containers with 0 matching items.
  2. Healed and AI badges lacked `widget-interactive-filter` class, click handlers, and `data-filter-dim` / `data-filter-val` bindings. Additionally, `matchesVariationFilter` lacked parsing for composite sub-mode qualifiers (e.g. `(Healed)`, `(AI)`, `(Clean)`), evaluating filters solely against the base status without asserting whether the variation had `h === true` or `m === 'AI'`.
- **Detection Gap ("What did we miss?"):**
  1. Existing list widget tests (`testUnitedContainerListWidgetCrossFilterDispatchingAndRecalculation`) verified that `recalculateContainerList` accepted `isFiltered` and matched variation IDs, but did not assert that non-matching DOM elements were styled with `display = 'none'`.
  2. Facet exclusion tests assumed list cards should act like distribution charts (excluding their own dimension) rather than itemized record views that require strict filtering across all dimensions.
- **Resolution:**
  1. Updated `recalculateContainerList` in `report-manager.js` to hide non-matching subitems (`sub.style.display = 'none'`), hide list groups with 0 matching subitems (`grp.style.display = 'none'`), hide flat list groups without matches, dynamically recalculate count badges, and display a `.widget-list-empty-filter` notice when zero items match across the card. Restored visibility for all items and groups when `!isFiltered`.
  2. Updated `updateContainerCardsVisualState` to set `excludeDim = null` whenever `presentation === 'LIST'`, ensuring list widgets strictly filter across all dimensions including their own metric dimension.
  3. Enhanced `matchesVariationFilter` in `report-manager.js` to detect composite qualifiers `(healed)`, `(ai)`, and `(clean)`: verifies variation healed status (`v.h === true || v.s === 'healed' || v.m === 'HEALED'`) and AI execution mode (`v.m === 'AI' || v.s.includes('ai')`), strips the qualifier, and evaluates the clean base value against the target dimension. Clicking `Passed (Healed)` now strictly matches only tests that are both passed AND healed.
  4. Updated `onContainerWidgetFilterClick` to support superset/subset disambiguation: selecting a qualified filter (e.g. `Passed (Healed)`) removes the base `Passed` filter from active filters, and selecting `Passed` removes any child qualifiers.
  5. Updated `dashboard-widgets.html` across pie slices, ratio bar chips, text KPIs, list headers, and list subitems to bind healed and AI badges with `widget-interactive-filter`, `cursor: pointer;`, `data-filter-dim`, and `data-filter-val="${label + ' (Healed)'}"` / `"${label + ' (AI)'}"`.
  6. Added emerald (`#34d399`) and violet (`#a78bfa`) styling and glow effects in `dashboard-styles.css` for `.badge-healed-inline.is-filter-active` and `.badge-ai-inline.is-filter-active`, added `.filter-healed` and `.filter-ai` tags to the container filter strip, and added `:has(.is-filter-active)` rule so parent containers never dim active child badges.
- **Safety Net Added:** Added `@Test testUnitedContainerListWidgetFilteringHidesNonMatchingItems()` and `@Test testUnitedContainerHealedAndAiInteractiveFilterBadges()` to `StaticResourceSyntaxTest.java`, asserting subitem and group hiding, list facet exclusion bypass, qualifier parsing, template attribute bindings, and active badge styling.
