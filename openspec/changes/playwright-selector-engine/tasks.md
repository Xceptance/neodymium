# Tasks

## 1. Selector Lexer & AST

- [x] 1.1 Implement quote-, bracket-, paren-, and regex-aware `PlaywrightSelectorLexer` and AST node hierarchy (`SelectorChain`, `SelectorStep`, `SelectorEngine`, `CssCompoundSelector`) in `neodymium-core`, verified by `PlaywrightSelectorLexerTest`.
- [x] 1.2 Implement `PlaywrightSelectorParser` with strict syntax validation (actionable error reporting with token name, column position, and alternatives) and Neodymium automation ID integration (`#xc...` / `data-ai=`), verified by `PlaywrightSelectorParserTest` covering all 13 defect symptoms from DEF-20261013-1642.

## 2. Browser JavaScript Evaluator (Shadow DOM, Text, Role, Layout)

- [x] 2.1 Implement `PlaywrightSelectorEvaluator.js` supporting recursive open Shadow DOM traversal in document order for all CSS, text, role, and attribute locators, verified by browser unit tests against shadow DOM test pages.
- [x] 2.2 Implement Playwright text matching (`textContent` normalization, case-insensitive substring, quoted exact, regex `/.../`) and pseudo-classes (`:has-text()`, `:text()`, `:text-is()`, `:text-matches()`, `:visible`, `:has()`, `:is()`, `:not()`, `:nth-match()`) in the JS evaluator, verified by browser unit tests.
- [x] 2.3 Implement W3C computed role and accessible name matching (`role=`, `internal:role=`) with state options (`name`, `checked`, `disabled`, `level`, `include-hidden`) and layout pseudo-classes (`:left-of()`, `:right-of()`, `:above()`, `:below()`, `:near()`) in the JS evaluator, verified by browser unit tests.

## 3. Selenium By Integration & LocatorResolver Refactoring

- [x] 3.1 Implement `PlaywrightBy` extending `org.openqa.selenium.By` that executes the JS evaluator with `JavascriptExecutor` across the document and open shadow roots, while routing XPath selectors directly without shadow DOM piercing, verified by `PlaywrightByTest`.
- [x] 3.2 Refactor `LocatorResolver.resolveLocator(String)` to parse selectors through `PlaywrightSelectorParser` and return `PlaywrightBy`, removing regex cascades and class/id bleeding, verified by running `LocatorResolverTest` and `LocatorResolverBrowserTest`.
- [x] 3.3 Update `SelenideElementFinder` to delegate shadow DOM search to `LocatorResolver` / `PlaywrightBy`, and eliminate silent fallbacks that search raw selector strings as page text on `InvalidSelectorException`, verified by `SelenideElementFinderTest`.

## 4. Integration, Prompt Guidance & Defect Verification

- [x] 4.1 Update `BrowserToolProvider` (`query_dom` selector execution and pre-resolution) to resolve all selectors via `LocatorResolver.resolveLocator`, ensuring discovery and action targeting share identical resolution semantics, verified by `BrowserToolProviderTest`.
- [x] 4.2 Update `ai-prompts/selenide-locator-rule.md` to document the supported Playwright locator grammar, pseudo-classes, and always-on open shadow DOM search behavior.
- [x] 4.3 Verify the regression safety net and fix for DEF-20261013-1642 by executing `SearchTest.liveAllDataSets[normal]` with `header.top-nav-bar a[href="index.html"]:contains("VÉRLA")`.
- [x] 4.4 Complete post-mortem defect log in `doc/defects/2026-10/DEF-20261013-1642-regex-lexed-selector-resolver-leaks.md` with closed date, verified root-cause resolution, and committed safety net tests.

## Workflow follow-up

- Archive the change after implementation and test suite verification are complete via `/opsx-archive`.
