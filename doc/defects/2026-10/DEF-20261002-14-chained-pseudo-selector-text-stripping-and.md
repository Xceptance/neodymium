# [DEF-20261002-14] Chained Pseudo-Selector Text Stripping and Unsupported Ordinal Selectors in LocatorResolver

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`LocatorResolver`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:**
  1. Chained selectors with pseudo text filters (e.g. `table >> tr:has-text("Alice") >> button`) lost their text predicate during translation in `toXPathSegment`, resulting in `//table//tr//button` and clicking the first row's button instead of Alice's button.
  2. Playwright ordinal syntax (`>> nth=N`, `:nth-match(...)`, `nth=N`) was either actively blocked with `InvalidSelectorException` or unhandled, preventing natural-language ordinal and relational element targeting in playbooks.
  3. `BrowserToolProvider.query_dom` fallback candidate selector omitted `tr, td, th, li, [role="row"], [role="cell"]`, leaving table and grid cells invisible to text queries unless wrapped in inner spans.
  4. Multi-token CSS selector segments inside chained locators (e.g. `#aria-orders [role="row"] >> nth=2`) treated whitespace as intra-element attribute combinations rather than descendant combinators (`//`), causing queries to search for impossible composite elements (`*[@id='aria-orders' and @role='row']`).
  5. Pseudo-class `:has(...)` and `:not(...)` translation used unbalanced greedy `[^)]+` regexes and failed to translate nested attribute selectors (e.g. `:has([role="cell"])` $\rightarrow$ invalid `descendant::[role="cell"]`).
- **Root Cause:**
  1. `toXPathSegment` invoked `buildPseudoSelectorXpath(cleanSeg, null, false)` with `textVal = null`, discarding `:has-text(...)` argument.
  2. `LocatorResolver` classified `:nth-match()` as an unsupported vendor pseudo-class and lacked parsing for `>> nth=N` segments.
  3. `query_dom` candidate query selector lacked structural table and grid element tags.
  4. `resolveChainedLocator` delegated each chain segment directly to `toXPathSegment` without tokenizing CSS combinators (` `, `>`), improperly treating whitespace as intra-tag selector criteria.
  5. `buildSegmentPredicate` lacked balanced parenthesis extraction and did not delegate nested selector tokens in `:has` / `:not` to `toXPathSegment`.
- **Detection Gap ("What did we miss?"):**
  Existing tests in `LocatorResolverTest` explicitly asserted that `:nth-match` threw an `InvalidSelectorException` rather than implementing translation, no tests checked compound pseudo text selectors inside `>>` chains, and no tests verified multi-token CSS segments combined with `>> nth=N`.
- **Resolution:**
  1. Updated `toXPathSegment` in `LocatorResolver` to parse `PLAYWRIGHT_PSEUDO_PATTERN` and forward extracted `textVal` and `isExact` flag to `buildPseudoSelectorXpath`.
  2. Added support for `>> nth=N` (0-based) and negative indexing (`nth=-1` for last, `nth=-2`, etc.), aliases (`first`, `last`), and translated `:nth-match(sel, N)` to `(//xpath)[N]`.
  3. Expanded `query_dom` fallback candidate query selector in `BrowserToolProvider` to include `tr, td, th, li, [role="row"], [role="cell"]`.
  4. Expanded ARIA role resolution in `LocatorResolver` to support `row`, `cell`, `gridcell`, `table`, and `tab`.
  5. Added `tokenizeCssSelector`, `hasCssCombinator`, and `extractBalancedPseudoArgs` to support multi-token CSS segments and balanced nested `:has(...)` / `:not(...)` expressions with attribute selectors (e.g. `[role="row"]:has([role="cell"])`).
  6. Clarified prompt rules in `selenide-locator-rule.md` to guide agents to target data rows (`tr:has(td)`, `[role="row"]:has([role="cell"])`) rather than including column header rows in ordinal calculations.
- **Safety Net Added:**
  Unit tests in `LocatorResolverTest` (`testPlaywrightNthMatch`, `testChainedOrdinals`, `testChainedPseudoSelectorsPreserveText`, `testChainedMultiTokenCssSegment`, `testChainedAriaDataGridRowWithHasAttribute`, `testHasWithMultipleCommaSeparatedSelectors`, `testNotWithNestedHas`) and comprehensive 29-case live integration test suite in `OrdinalIntegrationTest` (174 test runs passing 100%).
