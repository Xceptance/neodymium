# [DEF-20261028-1428] Batch History Tag Ratio Filtering Hides Runs and Clears Trend Chart

- **Status:** `Resolved`
- **Opened:** 2026-10-28 14:28
- **Closed:** 2026-10-28 14:34
- **Component:** `aura-manager`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** Selecting a segment or legend chip in any dynamic tag ratio card in Batch History (e.g. `Tag: Locale Ratio -> UK 23 (20%)`) causes the runs history table (`#batchRunsHistoryTableBody`) to become completely empty and the history trend chart (`#mainTrendSvg`) to wipe out all data paths and points, displaying only bare coordinate grid lines.
- **Root Cause:** 
  1. `TestRunEntity.getTagsJson()` serialized multi-valued run tags by emitting duplicate keys in a flat JSON object literal (`{"locale":"DE",...,"locale":"UK","locale":"US"}`). When parsed in the browser via `JSON.parse()`, ECMAScript duplicate-key semantics silently overwrote all prior values with the last key (`"US"`), destroying all earlier tag values.
  2. `matchTagFilters(el)` in `report-manager.js` assumed scalar string tag values without array candidate extraction or comma-delimited handling, comparing against single values and evaluating to `false` for all run rows.
  3. `renderDynamicTrendChart()` depends on visible run rows (`r.style.display !== 'none'`). When all rows were set to `display: 'none'`, `runsData.length` became 0, triggering the SVG wipe logic that resets all layer path `d` attributes to empty strings.
  4. `getElementTagValue()` lacked fallbacks to dedicated run attributes (`data-locales`/`data-location`, `data-browsers`/`data-browser`, `data-areas`/`data-area`, `data-env`) when evaluating standard dimensions.
- **Detection Gap ("What did we miss?"):** Existing test cases in `StaticResourceSyntaxTest` verified dynamic tag ratio filtering using artificial single-valued tag attributes (`{ release: '1.0', team: 'Core' }`), which masked multi-valued tags originating from real multi-locale and multi-browser test runs.
- **Resolution:**
  1. Updated `TestRunEntity.getTagsJson()` to group tags by key and serialize multi-valued tag keys into valid JSON arrays (`"locale":["UK","US","DE"]`).
  2. Enhanced `getElementTags()` in `report-manager.js` with regex-based duplicate-key preservation for backward compatibility with rendered/cached HTML.
  3. Added `extractCandidateTagValues()` and updated `getElementTagValue()`, `matchTagFilters()`, `matchEntryFilters()`, and `syncBatchRatioBarsVisualState()` to support arrays and fall back to dedicated attributes for standard dimensions (`locale`, `browser`, `area`, `env`).
- **Safety Net Added:**
  - `com.xceptance.aura.report.entity.TestRunEntityTest.testTagsJsonMultiValueArraySerialization`
  - `com.xceptance.aura.report.staticassets.StaticResourceSyntaxTest.testBatchHistoryMultiValueTagRatioFiltering`
