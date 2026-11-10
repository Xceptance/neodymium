# [DEF-20261002-17] Embedded Text Pseudo-Class Parsing Failure in LocatorResolver and Confabulated Visual RCA Diagnosis

- **Status:** Resolved
- **Opened:** 2026-10-02
- **Closed:** 2026-10-02
- **Component:** `neodymium-core` (`LocatorResolver`, `VisualRcaPrompt`, `VisualRcaStep`, `visual-rca-prompt.md`)
- **Scope:** `Framework`
- **Symptom:**
  1. Automated test failure: `InvalidSelectorException: Compound class names not permitted / invalid selector` when executing CSS descendant selector containing pseudo-class `:has-text(...)` followed by child elements (e.g. `div[data-ai="xckdm47x"] .flex.justify-between:has-text("Subtotal") span:nth-child(2)`).
  2. Confabulated Visual RCA diagnosis: When asserting expected subtotal `$31.98`, the vision model hallucinated that the cart contained previous items from SauceDemo (`Sauce Labs Backpack at $29.99` and `Sauce Labs Bike Light at $9.99`) summing to an aggregate subtotal of `$71.96`, despite the screenshot clearly displaying only the two T-shirts and the exact `$31.98` subtotal on Verla Store.
- **Root Cause:**
  1. `LocatorResolver.PLAYWRIGHT_PSEUDO_PATTERN` relied on end-of-string anchoring `\(((?:[^()]|\"[^\"]*\"|'[^']*')*)\)$`, which failed to match when pseudo-classes were embedded in descendant chains. Furthermore, its regex group greedily matched across trailing selector segments containing parentheses (like `:nth-child(2)`). As a result, the selector fell through to `By.cssSelector()`, which failed in WebDriver because `:has-text()` is not standard CSS. Additionally, regex class/attribute extraction on segments containing pseudo-text with punctuation (e.g. `v1.0 ($15.99)`) corrupted class and attribute matching.
  2. `VisualRcaPrompt` and `visual-rca-prompt.md` suffered from premise bias: the prompt phrased failure as an unquestioned premise ("Expected text '$31.98' was not found"). The model assumed the subtotal was wrong, and seeing `$15.99` triggered training memories of SauceDemo's `$15.99` Bolt T-Shirt, leading the model to hallucinate other standard SauceDemo products (`$29.99` and `$9.99`) and confabulate an arithmetic explanation to justify the supposed failure. Furthermore, the RCA prompt lacked SUT grounding (current page URL and page title).
- **Detection Gap ("What did we miss?"):**
  1. `LocatorResolverTest` only tested `:has-text(...)` at the very end of selectors (e.g. `button:has-text("Submit")`) or chained via `>>` (e.g. `table >> tr:has-text("Alice") >> button`), not space-delimited descendant chains with trailing segments like `:has-text(...) span:nth-child(2)`.
  2. Visual RCA tests were unit tested using mock responses without verifying zero-premise-bias instructions and SUT URL/title grounding in prompts.
- **Resolution:**
  1. Added `EXTENDED_PSEUDO_PATTERN` to `LocatorResolver` detecting `:has-text`, `:contains`, `:text`, etc. anywhere in the selector.
  2. Hardened argument parsing using balanced-parentheses stripping (`stripBalancedPseudo`), and upgraded `buildSegmentPredicate` to translate embedded `:has-text(...)` and `:text-is(...)` directly into XPath predicates (`contains(normalize-space(.), ...)` and `(normalize-space(.)=... or normalize-space(text())=...)`).
  3. Re-architected `visual-rca-prompt.md` to enforce **Verification First (Zero Premise Bias)**: models must first inspect the screenshot to verify if the expected value is visually present; if so, diagnose an automated locator/selector mismatch rather than SUT defect, and strictly forbids confabulating items from external demo stores (SauceDemo).
  4. Injected `pageUrl` and `pageTitle` into `VisualRcaPrompt` and `VisualRcaStep` to ground multimodal visual analysis in the active SUT context.
- **Safety Net Added:** Added unit tests `testEmbeddedHasTextInDescendantChain`, `testEmbeddedTextPseudoWithPunctuationAndQuotes`, `testEmbeddedExactTextPseudoInDescendantChain`, and `testSpaceDelimitedPseudoEquivalentToChainedLocator` in `LocatorResolverTest.java`, and prompt grounding tests in `VisualRcaPromptTest.java`.
