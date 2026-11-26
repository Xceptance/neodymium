# [DEF-20261014-1955] Playwright Selector Evaluator Syntax Error on :text-is Pseudo-Class

- **Status:** `Resolved`
- **Opened:** 2026-10-14 19:55
- **Closed:** 2026-10-14 20:25
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** AI-generated selector `button.size-btn:text-is("L")` in `GermanCheckoutTest` caused a native Chrome `SyntaxError: Failed to execute 'querySelectorAll' on 'Document': 'button.size-btn:text-is("L")' is not a valid selector.`, resulting in locator timeout, unnecessary visual escalation (`VISUAL_RICH`), marker generation, and subsequent coordinate drift test failure.
- **Root Cause:** In `neodymium-core/src/main/resources/ai-scripts/playwright-selector-evaluator.js`, the regex used to strip pseudo-selectors before delegating to `document.querySelectorAll`:
  `.replace(/:text(-is)?\((["']?).*?\1\)/gi, '')`
  contained a capturing group `(-is)?` that shifted the backreference `\1` for quotes. When matching `:text-is("L")`, `\1` matched `-is`, so the quote backreference failed to match `"`, leaving `:text-is("L")` un-stripped in `nativeCss`. Furthermore, `:text-is` had been grouped with general substring `:text(...)` matchers rather than strictly matching exact normalized inner text and preserving matched target elements when child elements exist.
- **Detection Gap ("What did we miss?"):** Prior browser tests in `PlaywrightSelectorEvaluatorBrowserTest` validated `:text(...)`, `:has(...)`, and directional locators (`:right-of`), but lacked dedicated test coverage for `:text-is(...)` and `:exact-text(...)` with quotes and surrounding tag selectors.
- **Resolution:**
  1. Fixed pseudo-class stripping in `playwright-selector-evaluator.js` by separating replacement patterns into dedicated non-capturing replacements (`:text-is(...)`, `:exact-text(...)`, `:has-text-is(...)`, `:text(...)`).
  2. Disentangled `exactTextMatch` from general `textMatch` to enforce exact text equality `normalizeText(text) === targetText`.
  3. Preserved element matches when `nativeCss !== '*'` so tag-scoped selectors like `button:text-is("Submit Order")` return the button even when its text is wrapped in an inner child element (e.g. `<span>`).
- **Safety Net Added:** Added automated tests in `PlaywrightSelectorEvaluatorBrowserTest.java`:
  - `testTextIsExactMatchExcludesSubstrings()`: validates exact matching on `button.size-btn:text-is("L")` vs `"XL"` with double quotes, single quotes, and `:exact-text`.
  - `testTextIsWithChildWrapper()`: validates exact matching when text resides in child `<span>`.
