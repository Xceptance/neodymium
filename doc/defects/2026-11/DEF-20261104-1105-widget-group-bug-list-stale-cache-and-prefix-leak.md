# [DEF-20261104-1105] Filtered Bug List Leaks Sister Test Variations Due to Loose Prefix Matching and Stale Script Cache

- **Status:** `Resolved`
- **Opened:** 2026-11-04 11:05
- **Closed:** 2026-11-04 11:10
- **Component:** `aura-manager`
- **Scope:** `Framework`
- **Symptom:** When cross-filtering in a united widget group (e.g. clicking "Succeeded Fixed" in the Status Pie Chart), non-matching bug occurrences sharing the same test method name remained visible in the List widget. Furthermore, hard-refreshing the dashboard in the browser reloaded the page in an unfiltered state with all tests shown, and standard hard-refresh shortcuts failed to invalidate the browser's cached script bundle due to a static cache buster.
- **Root Cause:**
  1. In `report-manager.js` (`recalculateContainerList`), fallback branches checked `occ.k.startsWith(subVarKey + '|') || subVarKey.startsWith(occ.k + '|')`. When two distinct variations of the same test class and method existed (e.g. `CreditCardPaymentTest|submit3dsCreditCard|DE` and `CreditCardPaymentTest|submit3dsCreditCard|US`), loose prefix matching could match across variations.
  2. Subitem tag comparison checked `st.startsWith('env:')` but missed the full tag string `environment: DEV` (`st.startsWith('environment:')`), bypassing tag mismatch rejection.
  3. `index.html` referenced `report-manager.js?v=1.1.4` without bumping the cache-buster query parameter upon script modification.
- **Detection Gap ("What did we miss?"):** Previous assertions in `StaticResourceSyntaxTest.java` checked for the presence of `matchedBugOccurrences` map creation and subitem attribute bindings, but did not assert strict equality (`===`) on variation identifiers or verify that `index.html` bumped asset cache buster versions.
- **Resolution:**
  1. Tightened `recalculateContainerList` in `report-manager.js` to require strict identity matching (`subVarId && occ.vid && subVarId === occ.vid` or `subVarKey && occ.k && subVarKey === occ.k`).
  2. Added normalization in tag mismatch evaluation for `environment:`, `env:`, `browser:`, and `locale:` / `location:`.
  3. Bumped asset cache-buster version in `index.html` from `report-manager.js?v=1.1.4` to `report-manager.js?v=1.1.5`.
  4. Installed the updated `aura-manager` artifact to `~/.m2`.
- **Safety Net Added:** Added automated regression assertions to `StaticResourceSyntaxTest.java` (`testUnitedContainerBugDeDuplicationAndSeverityRanking` and `testRunReportStatusFilterStateTransitions`) verifying strict equality checks (`subVarId === occ.vid`, `subVarKey === occ.k`), tag prefix normalization, and cache buster bump `report-manager.js?v=1.1.5`.
