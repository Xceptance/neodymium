# [DEF-20261006-0030] Text Pseudo-Selector and Semantic XPath Heuristic Scoring Penalty in LocatorImprover

- **Status:** `Resolved`
- **Opened:** 2026-10-06 00:30
- **Closed:** 2026-10-06 00:35
- **Component:** `neodymium-core` (`LocatorImprover.java`, `QualityJudgeToolInterceptor.java`)
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** During automated test execution (`SearchTest.livePerfect` step 8), `QualityJudgeToolInterceptor` intercepted valid Neodymium text pseudo-selectors (`.plp-sidebar a:text-is("Tops")`) as fragile (`locator quality score: 2/10`). Because the target element lacked ID/name/aria attributes, execution aborted with a soft `RETRY_WITH_FEEDBACK` error, forcing an unnecessary autonomous escalation to visual markers (`mark_elements`), quadrupling token consumption and recording fragile pixel coordinate clicks (`coord:.plp-sidebar@40,164`).
- **Root Cause:** In `LocatorImprover.scoreLocator(String)`:
  `if (trimmed.contains(">") || trimmed.contains(":") || trimmed.startsWith("/")) return 2;`
  The blanket `contains(":")` and `startsWith("/")` check indiscriminately penalized all selectors containing colons or slashes—including Neodymium's first-class text pseudo-classes (`:text-is`, `:exact-text`, `:has-text`, `:contains`), container pseudo-classes (`:has`), semantic XPath expressions (`//tag[normalize-space()='...']`), and attribute strings containing colons (e.g. `[href*="https://"]`)—treating them as fragile structural pseudo-classes (`:nth-child`). Furthermore, `QualityJudgeToolInterceptor` executed this 0ms marker shortcut before allowing the LLM Quality Judge to deliberate even when `@AiJudge(true)` was active, and falsely claimed "no resilient alternative could be generated from the DOM" even when `locatorImprover.enabled=false`.
- **Detection Gap ("What did we miss?"):** `LocatorImproverTest` verified scoring for stable IDs, test IDs, standard attributes, clean classes, synthetic `data-ai`, and complex combinators (`nth-child(2)`, `//xpath`), but lacked tests evaluating text pseudo-classes, semantic XPath expressions, URL attribute values with colons, or Playwright chained locators. In addition, no test existed verifying that `@AiJudge(true)` allows the LLM Judge to deliberate before falling back to visual markers on low-scoring locators.
- **Resolution:** 
  1. Refactored `LocatorImprover.scoreLocator` to:
     - Strip quoted string literals before structural checks so characters like `>`, `:`, `/`, or spaces inside text arguments (e.g. `button:text-is("Next > Step: 1")`) cannot contaminate syntax parsing.
     - Evaluate structural/positional fragility first (`:nth-child`, `:nth-of-type`, `:first-child`, `:last-child`, `>> nth=N`, `[\d+]`, `/html...`, `/body...`), assigning score `2`.
     - Score exact text pseudo-selectors (`:text-is`, `:exact-text`, `:has-text-is`) and exact semantic XPath (`//tag[normalize-space()='...']`) at `7`.
     - Score partial text pseudo-selectors (`:has-text`, `:contains`, `:text`), partial semantic XPath, and clean Playwright relational chaining (`container >> child`) at `6`.
     - Disregard colons inside attribute brackets `[...]` (e.g. `a[href*="https://"]`).
     - Ignore `>` within `:has(...)` containers or Playwright `>>` chaining.
  2. Updated `QualityJudgeToolInterceptor.evaluateCandidateScoring` so that when `@AiJudge(true)` (`isJudgeEnabled()=true`) is active, low-scoring locators route to LLM Judge deliberation first rather than immediately recommending visual markers.
  3. Corrected feedback messages to only claim alternative generation failure when `locatorImprover.enabled=true`.
- **Safety Net Added:** Added comprehensive test cases in `LocatorImproverTest` covering exact text, partial text, semantic XPath, quoted combinators, positional ID edge cases, and attribute colon parsing; added interceptor tests in `QualityJudgeToolInterceptorTest` for judge-before-marker routing and text pseudo-selector allowance.
