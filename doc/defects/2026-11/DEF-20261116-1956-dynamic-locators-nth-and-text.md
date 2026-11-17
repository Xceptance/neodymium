# [DEF-20261116-1956] Recorded AI locators overly static for ordinals and dynamic container items

- **Status:** `Resolved`
- **Opened:** 2026-11-16 19:56
- **Closed:** 2026-11-16 20:14
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** AI tests targeting dynamic elements such as "the nth element", "the third item in list", or asserting sorted list order recorded static element IDs (e.g. `#cherry`) or classes (e.g. `.lastItem`) instead of parent-scoped positional selectors (e.g. `.myList div:nth-of-type(3)`), causing replay to pass even when sorting broke. Similarly, steps targeting table rows by dynamic parameter (e.g. `tr[title="${name}"] td.price`) had container scoping stripped down to un-scoped selectors (e.g. `tr td.price`), causing replay failures when row order changed.
- **Root Cause:** 
  1. *Prompt Rules:* `quality-judge-prompt.md` (Rule 4) forbade dynamic values and text inside locators, explicitly prompting the judge to strip dynamic title and text attributes. `selenide-locator-rule.md` and `AgentToolLoopStep.java` gave blanket preference to IDs and semantic attributes while discouraging `:nth-child` and raw DOM positions without carving out an explicit requirement for commanded ordinals and container-scoped parameters.
  2. *Locator Scoring & Candidate Generation:* `LocatorImprover.scoreLocator` unconditionally scored all positional pseudo-classes as fragile (score 2) and did not recognize `[title=...]` as a standard resilient attribute (giving it score 5). Because score was `< 6`, `QualityJudgeToolInterceptor` treated valid unique positional locators as volatile, generated candidate alternatives from the DOM element (e.g. `#cherry` with ID weight 90), and heuristic or LLM judge deliberation hijacked the positional locator in favor of `#cherry`. Furthermore, `generateCandidates` did not inspect element `title` attributes or ancestor row/item containers (`tr`, `li`, `[role='row']`), preventing generation of container-scoped locators like `tr[title='...'] td.price`.
  3. *Locator Improver:* `LocatorImprover.improveLocator` lacked ordinal awareness and upgraded positional selectors to static element IDs.
- **Detection Gap ("What did we miss?"):** Prior tests only evaluated whether locator improvement succeeded in upgrading un-anchored synthetic or anonymous CSS-in-JS soup selectors to IDs. No regression tests checked whether an intentional positional locator commanded by an ordinal instruction (e.g. verifying list sort order) was protected from candidate hijacking, nor did tests verify container-scoped candidate generation and `title` attribute resilience scoring.
- **Resolution:**
  1. Updated `selenide-locator-rule.md`, `quality-judge-prompt.md`, `quality-judge-discussion-prompt.md`, and `AgentToolLoopStep.java` to mandate parent-scoped positional selectors (`:nth-of-type(N)`, `:nth-child(N)`, `>> nth=N`) when instructions command ordinals or sorted order, and mandated container scoping (`tr[title="${name}"] td.price`, `tr:has-text("${name}") >> td.price`) for dynamic parameters while keeping expected assertion text in `chosenValue`.
  2. Added `LocatorImprover.isPositional(...)` and `LocatorImprover.isOrdinalInstruction(...)`.
  3. Added `[title=...]` to `LocatorImprover.scoreLocator` (score 8) and `QualityJudgeToolInterceptor.determineStrategy` (`TITLE`, weight 70).
  4. Extended `LocatorImprover.generateCandidates` to extract `title` attributes and generate ancestor container-scoped candidates (`tr[title='...'] td.price`, `tr[data-testid='...'] td.price`, `#rowId td.price`).
  5. Updated `QualityJudgeToolInterceptor` to fast-path unique positional selectors when an ordinal instruction is present, avoid generating volatile candidate alternatives for them, and added `isPositionalHijack(...)` checks to reject non-positional candidates in consensus, refinement, exhaustion fallback, and heuristic deliberation.
  6. Updated `LocatorImprover.improveLocator` to preserve positional selectors when an ordinal instruction is present.
- **Safety Net Added:**
  - `QualityJudgeToolInterceptorTest.testOrdinalInstructionPreservesUniquePositionalSelectorInDom`
  - `QualityJudgeToolInterceptorTest.testPositionalHijackRejectedWhenOrdinalCommanded`
  - `QualityJudgeToolInterceptorTest.testContainerScopedLocatorWithTitleAllowedImmediately`
  - `LocatorImproverTest.testIsPositional`
  - `LocatorImproverTest.testIsOrdinalInstruction`
  - `LocatorImproverTest.testScoreLocator_titleAttribute`
  - `LocatorImproverTest.testGenerateCandidates_titleAttribute`
  - `LocatorImproverTest.testGenerateCandidates_ancestorContainerScoping`
  - `LocatorImproverTest.testImproveLocator_preservesPositionalLocatorsForOrdinals`
  - `LocatorImproverTest.testImproveLocator_preservesPositionalLocatorsWhenInstructionOmitted`
