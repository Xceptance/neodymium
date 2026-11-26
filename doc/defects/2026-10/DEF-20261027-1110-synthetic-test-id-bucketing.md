# [DEF-20261027-1110] Synthetic test id bucketing rewrites authentic DOM attributes to data-testid

- **Status:** `Resolved`
- **Opened:** 2026-10-27 10:30
- **Closed:** 2026-10-27 11:15
- **Component:** `neodymium-core`
- **Scope:** `Framework & Test/Harness`
- **Symptom:** Captured DOM output in `PageAnalyzer` unconditionally mapped `data-test`, `data-qa`, and `data-cy` into a synthetic `data-testid` attribute. Downstream LLM interactions, tool executions, and locator improvers consequently generated selectors like `[data-testid='order-btn']` which failed on the real browser DOM where only `data-qa="order-btn"` existed. Additionally, Playwright selector evaluator lacked support for `data-qa` and `data-cy` selector engines.
- **Root Cause:** In `PageAnalyzer.java` (lines ~504-515), `getPrimaryTestId` collapsed `data-testid`, `data-test`, `data-qa`, and `data-cy` into a single `dataTestId` key on the node map. During DOM string formatting (`formatElementNode`), the serializer emitted `appendAttribute(dom, "data-testid", dataTestId)`, masking the authentic attribute name. Furthermore, `LocatorImprover`, `SelenideLocatorProber`, and `PlaybookToolReplayer` only treated `data-testid` and `data-test` as Tier-1 test IDs, leaving `data-qa` and `data-cy` unranked or misclassified.
- **Detection Gap ("What did we miss?"):** The regression test `PageAnalyzerTest.testDataTestIdAttributeRendering` previously had an explicit assertion `assertTrue(standardDom.contains("data-testid=\"close-banner-btn\""), "STANDARD DOM must render data-testid mapped from data-test attribute")`. This codified and protected the synthetic attribute rewriting bug as expected framework behavior.
- **Resolution:**
  1. Updated `PageAnalyzer.java` extraction script with `getTestAttributes(el)` to preserve `testAttrs` containing each present test attribute (`data-testid`, `data-test-id`, `data-test`, `data-qa`, `data-cy`) verbatim without synthetic rewriting.
  2. Updated `formatElementNode` and `formatElement` to render each authentic test attribute as present on the element (`data-qa="order-btn"`, `data-cy="search"`).
  3. Added `DATA_QA` and `DATA_CY` selector engines to `SelectorEngine.java`, `PlaywrightSelectorParser.java`, and `playwright-selector-evaluator.js`.
  4. Updated `LocatorImprover.java` and `SelenideLocatorProber.java` to promote `data-test-id`, `data-qa`, and `data-cy` to Tier-1 test ID priority (score 10).
  5. Updated `PlaybookToolReplayer.java` to preserve genuine test attribute names during healed selector resolution.
  6. Created GitLab Issue #100 (`[neo] Preserve authentic test attributes in DOM extraction without synthetic data-testid bucketing`) documenting the architectural shift and reminding the team to discuss configurable priority and selector engine aliasing.
- **Safety Net Added:**
  - `PageAnalyzerTest.testDataTestIdAttributeRendering` updated to verify authentic attribute preservation (`data-test`, `data-qa`, `data-cy`) and assert absence of synthetic `data-testid` rewriting.
  - `LocatorImproverTest.testAuthenticTestIdAttributesScoringAndCandidates` added covering all 5 test attributes.
  - `HealedSelectorResolutionTest.testAuthenticTestAttributesPreservedInReplaySelector` added covering healed selector reconstruction for authentic attributes.
