# [DEF-20261010-1200] assert_text Fails on Literal Newline Escapes and query_dom Combinators

- **Status:** `Resolved`
- **Opened:** 2026-10-10 12:00
- **Closed:** 2026-10-11 17:35
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Live recording execution `CartTest.liveAllDataSets` on storefront dataset `bad` failed with `Expected text/pattern "CART\n1" was not found on selector "#cart-btn-anchor" within 3000ms.` despite the button rendering "CART 1" with a badge count of 1. Step 3 additionally consumed 14 turns and 75k tokens attempting to locate elements via `query_dom` using Playwright-style chained locators.
- **Root Cause:**
  1. In Chromium, `el.getText()` returns text separated by real newline characters (`\n`, ASCII 10). `query_dom` serialized this into JSON with `"text":"CART\n1"`. When the LLM formulated the subsequent `assert_text` tool call, it emitted `"expectedText":"CART\\n1"` with double-escaped backslashes. Jackson parsed this into a 7-character Java string containing a literal backslash `\` (ASCII 92) followed by `n`. In `BrowserToolProvider.java`, `unescapeLiteralText` only stripped regex metacharacters, leaving `\n` unescaped. `matchesCandidate` failed both substring matching and `\s+` whitespace collapsing because `\` and `n` are not whitespace characters.
  2. In `PlaybookToolReplayer.java`, `candidateMatchesAssertionExpectation` duplicated text assertion matching with an independent, divergent implementation that lacked regex pattern cleaning and literal unescaping, causing locator healing during replay to reject valid candidates.
  3. `BrowserToolProvider.createQueryDomTool` executed `document.querySelectorAll(sel)` directly in browser JavaScript. Standard browser engines throw a `DOMException: SyntaxError` on Playwright combinators (`>>`, `nth=0`) recommended by Neodymium system prompts. Because `query_dom` caught the exception and returned `matchCount: 0`, the LLM was misled into believing elements were missing and entered a costly retry loop.
- **Detection Gap ("What did we miss?"):** Existing unit tests for `assert_text` tested either single-line text or pre-unescaped Java string literals containing real `\n` (ASCII 10). None tested JSON-deserialized tool parameters containing escaped sequences (`"\\n"`). `query_dom` tests only tested standard CSS locators without Playwright-style chains.
- **Resolution:**
  1. Implemented progressive 3-tier matching in `BrowserToolProvider.matchesCandidate(String, String, boolean, boolean)`:
     - Tier 1: Direct literal match preserving raw literals, Windows paths (`C:\new\test.txt`), and exact strings.
     - Tier 2: Escape-interpreted match converting literal `\n`, `\r`, `\t` into actual whitespace characters for multiline checks.
     - Tier 3: Whitespace-collapsed fallback normalizing whitespace and literal escape sequences to single space.
  2. Made `BrowserToolProvider.matchesCandidate` public static and refactored `PlaybookToolReplayer.candidateMatchesAssertionExpectation` to delegate directly to it, ensuring unified assertion matching across recording, execution, and locator healing.
  3. Enhanced `assert_text` failure AssertionError to inspect target elements and include a preview of actual text (`(Actual element text: "...")`).
  4. Updated `query_dom` to detect complex selectors (`>>`, `:has-text`, `:contains`, `text=`, `//`, `xpath=`) and pre-resolve elements via `safeResolveLocator` in Java, with an automatic fallback if native CSS querying returns zero matches.
- **Safety Net Added:** Added automated unit regression tests to `BrowserToolProviderStabilityTest.java`:
  - `testMatchesCandidateProgressiveMatchingLiteralEscapeAndWhitespace()`: Verifies exact and substring matches across Tiers 1, 2, and 3, literal Windows path preservation, and regex modes.
  - `testAssertTextFailureMessageIncludesActualElementTextPreview()`: Verifies that assertion failure messages report actual element text.
  - `testQueryDomResolvesComplexChainedSelectorViaPreResolvedElements()`: Verifies that complex chained selectors (`>>`, `nth=0`) resolve via Java `safeResolveLocator` and pass live `WebElement`s into JavaScript extraction without syntax errors.
