# [DEF-20261102-1435] United Container Cross-Filtering Substring Collision Between Known and Unknown Failures

- **Status:** `Resolved`
- **Opened:** 2026-11-02 14:30
- **Closed:** 2026-11-02 14:35
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When filtering a united container by a dimension (e.g. `Browser: Chrome_1920x1080`) containing 1 passed and 1 unknown failed test:
  - The center KPI correctly showed `50% PASS`.
  - The donut chart only rendered a 50% green arc for `Passed`, leaving the second half of the donut as empty grey track.
  - The breakdown list displayed `Passed 1 (50.0%)`, `Known Fail 0 (0.0%)`, and `Unknown Fail 0 (0.0%)` with dimmed opacity instead of `Unknown Fail 1 (50.0%)`.
- **Root Cause:**
  1. In `recalculateContainerPieChart`, slice labels were resolved against `sliceCounts` using a bidirectional `String.prototype.includes` loop:
     ```javascript
     for (const k in sliceCounts) {
         if (rawVal.toLowerCase().includes(k.toLowerCase()) || k.toLowerCase().includes(rawVal.toLowerCase())) {
             cnt = sliceCounts[k];
             break;
         }
     }
     ```
     Because `"unknown fail"` contains the substring `"known fail"` (the word `unknown` ends with `known`), and `"Known Fail"` was iterated before `"Unknown Fail"` in `sliceCounts`, the loop matched `"Known Fail"` (count 0) and broke early, assigning count 0 to `Unknown Fail`.
  2. The SVG circle for `Unknown Fail` received percentage `0.0%` and was hidden (`display: none`), while the breakdown item was set to `0 (0.0%)` and dimmed to `opacity: 0.35`.
  3. Secondary vulnerabilities existed in `matchesVariationFilter` and `resolveVariationDimensionValue`, which checked `.includes('known')` prior to checking `unknown`, causing `"failed-unknown"` variations to falsely match known bug filters.
- **Detection Gap ("What did we miss?"):** Previous static resource syntax and DTO tests verified JavaScript parsing and DOM element presence, but did not execute label resolution across competing substring keys like `"Known Fail"` vs `"Unknown Fail"`.
- **Resolution:**
  1. Introduced canonical status normalizer `normalizeStatusCategory` in `report-manager.js` that evaluates `unknown` before `known`.
  2. Implemented collision-free count resolution helpers `resolveSliceCount` and `resolveDimensionCount` that perform exact dictionary lookups prior to fuzzy fallbacks, explicitly disallowing substring hijacking between known and unknown statuses.
  3. Re-ordered status inspections in `matchesVariationFilter` and `resolveVariationDimensionValue` to verify `unknown` before `known`, ensuring `s.includes('known')` requires `!s.includes('unknown')`.
  4. Added dynamic recalculation and conditional display for `.badge-healed-inline` and `.badge-ai-inline` in the pie breakdown list.
- **Safety Net Added:** Added `testUnitedContainerCrossFilterStatusIsolationAndSafety` in `StaticResourceSyntaxTest.java` verifying canonical status normalization, count resolution helpers, and precedence ordering between unknown and known statuses.
