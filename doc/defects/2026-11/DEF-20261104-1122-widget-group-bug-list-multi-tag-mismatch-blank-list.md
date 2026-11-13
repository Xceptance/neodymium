# [DEF-20261104-1122] Filtered Bug List Discards Matching Occurrences Due to Multi-Valued Tag Mismatch Logic

- **Status:** `Resolved`
- **Opened:** 2026-11-04 11:22
- **Closed:** 2026-11-04 11:35
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** In a united container (widget group), filtering by status "Succeeded Fixed" renders the Bugs list completely blank ("No items match current filters") even though one matching bug occurrence exists in the filtered slice.
- **Root Cause:**
  1. In `report-manager.js` (`recalculateContainerList`), after evaluating `varMatch`, the tag mismatch loop checked each tag in `data-subitem-tags` individually: `if (exp && exp !== occ.env) tagMismatch = true;`. When a subitem contained multi-valued tags across multiple environments (e.g. `environment: STG, environment: DEV`), the secondary tag (`DEV`) did not match the single occurrence environment (`STG`), flagging `tagMismatch = true`.
  2. Because `tagMismatch` caused `continue`, the matching occurrence was rejected despite having an exact variation ID match (`subVarId === occ.vid`).
- **Detection Gap ("What did we miss?"):** Previous unit tests verified individual tag prefix strings (`environment:`) but did not test subitems with multiple tags for the same dimension.
- **Resolution:**
  1. Made canonical variation identity authoritative: when `subVarId && occ.vid && subVarId === occ.vid` (or `subVarKey && occ.k && subVarKey === occ.k`), the identity match is exact and definitive, bypassing secondary tag mismatch heuristics.
  2. For fallback tag matching (when neither variation ID nor key is provided), collected tag values per dimension and asserted set containment (`envTags.includes(occ.env)`).
  3. Bumped script cache buster in `index.html` to `report-manager.js?v=1.1.6`.
  4. Installed the updated artifact to `~/.m2/repository`.
- **Safety Net Added:** Added automated regression assertions in `StaticResourceSyntaxTest.java` (`testUnitedContainerBugDeDuplicationAndSeverityRanking` and `testRunReportStatusFilterStateTransitions`) verifying set containment in fallback tags, authoritative variation ID matching, and cache buster bump `report-manager.js?v=1.1.6`.
