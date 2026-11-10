# [DEF-20260922-04] Selector Parsing Regressions, Unreachable Fallbacks, and Incomplete Element Matching in LocatorResolver

- **Status:** Resolved
- **Opened:** 2026-09-22
- **Closed:** 2026-09-22
- **Component:** `neodymium-core` (`LocatorResolver`, `LocatorResolverTest`)
- **Scope:** `Framework`
- **Symptom:**
  1. Standard CSS selectors starting with `text:` (such as SVG elements `text:nth-of-type(4)` or pseudo-elements `text::before`) were incorrectly parsed as Playwright text searches (`withText("nth-of-type(4)")`), rendering CSS fallback rules at Step 11 dead and unreachable.
  2. Text selectors using colon delimiters with values containing equals signs (e.g. `text:Status=OK`) were truncated to `OK` because delimiter detection checked `clean.contains("=")` globally across the entire string.
  3. `data-test=value` selectors were mapped to `[data-testid='value']`, failing to find elements with `data-test` attributes in Cypress/Playwright applications.
  4. Chaining bare ARIA roles (e.g. `#toolbar >> role=button`) produced broken XPath `//*[@id='toolbar']//role`, querying for non-existent `<role>` HTML tags.
  5. `label=value` selectors only matched `<input>` elements, ignoring labeled `<select>`, `<textarea>`, and `<button>` elements.
- **Root Cause:**
  1. Step 8 (`lower.startsWith("text:")`) greedily intercepted all `text:` selectors without checking for CSS pseudo-classes or pseudo-elements (`nth-`, `:`, `first-`, `last-`).
  2. Delimiter extraction used `indexOf(clean.contains("=") ? '=' : ':')`, which searched for `=` whenever an `=` appeared anywhere in the text content.
  3. `data-test=` was grouped with `data-testid=` and unconditionally hardcoded to `[data-testid='...']`.
  4. In `resolveChainedLocator`, bare roles returned `ByCssSelector` (`button, input[...]`), causing `roleBy instanceof ByXPath` to evaluate to false and falling into `toXPathSegment("role=button")`, which parsed `"role"` as a tag name.
  5. `label=` shorthand hardcoded `//input` in its XPath union rather than all labelable HTML form controls (`input`, `select`, `textarea`, `button`).
- **Detection Gap ("What did we miss?"):**
  Unit tests in `LocatorResolverTest` suffered from the "Accommodating Test" characterization trap: when tests failed against current behavior, assertions were altered to assert the broken implementation output (e.g. asserting `Selectors.withText("nth-of-type(4)")`) rather than enforcing specification contracts.
- **Resolution:**
  1. In `LocatorResolver`, guarded Step 8 so `text:` selectors followed by CSS pseudo syntax (`nth-`, `:`, `first-`, `last-`) fall through to CSS resolution preserving pseudo-colons (`*::before`, `*:nth-of-type(4)`).
  2. Fixed prefix delimiter detection to inspect whether the matched prefix itself ends in `:` or `=`.
  3. Updated `data-test=` to resolve to `[data-test='...'], [data-testid='...']` to match either attribute.
  4. In `resolveChainedLocator`, added `resolveBareRoleXPath` translating bare role locators into W3C XPath element predicates (`*[self::button or (self::input and (@type='button' or @type='submit')) or @role='button']`).
  5. Expanded `label=` XPath to match all standard labelable form elements (`input`, `select`, `textarea`, `button`).
- **Safety Net Added:**
  Expanded `LocatorResolverTest` with strict contract assertions verifying CSS pseudo-classes, colon-delimiter value preservation with equals signs, `data-test` matching, chained bare roles, and multi-element label resolution.
