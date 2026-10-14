# Proposal

## Why

The LLM is told to write Playwright-style selectors, but `LocatorResolver` only half-understands them: it scans raw selector
strings with regular expressions and `contains()` checks instead of parsing them. Characters inside quoted values, attribute
brackets, and pseudo-class arguments leak into other parts of the translation, unknown syntax is silently dropped, and several
Playwright features cannot be executed at all. The visible result is tests failing on correct selectors (DEF-20261013-1642:
`a[href="index.html"]:contains("VÉRLA")` gained a bogus `class="html"` condition) and a long series of point fixes for the same
root cause (DEF-20261002-14, DEF-20261002-17, DEF-20261004-0034, DEF-20261005-2200, DEF-20261006-0030, DEF-20261009-1155).
Supporting the full Playwright selector language removes the gap between what the LLM is allowed to write and what Neodymium
can execute.

## What Changes

- Introduce a real Playwright selector implementation in `neodymium-core`: a quote/bracket/paren-aware lexer, a parser that
  produces a selector syntax tree, and a validator that rejects anything it cannot execute with an actionable error.
- Execute selectors through a single custom Selenium `By` that either compiles to one native CSS/XPath query (when the result
  is exactly equivalent) or runs a query plan: native lookups followed by Java-side filtering, with element properties fetched
  in batched `executeScript` calls.
- Cover the Playwright selector language: CSS with Playwright extensions (`:has-text()`, `:text()`, `:text-is()`,
  `:text-matches()`, `:has()`, `:is()`, `:not()`, `:visible`, `:nth-match()`, layout pseudo-classes), the `text=`, `css=`,
  `xpath=`, `id=`, `data-testid=`/`data-test-id=`/`data-test=`, `role=`, `nth=`, `visible=` engines, `>>` chaining with `*`
  capture, legacy text/XPath shorthands, and the `internal:*` engines emitted by Playwright codegen and locator APIs.
- Match Playwright semantics for text (`textContent`-based, whitespace-normalized, case-insensitive substring vs. case-sensitive
  exact vs. regex), roles (browser-computed role and accessible name, hidden elements excluded by default), and open shadow DOM
  (always searched, as in Playwright).
- Keep Neodymium-specific shorthands that are not Playwright syntax (`#xc…`/`data-ai=` automation IDs, `placeholder=`, `alt=`,
  `title=`, `label=`) but resolve them through the parser so they only apply in selector positions, never inside quoted values.
- **BREAKING (behavioral):** Remove the LLM-forgiving deviations from Playwright semantics: `:not(tag)` no longer excludes
  elements containing the tag, unknown pseudo-classes and unsupported syntax raise `InvalidSelectorException` instead of
  being dropped, and invalid selectors are no longer retried as plain page-text searches.
- Replace the separate JavaScript shadow-DOM selector scripts in `SelenideElementFinder` with the engine's shadow-DOM support,
  and route `query_dom` through the engine so discovery and actions resolve selectors identically.
- Update the locator syntax section of the agent system prompt to describe the supported Playwright syntax.
- WebDriver BiDi is out of scope; the engine uses classic WebDriver only.

## Capabilities

### New Capabilities

- `selector-resolution`: How Neodymium parses, validates, and executes element selectors (Playwright selector language plus
  Neodymium shorthands), including text, role, visibility, and shadow-DOM semantics and the error contract for unsupported
  selectors.

### Modified Capabilities

None. No existing spec defines selector syntax or semantics; `unified-tooling-architecture` describes tools and the quality
judge locator guard but not how selectors resolve.

## Impact

- **Code:** `org.neodymium.ai.executor.selenide.LocatorResolver` (becomes a facade over the new engine),
  `SelenideElementFinder` (shadow-DOM scripts and guessing fallbacks), `BrowserToolProvider` (`query_dom` complex-selector
  detection), `SelectorSyntaxChecker`, `QualityJudgeToolInterceptor` and `SelenideLocatorProber` (consumers of `resolveLocator`),
  `ai-prompts/selenide-locator-rule.md`.
- **Behavior:** Selectors that previously matched by accident (case-sensitive text, ancestor matches for `text=`, dropped
  pseudo-classes) may now match different elements or fail with a clear error. Recorded playbooks replay through the same
  resolver and must be re-verified.
- **Performance:** Plan execution adds WebDriver round trips; native compilation remains the fast path. Always-on shadow-DOM
  search adds a host scan per query.
- **Dependencies:** None added. Pure Java/Selenium/Selenide; WebDriver BiDi not used.
- **Tests:** `LocatorResolverTest` stays as the regression suite; new parser, compiler, and plan tests; browser-backed
  semantics tests against `EmbeddedHtmlServer` pages; a differential corpus of real recorded selectors.
