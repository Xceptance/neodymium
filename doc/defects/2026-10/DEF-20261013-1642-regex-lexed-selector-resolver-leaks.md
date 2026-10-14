# [DEF-20261013-1642] Regex-Lexed Selector Resolver Leaks Tokens Across Selector Contexts

- **Status:** `Closed`
- **Opened:** 2026-10-13 16:42
- **Closed:** 2026-10-13 18:45
- **Component:** `neodymium-core` (`org.neodymium.ai.executor.selenide.LocatorResolver`)
- **Scope:** `Framework`
- **Symptom:**
  `SearchTest.liveAllDataSets[normal]` (`target/ai-results/SearchTest_liveAllDataSets_normal_20261013-155349.html`) failed at step 2
  (`Verify that the site logo or title containing 'VÉRLA' is visible.`). The agent proposed the correct selector
  `header.top-nav-bar a[href="index.html"]:contains("VÉRLA")`, but it resolved to
  `//header[contains(concat(' ', normalize-space(@class), ' '), ' top-nav-bar ')]//a[@href='index.html' and contains(concat(' ', normalize-space(@class), ' '), ' html ') and contains(normalize-space(.), 'VÉRLA')]`.
  The bogus `class="html"` condition matches nothing, so the assertion timed out although `<a href="index.html">VÉRLA.</a>` was visible.
- **Root Cause:**
  `LocatorResolver` does not parse selectors; it applies a cascade of context-blind regular expressions and `String.contains()`
  checks to raw selector strings. Characters are interpreted without knowing whether they sit inside a quoted value, an attribute
  bracket, or a pseudo-class argument. In `buildSegmentPredicate`, attribute expressions are matched but never removed from
  `cleanedRest`, so the class matcher `\.([a-zA-Z0-9_-]+)` reads `.html` from `href="index.html"` as a class selector.
  Code review of the class found further symptoms of the same root cause (not yet reproduced by tests):
  1. `#...` inside attribute values is read as an ID (`[href="#top"]` → `@id='top'`).
  2. `text=` inside a `>>` chain matches every ancestor (`*[contains(normalize-space(.), ...)]`); Selenide picks the outermost wrapper in document order.
  3. `>>` is split with `split("\\s*>>\\s*")`, breaking quoted values such as `text="A >> B"`.
  4. The `#xc...` automation-ID rewrite runs on the whole string, including text values (`text="Open #xcode"`).
  5. The `:visible`/`:hidden` pre-validation scans the whole string and rejects attribute values such as `[title="mode:hidden"]`.
  6. Attribute operators `$=`, `~=`, `|=` fall through to exact equality.
  7. `:nth-child(N)` / `:first-child` / `:last-child` on tagged segments compile to `position()`, i.e. `:nth-of-type` semantics; `an+b` forms are ignored.
  8. `:not(tag)` compiles to `not(self::tag or descendant::tag)`, excluding elements that merely contain the tag.
  9. Sibling combinators (`+`, `~`) and top-level selector lists (`,`) are not handled on the XPath path.
  10. Unknown pseudo-classes (`:enabled`, `:checked`, `:is()`, ...) are silently dropped, widening matches without warning.
  11. Text matching is case-sensitive and does not normalize NBSP, unlike Playwright `:has-text()` / unquoted `text=`.
  12. `role=` has two divergent implementations (Selenide `byRole` standalone vs hand-written XPath in chains); role options other than `name` are ignored.
  13. `findElements` falls back to searching the raw selector string as page text on `InvalidSelectorException`, producing misleading timeouts.
- **Detection Gap ("What did we miss?"):**
  `LocatorResolverTest` asserts hand-picked selector shapes; attribute values never contained `.`, `#`, `:` or `>>`, and no test
  compared resolved semantics against Playwright's documented behavior. The same resolver has accumulated repeated point fixes
  for this root cause (DEF-20261002-14, DEF-20261002-17, DEF-20261004-0034, DEF-20261005-2200, DEF-20261006-0030,
  DEF-20261009-1155), each patching one regex interaction while the regex-lexing architecture remained. Recurrence was
  treated as isolated defects rather than as evidence of a systemic design flaw.
- **Resolution:**
  Replaced the regex cascade with a full Playwright selector engine on branch `feat/playwright-selector-engine`:
  1. Implemented quote-, bracket-, paren-, and regex-aware `PlaywrightSelectorLexer` and AST node hierarchy (`SelectorChain`, `SelectorStep`, `SelectorEngine`, `CssAst`).
  2. Implemented `PlaywrightSelectorParser` with strict syntax validation, actionable error messages, and automation ID (`#xc...` / `data-ai=`) handling.
  3. Implemented `playwright-selector-evaluator.js` and `PlaywrightSelectorEvaluatorLoader` supporting recursive transparent open Shadow DOM traversal in document order for all CSS, text, role, and attribute locators.
  4. Implemented `PlaywrightBy` extending `org.openqa.selenium.By` to execute JS evaluation in browser while routing XPath directly without piercing shadow roots.
  5. Refactored `LocatorResolver.resolveLocator(String)` to return `PlaywrightBy`, completely eliminating class/id token bleeding.
  6. Updated `SelenideElementFinder` to delegate shadow DOM search to `LocatorResolver` and eliminate silent text search fallbacks on `InvalidSelectorException`.
  7. Updated `BrowserToolProvider` to pre-resolve selectors via `LocatorResolver.resolveLocator`.
- **Safety Net Added:**
  1. `PlaywrightSelectorLexerTest` (9 unit tests) covering quote, bracket, paren, and regex token isolation.
  2. `PlaywrightSelectorParserTest` (12 unit tests) covering all 13 defect symptoms including `header.top-nav-bar a[href="index.html"]:contains("VÉRLA")`.
  3. `PlaywrightSelectorEvaluatorBrowserTest` (10 live headless Chrome tests) covering shadow DOM traversal, text/contains/exact/regex matching, W3C roles, layout matchers, and visibility.
  4. `PlaywrightByTest` (3 unit tests) covering JavascriptExecutor delegation and XPath direct routing.
  5. `LocatorResolverTest` (18 unit tests) covering no token leakage, proper PlaywrightBy resolution, and automation ID mapping.
  6. `SelenideElementFinderTest` (11 unit tests) covering shadow DOM delegation, strict InvalidSelectorException propagation, and no silent text fallbacks.
  7. `LocatorResolverBrowserTest` (15 live browser tests) covering live DOM resolution for defect selector `header.top-nav-bar a[href="index.html"]:contains("VÉRLA")`, Shadow DOM, roles, text*, and nth-match.
  8. `BrowserToolProviderStabilityTest` (30 tests) verifying query_dom pre-resolution with Playwright locators.
  9. `SearchTest#replayNormal` (11 steps passed) confirming end-to-end storefront flow without logo assertion timeouts.
