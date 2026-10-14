# Design

## Context

See [proposal.md](proposal.md) for motivation and defect history (specifically [DEF-20261013-1642](../../doc/defects/2026-10/DEF-20261013-1642-regex-lexed-selector-resolver-leaks.md)).

Currently, `LocatorResolver` in `neodymium-core` converts selector strings using sequential regular expressions and string replacements. This causes cross-token leaks (e.g., `.html` in `[href="index.html"]` being parsed as a class `.html`), silent dropping of unhandled pseudo-classes, and inability to execute compound Playwright selectors like `>>` chains with text or role filters.

The system requires:
1. Universal, domain-neutral selector resolution adhering to Playwright selector specifications.
2. Transparent open Shadow DOM traversal for all CSS, text, role, and attribute locators by default.
3. Compatibility with Selenium 4 and Selenide through the standard `org.openqa.selenium.By` contract.
4. Fast execution without adding external dependencies (pure Java + browser JavaScript).

## Goals / Non-Goals

**Goals:**
- **Quote- and structure-aware Lexer and Parser**: Fully parse Playwright selector syntax (CSS, `>>` chaining, `*` capture, `text=`, `role=`, `has-text=`, `id=`, `data-testid=`, `internal:*`, and Neodymium shorthands like `#xc...` / `data-ai=`).
- **Always-on Open Shadow DOM Search**: Every CSS, text, role, and attribute selector pierces open shadow roots recursively in document order without manual host specification, matching Playwright behavior.
- **Accurate Semantics**: Support `:has-text()`, `:text()`, `:text-is()`, `:text-matches()`, `:visible`, `:has()`, `:is()`, `:where()`, `:not()`, `:nth-match()`, layout pseudos (`:left-of()`, etc.), and ARIA computed roles/accessible names.
- **Deterministic Error Handling**: Reject invalid syntax and unknown pseudo-classes with an `InvalidSelectorException` containing token name, column position, and suggested remedies. Discontinue silent fallbacks to plain page text searches.
- **Single Facade & Execution Contract**: `LocatorResolver.resolveLocator(String)` returns a custom `PlaywrightBy` (or native `By` when provably identical without shadow DOM or extensions) usable by Selenide, Selenium `WebDriver`, and DOM discovery tools.

**Non-Goals:**
- Closed Shadow DOM traversal (not accessible via standard DOM APIs).
- WebDriver BiDi / CDP-specific selector engines (must remain 100% compatible with classic WebDriver).
- Emulating full layout engines in Java (layout pseudos use browser `getBoundingClientRect`).
- Frame piercing via `internal:control=enter-frame` (explicit frame switching is required).

## Decisions

### Decision 1: Hybrid Architecture (Java Parser + In-Browser JS Selector Evaluator)
- **Choice**: Parse and validate the selector tree in Java; execute complex selectors and Shadow DOM traversal via a consolidated, sandboxed JavaScript evaluator function (`PlaywrightSelectorEvaluator.js`) executed in browser scope via `JavascriptExecutor`.
- **Rationale**:
  - Pure Java traversal using Selenium remote calls (`findElement`, `getShadowRoot`) incurs severe HTTP round-trip latency (tens to hundreds of network calls per query for deep or chained selectors).
  - A browser-side evaluator can traverse DOM and open shadow roots, inspect `textContent`, evaluate computed styles (`visibility`, `display`), calculate bounding boxes for layout pseudos, and compute ARIA roles in a single WebDriver call (1-3 ms).
  - Performing parsing in Java ensures strict upfront validation, exact syntax error reporting, and safe rewriting of Neodymium-specific tokens (`#xc...` -> `[data-ai=...]`) before browser execution.
- **Alternatives Considered**:
  - *Pure Java WebDriver traversal*: Rejected due to unacceptable latency on complex pages with multiple shadow roots.
  - *Full Node.js/Playwright bridge*: Rejected because Neodymium must remain a self-contained, lightweight Java library without Node.js runtime requirements.

### Decision 2: Shadow DOM Traversal Model
- **Choice**: Recursive document-order traversal across light DOM and all open `shadowRoot`s.
- **Details**:
  - The evaluator scans the target context (default `document`). For every element, if `element.shadowRoot` exists (open mode), its tree is traversed recursively.
  - Elements returned from shadow roots are sorted according to host tree position, ensuring stable document order.
  - XPath expressions bypass the evaluator and execute via native `By.xpath` / `document.evaluate`, which strictly do not pierce shadow DOM, preserving Playwright and W3C specifications.
- **Alternatives Considered**:
  - *Opt-in shadow DOM search*: Rejected; user requirement explicitly mandates "search it always, like Playwright".

### Decision 3: Custom Selenium `By` Implementation (`PlaywrightBy`)
- **Choice**: Implement `public final class PlaywrightBy extends By`.
- **Details**:
  - `PlaywrightBy` implements `List<WebElement> findElements(SearchContext context)`.
  - If `context` is a `WrapsDriver`, extracts the underlying `WebDriver` and invokes `((JavascriptExecutor) driver).executeScript(...)`.
  - For simple, light-DOM-safe native selectors where shadow roots do not exist, a fast-path can compile directly to native `By.cssSelector` or `By.xpath`. However, if the page may contain shadow DOM, `PlaywrightBy` delegates to the evaluator to guarantee shadow elements are not missed.
- **Alternatives Considered**:
  - *Rewriting selectors into complex XPath*: Rejected; XPath cannot pierce shadow DOM, cannot handle computed styles or ARIA roles, and regex-to-XPath conversion was the root cause of repeated defects.

### Decision 4: Playwright Text and Role Matching Engine
- **Choice**:
  - **Text**: Match DOM `textContent` (with normalized whitespace and NBSP conversion), supporting case-insensitive substring (default for unquoted), exact case-sensitive (quoted), and regex (`/.../`). Select smallest containing element for `text=` / `:text()`, and all matching ancestors for `:has-text()`.
  - **Role**: Evaluate implicit HTML tag semantics (e.g. `<button>`, `<input type="submit">`, `<h1>`-`<h6>`), explicit `role="..."` attributes, and accessible name computation (`aria-label`, `aria-labelledby`, inner text, placeholder, or title). Exclude hidden elements unless `[include-hidden]` is specified.
- **Alternatives Considered**:
  - *Approximating roles with XPath tag lists*: Rejected; misses ARIA overrides, dynamic roles, and computed accessible names.

### Decision 5: Error Handling and Fallback Elimination
- **Choice**: Throw `InvalidSelectorException` immediately on syntax errors or unsupported constructs.
- **Details**:
  - Remove fallback routines in `SelenideElementFinder` that fell back to searching raw selector strings as text when an `InvalidSelectorException` was caught.
  - Provide actionable error messages: `"Invalid selector 'button:invalid-pseudo': unknown pseudo-class ':invalid-pseudo' at position 7. Did you mean ':has-text()' or ':visible'?"`.

## Risks / Trade-offs

- **[Risk]** Evaluator JS script execution overhead on very large DOMs.
  - *Mitigation*: Cache the compiled JS script string; query light DOM first and only recurse into shadow roots if elements with `.shadowRoot` are detected.
- **[Risk]** Existing playbooks with malformed selectors that previously passed due to silent token dropping may now fail.
  - *Mitigation*: The failure is intentional and reports an actionable error; agent prompt instructions are updated to guide correct Playwright syntax generation.
- **[Risk]** Context switching: `SearchContext` passed to `By.findElements` might be a `WebElement` instead of `WebDriver`.
  - *Mitigation*: `PlaywrightBy` supports scoped evaluation relative to the provided `SearchContext` root element.

## Migration Plan

1. **Parser & AST**: Add `PlaywrightSelectorLexer`, `PlaywrightSelectorParser`, and AST classes in package `org.neodymium.ai.executor.selenide.selector`.
2. **Evaluator**: Create `PlaywrightSelectorEvaluator.js` resource containing shadow-DOM, text, role, and pseudo-class logic.
3. **Execution**: Implement `PlaywrightBy` implementing `org.openqa.selenium.By`.
4. **Integration**: Refactor `LocatorResolver` to delegate to `PlaywrightSelectorParser` and `PlaywrightBy`.
5. **Cleanup**: Remove legacy regex parsing methods and shadow-DOM fallbacks in `SelenideElementFinder`.
6. **Verification**: Run `LocatorResolverTest`, `LocatorResolverBrowserTest`, and end-to-end tests against `EmbeddedHtmlServer`.
