# [DEF-20261103-1056] United Container Ratio Bar Count Discrepancy for Area and Locale Dimensions

- **Status:** `Resolved`
- **Opened:** 2026-11-03 10:56
- **Closed:** 2026-11-03 10:59
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When filtering in united widget containers, Area ratio widgets displayed all segment values as 0 (0%) and rendered an empty distribution bar despite the card reporting matching items (e.g. 44 Items). Additionally, filtering by `locale` vs `location` or `environment` vs `env` risked facet exclusion failure due to dimension alias mismatch.
- **Root Cause:** 
  1. In `BatchLatestVariationDto.java`, variation area values are serialized with an `@` prefix and default suffix (e.g. `@Cart`, `@Browsing (default)`), whereas ratio bar segments and legend chips render clean values (`Cart`, `Browsing`). In `report-manager.js`, `resolveVariationDimensionValue` returned the raw `@`-prefixed string, and `resolveDimensionCount` enforced strict string equality (`k.toLowerCase().trim() === rl`), causing `"@cart"` to mismatch `"cart"` and evaluating all segment counts to 0.
  2. In `matchesContainerFilters` and `updateContainerCardsVisualState`, `locale` and `location` (as well as `environment` and `env`) were not canonicalized as synonyms, causing facet exclusion to fail when one component used `locale` and another used `location`.
- **Detection Gap ("What did we miss?"):** Prior syntax tests verified that `resolveDimensionCount` avoided fuzzy substring hijacking, but did not assert normalization across Area tag `@` prefixes or canonical equivalence for dimension synonyms.
- **Resolution:**
  1. Updated `resolveVariationDimensionValue(v, dim)` in `report-manager.js` to strip leading `@` and trailing `(default)` from Area values so variation counts match clean segment names.
  2. Updated `resolveDimensionCount(rawVal, counts, dim)` in `report-manager.js` with an exact normalized fallback that strips `@` and `(default)` while preserving strict non-fuzzy equality.
  3. Updated `matchesVariationFilter(v, filterDim, filterVal)` to strip `(default)` in addition to `@` for Area filters.
  4. Canonicalized `locale`/`location` and `environment`/`env` in `matchesContainerFilters` and `updateContainerCardsVisualState` so facet exclusion treats them as interchangeable synonyms.
- **Safety Net Added:** Added `@Test testUnitedContainerAreaAndLocaleRatioBarNormalizationSupport()` in `StaticResourceSyntaxTest.java`, asserting clean Area resolution, exact equality normalization, and facet dimension canonicalization.
