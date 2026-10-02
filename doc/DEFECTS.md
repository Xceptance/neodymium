# Defect Log & Post-Mortem Registry

This document tracks diagnosed defects, regressions, and behavioral bugs encountered during development and testing across Neodymium, its test suites, and connected Systems Under Test (SUTs).

The objective is to maintain an actionable learning record: to understand **why** defects occurred, identify detection gaps (**"what did we miss?"**), and ensure safety nets (regression tests, linters, or architectural assertions) prevent recurrence.

---

## Logging Guidelines & Criteria

### When to Log a Defect (In Scope)
- **Framework Regressions & Logic Bugs**: Any semantic bug in Neodymium core, AI engine, state machine, runners, parsers, or reporters.
- **Test Harness / Fixture Failures**: Faulty assertions, broken test setups, incorrect wait conditions, or selector fragility that caused false positives or false negatives.
- **SUT Behavioral Defects**: Confirmed functional, visual, or layout defects detected in the System Under Test (e.g., Verla demo store).
- **Silent Failures or Masked Exceptions**: Situations where errors were swallowed or misclassified.

### When NOT to Log (Out of Scope)
- **Normal TDD Red Phase**: Expected test failures during active test-first development prior to implementing the feature.
- **In-Progress Compilation / Syntax Typos**: Errors resolved during the immediate editing cycle.
- **Transient External Outages**: Temporary network loss, upstream LLM provider 503/429 quota limits, or local OS process termination.

---

## Defect Entry Template

When recording a defect, add a new entry directly under the [Active Defect Records](#active-defect-records) section in **reverse chronological order** (newest entries first).

```markdown
### [DEF-YYYYMMDD-01] Concise Description of Defect
- **Date:** YYYY-MM-DD
- **Component:** `neodymium-core` / `aura-visual` / `playbook-engine` / `verla-fixture` / etc.
- **Scope:** `Framework` | `Test/Harness` | `SUT`
- **Symptom:** Observed failure, error message, or unexpected behavior.
- **Root Cause:** Technical explanation of why the defect occurred.
- **Detection Gap ("What did we miss?"):** Why existing unit tests, linters, or type systems failed to catch this earlier.
- **Resolution:** Summary of code changes made to resolve the issue.
- **Safety Net Added:** Reference to the regression test, assertion, or linter rule preventing recurrence.
```

---

## Active Defect Records

### [DEF-20261002-06] Redundant Viewport Screenshot Re-Capture and Context Inflation on Read-Only Discovery Tools
- **Date:** 2026-10-02
- **Component:** `neodymium-core` (`AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** Multi-turn discovery steps (e.g. `query_dom`, `inspect`, `store`) in standard non-visual instructions consumed tens of thousands of redundant tokens (e.g. ~100k tokens in `CheckoutTest_live_tailwind-by-claude` Step #11, ~81k in Step #1, ~63k in Step #4), rapidly exhausting the test step token budget.
- **Root Cause:**
  1. `AgentToolLoopStep` unconditionally invoked `executor.captureState(ContextLevel.VISUAL, isFullPage)` after every turn and attached the resulting viewport screenshot to subsequent observation turns, even when only read-only discovery/inspection tools (`query_dom`, `inspect`, `request_context`, `store`) were executed.
  2. On multimodal models (Gemini), each unneeded PNG image attachment adds ~2,000–2,500 input tokens per turn, inflating cumulative context by 60k–100k tokens over 5–10 discovery turns on an unchanged page.
  3. `isMutatingTool()` omitted `"scroll"`, causing viewport repositioning actions to be misclassified as non-mutating.
- **Detection Gap ("What did we miss?"):** Existing multi-turn unit tests in `AgentToolLoopStepTest` only verified turn count and mock responses without asserting that non-visual discovery turns omit intermediate screenshot capture and attachments.
- **Resolution:**
  1. Added `"scroll"` to `AgentToolLoopStep.isMutatingTool()`, while ensuring `filterTools()` permits `scroll` for visual assertion repositioning.
  2. Tracked `turnHadMutatingAction` per turn in `executeStep()`.
  3. Gated intermediate `captureState(ContextLevel.VISUAL)` so that viewport screenshots are captured and attached only when the step is visual (`isVisualStep`) or when a mutating action actually executed in the turn (`turnHadMutatingAction`).
  4. Updated turn prompts to inform the model when visual state is unchanged: `"Note: Page visual state is unchanged after discovery/inspection tool execution (viewport screenshot omitted)."`.
- **Safety Net Added:** Added regression tests `testDiscoveryToolOmitsVisualStateCaptureAndAttachments`, `testMutatingActionRetainsVisualStateCaptureAndAttachments`, and `testScrollToolTreatedAsMutatingActionCapturesVisualState` in `AgentToolLoopStepTest.java`.

### [DEF-20261002-05] Visual Assertion Step Derailment via DOM Context Escalation and Runaway Scrolling
- **Date:** 2026-10-02
- **Component:** `neodymium-core` (`AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** In live test runs with visual verification steps (e.g. `SearchGermanTest_live_DE` Step 19), an instruction marked `(visual)` executed 9 scroll actions and invoked `request_context({"level": "STANDARD"})`, escalating context to DOM. Prompt tokens spiked from ~5,500 to >21,000 per turn, burning 115,804 tokens and failing with `TokenBudgetExceededException`.
- **Root Cause:**
  1. `AgentToolLoopStep.filterTools()` omitted mutating tools and DOM assertion tools for pure visual assertions (`isVisualAssertion && !hasInteractive`), but left `request_context` in `availableTools`.
  2. The target element (*Angebot* filter) was cut off below the viewport fold and pinned inside a CSS `position: sticky; top: 100px;` sidebar. Window scrolling (`window.scrollBy`) moved the page content but left the sticky sidebar pinned and cut off in the viewport.
  3. Unable to reveal the element via scrolling, the agent invoked `request_context(level="STANDARD")`, injecting the full DOM tree into a visual verification turn in violation of Rule 5 ("Do not query DOM for visual checks").
  4. Consecutive identical call detection was bypassed by varying `yOffset` on each turn (`300`, `250`, `200`, `350`), permitting runaway scrolling loops.
- **Detection Gap ("What did we miss?"):** `AgentToolLoopStepTest.testVisualAssertionFiltersDomAndMutatingTools` explicitly asserted that `request_context` was present in visual step tools without checking whether requesting DOM levels was prohibited, and no test bounded consecutive scrolling in visual steps.
- **Resolution:**
  1. Excluded `request_context` from `filterTools()` when `isVisualAssertion && !hasInteractive`.
  2. Added a defensive guard against non-screenshot context level escalation via tool requests in pure visual steps.
  3. Clarified the `Visual Inspection Directive` and Rule 5 so the agent concludes the visual evaluation instead of attempting DOM recovery.
  4. Capped consecutive `scroll` interactions during visual assertion steps to 3 attempts, warning at attempt 3 and failing with `ConclusiveFailureException` at attempt 4.
- **Safety Net Added:** Updated `AgentToolLoopStepTest.testVisualAssertionFiltersDomAndMutatingTools` to assert `request_context` is excluded from visual assertion tools, and added regression test `testVisualAssertionExcessiveScrollingTerminatesConclusively`.

### [DEF-20261002-04] Chained Pseudo-Selector Text Stripping and Unsupported Ordinal Selectors in LocatorResolver
- **Date:** 2026-10-02
- **Component:** `neodymium-core` (`LocatorResolver`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:**
  1. Chained selectors with pseudo text filters (e.g. `table >> tr:has-text("Alice") >> button`) lost their text predicate during translation in `toXPathSegment`, resulting in `//table//tr//button` and clicking the first row's button instead of Alice's button.
  2. Playwright ordinal syntax (`>> nth=N`, `:nth-match(...)`, `nth=N`) was either actively blocked with `InvalidSelectorException` or unhandled, preventing natural-language ordinal and relational element targeting in playbooks.
  3. `BrowserToolProvider.query_dom` fallback candidate selector omitted `tr, td, th, li, [role="row"], [role="cell"]`, leaving table and grid cells invisible to text queries unless wrapped in inner spans.
  4. Multi-token CSS selector segments inside chained locators (e.g. `#aria-orders [role="row"] >> nth=2`) treated whitespace as intra-element attribute combinations rather than descendant combinators (`//`), causing queries to search for impossible composite elements (`*[@id='aria-orders' and @role='row']`).
  5. Pseudo-class `:has(...)` and `:not(...)` translation used unbalanced greedy `[^)]+` regexes and failed to translate nested attribute selectors (e.g. `:has([role="cell"])` $\rightarrow$ invalid `descendant::[role="cell"]`).
- **Root Cause:**
  1. `toXPathSegment` invoked `buildPseudoSelectorXpath(cleanSeg, null, false)` with `textVal = null`, discarding `:has-text(...)` argument.
  2. `LocatorResolver` classified `:nth-match()` as an unsupported vendor pseudo-class and lacked parsing for `>> nth=N` segments.
  3. `query_dom` candidate query selector lacked structural table and grid element tags.
  4. `resolveChainedLocator` delegated each chain segment directly to `toXPathSegment` without tokenizing CSS combinators (` `, `>`), improperly treating whitespace as intra-tag selector criteria.
  5. `buildSegmentPredicate` lacked balanced parenthesis extraction and did not delegate nested selector tokens in `:has` / `:not` to `toXPathSegment`.
- **Detection Gap ("What did we miss?"):**
  Existing tests in `LocatorResolverTest` explicitly asserted that `:nth-match` threw an `InvalidSelectorException` rather than implementing translation, no tests checked compound pseudo text selectors inside `>>` chains, and no tests verified multi-token CSS segments combined with `>> nth=N`.
- **Resolution:**
  1. Updated `toXPathSegment` in `LocatorResolver` to parse `PLAYWRIGHT_PSEUDO_PATTERN` and forward extracted `textVal` and `isExact` flag to `buildPseudoSelectorXpath`.
  2. Added support for `>> nth=N` (0-based) and negative indexing (`nth=-1` for last, `nth=-2`, etc.), aliases (`first`, `last`), and translated `:nth-match(sel, N)` to `(//xpath)[N]`.
  3. Expanded `query_dom` fallback candidate query selector in `BrowserToolProvider` to include `tr, td, th, li, [role="row"], [role="cell"]`.
  4. Expanded ARIA role resolution in `LocatorResolver` to support `row`, `cell`, `gridcell`, `table`, and `tab`.
  5. Added `tokenizeCssSelector`, `hasCssCombinator`, and `extractBalancedPseudoArgs` to support multi-token CSS segments and balanced nested `:has(...)` / `:not(...)` expressions with attribute selectors (e.g. `[role="row"]:has([role="cell"])`).
  6. Clarified prompt rules in `selenide-locator-rule.md` to guide agents to target data rows (`tr:has(td)`, `[role="row"]:has([role="cell"])`) rather than including column header rows in ordinal calculations.
- **Safety Net Added:**
  Unit tests in `LocatorResolverTest` (`testPlaywrightNthMatch`, `testChainedOrdinals`, `testChainedPseudoSelectorsPreserveText`, `testChainedMultiTokenCssSegment`, `testChainedAriaDataGridRowWithHasAttribute`, `testHasWithMultipleCommaSeparatedSelectors`, `testNotWithNestedHas`) and comprehensive 29-case live integration test suite in `OrdinalIntegrationTest` (174 test runs passing 100%).

### [DEF-20261002-03] False Positive inViewport Detection and Weak Selectors in query_dom Tool
- **Date:** 2026-10-02
- **Component:** `neodymium-core` (`BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:**
  In `CheckoutTest.live()`, Step #2 ("Validate that 'United States' is shown as the current country context") consumed 11 agent turns and 95,598 tokens. `query_dom` returned matching elements located inside closed modals/overlays (e.g. `visibility: hidden; opacity: 0;`) with `inViewport: true` and generic selectors like `span`. This misled the AI agent into believing the element was actively visible in the current viewport, triggering repetitive inspection loops and locator failures.
- **Root Cause:**
  1. `query_dom` evaluated viewport inclusion purely via layout boundaries (`rect.top < window.innerHeight && rect.bottom > 0 ...`), completely ignoring CSS `display`, `visibility`, `opacity`, and `content-visibility`. Full-screen modal overlays covering `(0, 0, 1200, 800)` satisfied the bounding box checks despite being styled with `visibility: hidden` and `opacity: 0`.
  2. Element visibility was never explicitly evaluated or returned as a boolean property (`visible`) in `query_dom` result payloads.
  3. `selector` generation used naive `tag + idStr + clsStr`, emitting bare tag names (e.g. `span`) when no ID was present and including invalid CSS selector characters (e.g. Tailwind `:` in `hover:bg-gray-100`) without leveraging deterministic `data-ai` automation IDs.
  4. Search results did not prioritize visible elements over hidden elements, and did not inform the agent when all matching elements were hidden inside closed containers.
- **Detection Gap ("What did we miss?"):**
  Existing `query_dom` unit tests (`testBrowserQueryDomAncestorSuppressionAndSorting`) tested ancestor suppression and literal text matching against simple visible DOM trees, but did not test hidden elements (`visibility: hidden`, `display: none`, `opacity: 0`) or verify accurate boolean flags (`visible`, `inViewport`) and selector enrichment.
- **Resolution:**
  1. Implemented comprehensive `isVisible(node)` checking in `query_dom`'s injected JavaScript using modern `node.checkVisibility({ checkOpacity: true, checkVisibilityCSS: true })` with recursive ancestor computed-style fallback checking `display`, `visibility`, and `opacity < 0.01`.
  2. Gated `inViewport` calculation so that `inViewport = vis && inRect`, guaranteeing invisible elements never report `inViewport: true`.
  3. Enriched result objects with explicit `visible: vis` and `dataAi: autoId || null` fields.
  4. Prioritized visible elements over hidden elements in candidate sorting (`visB - visA`).
  5. Enhanced selector construction: uses `tag#id` if valid ID exists, falls back to `tag[data-ai="..."]` if automation ID exists, and sanitizes class names to prevent invalid CSS selectors with colons or slashes.
  6. Added diagnostic `note` when all matching elements are hidden: `"All X matching element(s) are currently hidden (visible=false). Verify if a parent dropdown, menu, or modal needs to be opened first"`.
- **Safety Net Added:**
  Added unit and regression test `testBrowserQueryDomAccurateVisibilityAndDataAiSelector` in `BrowserToolsTest.java` verifying that elements with `visibility: hidden`, `opacity: 0`, and `display: none` return `visible: false` and `inViewport: false`, that `data-ai` selectors are properly constructed, and that the diagnostic hidden note is returned.

### [DEF-20261002-02] Replay Failure on Assertion Tools Due to Missing Automation ID DOM Stamping
- **Date:** 2026-10-02
- **Component:** `neodymium-core` (`BrowserToolProvider`, `SelenideElementFinder`, `LocatorResolver`)
- **Scope:** `Framework`
- **Symptom:**
  In `CheckoutTest.replay()` for dataset `tailwind-by-claude`, replay failed at step #15 ("Verify that the order summary shows the $31.98.") with `AssertionError: Expected text/pattern "$31.98" was not found on selector "[data-ai="xcrxcvi"]" within 3000ms.` The visual RCA erroneously diagnosed that the order summary showed $29.98 instead of $31.98, whereas viewport screenshots confirmed the page rendered $31.98 correctly.
- **Root Cause:**
  1. During live recording, `AgentToolLoopStep` stamped deterministic automation IDs (`data-ai="xc..."`) onto the live DOM via `PageAnalyzer.captureSimplifiedDom(ContextLevel.STANDARD)` on every agent turn. The LLM emitted `assert_text(selector="[data-ai=\"xcrxcvi\"]", text="31.98")`.
  2. In replay mode, the test navigated from `cart.html` to `checkout.html` via `click` and `wait_for_condition(type="url_matches")`. No interaction tool (`click`, `fill`) was invoked on `checkout.html` before the assertion.
  3. Interaction tools resolve elements via `SelenideElementFinder.findElement`, which triggers on-demand DOM stamping via `tryResolveAutomationId`. However, assertion tools (`assert_text`, `assert_count`, `assert_element_state`, `assert_attribute`) directly invoked `BrowserToolProvider.findElements` and `BrowserToolProvider.resolveLazyElement`. Both methods delegated to `LocatorResolver.resolveLocator` which merely translated CSS/XPath syntax without checking if the page was stamped with `data-ai`. As a result, `[data-ai="..."]` matched 0 elements in the unstamped DOM and timed out after 3,000ms.
  4. Additionally, `assert_text`, `assert_element_state`, and `assert_attribute` did not capture and attach `domFeatureVector` upon success during live execution, leaving the recorded playbook action without a feature vector and preventing `PlaybookToolReplayer` from performing cascade healing.
  5. Furthermore, `LocatorResolver` did not transform `#xc...` into `[data-ai='...']`, leaving `#xc...` resolving as HTML element `id` instead of synthetic `data-ai`.
- **Detection Gap ("What did we miss?"):**
  Previous unit tests for `assert_text` tested plain text assertions or mocked elements where `data-ai` was already present. There were no replay integration tests asserting on newly navigated pages targeting automation IDs before any user interaction occurred.
- **Resolution:**
  1. In `SelenideElementFinder`, introduced `isAutomationIdSelector(target)` and `ensureAutomationIdsStampedIfNeeded(target)` to dynamically stamp `data-ai` via `PageAnalyzer` whenever an automation ID (`[data-ai=...]`, `#xc...`, `xc...`) is targeted on an unstamped page or after a URL change.
  2. Updated `BrowserToolProvider.findElements(selector)` and `BrowserToolProvider.resolveLazyElement(selector)` to ensure automation IDs are stamped on demand before evaluating Selenide collections or lazy element proxies.
  3. Updated `LocatorResolver` and `SelenideElementFinder.resolveLocator` to normalize `#xc...` and bare `xc...` selectors into `[data-ai='...']`.
  4. Updated `BrowserToolProvider.assert_text`, `assert_element_state`, and `assert_attribute` to extract and attach `domFeatureVector` to the tool call result so recorded playbooks preserve feature vectors for cascade healing.
- **Safety Net Added:**
  Added unit tests in `LocatorResolverTest`: `testAutomationAndTestIdAttributes` asserting `#xc...` and `xc...` resolve to `[data-ai='...']`. Added unit tests in `SelenideElementFinderTest`: `testIsAutomationIdSelector` and `testResolveLocatorTransformsHashAutomationId`. Added regression test in `BrowserToolsTest`: `testAssertTextOnUnstampedPageWithAutomationIdTriggersStamping` verifying that calling `assert_text` with an automation ID on an unstamped page dynamically triggers stamping and succeeds. Verified full end-to-end replay passes in `CheckoutTest.replay()` with 0 LLM calls and 0 tokens.

### [DEF-20261002-01] Premature TokenBudgetExceededException on Completing Turns & Dead includeAncestors in query_dom
- **Date:** 2026-10-02
- **Component:** `neodymium-core` (`AgentToolLoopStep`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:**
  In `CheckoutTest.live()`, step #2 ("Validate that United States as country is selected.") failed with `TokenBudgetExceededException` (104,443 tokens consumed vs 100,000 budget limit) at turn 11 right after the agent had successfully verified the country and issued `complete_step`. Additionally, the agent struggled over multiple turns to locate the country picker container because `query_dom`'s `includeAncestors` parameter was dead code.
- **Root Cause:**
  1. `AgentToolLoopStep` evaluated `stepCumulativeTokens > this.maxTokens` immediately upon receiving the LLM response without inspecting whether the response proposed completing the step (`complete_step`, `include`, or single-shot action). Even though turn 11 achieved the goal and issued `complete_step`, it was aborted before executing the completion tool call.
  2. In `BrowserToolProvider`, `query_dom` declared `includeAncestors` in its JSON schema, but never passed the parameter to the client-side JavaScript `queryScript` and never traversed ancestors in DOM query results.
- **Detection Gap ("What did we miss?"):**
  Unit tests in `AgentToolLoopStepTest` tested token budget exhaustion with an ongoing action (`token_action`) rather than testing a turn that proposes `complete_step` slightly above the budget. `BrowserToolsTest` verified basic `query_dom` results without testing the `includeAncestors` parameter.
- **Resolution:**
  1. In `AgentToolLoopStep`, deferred immediate token budget exceptions for turns proposing completion (`complete_step`, `include`, or single-shot action), granting a 20% grace limit (e.g. up to 120k for a 100k budget) to execute completion. If the step fails to complete or is rejected, the budget exception is enforced at the end of the turn before proceeding to any subsequent turns.
  2. In `BrowserToolProvider`, wired `includeAncestors` into `queryScript`, traversing up parent nodes up to `ancestorLevels` with deduplication and 500-char `outerHtml` output.
- **Safety Net Added:**
  Added unit tests in `AgentToolLoopStepTest`: `testStepTokenBudgetGraceAllowedForCompletingTurn`, `testStepTokenBudgetGraceExceededEvenIfProposesCompletion`, and `testStepTokenBudgetGraceEnforcedIfCompletingTurnFailsToComplete`. Added unit test in `BrowserToolsTest` verifying `includeAncestors: 1` hierarchy traversal.

### [DEF-20261001-10] Color Wireframe CSS Invisibility & Action Step Replay State
- **Date:** 2026-10-01
- **Component:** `neodymium-core` (`PageAnalyzer`, `ExecuteActionsStep`, `VisualBaselineGateStep`, `PlaybookStep`)
- **Scope:** `Framework`
- **Symptom:**
  1. Live browser wireframe stylesheet rendered invisible text and transparent image cutouts rather than brand-colored skeleton bars and gray media boxes.
  2. Action steps with `(layout)` failed replay due to comparing un-wireframed post-action screenshots against wireframed baselines.
  3. `(visual-full)` syntax was not recognized as a full-page directive.
- **Root Cause:**
  1. Setting `color: transparent !important;` forces `currentColor` to evaluate to `transparent`, hiding backgrounds. Setting `visibility: hidden !important;` suppresses box painting.
  2. `ExecuteActionsStep` captured post-step state without wireframe for layout steps.
  3. `VISUAL_FULL_PATTERN` only matched colon syntax `(visual: full)`.
- **Detection Gap ("What did we miss?"):** Unit tests in `VisualBaselineGateStepTest` used synthetic in-memory `BufferedImage` mocks rather than evaluating injected CSS in real browser execution.
- **Resolution:**
  Updated `neodymium-color-wireframe.js` with `-webkit-text-fill-color: transparent !important;` and `object-position: -99999px !important;`. Updated `ExecuteActionsStep` and `VisualBaselineGateStep` to wireframe layout steps during post-action capture. Added `(visual-full)` / `(layout-full)` aliases to `PlaybookStep`.
- **Safety Net Added:**
  Created `VisualAndLayoutIntegrationTest.java` in `src/test/java/org/neodymium/ai/integration/live/` verifying `(visual)`, `(visual-full)`, and `(layout)` in live headless Chrome.

### [DEF-20261001-09] Hidden Select Native Click Cascading Retries and Rapid Re-stamping Bottleneck Replay Latency
- **Date:** 2026-10-01
- **Component:** `neodymium-core` (`BrowserToolProvider`, `SelenideElementFinder`)
- **Scope:** `Framework`
- **Symptom:** In test cases with styled/hidden `<select>` elements (e.g., Select2 dropdowns in `SpaLocatorTest`), dropdown selection in replay mode incurred ~9.2 seconds of delay per select action. Concurrently, asynchronous modal mounting caused `tryResolveAutomationId` to execute heavy full-DOM serialization (`captureSimplifiedDom`) on every 100ms poll tick, adding ~10-13 seconds of latency.
- **Root Cause:**
  1. `BrowserToolProvider.selectDropdownOption` attempted standard Selenide click-based option selection (`selectOption`, `selectOptionByValue`, `selectOptionContainingText`) even when the target `<select>` element was visually hidden by CSS. When ChromeDriver failed with `ElementNotInteractableException`, Selenide waited up to `Configuration.timeout` (4,000ms) per method before cascading into two additional 4,000ms retry fallbacks.
  2. `SelenideElementFinder.tryResolveAutomationId` unconditionally re-analyzed and stamped the entire DOM via `PageAnalyzer.captureSimplifiedDom(ContextLevel.STANDARD)` whenever an automation ID was not immediately matched, invoking heavy DOM serialization up to 10 times per second during async modal appearance.
  3. `SelenideElementFinder` contained an SUT-specific class check (`select2-hidden-accessible`) violating framework neutrality.
- **Detection Gap ("What did we miss?"):** Existing stability tests in `BrowserToolProviderStabilityTest` used mock drivers that simulated synchronous option click completion without modeling ChromeDriver's `ElementNotInteractableException` timeout retry loops on hidden elements. Furthermore, no polling benchmark measured the CPU and serialization overhead of unthrottled `captureSimplifiedDom` calls during element polling.
- **Resolution:**
  1. Enhanced `BrowserToolProvider.selectDropdownOption` to inspect element visibility in JavaScript. For visually hidden or non-interactable selects, it immediately updates `selectedIndex`, `selected`, `value`, and dispatches standard W3C `input` and `change` events in ~1ms without blocking.
  2. Added non-blocking DOM event fallback (`applyDomSelectFallback`) if native selection throws an exception, eliminating cascading 4-second timeout retries.
  3. Throttled `captureSimplifiedDom` in `SelenideElementFinder.tryResolveAutomationId` to a minimum interval of 1200ms per URL, preventing high-frequency DOM re-stamping while keeping 100ms lightweight CSS polling.
  4. Removed the SUT-specific `select2-hidden-accessible` class check from `SelenideElementFinder`, universally supporting any `<select>` element.
- **Safety Net Added:**
  - `BrowserToolProviderStabilityTest.testSelectToolFastPathForHiddenSelectElement`: Verifies that hidden select elements immediately execute W3C DOM dispatch via `domSelected` without timeout delay.
  - `SelenideElementFinderTest.testResetDomStampCacheForTesting`: Verifies DOM stamping cache reset capability.

### [DEF-20261001-08] Unresolved Variable Placeholders Silently Preserved Due to Lenient Step-Level Resolution
- **Date:** 2026-10-01
- **Component:** `neodymium-core` (`ExecuteActionsStep`, `VisualBaselineGateStep`, `SessionData`)
- **Scope:** `Framework`
- **Symptom:** A playbook step referencing an unmapped variable (`${cartName}` instead of `${cartHeadline}`) passed silently in replay mode because lenient variable resolution left the placeholder intact and visual baseline dHash bypassed validation.
- **Root Cause:** PlaybookStep instructions were resolved with lenient `resolveAvailableVariables()` at step execution dispatch, preserving unresolvable placeholders rather than invoking strict `resolveVariables()`. Visual baseline comparison subsequently matched the rendered page and short-circuited execution without checking variable integrity.
- **Detection Gap ("What did we miss?"):** Existing test `testLeafStepWithRuntimeVariablesPreservesPlaceholdersWithoutFailing` explicitly encoded and tested the lenient behavior, and no test verified that executing a leaf step with an unresolvable variable throws `UnresolvableVariableException` prior to visual baseline gating.
- **Resolution:**
  1. Enforced strict resolution (`sessionData.resolveVariables()`) at leaf step execution dispatch in `ExecuteActionsStep`.
  2. Preserved template immutability on `PlaybookStep.instruction` (stored resolved text strictly in `ExecutionContext.KEY_CURRENT_INSTRUCTION`), ensuring JSON recordings and multi-dataset iterations are not corrupted.
  3. Simplified downstream helpers (`VisualBaselineGateStep`) to consume `ExecutionContext.KEY_CURRENT_INSTRUCTION` directly instead of redundantly re-resolving variables.
- **Safety Net Added:** Comprehensive unit tests in `ExecuteActionsStepTest`:
  - `testLeafStepWithUnresolvableVariableThrowsExceptionAndPreservesTemplate`: Asserts that missing variables throw `UnresolvableVariableException` and keep the original template pristine.
  - `testLeafStepWithUnresolvableVariableFailsBeforeVisualBaselineGating`: Asserts that visual steps with unresolvable variables fail before visual baseline gating, preventing false-positive test bypass.
  - `testLeafStepWithMissingRuntimeVariableFailsStrictly`: Asserts strict failure on missing runtime variables, succeeded once runtime variable is provided.
  - `testMultiDataSetSequentialExecutionPreservesTemplateIsolation`: Asserts that executing the exact same `PlaybookStep` across sequential dataset iterations preserves template immutability and isolates resolved instructions without cross-contamination.
  - `testUnrolledSubStepWithUnresolvableVariableFailsStrictlyOnChild`: Asserts that when a parent step with recorded sub-steps is unrolled in replay mode, an unmapped placeholder on child 2 strictly fails with `UnresolvableVariableException` after child 1 executes.
  - `testStateMachineRunnerFailsConclusivelyOnUnresolvableVariable`: Asserts that `StateMachineRunner` propagates `UnresolvableVariableException` directly and marks failing playbook step status as `FAILED`.

### [DEF-20261001-07] Ephemeral Verification Modals/Dropdowns Leak Across Steps Blocking Subsequent Actions
- **Date:** 2026-10-01
- **Component:** `neodymium-core` (`AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** During live execution of CheckoutTest (e.g. `CheckoutTest_live_tailwind_20261001-220223.html`), verification steps like Step 2 ("Validate that United States as country is selected.") opened ephemeral modals/dropdowns to assert the selected list item and immediately called `complete_step` without closing them. In subsequent steps (such as Step 6: "Go to the cart using the mini cart"), the lingering modal and its backdrop blocked interaction with navigation elements, forcing the agent to spend 7 turns, 26 seconds, and over 60,000 tokens recovering.
- **Root Cause:**
  1. System Prompt Rule 4 did not instruct the agent to inspect active trigger/header elements first before expanding menus, nor did it mandate restoring initial page state (closing ephemeral modals/dropdowns/overlays) before invoking `complete_step`.
  2. Turn prompts for verification steps did not remind the agent to close opened dialogs/overlays prior to step completion.
- **Detection Gap ("What did we miss?"):** Existing tests in `AgentToolLoopStepTest` verified that assertion tools were called before `complete_step`, but never validated instructions preventing state leakage of ephemeral overlays into subsequent steps.
- **Resolution:**
  1. Hardened System Prompt Rule 4 in `AgentToolLoopStep`: Added explicit instructions to first check active trigger/header elements directly without opening menus, and mandated restoring initial page state (closing modals, dialogs, overlays, or dropdowns) before calling `complete_step`. Strictly forbade invoking `complete_step` while ephemeral dialogs or backdrops remain open.
  2. Enhanced Turn prompts in `AgentToolLoopStep` (both full DOM and visual screenshot branches) to explicitly remind the agent: *"ensure any opened modal or dropdown is closed to restore initial page state before completing"*.
- **Safety Net Added:** Regression unit test `AgentToolLoopStepTest.testVerificationStepSystemPromptInstructsModalClosureAndStateRestoration` verifying that both System Prompt Rule 4 and turn prompts explicitly enforce modal closure and state restoration before step completion.

### [DEF-20261001-06] Inflexible Context Level Syntax in PlaybookStep Causes Silent Fallback to Full DOM
- **Date:** 2026-10-01
- **Component:** `neodymium-core` (`PlaybookStep`, `ContextLevel`, `AgentToolLoopStep`, `ExecuteActionsStep`)
- **Scope:** `Framework`
- **Symptom:** Step annotations using concise syntax such as `(context: lean)` or `(context: none)` were silently ignored by `PlaybookStep` because `CONTEXT_LEVEL_PATTERN` only matched the longer `contextlevel` prefix. The step silently fell back to the project default `STANDARD`, incurring unwanted ~25k token full-DOM dumps.
- **Root Cause:**
  1. `PlaybookStep.CONTEXT_LEVEL_PATTERN` only matched `contextlevel[:=]` and did not accept `context[:=]`.
  2. `ContextLevel.fromString` did not recognize `NONE` or `ZERO` as synonyms for zero-DOM `HINT`.
  3. `AgentToolLoopStep` did not check `step.getContextLevel()` directly as the primary authority over transient state.
- **Detection Gap ("What did we miss?"):** `PlaybookStepTest` only tested `(contextlevel=...)`, missing the shorter and more ergonomic `(context: ...)` syntax.
- **Resolution:**
  1. Updated `PlaybookStep.CONTEXT_LEVEL_PATTERN` to support both `contextlevel` and `context` with `:` or `=`.
  2. Added `NONE`, `ZERO`, `NODOM`, and `NO_DOM` mappings to `ContextLevel.HINT` in `ContextLevel.fromString`.
  3. Enforced explicit `step.getContextLevel()` precedence in `AgentToolLoopStep` and `ExecuteActionsStep`.
- **Safety Net Added:** Unit tests in `PlaybookStepTest` (`testContextLevelTagParsing`), `ContextLevelTest` (`testFromStringParsing`), and `AgentToolLoopStepTest` (`testExplicitStepContextLevelOverridesConfiguredDefaultAndTransientState`, `testExplicitStepContextNoneResolvesToHintZeroDom`).

### [DEF-20261001-05] Strict Replay Incurs Massive Latency from Pre-Execution Feature Vector Scans and Dropdown Timeout Traps
- **Date:** 2026-10-01
- **Component:** `neodymium-core` (`PlaybookToolReplayer`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** Strict replay (`REPLAY_STRICT`) of small 13-step test cases required ~92–94s despite 0 LLM calls ($0.00 cost).
- **Root Cause:**
  1. `PlaybookToolReplayer.attemptHealing` was executed unconditionally during replay, even in `REPLAY_STRICT`. When `isDirectlyPresent` was false or delayed (e.g. async dropdowns, Select2 hidden selects, un-stamped data-ai), it invoked `new PageAnalyzer().extractFeatureVectors(driver)` which crawled the full DOM and forced CSS layout reflows across hundreds of elements.
  2. `BrowserToolProvider`'s `select` tool attempted `selectOptionByValue` before `selectOptionContainingText`. When the recorded parameter was display text (e.g. "15 Miles") instead of an option value, Selenide blocked for its full 3,000ms timeout before falling back to text.
  3. `assert_count` with `visibleOnly=true` performed sequential `el.isDisplayed()` calls over WebDriver wire for every matching element (25+ elements = ~1.5s IPC wire overhead).
- **Detection Gap ("What did we miss?"):** Existing replay unit tests ran against minimal in-memory mock HTML fixtures (<10 elements) where full-DOM feature extraction took <10ms and select options had matching values and text.
- **Resolution:**
  1. Updated `PlaybookToolReplayer` to completely bypass `attemptHealing` when `mode == ExecutionMode.REPLAY_STRICT` or when healing is disabled.
  2. Updated `BrowserToolProvider` `select` tool with `selectDropdownOption` inspecting options via JavaScript in <1ms to avoid blocking on Selenide's full timeout when distinguishing between option value and visible text.
  3. Replaced sequential `el.isDisplayed()` wire calls in `assert_count` with a single batch JavaScript visibility count.
- **Safety Net Added:** Unit tests in `PlaybookToolReplayTest` (`testReplayStrictBypassesHealingEvenWithLiveCandidatesAndFeatureVectors`) verifying `REPLAY_STRICT` bypasses `attemptHealing`, and `BrowserToolProviderStabilityTest` (`testAssertCountBatchVisibilityUsesJavaScriptExecutorWhenAvailable`) verifying batch visibility script execution.

### [DEF-20261001-04] Strict Replay Suffers Massive Latency Penalty Due to Redundant DOM Serialization and Routine Visual Captures
- **Date:** 2026-10-01
- **Component:** `neodymium-core` (`ExecuteActionsStep`, `PlaybookStep`, `AiConfiguration`)
- **Scope:** `Framework`
- **Symptom:** Replaying test playbooks in `REPLAY_STRICT` mode took nearly as long as initial LLM recording runs (e.g. 125s vs 171s), despite 100% LLM bypass ($0.00 cost, 0 LLM calls).
- **Root Cause:**
  1. `ExecuteActionsStep` unconditionally executed `executor.captureState(ContextLevel.STANDARD)` before every step (originally intended for data-ai stamping and healing fallback). In `REPLAY_STRICT`, healing is disabled (`mode.supportsHealing() == false`), and locators targeting automation IDs (`[data-ai="..."]`) are stamped dynamically on-demand by `SelenideElementFinder.tryResolveAutomationId` if absent.
  2. `ExecuteActionsStep` unconditionally executed `executor.captureState(ContextLevel.VISUAL)` after every single step (for preliminary HTML report screenshot attachments). Routine action steps during strict replay do not perform visual regression gating, wasting 1.5s–2.0s per step on DOM tree serialization and DevTools screenshot fallbacks.
  3. `PlaybookStep.isVisualStep()` only checked instruction text patterns, omitting `Boolean.TRUE.equals(this.fullPage)`. This created architectural inconsistency where `isFullPageVisualStep()` was true but `isVisualStep()` was false, forcing call-sites to defensively write `isVisualStep() || isFullPageVisualStep()`.
- **Detection Gap ("What did we miss?"):** Unit tests for `ExecuteActionsStep` used in-memory `MockTargetExecutor` where `captureState` completes instantaneously without WebDriver screenshotting, DevTools fallback, or full DOM tree serialization.
- **Resolution:**
  1. Updated `PlaybookStep.isVisualStep()` to check `Boolean.TRUE.equals(this.fullPage)` first, ensuring every full-page visual step is guaranteed to be a visual step and centralizing visual classification.
  2. Added `neodymium.ai.replay.leanStateCapture` (default: `true`) and `neodymium.ai.replay.captureScreenshots` (default: `false` in `REPLAY_STRICT`) in `AiConfiguration`.
  3. Updated `ExecuteActionsStep` to bypass pre-step DOM traversal during replay when lean state capture is active or healing is unsupported, while lazily capturing state in `HealingRequiredException` if healing is ever triggered.
  4. Conditioned post-action visual capture on `isVisualRequired` (`step.isVisualStep() || isSemanticVerificationEnabled() || !isReplay || isReplayScreenshotCaptureEnabled(mode)`), eliminating routine screenshots on passing non-visual replay steps while preserving visual baseline gating and failure state capture in `StateMachineRunner`.
- **Safety Net Added:** Added `testReplayStrictNonVisualStepBypassesStateCapture` and `testReplayStrictVisualStepCapturesVisualState` in `ExecuteActionsStepTest.java`, and verified `isVisualStep` consistency in `PlaybookStepFullPagePersistenceTest.java`.

### [DEF-20261001-03] False-Positive Failure in `assert_element_state` on Multi-Candidate Selectors with Inactive Leading Elements (e.g. Slick Carousels)
- **Date:** 2026-10-01
- **Component:** `neodymium-core` (`BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** AI validation step asserting visibility of elements using class selectors (e.g. `.c-product-recommendations .product-tile`) failed with `Element should be visible ... Actual value: hidden` even though multiple matching elements were prominently displayed on the page.
- **Root Cause:** `BrowserToolProvider.createAssertElementStateTool()` resolved targets via `resolveLazyElement(selector)` which maps strictly to `$(selector)` (the first element in DOM order). In carousel components such as Slick (`slick-slider`), element 0 (`#slick-slide00`) is often an inactive or cloned slide with `display: none` (`displayed:false`), while subsequent elements (`#slick-slide01` etc.) are actively displayed. Unlike `assert_text` which iterates over matching candidates, `assert_element_state` tested only element 0 without checking whether other candidates matched the asserted state.
- **Detection Gap ("What did we miss?"):** `BrowserToolProviderStabilityTest` tested `assert_element_state` only on single-element locators (`#readonly-input`), never on selectors matching multiple elements where index 0 is hidden or inactive.
- **Resolution:**
  1. Updated `BrowserToolProvider.createAssertElementStateTool()`: when the initial candidate is not visible, check `findElements(selector).filter(Condition.visible)` and reassign `el` to the visible candidate before assertion, or fall back to collection polling via `findBy(Condition.visible)` in `case "visible"`.
  2. For negative assertions (`state: "hidden"` / `absent`), verify that no candidate matching the selector is currently visible before passing.
  3. Added `"displayed"` normalization to `"visible"` in `BrowserToolProvider.normalizeElementState()`.
- **Safety Net Added:** Added `testAssertElementStateMultiCandidateVisibilityFallback` and `normalizeElementState` assertions in `BrowserToolProviderStabilityTest.java`.

### [DEF-20261001-02] Playbook Replayer Stalls on Select2 Hidden Select Elements and Unrestricted Full-DOM Self-Healing on Optional/Async Steps
- **Date:** 2026-10-01
- **Component:** `neodymium-core` (`BrowserToolProvider`, `SelenideElementFinder`, `PlaybookToolReplayer`)
- **Scope:** `Framework`
- **Symptom:** Playbook execution in `REPLAY_STRICT` mode experiences multi-second stalling (up to 20 seconds per step) on interactive elements, specifically when selecting options in dropdowns styled with Select2 (e.g. `#radius` in `SpaLocatorTest`), checking optional elements that may not be present (e.g. dismissible cookie consent modals), or selecting asynchronous autocomplete popups.
- **Root Cause:**
  1. `BrowserToolProvider.createSelectTool` strictly enforced `el = findElement(selector).shouldBe(Condition.visible).shouldBe(Condition.enabled)`. Modern UI widget libraries (like Select2, Chosen, and accessible form styling) hide standard HTML `<select>` elements (`display: none` or `.select2-hidden-accessible` with 1x1 dimensions) behind a customized `<div>` wrapper while delegating option values to the underlying `<select>`. Enforcing Selenide `Condition.visible` forced `findElement` to exhaust full polling timeouts (3,000ms - 5,000ms) before failing or resorting to expensive DOM scans.
  2. `SelenideElementFinder.findDirect` discarded attached `<select>` elements if they were not visible in the light DOM, causing `isDirectlyPresent("#radius")` to return `false` despite the `<select>` being present and interactive in the DOM.
  3. `PlaybookToolReplayer.attemptHealing` triggered heavy full-DOM feature vector extractions (`extractFeatureVectors(driver)`) unconditionally on absent targets, even for optional steps (`step.isOptional()`) like cookie banners or promotional modals where element absence is expected and harmless.
  4. For live pages, `attemptHealing` triggered full-DOM scans immediately without brief async polling for standard CSS/ID locators that were in the process of rendering (e.g. Google Maps dropdowns).
- **Detection Gap ("What did we miss?"):** `BrowserToolProviderStabilityTest` tested `createSelectTool` only with mock `WebElement`s where `isDisplayed() == true`. No unit tests evaluated hidden or Select2-styled `<select>` elements where the `<select>` has class `select2-hidden-accessible` and `isDisplayed() == false`. Integration tests in Verla demo store used native HTML5 `<select>` elements rather than Select2 or Chosen widgets.
- **Resolution:**
  1. Relaxed `createSelectTool` to check `if (found.is(Condition.visible)) { el = found.shouldBe(Condition.enabled); } else { el = found.shouldBe(Condition.exist).shouldBe(Condition.enabled); }`.
  2. Enhanced `SelenideElementFinder.findDirect` to fall back to attached `<select>` elements or elements with class `select2-hidden-accessible` if no visible element was matched.
  3. Added a guard in `PlaybookToolReplayer.attemptHealing` to skip expensive full-DOM extraction if `step != null && step.isOptional()`.
  4. Added brief polling before triggering full-page feature extraction on live drivers for standard CSS/ID locators, while ensuring context-provided `liveCandidates` bypass polling for sub-millisecond offline execution.
- **Safety Net Added:** Added `testSelectToolAllowsHiddenSelectElementsWithSelect2` in `BrowserToolProviderStabilityTest.java` and verified offline sub-millisecond replay in `PlaybookToolReplayTest.java`.

### [DEF-20261001-01] Batched Assertions Forcibly Truncated to Single Call per Turn Causing Step Token Budget Exhaustion
- **Date:** 2026-10-01
- **Component:** `neodymium-core` (`AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** Compound verification steps containing multiple assertions (e.g. verifying 10 header elements in a single step) fail with `Token budget exceeded for step: Total tokens consumed (101110) exceeded configured step token budget (100000)` after thrashing across 11 turns, despite the LLM correctly proposing all assertions in Turn 1.
- **Root Cause:** `AgentToolLoopStep` restricted cohesive batch execution exclusively to form inputs via `isCohesiveFormInputBatch`. When the LLM proposed multiple `assert_*` calls alongside `complete_step`, the engine evaluated the batch as invalid, executed only the first call (`assert_title`), and discarded the remaining 13 calls to "prevent stale DOM errors" (even though assertions are read-only and non-navigating). This forced the agent into single-call turns, triggered DOM amnesia and `query_dom` fallbacks, and exceeded the token budget.
- **Detection Gap ("What did we miss?"):** `AgentToolLoopStepTest.testCohesiveFormInputBatchExecutesAllInputsInSingleTurn` verified cohesive form input batches (`fill`), but there were no tests validating cohesive assertion batches (`assert_*`), and `testBatchedToolCallsTruncatedToSingleActionPerTurn` assumed any non-form batch should be truncated.
- **Resolution:** Replaced `isCohesiveFormInputBatch` with `isCohesiveBatch` in `AgentToolLoopStep.java`, recognizing both form inputs (`isFormInputAction`) and assertions (`isAssertionTool`) as safe batchable operations. Added synthetic `SKIPPED` handling for interrupted batches and updated System Prompt Rule 4 to guide cohesive multi-assertion generation.
- **Safety Net Added:** Added `testCohesiveAssertionBatchExecutesAllAssertionsInSingleTurn` and `testCohesiveAssertionBatchInterruptedOnAssertionFailure` in `AgentToolLoopStepTest.java`.

### [DEF-20261001-15] Variables from Test Data Cannot Be Inserted into Fragment Steps Being Edited Inline
- **Date:** 2026-10-01
- **Component:** `aura-manager` (`Visual Playbook Editor`, `dashboard-editor.js`, `editor.html`)
- **Scope:** `Framework`
- **Symptom:** Variables from test file test data could not be inserted into steps of fragment include cards being edited inside a test file, and inserted variables were not dynamically updated in the include card's variable dropdown with the flag "required from using file".
- **Root Cause:** `insertVariableFromInput` in `dashboard-editor.js` lacked active nested step detection (`nestedStep`), and the editor did not trigger dynamic variable dropdown list refresh (`updateIncludeVarsDropdown`) when editing fragment step lines.
- **Detection Gap ("What did we miss?"):** Existing UI tests focused on main playbook step insertion and pre-existing include tree variables, missing dynamic variable insertion into nested inline fragment steps.
- **Resolution:** Updated `insertVariableFromInput` to check for active nested fragment steps and insert `${varName}` cleanly; introduced `updateIncludeVarsDropdown` to dynamically refresh the "Variables in Include" dropdown with flag "required from using file" (`scope-required active-required`).
- **Safety Net Added:** Added UI test `testInsertVariableFromTestDataIntoInlineIncludeFragment()` in `AuraManagerEditorUiTest.java`.

### [DEF-20261001-14] Warning Banner Remains Visible in Include Card After Saving Non-Existent Include File
- **Date:** 2026-10-01
- **Component:** `aura-manager` (`Visual Playbook Editor`, `dashboard-editor.js`)
- **Scope:** `Test/Harness`
- **Symptom:** The non-existent include file warning banner ("Included file does not exist on disk. Edit steps below and save to create it.") stays visible inside the include card even after saving the created steps to disk.
- **Root Cause:** `saveIncludeInline(cardId)` posted step content to `/api/save` and updated unsaved badges, but did not remove the `.include-warning-banner` DOM node rendered when the card was initially loaded.
- **Detection Gap ("What did we miss?"):** The initial UI test verified toast notification and unsaved badge removal, but did not assert that `.include-warning-banner` was removed after save.
- **Resolution:** Updated `saveIncludeInline(cardId)` in `dashboard-editor.js` to locate and remove `.include-warning-banner` from `treeCard` when the save request succeeds.
- **Safety Net Added:** Added assertion `includeCard.$(".include-warning-banner").shouldNotBe(Condition.visible)` in `AuraManagerEditorUiTest.java`.

### [DEF-20261001-13] Visual Playbook Editor Include Fragment Preview Lacks Step Lines and Add-Step Capability for Non-Existent Includes
- **Date:** 2026-10-01
- **Component:** `aura-manager` (`Visual Playbook Editor`, `editor.html`, `dashboard-editor.js`)
- **Scope:** `Test/Harness`
- **Symptom:** When an include file does not exist on disk, the visual editor preview displays a warning ("Included file does not exist on disk. Edit steps below and save to create it."), but clicking "Edit File" provides no step line or UI mechanism to add a step line, preventing users from creating/editing the missing include.
- **Root Cause:** Non-existent includes render 0 `.nested-editable-step` DOM elements (`includeSteps` is empty). `enableIncludeEdit(cardId)` only attempted to focus `steps[0]`. With 0 steps, no line was focused, and no empty placeholder or add step handler was provided.
- **Detection Gap ("What did we miss?"):** Existing UI tests verified editing existing step fragments (`Child.steps`, `MultiChild.steps`), but lacked test coverage for editing missing/non-existent step fragment files.
- **Resolution:** Added `addNestedStep(cardId)` helper, auto-insertion of an initial step row on `enableIncludeEdit` when steps are empty, an empty steps placeholder in Thymeleaf template, and an "+ Add Step" button to include cards.
- **Safety Net Added:** Added Selenide UI test `testEditNonExistentIncludeFileCreatesStepAndSavesFile` in `AuraManagerEditorUiTest.java`.

### [DEF-20261001-12] Test Run Storage Directory Contains Duplicate Dummy console-execution-1.json Files Beside Higher-Indexed Files
- **Date:** 2026-10-01
- **Component:** `neodymium-core` (`InteractiveConsoleEngine`, `AuraInteractiveService`, `AuraQueueService`)
- **Scope:** `Framework`
- **Symptom:** Run storage folders for subsequent test classes in a batch contain both `console-execution-1.json` (a dummy 0-step fallback file) and `console-execution-3.json` (the actual test log).
- **Root Cause:** `InteractiveConsoleEngine` and `AuraInteractiveService` used global counters across all test classes in a batch run to index `console-execution-*.json` files. Subsequent test classes received indexes > 1 (e.g. 3). Later, `AuraQueueService` checked for index 1 in the class folder, saw it missing, and created a dummy fallback `console-execution-1.json`. Furthermore, `markRunningOrMissingExecutionsAsSkipped` failed to mark existing completed execution snapshots as processed, triggering fallback creation.
- **Detection Gap ("What did we miss?"):** Tests verified multi-dataset indexing within a single test class, but did not assert per-class file indexing boundaries across multi-class batch execution.
- **Resolution:** Refactored `InteractiveConsoleEngine` and `AuraInteractiveService` to map execution indexes per test class folder. Updated `AuraQueueService.markRunningOrMissingExecutionsAsSkipped` to recognize existing execution files with final statuses.
- **Safety Net Added:** Added unit tests verifying per-class execution file indexing in `InteractiveConsoleEngineTest`.

### [DEF-20261001-11] Queue Execution Report Total Duration Displays Multimillion Minutes Due to Unfiltered Zero Timestamps
- **Date:** 2026-10-01
- **Component:** `aura-manager` (`RunReportDto`)
- **Scope:** `Framework`
- **Symptom:** Total execution duration for queue runs (`run_20261001_12564`) showed invalid values such as `29847542 min 38 s` instead of actual wall-clock execution time (~4-5 min).
- **Root Cause:** `RunReportDto.getTotalDurationMs()` evaluated `(maxStartMs - minStartMs) + latestExec.getDurationMs()` without filtering out uninitialized or missing start timestamps (`startMs = 0L`, Jan 1 1970). When an uninitialized execution stub or execution snapshot with `0L` timestamp was present, `minStartMs` was set to `0L`, causing the subtraction `maxStartMs - 0L` to evaluate to the current Epoch timestamp (~1.79x10^12 ms = 29,847,542 minutes).
- **Detection Gap ("What did we miss?"):** Existing unit tests for `RunReportDto` verified total duration only with valid mock timestamps or single executions, missing test coverage for queue runs where some execution snapshots have `0L` start timestamps.
- **Resolution:** Updated `RunReportDto.getTotalDurationMs()` to filter out invalid start timestamps (`startMs <= 0L`) when calculating wall-clock spans and fall back to the sum of test execution durations when valid start timestamps are missing or insufficient.
- **Safety Net Added:** Added unit test `testGetTotalDurationMs_ignoresZeroTimestampAndFallsBackToSum` in `RunReportDtoTest.java`.

### [DEF-20261001-10] Conditional Include Main Step Recorded as Substep in Console Execution Reports
- **Date:** 2026-10-01
- **Component:** `neodymium-core` (`PreliminaryReportListener` / `ExecuteActionsStep` / `InteractiveStateBuilder`)
- **Scope:** `Framework`
- **Symptom:** In `console-execution-*.json` and execution reports for steps with conditional includes (e.g. `Add product to cart:` with child `- If (condition) then _include: ...`), the main step's conditional instruction was recorded as `Substep 0` inside its own `subSteps` array alongside the actual included steps, and `subSteps` of included steps contained nested duplicates of the main step.
- **Root Cause:** When a parent step has a child step containing `_include:`, `PreliminaryReportListener` pre-populated `subSteps` with the conditional instruction as `Substep 0` prior to runtime include expansion. When `ExecuteActionsStep` and `IncludeAction` ran, `stepStats.getSubStats()` contained the container step, causing `mergeStepStats` to overwrite `Substep 0` with the conditional instruction.
- **Detection Gap ("What did we miss?"):** Existing tests for `IncludeAction` verified step execution order and execution results, but did not assert that the report's `subSteps` array excludes the conditional include step itself.
- **Resolution:** Updated `ExecuteActionsStep` to extract effective leaf sub-steps when populating sub-stats, updated `PreliminaryReportListener` to clean up intermediate container/include steps from `subSteps`, and updated `InteractiveStateBuilder` to filter out include container instructions during subStep serialization.
- **Safety Net Added:** Added unit test `testConditionalIncludeSubStepsExcludesMainStep` in `SubStepReportingAndScopingTest.java`.

### [DEF-20260930-14] Complete Stripping of CSS Classes in Non-RICH Context Levels Due to Synthetic `autoId` Checked in `hasSemanticLocator`
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`PageAnalyzer`)
- **Scope:** `Framework`
- **Symptom:** AI verification steps targeting structural containers without IDs or text (e.g. `Verify cart items section is displayed`) fail on Turn 1 because container CSS classes (such as `class="b-basket-content-items"`) are completely omitted from the simplified DOM, forcing the agent into expensive 9-turn discovery loops (`query_dom`, `inspect_element`) taking 22+ seconds.
- **Root Cause:** In commit `af748f3b`, `PageAnalyzer.formatElementNode` and `formatElement` introduced a token optimization to omit presentation classes when an element had a semantic locator (`hasSemanticLocator`). However, `autoId != null` was included in the condition. Because `PageAnalyzer` automatically assigns a synthetic `automationId` (`data-ai="xc..."`) to 100% of all extracted elements in the DOM tree, `autoId != null` evaluated to true for every single container and leaf element. Since `level.includesRichMetadata()` is false for `MINIMAL`, `LEAN`, and `STANDARD`, `appendSanitizedClassAttribute` was never called, stripping CSS classes entirely across all non-RICH context levels.
- **Detection Gap ("What did we miss?"):** `PageAnalyzerTest` asserted element tags (`<table`, `<tr`, `<td`, `<form`), interactive attributes (`data-testid`, `role`, `aria-label`, `href`, `data-ai`), and text strings (`"Company Brand Logo"`), but contained zero assertions validating the presence of `class` attributes on containers. Integration tests asserted overall step success without asserting turn-count efficiency (`turnCount == 1`).
- **Resolution:** Removed `autoId != null` from `hasSemanticLocator` in both `formatElementNode` (line 1237) and `formatElement` (line 1297) of `PageAnalyzer.java`, restoring CSS classes on all elements that lack semantic identifiers (`id`, `name`, `data-testid`, `role`, `aria-label`).
- **Safety Net Added:** Added unit regression test `testContainerClassPreservationWhenAutomationIdPresent` in `PageAnalyzerTest.java` verifying that containers with `automationId` present retain their `class` attribute in `STANDARD` and `LEAN` modes.

### [DEF-20260930-13] High Latency in AI Test Execution Due to Turn 1 Context Starvation, Intercepted Click Retries, and Unchecked Quality Judge
- **Date:** 2026-09-30
- **Component:** AI Engine (`AgentToolLoopStep`, `BrowserToolProvider`, `QualityJudgeToolInterceptor`)
- **Scope:** `Framework`
- **Symptom:** E-commerce test runs (e.g. `AddToCartTest`) take 324s (~5.4 min) for 24 steps; verification steps require 5–8 LLM turns (15s–26s each), and element clicks suffer 12s–23s in browser retry timeouts.
- **Root Cause:**
  1. `AgentToolLoopStep` defaulted Turn 1 context to `ContextLevel.LEAN`, stripping static text leaves (`div`, `span`, `p`, `td`) needed for price/subtotal/total verifications, forcing the LLM into expensive multi-turn `query_dom` / `inspect` exploratory loops.
  2. `QualityJudgeToolInterceptor` defaulted `this.enabled = true` and did not check `config.isJudgeEnabled()`, triggering WebDriver queries, attribute scans, and candidate scoring even when disabled.
  3. `BrowserToolProvider.executeElementClick` incurred Selenide's full retry timeout and disk report attachments upon `ElementClickInterceptedException` before falling back to JavaScript click.
- **Detection Gap ("What did we miss?"):** Existing unit tests mocked LLM tool calls with pre-canned selectors and did not evaluate turn efficiency on static text assertions or measure real-browser timeout cascading on intercepted clicks.
- **Resolution:**
  1. Updated `AgentToolLoopStep` to resolve initial context level via `AiConfiguration.getContextLevel()` (configured to `STANDARD`), ensuring all text content is visible on Turn 1 in a universal, language-agnostic manner.
  2. Bypassed all DOM queries and scoring in `QualityJudgeToolInterceptor.intercept()` when `!config.isJudgeEnabled()`.
  3. Fast-pathed intercepted clicks in `BrowserToolProvider.executeElementClick` directly to JavaScript click.
- **Safety Net Added:** Unit tests asserting `ContextLevel.STANDARD` propagation from configuration, zero DOM queries when Quality Judge is disabled, and fast JavaScript click execution on intercepted elements.

### [DEF-20260930-12] Redundant Duplicate Pre-Step Visual Capture and Blind Settle Sleep in `ExecuteActionsStep`
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`ExecuteActionsStep`)
- **Scope:** `Framework`
- **Symptom:** Every test step transition incurred an average 3.95s dead gap (~90.8s total overhead across 23 transitions) between goal completion and starting the next step.
- **Root Cause:**
  1. `ExecuteActionsStep` captured back-to-back screenshots: post-step visual state capture at the end of Step N (~1.2s) followed immediately by pre-step visual state capture at the start of Step N+1 (~1.0s) across identical, unchanged browser states.
  2. `ExecuteActionsStep` executed an unconditional `Thread.sleep(1000)` post-action settle pause on all mutating steps regardless of whether visual baselines were requested or whether DOM quiescence had already settled.
- **Detection Gap ("What did we miss?"):** End-to-end timing tests only evaluated step-internal durations, failing to track inter-step lifecycle transitions and screenshot redundancy.
- **Resolution:**
  1. Updated `ExecuteActionsStep` to reuse the preceding step's `POST_ACTION_STATE` as the current step's `PRE_ACTION_STATE` when not in full-page capture mode, eliminating duplicate screenshot capture.
  2. Conditioned the full post-action settle sleep on `step.isVisualStep()`, delegating non-visual mutating steps to `DomQuiescenceWatcher.waitForDomQuiet(Duration.ofMillis(200), Duration.ofMillis(50))` to confirm stability without blind multi-second sleep.
- **Safety Net Added:** Unit tests in `ExecuteActionsStepTest` asserting state reuse across step boundaries and quiescence integration.

### [DEF-20260930-11] Eager `SelenideElementFinder` Polling on State Assertions and Blocking `interactable` Timeouts on Click
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** In test executions (such as `AddToCartTest.executeAddToCart`), each `assert_element_state` call took 5.1s–5.3s (~76.5s across 14 assertions) even when elements were already visible, and `click` calls took 11.6s–22.8s on animated/overlay elements (~76.8s across 5 clicks).
- **Root Cause:**
  1. `BrowserToolProvider` resolved assertion targets via `SelenideElementFinder.findElement`, which executes an eager, custom 5,000ms polling loop requiring visible candidate matching rather than utilizing Selenide's native lazy element proxy (`Selenide.$(...)`). When asserting `hidden` or `absent` states, or when elements failed strict pre-visibility filters, it polled until the full 5,000ms timeout before falling back.
  2. `BrowserToolProvider.executeElementClick` asserted `el.shouldBe(Condition.interactable)` prior to click. When overlay wrappers, banners, or CSS animations were present, Selenide blocked for its condition timeout (5,000ms–10,000ms), generated attachment failure dumps to disk, and only then fell back to JavaScript click.
- **Detection Gap ("What did we miss?"):** Unit tests executed against mock WebDrivers or simple static fixtures where elements were instantly interactable and assertions were not timed against live multi-second timeouts.
- **Resolution:**
  1. Added `resolveLazyElement` to `BrowserToolProvider` using standard Selenide `Selenide.$(LocatorResolver.resolveLocator(selector))` for assertions (`assert_element_state`, `assert_attribute`, `assert_text`), delegating condition polling directly to Selenide's `shouldBe`.
  2. Refactored `executeElementClick` to attempt native `el.click()` directly after `shouldBe(Condition.visible)`, catching `ElementClickInterceptedException` / `ElementNotInteractableException` immediately and falling back to JS click in < 50ms without waiting out a multi-second interactable timeout.
- **Safety Net Added:** Unit tests verifying rapid assertion completion and immediate JS click fallback in `BrowserToolsTest`.

### [DEF-20260930-10] Multi-Turn query_dom Overhead Caused by Turn 1 DOM Pruning, Ancestor Container Bubbling, and Missing ContextLevel Overrides
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`ai-tool`, `ai-model`, `ai-pipeline`, `ai-config`)
- **Scope:** `Framework`
- **Symptom:** In test executions (such as `AddToCartTest.executeAddToCart`), verification steps targeting non-interactive elements (e.g. "Verify the estimated total is displayed") took 3 LLM turns and 2 redundant `query_dom` calls. In Turn 1, the DOM was omitted under `LEAN` mode; in Turn 2, `query_dom({"text":"total"})` suffered from container bubbling where outer layout wrappers (`div.page`, `div#maincontent`, etc.) consumed all 10 match slots and pushed the leaf match past the limit; in Turn 3, a narrower query `query_dom({"text":"Estimated Total"})` finally resolved the selector.
- **Root Cause:**
  1. Default `ContextLevel.LEAN` intentionally strips static copy and non-interactive text wrappers to minimize token consumption, leaving the agent without leaf copy targets on Turn 1 when verifying text.
  2. `PlaybookStep` lacked parsing for step-level `(contextlevel=...)` control tags, and `AiConfiguration` lacked a global `neodymium.ai.contextLevel` property to allow suites or steps to select `STANDARD` or `RICH` DOM fidelity when verifying complex copy.
  3. `BrowserToolProvider.query_dom` used a low default limit of 10 elements and traversed DOM nodes in document pre-order without container de-bubbling (suppressing matching ancestors when child elements match) or sorting matches by shortest text length, allowing large outer containers to crowd out leaf matches.
- **Detection Gap ("What did we miss?"):** Previous unit tests for `query_dom` tested mocked elements or small isolated snippets without deeply nested ancestor hierarchies, failing to reveal that pre-order DOM queries match parent containers ahead of leaf nodes and exhaust the return limit.
- **Resolution:**
  1. Implemented step-level `(contextlevel=<level>)` control tag parsing in `PlaybookStep` supporting both `=` and `:` syntax, stripping it from natural language instructions.
  2. Added `neodymium.ai.contextLevel` configuration in `AiConfiguration` with default `LEAN`.
  3. Updated `ExecuteActionsStep` to resolve `initialLevel` from `AiConfiguration` and allow step-level `(contextlevel=...)` tags to take final override precedence.
  4. Enhanced `BrowserToolProvider.query_dom` to suppress ancestor containers when matching descendant elements exist (`cand.contains(other)`), sort surviving matches by text length ascending (placing innermost leaf targets first), and increased the default element limit from 10 to 20.
  5. Updated Operating Rule 2 in `AgentToolLoopStep` to instruct agents to use distinctive multi-word phrases or specific selectors from the instruction when invoking `query_dom`.
  6. Documented `(contextlevel=<level>)` and `neodymium.ai.contextLevel` across `ai.properties`, `doc/DOCUMENTATION.md`, and `doc/AI_EXECUTION_SUMMARY.md`.
- **Safety Net Added:** Added unit test `PlaybookStepTest.testContextLevelTagParsing` for `(contextlevel=...)` variations, `BrowserToolsTest.testBrowserQueryDomToolSchema` verifying limit 20, and `BrowserToolsTest.testBrowserQueryDomAncestorSuppressionAndSorting` asserting ancestor container suppression and leaf-first sorting against nested DOM hierarchies.

### [DEF-20260930-09] Reasoning Effort Hardcoded in Agent Tool Loop and Missing Property Resolution
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-config`)
- **Scope:** `Framework`
- **Symptom:** Setting `neodymium.ai.reasoningEffort` or expecting a configurable thinking level has no effect on execution; agent execution turns always run with hardcoded `ReasoningEffort.LOW`.
- **Root Cause:** In `AgentToolLoopStep.java:463`, `ReasoningEffort.LOW` was hardcoded during `LlmRequest` creation. Furthermore, `AiConfiguration` only read `neodymium.ai.reasoningEffort` as a fallback in `getLinterReasoningEffort()`, lacking general and role-based resolution or support for the intuitive `thinkingLevel` property name.
- **Detection Gap ("What did we miss?"):** Unit tests in `AgentToolLoopStepTest` mocked LLM callers without verifying whether configured or dynamic reasoning effort properties were propagated to outgoing `LlmRequest` instances.
- **Resolution:**
  1. Implemented `getReasoningEffort(String role, ReasoningEffort defaultEffort)` and `getReasoningEffort()` in `AiConfiguration`, supporting role overrides (`neodymium.ai.<role>.reasoningEffort` / `thinkingLevel`), global properties, and alias parsing (`MINIMAL`, `NONE` -> `OFF`).
  2. Updated `AgentToolLoopStep` to dynamically resolve `reasoningEffort` from `AiConfiguration` and session data instead of hardcoding `LOW`.
  3. Updated `PlaybookLinter` to utilize the shared `AiConfiguration.parseReasoningEffort` helper.
  4. Documented the property options in `config/ai.properties`.
- **Safety Net Added:** Added `AiConfigurationTest.testReasoningEffortResolutionAndAliases` testing property hierarchies, role overrides, and alias parsing, and `AgentToolLoopStepTest.testConfiguredReasoningEffortPassedToLlmRequest` verifying effort propagation to `LlmRequest`.

### [DEF-20260930-08] Missing Model Pricing for Gemini 3.8 Flash in MetricsCollector
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`ai-telemetry`)
- **Scope:** `Framework`
- **Symptom:** AI test runs configured with `neodymium.ai.model = gemini-3.8-flash` log a warning (`Unknown model 'gemini-3.8-flash' encountered for cost estimation. Cost calculation skipped (set to $0.00).`) and calculate $0.00 estimated USD cost in reports and session telemetry.
- **Root Cause:** `MetricsCollector.getModelRate` lacked a rate entry for `3.8-flash`.
- **Detection Gap ("What did we miss?"):** Unit tests in `MetricsCollectorTest` only asserted pricing for versions up through `3.7-flash`, allowing newly configured Flash models to silently evaluate to zero cost.
- **Resolution:** Added `3.8-flash` rate resolution ($0.75 input, $3.75 output, $0.1875 cached per 1M tokens) matching `3.7-flash` rates.
- **Safety Net Added:** Added test assertion for `gemini-3.8-flash` in `MetricsCollectorTest.testCostCalculationForSupportedGeminiModels`.

### [DEF-20260930-07] Unpruned DOM in Multi-Turn Verification Observation Causing Token Doubling and Visual Call Bloat
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In execution reports, non-mutating verification steps that required 2 turns (Turn 1 executing `assert_text` or `assert_element_state` and Turn 2 executing `complete_step`) consumed 19,000–35,000 input tokens on Turn 2 instead of ~2,000–5,000 tokens, doubling the token consumption of every verification step.
- **Root Cause:** In `AgentToolLoopStep.java:1132`, `pruneExpiredDomFromConversation` was guarded by `hasFreshDomIncoming = (requireDomForNextTurn || hasMutated)`. When an assertion succeeded without remaining milestones, `hasMutated == false` and `requireDomForNextTurn == false`, causing `pruneExpiredDomFromConversation` to preserve the entire 18,000–34,000 token Turn 1 DOM in conversation history alongside the viewport screenshot attached for Turn 2 visual observation.
- **Detection Gap ("What did we miss?"):** [DEF-20260930-04] prevented Turn 1 DOM pruning when `hasFreshDomIncoming == false` to avoid multi-turn selector amnesia, and unit test `testTurn2PreservesDomWhenNonMutatingToolExecutedAndNoFreshDomIncoming` explicitly enforced retaining the DOM without verifying token efficiency or supporting on-demand DOM queries (`query_dom` / `request_context`).
- **Resolution:**
  1. Updated `AgentToolLoopStep.java` to unconditionally prune expired DOM snapshots from prior turns before entering visual observation turns, reducing Turn 1 user message content to `[Initial page state omitted after Turn 1 — use browser tools for current page state]`.
  2. Updated the Turn 2 observation prompt and System Prompt rules to explicitly notify the agent that prior DOM snapshots are omitted to minimize context, and instructed it to call `request_context` (for full fresh DOM) or `query_dom` (for specific elements) if DOM targeting is needed.
- **Safety Net Added:** Updated `AgentToolLoopStepTest.java` (`testTurn2PrunesDomOnVisualObservationTurnAndAllowsContextRequest`) asserting that Turn 2 prunes the Turn 1 DOM on visual observation turns, and added unit test `testTurn2ContextEscalationViaRequestContext` verifying that calling `request_context` in Turn 2 delivers a fresh DOM snapshot in Turn 3.

### [DEF-20260930-06] Misleading Step Token Budget Exception Message and Missing Step Budget Aliases
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-config`)
- **Scope:** `Framework`
- **Symptom:** When a composite or multi-turn playbook step exceeded the per-step token limit (`neodymium.ai.step.maxTokens`), the exception message reported `Token budget exceeded for test run: Total tokens consumed (...) exceeded configured step token budget (100000). Test run aborted.`. This led users to believe the test-level token budget had failed to pick up custom YAML `_properties:` (or was restricted to 100k despite total stats showing 600k+ tokens consumed). Furthermore, `AiConfiguration.getStepTokenBudget()` lacked intuitive aliases (`neodymium.ai.tokenBudget.step`, `tokenBudget.step`).
- **Root Cause:**
  1. `TokenBudgetExceededException.formatMessage` used a static prefix `"Token budget exceeded for test run:"` regardless of whether the exceeded budget was a test-level budget (`BudgetType.INPUT` / `BudgetType.OUTPUT`) or a per-step budget (`BudgetType.TOTAL`).
  2. `AiConfiguration.getStepTokenBudget()` only resolved the canonical key `neodymium.ai.step.maxTokens` and lacked fallback aliases matching the token budget naming pattern.
- **Detection Gap ("What did we miss?"):** Unit tests in `TokenBudgetGuardTest` and `AgentToolLoopStepTest` only checked that the exception was thrown and that it contained the numeric token limit, without asserting that the message clearly differentiated step-level failures from whole-test aborts.
- **Resolution:**
  1. Differentiated message formatting in `TokenBudgetExceededException.formatMessage`: `BudgetType.TOTAL` now explicitly states `"Token budget exceeded for step: Total tokens consumed (%d) exceeded configured step token budget (%d). Step aborted."`.
  2. Enhanced `AiConfiguration.getStepTokenBudget()` to support fallback aliases: `neodymium.ai.step.maxTokens`, `neodymium.ai.tokenBudget.step`, `neodymium.ai.step.tokenBudget`, and `tokenBudget.step`.
- **Safety Net Added:** Added unit tests in `PropertyPrecedenceOrderTest` (`testStepTokenBudgetAliasesAndOverrides` and `testTokenBudgetExceededExceptionStepMessageFormatting`) validating all aliases, precedence order, and message differentiation.

### [DEF-20260930-05] Missing _properties Block Parsing in YamlPlaybookParser Causing Ignored Playbook Configuration and Token Budget Failures
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`ai-playbook`, `config`, `junit`)
- **Scope:** `Framework`
- **Symptom:** Setting framework configurations (such as `neodymium.ai.tokenBudget.input`) via `_properties:` or `properties:` in YAML playbooks had no effect. AI test runs aborted prematurely with `Token budget exceeded for test run: Input tokens consumed (...) exceeded configured input token budget (500000)`.
- **Root Cause:**
  1. `YamlPlaybookParser.java` omitted parsing for `_properties` and `properties` at both the root map level and inside dataset items, and omitted `_properties|properties` from `YAML_BLOCK_PATTERN`.
  2. For playbooks with `_properties` but without an explicit `data:` block, no default dataset was generated, preventing thread-local variables and `SessionData` from being populated.
  3. `NeodymiumAiRunner.java` and `TokenBudgetGuard.java` used disjoint data stores (`transientData`, annotations) rather than standardizing on `Neodymium.getData()` as the single source of truth for runtime execution settings.
- **Detection Gap ("What did we miss?"):** Existing `YamlPlaybookParserTest` cases tested `data:` and `steps:`, but lacked test cases exercising `_properties:` or `properties:` blocks.
- **Resolution:**
  1. Updated `YamlPlaybookParser.java` to recognize `_properties` and `properties` in `YAML_BLOCK_PATTERN`, flatten nested maps to dotted property keys, propagate root properties to all datasets, and generate a default dataset when properties exist without a `data:` section.
  2. Standardized runtime test configuration on `Neodymium.getData()`: `NeodymiumAiRunner` writes all dataset properties and `@AiContext` overrides directly into `Neodymium.getData()`, and `TokenBudgetGuard` / `AiConfiguration` read directly from it.
  3. Reset thread-local context cleanly in `beforeEach` and `afterEach` via `Neodymium.clearThreadContext()` to ensure zero property bleed across test iterations.
- **Safety Net Added:** Added unit tests in `YamlPlaybookParserTest` for root and dataset-level `_properties` flattening, and in `TokenBudgetGuardTest` for dynamic `Neodymium.getData()` budget resolution.

### [DEF-20260930-04] DOM Amnesia in Multi-Turn Verification and Incomplete Single-Turn Assertion Batching
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In natural language verification steps (e.g. `Validate that United States as country is selected`), tests on inaccessible storefronts failed with `Stop Criterion 2: Assertion failure` due to hallucinated selectors (`img[alt*="United States"]`, `img[src*="us.svg"]`).
- **Root Cause:**
  1. In `AgentToolLoopStep.java:1131`, `pruneExpiredDomFromConversation(conversation)` unconditionally hardcoded `hasFreshDomIncoming = true`, wiping the Turn 1 DOM even when `requireDomForNextTurn` was `false`. Turn 2 was left with `[Initial page state omitted after Turn 1]`, completely blinding the model.
  2. Rule 4 in `AgentToolLoopStep` did not instruct the model to propose `[assertion, complete_step]` in the same turn for single verification checks, leading the LLM to execute partial container visibility checks in Turn 1 and attempt content verification in a blind Turn 2.
- **Detection Gap ("What did we miss?"):** Tests in `AgentToolLoopStepTest` only verified DOM pruning when mutating tools (`mock_click`) were executed, but did not test non-mutating assertion turns without incoming fresh DOM.
- **Resolution:**
  1. Updated `AgentToolLoopStep.java:1131` to preserve the latest DOM snapshot across turns when a non-mutating tool executes without fresh DOM incoming (`pruneExpiredDomFromConversation(conversation, requireDomForNextTurn || hasMutated)`).
  2. Added `[assertion, complete_step]` single-turn completion and direct assertion guidance to Rule 4 in `AgentToolLoopStep`.
- **Safety Net Added:** Added unit test `testTurn2PreservesDomWhenNonMutatingToolExecutedAndNoFreshDomIncoming` in `AgentToolLoopStepTest` verifying that Turn 2 retains the Turn 1 DOM when a non-mutating assertion executes without a fresh DOM snapshot incoming.

### [DEF-20260930-03] BrowserToolProvider Uncaught ElementNotFound/AssertionError in Retry Loops
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`tool/browser/BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** Replay tests failed with `ElementNotFound {#cart-btn-anchor.snapshot(1 elements)[0]} Expected: exist` caused by `StaleElementReferenceException` during `assert_text` evaluation.
- **Root Cause:** `matchesElementText`, `matchesElementOrAssociatedLabel`, `safeGetText`, and the `assert_text` retry loop caught only `java.lang.Exception`. In Selenide, `ElementNotFound` and `UIAssertionError` inherit from `java.lang.AssertionError` (subclass of `Error`), allowing stale/detached elements in dynamic collections to escape unhandled and prematurely abort the retry loop before the timeout.
- **Detection Gap ("What did we miss?"):** Previous unit tests inspected static DOMs without concurrent DOM mutations or stale collection snapshots during assertion retries.
- **Resolution:** Updated `matchesElementText`, `matchesElementOrAssociatedLabel`, `safeGetText`, `resolveElementBySelectorAndText`, and the `assert_text` retry loop in `BrowserToolProvider` to catch `(final Exception | AssertionError ignored)` per the `java_test_exception_handling` knowledge pattern.
- **Safety Net Added:** Verified via `CartTest.livePerfect` and `CartTest.replayPerfect` with dynamic cart badge DOM updates passing end-to-end.

### [DEF-20260930-02] BrowserToolProvider Click/Hover Text Disambiguation & Checkout/Search Fixture Sync Issues
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`tool/browser/BrowserToolProvider` / `verla-playbooks` / `PrelinterRuleMatrixLiveTest`)
- **Scope:** `Framework` & `Test/Harness`
- **Symptom:** 1) `CartTest` (`basic`, `full`, `judge`) failed when the agent called `click(selector="article[...] button", text="S")`: the primary button was clicked instead of the size button, leaving cart count at 0. 2) `SearchGermanTest` failed dropdown assertions when the search form was submitted prematurely on Enter. 3) `CheckoutTest` on `tailwind_by_claude` failed asserting updated order summary subtotal ($31.98) against initial captured subtotal ($15.99). 4) `PrelinterRuleMatrixLiveTest.testVagueVerification_Spanish_Aviation` failed asserting `VAGUE_VERIFICATION` because Spanish phrasing without explicit verification keywords triggered `MISSING_VISUAL_TAG`.
- **Root Cause:** 1) `BrowserToolProvider.executeElementClick` and `executeHover` favored non-blank `selector` exclusively without filtering candidates by `text` when both parameters were present. 2) The instruction `Gib "${searchQuery}" in das Suchfeld ein.` led the LLM to submit the form immediately via `pressEnter: true`. 3) The playbook lacked a variable re-capture step after incrementing quantity. 4) Phrasing "se vea correcto y ordenado" triggered visual appearance rules rather than subjective verification.
- **Detection Gap ("What did we miss?"):** Tool parameter interaction tests did not cover compound selector-plus-text resolution, and multi-language linter matrix phrases were not evaluated against the full pre-flight taxonomy.
- **Resolution:** Implemented `resolveElementBySelectorAndText` and `safeGetText` in `BrowserToolProvider` to search matching visible candidates by text; updated search instruction to `Tippe...`; re-captured `${subtotal}` in the checkout playbook; and refined the Spanish linter prompt to `Compruebe que el plan de vuelo funcione correctamente.`.
- **Safety Net Added:** Verified resolution across all four affected test suites (`CartTest`, `SearchGermanTest`, `CheckoutTest`, `PrelinterRuleMatrixLiveTest`).

### [DEF-20260930-01] PlaybookStep (visual: full) Tag Parsing Inconsistency & VerifyOutcomeStep Unit Test Misconfiguration
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`model` / `runner` / `test-fixtures`)
- **Scope:** `Framework` & `Test/Harness`
- **Symptom:** `PlaybookStepFullPagePersistenceTest` failed with `expected: <null> but was: <true>`; `RunnerIntegrationTest.testVerifyOutcomeStepFailure` failed with `Expected VerificationFailureException to be thrown, but nothing was thrown`; and `ProgrammaticDemoTest.test7` threw `Failed to parse playbook: ...ProgrammaticDemoTest_test7_...yaml`.
- **Root Cause:** 1) `PlaybookStep.setInstruction` automatically parses `(visual: full)` into `this.fullPage = true`, which contradicted an obsolete unit test assertion expecting `null`. 2) `VerifyOutcomeStep` requires `failOnError=true` (or transient configuration `neodymium.ai.semanticVerification.failOnError=true`) to throw `VerificationFailureException` rather than logging a soft warning. 3) The test class was refactored from `VerlaProgrammaticDemoTest` to `ProgrammaticDemoTest`, leaving the convention-based classpath YAML and JSON fixtures mismatched.
- **Detection Gap ("What did we miss?"):** Unit tests and convention-based integration fixtures were not verified in an end-to-end reactor run following the class rename and model tag parser enhancements.
- **Resolution:** Updated assertion in `PlaybookStepFullPagePersistenceTest` to expect `Boolean.TRUE`, configured `neodymium.ai.semanticVerification.failOnError=true` in `RunnerIntegrationTest`, and aligned convention-based YAML and JSON fixture filenames for `ProgrammaticDemoTest`.
- **Safety Net Added:** Verified unit suite passes cleanly with zero failures via `mvn test -pl neodymium-core -Dtest="PlaybookStepFullPagePersistenceTest,RunnerIntegrationTest,ProgrammaticDemoTest#test7*"`.

### [DEF-20260930-21] Successful Executions Overwritten to Failed and SLF4J Warnings Extracted as Process Errors
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`AuraQueueService`)
- **Scope:** `Framework`
- **Symptom:** Successful test executions with completed steps (`totalSteps > 0`) in a multi-dataset batch were overwritten to status `"failed"` with SLF4J warning messages (e.g., `WARN ... BiDiException`) listed as `failureReason`.
- **Root Cause:**
  1. `AuraQueueService.executeQueue` updated all non-failed execution JSONs to `"failed"` when `isFailedRun` was true, missing the `isZeroStep` check to distinguish unexecuted tests from completed successful ones.
  2. `extractSubprocessErrorMessage` checked `line.contains("WARN:")` with a required colon, failing to match SLF4J log lines (`[main] WARN ...`) which omit the colon, allowing lines containing `BiDiException:` to be parsed as error messages.
- **Detection Gap ("What did we miss?"):** Tests did not assert that multi-dataset execution batches containing both a failing test and a passing test retain `"passed"` status for the completed test, nor did tests cover SLF4J `WARN` log formats without trailing colons.
- **Resolution:** Re-enforced `isZeroStep` check in `AuraQueueService` when updating non-failed execution states on `isFailedRun`, and updated `extractSubprocessErrorMessage` to inspect `WARN` and `WARNING` without requiring trailing colons.
- **Safety Net Added:** Added unit tests in `AuraQueueServiceTest.java` for SLF4J `WARN` filtering and zero-step status update bounds.

### [DEF-20260930-20] Dynamically included playbook steps unlinked as sub-steps of active include step
- **Date:** 2026-09-30
- **Component:** `org.neodymium.ai.executor.selenide.plugins.IncludeAction`
- **Scope:** `Framework`
- **Symptom:** Playbook steps dynamically included at runtime via `include(...)` (such as inside conditional `If ... _include:` branches) executed as flat top-level steps on the execution context stack without being linked to the active `include` parent step in execution reports or console execution logs.
- **Root Cause:** In `IncludeAction.java`, parsed steps from included playbooks were mapped to pipeline steps and pushed onto `ExecutionContext.runStack` without setting `subStep.setParent(currentStep)` or registering them under `currentStep.getSubSteps()`.
- **Detection Gap ("What did we miss?"):** Tests for `IncludeAction` verified that included steps executed on the browser, but did not assert that dynamically included steps were attached as `subSteps` of the active `currentStep` in `ExecutionContext` and execution reports.
- **Resolution:** Updated `IncludeAction.java` to retrieve the active `currentStep` from `ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP`, set `subStep.setParent(currentStep)` for each included step, and populate `currentStep.getSubSteps()`.
- **Safety Net Added:** Verified dynamic sub-step linking across `PreliminaryReportListenerTest` and `SubStepReportingAndScopingTest`.

### [DEF-20260930-19] IllegalArgumentException on natural language step list items containing colons or hints
- **Date:** 2026-09-30
- **Component:** `org.neodymium.ai.playbook.YamlPlaybookParser`
- **Scope:** `Framework`
- **Symptom:** Parsing playbooks with natural language YAML list steps containing colons within step text (such as `- Generate random email address (hint: use java method)`) fails with `java.lang.IllegalArgumentException: Invalid playbook step format in file: ... Expected string step, 'include' map, or 'instruction' map, but found map keys: [Generate random email address (hint]`.
- **Root Cause:** SnakeYAML parses list items containing `: ` (`- key: value`) into single-entry `Map` objects. `YamlPlaybookParser.parseStepBlock` rejected single-entry maps whose scalar values did not contain an `include` keyword, failing to recognize natural language step instructions containing colons (such as parenthetical hints `(hint: ...)` or formatted text `Label: text`).
- **Detection Gap ("What did we miss?"):** Unit tests only tested simple string list steps without inline colons or parenthetical hints containing `: `.
- **Resolution:** In `YamlPlaybookParser.java`, updated single-entry map scalar handling in `parseStepBlock` to reconstruct `key + ": " + value` into a full step instruction for any single-entry map in a step list, while maintaining strict `IllegalArgumentException` validation for invalid multi-key step maps.
- **Safety Net Added:** Added unit test `testParseYamlListStepWithParentheticalHintColon()` in `YamlPlaybookParserTest.java` validating natural language steps with colons in parenthetical hints or step text.

### [DEF-20260930-18] IllegalArgumentException on YAML list steps containing colons or inline includes
- **Date:** 2026-09-30
- **Component:** `org.neodymium.ai.playbook.YamlPlaybookParser`
- **Scope:** `Framework`
- **Symptom:** Parsing playbooks containing list steps with colons in step text (such as `- ... and _include: fragment.steps`) fails with `java.lang.IllegalArgumentException: Invalid playbook step format in file: ... Expected string step, 'include' map, or 'instruction' map, but found map keys: [...]`.
- **Root Cause:** SnakeYAML parses list items containing `: ` into single-entry `Map` objects. In `YamlPlaybookParser.parseStepBlock`, single-entry maps were only handled if the value was a `List` or `Map` (sub-steps) or if the key was exact `_include`/`instruction`. Single-entry maps with string/primitive values (resulting from natural language step lines containing colons or inline `_include:`) threw an `IllegalArgumentException`.
- **Detection Gap ("What did we miss?"):** Unit tests in `YamlPlaybookParserTest` did not cover YAML list items containing inline colons (`:`) or inline `_include:` parameters within step text.
- **Resolution:** In `YamlPlaybookParser.java`, extended single-entry map handling in `parseStepBlock` to reconstruct `key + ": " + value` into full step instruction text when the value is scalar (`String`, primitive, or `null`), correctly creating `PlaybookStep` instances or resolving inline include directives.
- **Safety Net Added:** Added unit test `testParseYamlListStepWithInlineColonAndInclude()` in `YamlPlaybookParserTest.java` validating list steps containing inline colons and `_include:` targets.

### [DEF-20260930-17] ClassCastException when parsing playbook fragments with top-level YAML array list
- **Date:** 2026-09-30
- **Component:** `org.neodymium.ai.playbook.YamlPlaybookParser`
- **Scope:** `Framework`
- **Symptom:** Parsing playbooks containing included fragment `.steps` files (or any YAML file structured as a top-level list `- ...`) fails with `java.lang.RuntimeException: Failed to parse playbook: <path> Caused by: java.lang.ClassCastException: class java.util.ArrayList cannot be cast to class java.util.Map`.
- **Root Cause:** In `YamlPlaybookParser.java`, SnakeYAML's `yaml.load(fileContent)` was directly assigned to a `Map<String, Object>` variable. When an included fragment file (such as `checkout-with-paypal.steps` or `proceed-to-payment.steps`) contains a top-level YAML list (`- step1\n- step2`), `yaml.load(fileContent)` returns a `java.util.ArrayList`, causing an unhandled `ClassCastException`.
- **Detection Gap ("What did we miss?"):** Existing `YamlPlaybookParserTest` unit tests only tested YAML files where the top-level structure was a YAML dictionary (e.g., `steps: ...`). There were no test cases for included fragment `.steps` files structured as top-level YAML array lists (`- ...`).
- **Resolution:** Updated `YamlPlaybookParser.java` to capture the output of `yaml.load(fileContent)` as `Object loadedObject`. If `loadedObject` is a `Map<?, ?>`, process standard top-level keys (`data`, `steps`, `before`, `after`). If `loadedObject` is a `List<?>` or `String`, delegate directly to `parseStepBlock`.
- **Safety Net Added:** Added unit test `testParseIncludedStepListFragment()` in `YamlPlaybookParserTest.java` validating recursive inclusion of fragment `.steps` files containing top-level YAML array lists.

### [DEF-20260930-16] Main Page Console (Playbook & Queue) Hangs During Test Execution Under High Log Volume
- **Date:** 2026-09-30
- **Component:** `aura-manager` (`dashboard-runner.js`, `dashboard-styles.css`)
- **Scope:** `Framework`
- **Symptom:** The aura-manager main UI page (playbook & queue) freezes/hangs during test execution when large amounts of stdout/console logs are printed by the test process.
- **Root Cause:** `dashboard-runner.js` invoked `localStorage.setItem('aura_previous_console_logs', terminalConsole.innerHTML)` synchronously on every single streamed log line. As log output grew to thousands of lines, writing multi-megabyte HTML strings synchronously to `localStorage` on the main JS thread dozens/hundreds of times per second blocked the browser event loop. Additionally, per-line unbatched DOM appends (`insertAdjacentHTML`) and layout queries (`innerText`) caused severe browser layout thrashing.
- **Detection Gap ("What did we miss?"):** UI tests did not run stress tests with high-frequency console output streams to measure browser event-loop latency and DOM reflow overhead.
- **Resolution:** Replaced per-line synchronous `localStorage` writes with debounced persistence (`debouncedSaveConsoleLogs`, throttled to 1 second), batched incoming log lines into single-pass DOM HTML appends (`appendLogsBatch`) per polling tick, replaced reflow-triggering `innerText` with `textContent` in filter updates, and added CSS layout containment (`contain: content`) to `#terminalConsole`—preserving 100% of all log lines without truncating output.
- **Safety Net Added:** Updated `dashboard-runner.js` and `dashboard-styles.css` with batch DOM appends, debounced persistence, and `textContent` filtering.

### [DEF-20260930-15] Aura Subprocess Playbook Parse Failures Logged as Passed in Console Execution Reports
- **Date:** 2026-09-30
- **Component:** `neodymium-core` (`AuraQueueService`)
- **Scope:** `Framework`
- **Symptom:** When a test subprocess fails due to playbook parsing errors (e.g., `Failed to parse playbook`), `console-execution-1.json` was retained with status `"passed"` and missing failure message.
- **Root Cause:** `isFailedRun` in `AuraQueueService` did not account for `fileErrors`/`fileFailures` counters, and the execution JSON updater required `isZeroStep` (stepIndex == 0 && totalSteps == 0) to overwrite existing `"passed"` execution states.
- **Detection Gap ("What did we miss?"):** Tests did not assert that existing `console-execution-*.json` files with non-zero step metrics are overridden to `"failed"` with `failureReason` when the batch subprocess fails.
- **Resolution:** Updated `AuraQueueService` to include `fileErrors`/`fileFailures` in `isFailedRun`, relaxed `isZeroStep` restriction when `isFailedRun` is true to force-update non-failed execution states with `failureReason`, and expanded `extractSubprocessErrorMessage` trace parsing.
- **Safety Net Added:** Added unit test `testExtractSubprocessErrorMessageAndExecutionStatusUpdateOnParseError` in `AuraQueueServiceTest.java`.

### [DEF-20260929-08] Multi-Scroll Virtualized List Item Traversal Exceeds Default Step Token Budget
- **Date:** 2026-09-29
- **Component:** `neodymium-core` (`sandbox-tests` / `live-integration` / `VirtualizedListSandboxLiveTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Running `VirtualizedListSandboxLiveTest.testVirtualizedListLive` aborted with `TokenBudgetExceeded: Total tokens consumed (103894) exceeded configured step token budget (100000)`.
- **Root Cause:** Locating dynamically unmounted items in a virtualized DOM feed required multiple scroll-and-inspect cycles, accumulating 103,894 prompt tokens across intermediate DOM snapshots and exceeding the default 100k safety budget.
- **Detection Gap ("What did we miss?"):** The default 100k token guardrail was calibrated for typical forms and standard pages, without configuring an elevated limit for heavy virtualized feed traversal tests.
- **Resolution:** Configured `neodymium.ai.step.maxTokens` to 250,000 via session test data in `setupProperties`.
- **Safety Net Added:** Verified `VirtualizedListSandboxLiveTest` passes cleanly across all modes (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`).

### [DEF-20260929-07] Shadow DOM Test Step Inadvertently Scopes Status Assertion to Component Host
- **Date:** 2026-09-29
- **Component:** `neodymium-core` (`sandbox-tests` / `live-integration` / `ShadowDomSandboxLiveTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Running `ShadowDomSandboxLiveTest.testShadowDomLive` failed with `Expected text/pattern "Login successful for: admin" was not found on selector "#login-form-host #shadow-status" within 3000ms.`
- **Root Cause:** Test step instruction `Verify that #shadow-status shows "Login successful for: admin"` directly followed three steps targeting elements "inside the login form", causing the LLM to scope `#shadow-status` inside `#login-form-host` rather than querying the top-level status span in the main document.
- **Detection Gap ("What did we miss?"):** Mock tests explicitly hardcoded the top-level selector `#shadow-status` in queued responses, masking the contextual bias introduced by the phrasing in the live test prompt.
- **Resolution:** Clarified step instruction in `ShadowDomSandboxLiveTest.java` to `Verify that the page status #shadow-status shows "Login successful for: admin"`.
- **Safety Net Added:** Verified `ShadowDomSandboxLiveTest` passes across all modes (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`).

### [DEF-20260929-06] Live Timeout Fast-Failure Test Flakily Asserts Wall-Clock Network Latency
- **Date:** 2026-09-29
- **Component:** `neodymium-core` (`ai-testing` / `live-integration` / `TimeoutIntegrationTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Running `TimeoutIntegrationTest.testTimeoutFastFailureOnNonExistentElement` failed with `Test should fail fast (under 3 seconds) due to (timeout:50ms) tag, but took 13359 ms ==> expected: <true> but was: <false>`.
- **Root Cause:** The test asserted `duration < 3000ms` on `session.execute` across an entire multi-step scenario communicating with live cloud Gemini APIs over the internet. When the 50ms element lookup timed out as designed, the engine executed Visual RCA and multi-turn error recovery over the network, totaling ~13 seconds. Wall-clock latency under 3 seconds is unrealistic and unstable for live cloud LLM calls.
- **Detection Gap ("What did we miss?"):** The duration check was ported from mock tests where mock responses return in 0ms without network roundtrips.
- **Resolution:** Disabled Visual RCA for the fast-failure test method, asserted `AssertionError` on the missing selector, and adjusted the wall-clock guardrail to a realistic non-hanging limit.
- **Safety Net Added:** Verified `TimeoutIntegrationTest` passes across all live modes.

### [DEF-20260929-05] Optional Failing Step Incorrectly Asserts LLM Invocations in Healing Replay Mode
- **Date:** 2026-09-29
- **Component:** `neodymium-core` (`ai-testing` / `live-integration` / `OptionalIntegrationTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Running `OptionalIntegrationTest.testOptionalFailingStepBypassed` failed in `REPLAY_WITH_HEALING` mode with `org.opentest4j.AssertionFailedError: Expected at least 1 LLM call, but 0 calls were made.`
- **Root Cause:** In `OptionalIntegrationTest.java` line 81, the metrics assertion `.onHealing(m -> m.hasLlmCalls())` expected LLM calls during healing replay. However, for an optional failing step, 0 actions were recorded in the companion JSON during recording. In `REPLAY_WITH_HEALING`, there are no recorded actions to execute and fail, so self-healing is never triggered, resulting in 0 LLM calls.
- **Detection Gap ("What did we miss?"):** The metric assertion was copied from self-healing test cases where steps had broken actions that actively triggered the healing agent loop.
- **Resolution:** Updated the assertion in `OptionalIntegrationTest.java` from `.onHealing(m -> m.hasLlmCalls())` to `.onHealing(m -> m.hasNoLlmCalls())`.
- **Safety Net Added:** Verified `OptionalIntegrationTest` passes across all modes (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`).

### [DEF-20260929-04] Mock Action Type Discrepancy Causes Rich Editor Initial Content Retention
- **Date:** 2026-09-29
- **Component:** `neodymium-core` (`sandbox-tests` / `mock-integration` / `RichEditorSandboxMockTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Running `RichEditorSandboxMockTest.testRichEditorAutonomousTypingAndSave` failed with `Element should have text "Document saved: Autonomous release notes for Q3 2026." {#saved-message}` because the actual text was `Document saved: Initial draft notes for product release.Autonomous release notes for Q3 2026.`.
- **Root Cause:** In `RichEditorSandboxMockTest.java`, the mock LLM action response specified `"action": "TYPE"`, which maps to `browser_type` with append semantics (`clearFirst = false`). In the live agent implementation, typing into an input/editor defaults to `fill` with replace semantics (`clearFirst = true`). Consequently, the mock test retained the initial HTML placeholder text inside `<div id="rich-editor" contenteditable="true">`.
- **Detection Gap ("What did we miss?"):** The mock response was crafted using legacy `TYPE` action terminology without reflecting the modern `fill` default tool behavior executed by live models on rich text inputs.
- **Resolution:** Updated mock action responses in `RichEditorSandboxMockTest.java` from `"action": "TYPE"` to `"action": "FILL"`.
- **Safety Net Added:** Verified `RichEditorSandboxMockTest` passes all 5 tests (100%).

### [DEF-20260929-03] HTML Fixture Title Discrepancy Causes Replay Assertion Mismatch in ForwardIntegrationTest
- **Date:** 2026-09-29
- **Component:** `neodymium-core` (`test-fixtures` / `ai-test-pages` / `ForwardActionTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Executing `ForwardIntegrationTest` failed title assertions during replay or recording when verifying page navigation. The test expected page title to contain "Forward Test Page X", but HTML fixture `<title>` was "Forward Action Test - Page X".
- **Root Cause:** In `ForwardActionTest/page1.html`, `page2.html`, and `page3.html`, the `<title>` tag was set to `Forward Action Test - Page X` while the page `<h1>` and test instructions specified `Forward Test Page X`. Because `assert_title` verifies that the actual document title contains the expected string, the word "Action" prevented substring matching.
- **Detection Gap ("What did we miss?"):** The fixture titles were created before the test assertions were finalized, and mock recordings had recorded the mismatch without reconciling the natural language step requirement with the HTML title tag.
- **Resolution:** Updated `<title>` in `page1.html`, `page2.html`, and `page3.html` to `Forward Action Test - Page X - Forward Test Page X`, satisfying both existing recordings and step assertions.
- **Safety Net Added:** Verified `ForwardIntegrationTest` passes in both mock and live execution modes (20/20 test runs).

### [DEF-20260929-02] Legacy Action Format STORE Parameter Mapping Discrepancy Causes Missing variableName Tool Error
- **Date:** 2026-09-29
- **Component:** `neodymium-core` (`action-mapping` / `browser-tool-provider` / `AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** Executing `StoreIntegrationTest.testStoreMock` failed with `ConclusiveFailure: Action execution failed: {"status":"ERROR","message":"store requires a non-empty 'variableName'","error":"store requires a non-empty 'variableName'"}` during recording, and subsequently failed `REPLAY_STRICT` due to the voided recording file.
- **Root Cause:** Legacy `Action` objects and mock LLM actions defined element text store as `locator: "#id", value: "varName"`. When converted to tool calls in `AgentToolLoopStep`, `storedOrderId` remained in `value` and was not mapped to `variableName`. Similarly, `Action.toToolCall()` omitted an explicit case for `STORE`, defaulting to storing the variable name in `args.value`. `BrowserToolProvider` strictly mandated `args.hasNonNull("variableName")`, causing execution rejection.
- **Detection Gap ("What did we miss?"):** Unit tests for `BrowserStoreToolTest` tested native tool call structures (`variableName: "..."`), while `StoreActionTest` tested legacy `Action` executions via `SelenideTargetExecutor`. The bridge conversion between legacy action JSON candidates and `BrowserToolProvider`'s `store` tool was not covered in unit isolation.
- **Resolution:** Added `store` argument normalization in `AgentToolLoopStep.parseToolCallFromCandidate()` and `Action.toToolCall()`, and added fallback extraction in `BrowserToolProvider` when `value` is supplied alongside a selector without explicit `variableName`.
- **Safety Net Added:** Verified `StoreIntegrationTest` in both `FORCE_RECORDING` and `REPLAY_STRICT`, along with unit tests in `BrowserStoreToolTest` and `ActionTest`.

### [DEF-20260929-01] Multi-Module Working Directory Discrepancy Causes Premature Playbook Deletion and Replay Failure
- **Date:** 2026-09-29
- **Component:** `neodymium-core` (`ai-testing` / `mock-integration` / `BaseAiTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Mock integration tests (`ClickIntegrationTest`, `AssertIntegrationTest`, `HoverIntegrationTest`, etc.) executing with `@AiExecutionMode({AiExecutionMode.Type.FORCE_RECORDING, AiExecutionMode.Type.REPLAY_STRICT})` passed the recording step but failed on assertions like `assertTrue(new File("src/test/resources/...").exists())`. This caused `NeodymiumAiRunner.afterEach` to void/delete the newly recorded companion JSON file, causing the subsequent `REPLAY_STRICT` iteration to fail with `FileNotFoundException: No recorded companion JSON file found`.
- **Root Cause:** When running Maven from the aggregator reactor root (`neodymium-library`), Surefire executed tests with `user.dir` set to the repository root where `src/test/resources` does not exist (the test resources reside in `neodymium-core/src/test/resources`). Tests constructing `new File("src/test/resources/...")` resolved against the repository root instead of the module directory or classpath.
- **Detection Gap ("What did we miss?"):** Tests previously ran in IDEs or directly within the `neodymium-core` submodule directory where `user.dir` was set to `neodymium-core`. When executed from the reactor root in Maven, the relative file paths failed silently.
- **Resolution:** Added `getTestResourceFile(final String relativePath)` helper to `BaseAiTest.java` that inspects both submodule (`src/test/resources/...`) and multi-module aggregator (`neodymium-core/src/test/resources/...`) paths. Refactored all 18 mock integration test suites to use `getTestResourceFile(...)` and standardized deprecated `@AiPlaybook(name = ...)` usages to `@AiPlaybook(recordingFileName = ...)`.
- **Safety Net Added:** Verified all 18 mock integration test suites (`mvn test -pl neodymium-core -Dtest="org.neodymium.ai.integration.mock.*Test"`), passing 68/70 tests cleanly (remaining 2 errors isolated to `StoreIntegrationTest` validator in Issue #2).

### [DEF-20260929-09] Static Includes Create Synthetic Wrapper Step Nodes and Duplicate Substeps in Console Execution Reports
- **Date:** 2026-09-29
- **Component:** `neodymium-core` (`playbook-parser` / `console-reporting`)
- **Scope:** `Framework`
- **Symptom:** Unconditional static includes (`_include: file.steps`) generated synthetic container step nodes with duplicated substeps in `console-execution.json` instead of clean inlined top-level steps.
- **Root Cause:** `YamlPlaybookParser` wrapped static include steps inside a synthetic `PlaybookStep("_include: ...")` container and assigned `subStep.setParent(containerStep)`. Even after `flattenSteps()` flattened `flatSteps`, child steps retained `parent != null`, causing `PreliminaryReportListener` to reconstruct parent-child step hierarchies in `TestExecutionReport`.
- **Detection Gap ("What did we miss?"):** `YamlPlaybookParserTest` verified step counts after flattening but did not assert `parent == null` or check step hierarchy rendering for static includes.
- **Resolution:** Updated `YamlPlaybookParser` to inline static include steps directly into the playbook step list without synthetic wrapper nodes or parent links, keeping their origin `sourceFile` and `lineNumber` intact.
- **Safety Net Added:** Added unit tests in `YamlPlaybookParserTest` asserting `steps.size() == 5`, `parent == null`, `subSteps.isEmpty()`, and correct origin source file metadata for inlined static include steps.

### [DEF-20260928-07] AiSession Default Mock LLM Provider Returns Empty Tool Calls Breaking LLM-Mode Unit Tests
- **Date:** 2026-09-28
- **Component:** `neodymium-core` (`ai-session` / `mock-testing` / `agent-loop`)
- **Scope:** `Framework`
- **Symptom:** `AiSessionTest.testExecuteInlineStepsString`, `testExecutePlaybook`, and `testExecuteInlineYamlAutoSelectsFirstDataSet` fail with `InvalidAgentResponseException: Agent turn did not produce a valid tool call after warning`.
- **Root Cause:** `AiSession.createMockLlmProvider()` returned `new LlmResponse("[]", ...)` with empty tool calls. Under the tightened agent loop contract (`AgentToolLoopStep`), turns must produce a tool call (such as `complete_step`) or be rejected.
- **Detection Gap ("What did we miss?"):** When `AgentToolLoopStep` added mandatory tool call enforcement in commit 87651a09a, `createMockLlmProvider()` in `AiSession.java` was not updated to return a default `complete_step` tool call.
- **Resolution:** Updated `AiSession.createMockLlmProvider()` to return an `LlmResponse` populated with a `complete_step` `ToolCall`.
- **Safety Net Added:** `AiSessionTest` suite execution verifying all 17 unit tests pass cleanly.

### [DEF-20260928-06] Programmatic AiSession Replay Drops Step Status From Companion JSON Breaking 0-Action Step Replay
- **Date:** 2026-09-28
- **Component:** `neodymium-core` (`ai-session` / `playbook-replay` / `programmatic-execution`)
- **Scope:** `Framework`
- **Symptom:** Programmatic tests (`session.execute(...)`) containing steps recorded with 0 actions (such as verification steps completed via `complete_step` or conditional branches) passed during `FORCE_RECORDING`, but failed in `REPLAY_STRICT` with `ConclusiveFailureException: No recorded tool calls found for step '...' in REPLAY_STRICT mode`, and in `REPLAY_WITH_HEALING` by triggering unexpected live LLM fallback calls and failing replay metrics.
- **Root Cause:** In `AiSession.java`, step merging during replay copied `actions`, `toolCalls`, and hashes from `sessionSteps` to parsed inline steps, but omitted `status`, `failed`, and `failureReason`. Consequently, `parsed.getStatus()` remained `PlaybookStepStatus.PENDING`, causing `ExecuteActionsStep`'s `isRecordedCompletedStep` check to evaluate to `false`.
- **Detection Gap ("What did we miss?"):** File-based playbooks loaded their step status directly from JSON via `YamlPlaybookParser`, satisfying `isRecordedCompletedStep`. Programmatic test suites lacked unit tests verifying step status preservation across inline playbooks with 0-action steps.
- **Resolution:** Added copying of `status`, `failed`, and `failureReason` in `AiSession.java` step merging, and updated Step 9 of `ForwardIntegrationTest_testForwardSynonyms_Chrome_headless.json` with an explicit `assert_title` call.
- **Safety Net Added:** Added unit regression assertions in `AiSessionReplayTest` confirming that 0-action recorded steps retain `status` and replay successfully without triggering live LLM execution or `ConclusiveFailureException`.

### [DEF-20260928-05] Circular Self-Referencing Variable Sanitization in Literal STORE Actions Breaks Playbook Replay
- **Date:** 2026-09-28
- **Component:** `neodymium-core` (`action-sanitization` / `tool-loop` / `store-action`)
- **Scope:** `Framework`
- **Symptom:** Tests executing literal `STORE` actions passed in `FORCE_RECORDING` but failed in `REPLAY_STRICT` and `REPLAY_WITH_HEALING` with `UnresolvableVariableException: Unresolvable variable placeholder '${varName}' in template: "${varName}"` at `PlaybookToolReplayer.java:346`.
- **Root Cause:** When `store(variableName="varName", value="literalVal")` executed, it stored `"varName" -> "literalVal"` into `SessionData`. At step finalization, `DefaultActionSanitizer` and `AgentToolLoopStep.sanitizeToolCall` matched `"literalVal"` against `SessionData` and replaced it with `"${varName}"` inside the `store` tool's own arguments (`values: ["varName", "${varName}"]` and `arguments: {"variableName": "varName", "value": "${varName}"}`). On replay, `PlaybookToolReplayer` attempted to resolve `${varName}` before executing the `store` step, causing an unresolvable cyclic dependency.
- **Detection Gap ("What did we miss?"):** `DefaultActionSanitizerTest` only verified downstream consumer actions (e.g. `ASSERT_TEXT` referencing an existing variable), never testing sanitization of the `STORE` action itself or literal store tool calls.
- **Resolution:** Excluded the target variable name from candidate replacement variables during `STORE` action and `store` tool call sanitization, guarded `PlaybookToolReplayer` against resolving variable identifier keys, and repaired the recorded test playbook.
- **Safety Net Added:** Added unit regression test `testSanitizeStoreActionDoesNotSelfReferenceTargetVariable` in `DefaultActionSanitizerTest.java`.

### [DEF-20260928-04] Compile Error in StoreIntegrationTest Due to Undefined SessionData Method
- **Date:** 2026-09-28
- **Component:** `neodymium-core` (`live-integration` / `StoreIntegrationTest`)
- **Scope:** `Test/Harness`
- **Symptom:** `StoreIntegrationTest.java` lines 136-137 failed compilation in the IDE with `The method getDynamic(String) is undefined for the type SessionData`.
- **Root Cause:** In `testStoreMultipleVariables()`, `session.data().getDynamic(...)` was mistakenly used instead of `session.data().get(...)`. Incremental compilation in `mvn test-compile` masked the compiler error by treating the previously built `.class` file as up to date.
- **Detection Gap ("What did we miss?"):** Incremental Maven builds do not always recompile every test when target timestamps precede source edits; full clean test compilation was needed to uncover the syntax discrepancy.
- **Resolution:** Replaced `session.data().getDynamic(...)` calls with `session.data().get(...)`.
- **Safety Net Added:** Verified clean test compilation (`mvn clean test-compile`) ensuring 0 compilation errors across all test sources.

### [DEF-20260928-03] Premature Runtime Initialization Failures and Scope Deficits in Live Integration Tests (Scroll, Store, Timeout)
- **Date:** 2026-09-28
- **Component:** `neodymium-core` (`live-integration` / `action-plugins`)
- **Scope:** `Test/Harness`
- **Symptom:** `ScrollIntegrationTest`, `StoreIntegrationTest`, and `TimeoutIntegrationTest` failed immediately on invocation with `IllegalArgumentException: No datasets defined in playbook 'programmatic', but @AiDataSet filter [...] was specified`, preventing execution. In addition, `RefreshIntegrationTest`, `ScrollIntegrationTest`, `SelectOptionIntegrationTest`, `StoreIntegrationTest`, and `TimeoutIntegrationTest` lacked `@AiLinter(false)`, omitted `.verifyMetrics()` chaining, lacked `@BeforeEach` lifecycle isolation, suffered from tagging inconsistencies (`@Tag("integration")`), and left major action scenarios unverified.
- **Root Cause:** Programmatic playbooks without external static YAML datasets cannot use `@AiDataSet`; legacy test classes contained orphaned dataset annotations and inline `data:` blocks. The test classes were never upgraded to the modern lifecycle, metric verification, and multi-scenario standards.
- **Detection Gap ("What did we miss?"):** The live integration test suite was not run with full test discovery across all live action classes in CI; individual test runs masked runner initialization failures.
- **Resolution:** Removed orphaned dataset annotations and dead data blocks, added `@AiLinter(false)`, isolated URL initialization into `@BeforeEach`, aligned JUnit tags to `@Tag("AuraIntegration")` and `@Tag("LiveAPI")`, enabled full mode verification (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`) with strict `.verifyMetrics()` chaining, and expanded scenario coverage across all 5 test classes.
- **Safety Net Added:** Modernized live integration test suites in `RefreshIntegrationTest.java`, `ScrollIntegrationTest.java`, `SelectOptionIntegrationTest.java`, `StoreIntegrationTest.java`, and `TimeoutIntegrationTest.java` with comprehensive multi-scenario execution and strict metric assertions.

### [DEF-20260928-02] Premature Runtime Initialization Failure and Metric Deficit in TypeIntegrationTest
- **Date:** 2026-09-28
- **Component:** `neodymium-core` (`live-integration` / `action-plugins`)
- **Scope:** `Test/Harness`
- **Symptom:** `TypeIntegrationTest` crashed immediately upon test startup with `IllegalArgumentException: No datasets defined in playbook 'programmatic', but @AiDataSet filter [typeData] was specified`. In addition, execution metrics were discarded without validation, pre-conditions were unverified, and the test suite lacked coverage for multiline textareas, sequential multi-field submission, natural language semantic locators, value overwriting, and negative failure handling on disabled, readonly, and non-existent elements.
- **Root Cause:** Programmatic playbooks without external static YAML datasets cannot use `@AiDataSet`; the test harness contained an orphaned `@AiDataSet("typeData")` annotation and inline `data:` block. The test had not been modernized to adopt the `@BeforeEach` setup, `@AiLinter(false)`, `REPLAY_WITH_HEALING`, or `.verifyMetrics()` standards established across other action integration suites.
- **Detection Gap ("What did we miss?"):** Test suite sweeps did not execute `TypeIntegrationTest` individually in CI; linting and metric chaining requirements were not enforced on legacy live action tests.
- **Resolution:** Removed orphaned dataset annotations and unused imports, added `@AiLinter(false)`, isolated URL setup into `@BeforeEach`, enabled full mode verification (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`) with `.verifyMetrics()`, and expanded coverage across single-line input, multiline textareas, sequential form submission, natural language semantic locators, value overwriting, and negative failure handling for disabled, readonly, and missing inputs.
- **Safety Net Added:** Modernized live integration test suite in `TypeIntegrationTest.java` with 8 comprehensive scenarios and strict metric assertions.

### [DEF-20260928-01] Premature Runtime Initialization Failure and Metric Deficit in WaitIntegrationTest
- **Date:** 2026-09-28
- **Component:** `neodymium-core` (`live-integration` / `action-plugins`)
- **Scope:** `Test/Harness`
- **Symptom:** `WaitIntegrationTest` previously failed during test template setup with `IllegalArgumentException: No datasets defined in playbook 'programmatic', but @AiDataSet filter [waitData] was specified`, discarded the execution result without metric validations (`verifyMetrics()`) allowing silent drops to go undetected, suffered from a weak assertion oracle (no pre-condition check), and lacked reach into static duration pauses (`wait` tool), dynamic DOM pop-in, content mutations, and loader disappearance.
- **Root Cause:** Programmatic playbooks without static YAML dataset definitions cannot use `@AiDataSet`; the test harness only contained a single happy-path method that triggered `assert_element_state` rather than exercising duration-based wait tools or multiple asynchronous DOM transition states provided in `testWaitHappyPath.html`.
- **Detection Gap ("What did we miss?"):** Test suite reviews did not enforce metric chaining or full fixture scenario reach for `WaitIntegrationTest`, leaving orphaned annotations, unused imports, and narrow reach undetected.
- **Resolution:** Removed orphaned dataset annotations and unused imports, added `@AiLinter(false)`, isolated URL setup into `@BeforeEach`, enabled full mode verification (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`) with `.verifyMetrics()`, and expanded coverage across pre-condition checks, hidden element reveals, dynamic pop-ins, content mutations, loader disappearance, and static duration pauses.
- **Safety Net Added:** Modernized live integration test suite in `WaitIntegrationTest.java` with comprehensive multi-scenario execution and strict metric assertions.

### [DEF-20260927-03] False Negative in Temporal Flow Anomaly Test and Non-Agnostic Linter Prompt Examples
- **Date:** 2026-09-27
- **Component:** `neodymium-core` (`ai-prompts` / `live-integration`)
- **Scope:** `Test/Harness` & `Framework`
- **Symptom:** `PrelinterRuleMatrixLiveTest.testTemporalFlowAnomaly_English_CloudIam` failed with `AssertionFailedError: Expected at least one linter finding ==> expected: <false> but was: <true>` due to zero linter findings; `playbook-linter-prompt.md` contained hardcoded language token lists (`and, und, et`, `If, Falls, Wenn`).
- **Root Cause:** In `testTemporalFlowAnomaly_English_CloudIam`, Step 2 clicked "Configure Role" to open general settings rather than opening the Revocation modal referenced in Step 1, so the linter correctly evaluated the sequence as non-inverted. Furthermore, the prompt relied on language-specific keyword lists rather than universal semantic definitions.
- **Detection Gap ("What did we miss?"):** Test fixture steps were written without verifying that the second step unambiguously targeted the opening of the dialog in the first step; prompt review failed to catch non-agnostic keyword enumerations.
- **Resolution:** Updated `testTemporalFlowAnomaly_English_CloudIam` so Step 2 explicitly triggers the confirmation modal dialog (`Click "Revoke Role" to open the confirmation modal dialog`). Refactored `playbook-linter-prompt.md` Rules 1 and 9 to excise language-specific keywords in favor of universal syntactic/semantic definitions.
- **Safety Net Added:** Deterministic unit tests in `PlaybookLinterPromptTest` and verified live isolation tests in `PrelinterRuleMatrixLiveTest`.

### [DEF-20260927-02] Broken Replay Reference and Invalid Browser Agent Dispatch in PrelinterChallengeIntegrationTest
- **Date:** 2026-09-27
- **Component:** `neodymium-core` (`ai-testing` / `live-integration`)
- **Scope:** `Test/Harness`
- **Symptom:** `PrelinterChallengeIntegrationTest` fails when executed in full: Method 3 (`testPrelinterBypassedInReplayStrict`) fails with `FileNotFoundException` during runner setup because no recording JSON file exists; Method 2 (`testPrelinterDisabledBypassesExecution`) triggers live browser LLM agent loops against intentionally flawed challenge steps.
- **Root Cause:** Method 2 configured `@AiMode(ExecutionMode.LLM_ONLY)` on a class pointing to `PrelinterChallengeTest.yaml`. Because it was not `LINTER_ONLY`, `NeodymiumAiRunner` dispatched `AgentToolLoopStep` to execute defective steps in the browser, while `LLM_ONLY` omitted recording generation (`isRecording() == false`). Method 3 attempted `REPLAY_STRICT` from Method 2's non-existent recording, causing immediate failure.
- **Detection Gap ("What did we miss?"):** Previous manual test runs and verification scripts executed only `-Dtest=PrelinterChallengeIntegrationTest#testPrelinterEnabledChallengesAllRules`, leaving Methods 2 and 3 unverified in CI.
- **Resolution:** Retired the broken `PrelinterChallengeIntegrationTest` and its orphaned fixtures (`PrelinterChallengeTest.yaml`, `PrelinterChallengeTest/index.html`).
- **Safety Net Added:** Upfront linter configuration, toggles, replay bypass, and fail-on-findings behavior remain fully guarded by deterministic unit tests in `PlaybookLinterTest`, and all 9 quality rules remain verified in `ExecutionMode.LINTER_ONLY` by `PrelinterRuleMatrixLiveTest`.

### [DEF-20260927-01] Missing Failure Status on Optional Steps During Live StateMachine Execution
- **Date:** 2026-09-27
- **Component:** `neodymium-core` (`ai-engine` / `StateMachineRunner`)
- **Scope:** `Framework`
- **Symptom:** When a step tagged with `(optional)` fails during live execution, the failure is bypassed without throwing an exception, but `metrics.getSoftFailedStepCount()` reports 0 instead of 1, and the recorded step status is incorrectly saved as `SUCCESS`.
- **Root Cause:** In `StateMachineRunner.java`, the exception handler for `playbookStep.isOptional()` logged a warning and popped try-catch scopes, but omitted setting `playbookStep.setStatus(PlaybookStepStatus.FAILED)`, `playbookStep.setFailed(true)`, and `playbookStep.setFailureReason(...)`. As a result, the subsequent `ExecuteActionsStep` post-step hook evaluated `!step.isFailed()` as true and promoted the step status to `PlaybookStepStatus.SUCCESS`.
- **Detection Gap ("What did we miss?"):** Existing optional integration tests (`mock.OptionalIntegrationTest` and legacy `live.OptionalIntegrationTest`) only verified that execution completed without throwing unhandled exceptions; neither asserted on step statuses or asserted via `.verifyMetrics().hasSoftFailedStepCount(1)`.
- **Resolution:** Updated `StateMachineRunner.java` in the `playbookStep.isOptional()` branch to explicitly mark `playbookStep` and any active sub-steps/parent steps as `FAILED`, `setFailed(true)`, and record the failure cause before continuing execution.
- **Safety Net Added:** Modernized `live.OptionalIntegrationTest` asserting `.verifyMetrics().hasStepCount(2).hasSoftFailedStepCount(1)` on bypassed failures (with `.onLive(m -> m.hasLlmCalls())`, `.onStrictReplay(m -> m.hasNoLlmCalls())`, and `.onHealing(m -> m.hasLlmCalls())`) and `.hasNoSoftFailures()` on passing optional steps across `FORCE_RECORDING`, `REPLAY_STRICT`, and `REPLAY_WITH_HEALING`.

### [DEF-20260926-01] NullPointerException Unboxing Null Token Counts in GeminiLlmProvider and LLM Providers
- **Date:** 2026-09-26
- **Component:** `neodymium-core` (`ai-client`)
- **Scope:** `Framework`
- **Symptom:** In live agent execution (e.g., `IncludeIntegrationTest.testIncludeConditionalIfThenFalse`), the request fails with `ConclusiveFailureException` caused by `java.io.IOException: Failed to execute Gemini chat request: Cannot invoke "java.lang.Integer.intValue()" because the return value of "dev.langchain4j.model.output.TokenUsage.outputTokenCount()" is null`.
- **Root Cause:** In `GeminiLlmProvider.java` (and similarly in `OpenAiLlmProvider`, `VertexAiLlamaProvider`, and `MistralLlmProvider`), boxed `Integer` counts from LangChain4j's `dev.langchain4j.model.output.TokenUsage` were directly passed into primitive `int` constructor parameters of `TokenUsage(int, int, int, int)`. When LLM responses omit candidate/output token metadata (typical for tool calls or specific finish reasons), LangChain4j yields `null`, triggering an NPE during implicit auto-unboxing.
- **Detection Gap ("What did we miss?"):** Mock LLM unit tests construct `TokenUsage` with explicit non-null primitive integers (`new TokenUsage(10, 10, 20)`). Provider unit tests did not simulate LangChain4j responses with `null` token counters.
- **Resolution:** Introduced null-safe factory methods `TokenUsage.of(Integer, Integer, Integer, Integer)` and `TokenUsage.of(Integer, Integer, Integer)` in `TokenUsage.java` that coalesce `null` values to 0 and derive `totalTokenCount` if absent. Updated all four LLM providers (`GeminiLlmProvider`, `OpenAiLlmProvider`, `VertexAiLlamaProvider`, `MistralLlmProvider`) to use `TokenUsage.of(...)`.
- **Safety Net Added:** Unit tests in `TokenUsageTest` testing null and partial-null permutations for `TokenUsage.of(...)`.

### [DEF-20260925-03] Inclusion Tool Execution and Conditional Branching Fallback in AgentToolLoopStep
- **Date:** 2026-09-25
- **Component:** `neodymium-core` (`ai-agent-engine`)
- **Scope:** `Framework`
- **Symptom:** In live agent execution, conditional include playbooks (such as `testIncludeConditionalIfElseFallback`) failed because the agent could not invoke includes as native tools, instructions lost target context when hints were stripped (e.g. producing `"If is visible"` without a selector subject), and executing an include did not immediately terminate the turn loop, causing the agent to execute redundant duplicate turns.
- **Root Cause:**
  1. `BrowserToolProvider` lacked an `include` tool, forcing the LLM to either hallucinate actions or fail to invoke external playbooks directly when evaluating branches.
  2. `ExecuteActionsStep.prepareInstruction` stripped `(hint: ...)` from step instructions, resulting in grammatically incomplete instructions like `"If is visible"` when the condition relied on hint selectors.
  3. When an `include` action executes, it pushes steps onto `ExecutionContext`'s step stack to be processed by `StateMachineRunner`. Because `AgentToolLoopStep` did not treat successful `include` calls as a loop-completing goal, the turn loop continued running against an unchanged DOM.
- **Detection Gap ("What did we miss?"):** Integration tests with `MockLlmProvider` mocked pre-determined leaf actions directly on `MockLlmProvider`, bypassing the agent's interactive tool loop and dynamic prompt generation for include instructions.
- **Resolution:**
  1. Registered `include` tool in `BrowserToolProvider` accepting `path` (with aliases `file` and `target`) and executing `IncludeAction` against `ExecutionContext`.
  2. Enhanced `AgentToolLoopStep` prompt generation to detect selector hints from `rawInstruction` and repair `"If is visible"` to `"If <selector> is visible"`, appending explicit Target Selector Hint blocks.
  3. Added Rule #6 for conditional/include steps to the agent system prompt.
  4. Added immediate turn completion upon successful execution of the `include` tool, cleanly handing off execution to the pushed playbook steps on the execution stack.
- **Safety Net Added:** Unit tests in `BrowserToolsTest.testBrowserIncludeToolSchemaAndExecution()` verifying schema validation, missing path/context error handling, and stack pushing; integration tests in `mock.IncludeIntegrationTest` and `live.IncludeIntegrationTest`.

### [DEF-20260925-02] Dropped Branch Condition, Then, and Else Payloads in Action.fromToolCall
- **Date:** 2026-09-25
- **Component:** `neodymium-core` (`action-engine`)
- **Scope:** `Framework`
- **Symptom:** In `mock.IncludeIntegrationTest.testIncludeConditionalIfThen` and `mock.BranchIntegrationTest.testBranchMock`, conditional branch tool calls generated by the LLM silently dropped all `condition`, `then`, and `else` actions, causing the branch step to no-op and downstream assertions (`Cookies Accepted!`) to fail on empty DOM elements.
- **Root Cause:** `Action.fromToolCall(call)` in `Action.java` handled the `"branch"` tool name by setting `type = "BRANCH"` but never deserialized `args.path("condition")`, `args.path("then")`, or `args.path("else")` into `action.setCondition()`, `action.setThen()`, and `action.setElse()`. `BranchAction` therefore received a branch action with null branch collections. Furthermore, `Action.toToolCall()` lacked serialization of branch child collections, and `parseNestedAction` passed null `callId` into the `ToolCall` record constructor.
- **Detection Gap ("What did we miss?"):** Existing unit tests for `Action.fromToolCall` covered only primitive leaf actions (`CLICK`, `TYPE`, `NAVIGATE`), but lacked coverage for composite tool calls with nested action arrays.
- **Resolution:** Updated `Action.fromToolCall` in `Action.java` to recursively deserialize nested action lists from `condition`, `then`, and `else` JsonNode arrays into `Action` objects (generating unique UUID callIds where missing). Added branch collection serialization to `Action.toToolCall()`.
- **Safety Net Added:** Unit tests in `ActionTest` verifying `Action.fromToolCall` and `toToolCall` round-tripping with nested `condition`, `then`, and `else` branches, and verified clean execution in `mock.IncludeIntegrationTest.testIncludeConditionalIfThen` and `mock.BranchIntegrationTest`.

### [DEF-20260925-01] Pre-Condition Assertion Timing and Inter-Test Hover State Leakage in HoverIntegrationTest
- **Date:** 2026-09-25
- **Component:** `neodymium-core` (`live-integration-tests`)
- **Scope:** `Test/Harness`
- **Symptom:** In `HoverIntegrationTest.testHoverNonExistentElementFailure`, running the full test suite resulted in state contamination where `#categories-dropdown` and `#preview-card` pre-condition assertions failed before the test execution started. Additionally, the Visual RCA diagnostic reported misleading application-state failure ("application may not have reached expected checkout state").
- **Root Cause:** 
  1. The test asserted pre-conditions (`$("#categories-dropdown").shouldNotBe(visible)`) before `session.execute(...)` was called. Since `Open ${hover.test.url}` was inside `session.execute(...)`, the assertions ran against the dirty DOM left by preceding tests (`testCssHoverDropdown` and `testDelayedHoverActivity`).
  2. The autonomous agent probed `button#btn-categories` during its exploratory search for the missing element, altering page state before throwing `ConclusiveFailureException`.
  3. The non-existent element name was specified as `'Place Order'` on a catalog page, prompting the multimodal RCA agent to hypothesize an incomplete e-commerce checkout transition.
- **Detection Gap ("What did we miss?"):** Single-method test runs initialized a fresh browser session and did not expose the inter-method state leakage that occurs when running the full class in sequence.
- **Resolution:** Explicitly navigate to the test URL prior to checking initial pre-conditions, add an `@AfterEach` cleanup step to neutralize mouse position, and rename the negative test target to a domain-neutral action (`'Non-Existent Action'`).
- **Safety Net Added:** Full test class suite execution passing cleanly in sequence with isolated pre/post assertions and neutral RCA diagnostics.

### [DEF-20260924-05] Suite-Wide Underchecked Exception Types in Live Integration Tests
- **Date:** 2026-09-24
- **Component:** `neodymium-core` (`live-integration-tests`)
- **Scope:** `Test/Harness`
- **Symptom:** 28 negative test cases across 6 live integration test classes (`AssertIntegrationTest`, `CheckIntegrationTest`, `ClearIntegrationTest`, `ClickIntegrationTest`, `HoverIntegrationTest`, `TimeoutIntegrationTest`) used overly broad exception assertions (`Throwable.class`, `Exception.class`, or untyped `catch (final Exception e)` blocks).
- **Root Cause:** Historical use of generic exception catch-alls during initial test harness scaffolding. This violates test isolation and robustness principles:
  1. Catching `Throwable.class` masks fatal JVM Errors (`OutOfMemoryError`, `StackOverflowError`, `LinkageError`).
  2. Catching broad `Exception.class` allows arbitrary syntax errors, timeouts, or configuration glitches to falsely satisfy tests.
  3. Action steps (e.g. clicking/clearing/checking/hovering missing or disabled elements) have a distinct contract from verification steps (e.g. asserting text, URL, visibility, count, or attributes): action failures conclusively throw `ConclusiveFailureException`, while verification failures throw `AssertionError`.
- **Detection Gap ("What did we miss?"):** Linters and CI only checked whether tests passed green, without validating exception specificity against the framework's execution pipeline contracts.
- **Resolution:** Refactored all 28 negative test methods across the 6 live test classes to assert the exact expected exception type:
  1. `AssertIntegrationTest` (19 methods) & `TimeoutIntegrationTest` (1 method): migrated to `assertThrows(AssertionError.class, ...)`.
  2. `CheckIntegrationTest` (2 methods), `ClearIntegrationTest` (3 methods), `ClickIntegrationTest` (2 methods), and `HoverIntegrationTest` (1 method): migrated to `assertThrows(ConclusiveFailureException.class, ...)`.
- **Safety Net Added:** Exact type contracts enforced across all live suite negative tests, preventing false passes on pipeline or environmental crashes.

### [DEF-20260924-04] Agent Loop Misclassifies Plain-Text Action Failure Report as InvalidAgentResponseException
- **Date:** 2026-09-24
- **Component:** `neodymium-core` (`AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** `HoverIntegrationTest.testHoverNonExistentElementFailure` threw `InvalidAgentResponseException: Agent turn did not produce a valid tool call after warning` instead of failing conclusively with `ConclusiveFailureException` when hovering over a missing element (`Hover over 'Place Order'`).
- **Root Cause:** Operating Rule 3 in the agent prompt directs: *"If an action fails (e.g., target element is disabled, missing, or non-interactable), DO NOT substitute uncommanded assertions and DO NOT call 'complete_step'; report the failure."* In `HoverIntegrationTest`, after `hover` failed on Turn 1 and exploratory DOM queries confirmed the element was missing, the LLM faithfully followed Rule 3 by reporting the failure in plain text without proposing tools. However, `AgentToolLoopStep` blindly treated any turn without tool calls (`proposedCalls == null || proposedCalls.isEmpty()`) as an invalid response, sent a warning scolding the model to invoke `complete_step`, and threw `InvalidAgentResponseException` on repeat. This misclassified an expected SUT action failure as an agent communication glitch and bypassed Visual RCA failure classification.
- **Detection Gap ("What did we miss?"):** Previous tests (e.g. `ClickIntegrationTest`) worked by accident because the model gave in to the warning and called `complete_step`, which was intercepted at line 719. Unit tests for `AgentToolLoopStepTest` only tested action failure rejection when the agent explicitly called `complete_step`, never when the agent adhered to Rule 3 and reported failure via plain text.
- **Resolution:** Updated `AgentToolLoopStep` so that when `hasActionToolFailed` is true for an interactive action instruction and no tool calls are proposed, the runner recognizes that the agent has concluded failure and immediately throws `ConclusiveFailureException` with the action failure details instead of issuing an invalid response warning or throwing `InvalidAgentResponseException`.
- **Safety Net Added:** Unit test in `AgentToolLoopStepTest.testActionFailureReportedInPlainTextThrowsConclusiveFailureException`.

### [DEF-20260924-03] Hover Action Tool Fallback to Body and Missing from Mutating Action Validation
- **Date:** 2026-09-24
- **Component:** `neodymium-core` (`BrowserToolProvider`, `AgentToolLoopStep`)
- **Scope:** `Framework`
- **Symptom:** Hovering over a non-existent element in `HoverIntegrationTest.testHoverNonExistentElementFailure` (`Hover over 'Add to cart'`) did not throw an exception and falsely reported success.
- **Root Cause:** Dual framework defects:
  1. `BrowserToolProvider.createHoverTool()` only declared `selector` in its schema and only queried `resolveSelector(args)` (ignoring `text`). When the LLM invoked `hover` with `{"text": "Add to cart"}`, `resolveSelector` returned `""`. `findElement("")` defaulted to `$("body")`, causing the tool to hover over the visible document `<body>` and return a false success. Additionally, empty target arguments were not validated before execution.
  2. `AgentToolLoopStep.isMutatingTool(name)` omitted `"hover"` (and `"scroll"`). Even when `hover` failed on a non-existent element, `hasActionToolFailed` was never set. When the agent subsequently called `complete_step`, interactive step failure validation was bypassed because `hover` was not tracked as a mutating action tool.
- **Detection Gap ("What did we miss?"):** Unit tests for browser tools verified tool registration but did not test hover argument parsing, text fallback, or empty target validation. `AgentToolLoopStepTest` tested action failure handling for `check` and `click`, but lacked coverage for `hover` and missing mutating tool executions on interactive instructions.
- **Resolution:**
  1. Updated `BrowserToolProvider.createHoverTool` to accept `text` alongside `selector` in the JSON schema, validate that at least one is provided (returning `ToolResult.error` otherwise), and resolve targets via text or CSS.
  2. Added `"hover"` and `"scroll"` to `AgentToolLoopStep.isMutatingTool(name)`, updated `callSelector` extraction to check `text` and `target` properties, and required that interactive action instructions execute at least one successful mutating action tool before `complete_step` is accepted.
- **Safety Net Added:** Unit tests in `BrowserToolsTest.testHoverToolSchemaAndTargetValidation` and `AgentToolLoopStepTest.testCompleteStepRejectedWhenHoverActionToolFailed` and `testCompleteStepRejectedWhenInteractiveActionInstructionHasNoMutatingToolExecuted`.

### [DEF-20260924-02] HoverIntegrationTest Catches Throwable Masking JVM Errors and Lacks State Assertion
- **Date:** 2026-09-24
- **Component:** `neodymium-core` (`HoverIntegrationTest`, `live-integration-tests`)
- **Scope:** `Test/Harness`
- **Symptom:** `HoverIntegrationTest.testHoverNonExistentElementFailure` used `assertThrows(Throwable.class, ...)`. If a fatal JVM Error (such as `OutOfMemoryError`, `StackOverflowError`, or linkage failure) occurred during execution, the test would catch it and falsely report success. Additionally, no post-condition DOM state was asserted after failure.
- **Root Cause:** Incomplete test harness exception targeting. Catching `Throwable` violates framework test guidelines, which mandate catching `Exception.class` to prevent swallowing VM-level errors while verifying DOM stability after failure.
- **Detection Gap ("What did we miss?"):** The negative test passed during green runs because the framework properly threw an exception, but code analysis and linters did not flag `Throwable.class` usage in test assertions.
- **Resolution:** Replaced `Throwable.class` with `Exception.class` in `testHoverNonExistentElementFailure` and added post-condition oracle assertion `$(".dropdown-content").shouldNotBe(visible)`.
- **Safety Net Added:** Clean code and exception audit rule enforced across live integration test suite.

### [DEF-20260924-01] ForwardIntegrationTest Fails Initialization Due to Invalid @AiDataSet on Programmatic Playbook
- **Date:** 2026-09-24
- **Component:** `neodymium-core` (`NeodymiumAiRunner`, `live-integration-tests`)
- **Scope:** `Test/Harness`
- **Symptom:** Running `ForwardIntegrationTest` immediately crashed during JUnit Jupiter test template parameterization with `java.lang.IllegalArgumentException: No datasets defined in playbook 'programmatic', but @AiDataSet filter [forwardData] was specified.`
- **Root Cause:** `ForwardIntegrationTest` was annotated with `@AiDataSet("forwardData")` while using `@AiPlaybook("programmatic")`. `NeodymiumAiRunner` attempts to resolve and filter dataset names against pre-parsed YAML playbook tables when building invocation contexts before test execution begins. Because `"programmatic"` playbooks do not define static YAML dataset tables upfront, filtering against an undeclared dataset is illegal. The inline `data:` block passed to `session.execute(...)` was only evaluated at runtime, long after test invocation discovery had already aborted.
- **Detection Gap ("What did we miss?"):** The test was tagged with `@Tag("LiveAPI")` and `@Tag("AuraIntegration")`, which are frequently excluded during routine offline CI or unit test runs without LLM credentials, masking the fact that the test method failed at JUnit discovery/parameterization time before any browser or LLM connection was initiated.
- **Resolution:** Removed the `@AiDataSet` annotation and inline `data:` block from `ForwardIntegrationTest`. Standardized fixture parameter injection in `@BeforeEach` via `session.data().putDynamic(...)`. Modernized the test suite to use real link click navigation across dedicated test fixture pages (`ForwardActionTest/page1.html`, `page2.html`, `page3.html`), and added synonym phrasing (`testForwardSynonyms`), sequential multi-step history traversals (`testMultipleForward`), and fluent metric assertions (`verifyMetrics()`).
- **Safety Net Added:** Verified test parameterization across all execution modes (`FORCE_RECORDING`, `REPLAY_STRICT`, `REPLAY_WITH_HEALING`) via `mvn test -Dtest=ForwardIntegrationTest`.

### [DEF-20260923-06] Clear Action Silent No-Op on Checkbox Elements Leaving Checkboxes Selected
- **Date:** 2026-09-23
- **Component:** `neodymium-core` (`ClearAction`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** Executing `Clear #checkbox` on a checked `<input type="checkbox">` executed without error but left the checkbox in a checked/selected state (`isSelected() == true`). Additionally, negative tests expecting an exception failed with `AssertionFailedError: Expected java.lang.Throwable to be thrown, but nothing was thrown` because ChromeDriver permits `clear()` on any `<input>` element without error.
- **Root Cause:** Both `ClearAction` and `BrowserToolProvider#createClearTool()` delegated directly to `element.clear()`. While W3C WebDriver / ChromeDriver allows `clear()` on `<input type="checkbox">` by resetting its value string attribute, it does not toggle or uncheck the checkbox. Neither component inspected element types to uncheck checkboxes or assert editability constraints on checkboxes.
- **Detection Gap ("What did we miss?"):** No tests previously verified `ClearAction` or `BrowserToolProvider` against checkbox inputs. Tests had only focused on text inputs, textareas, and contenteditable elements.
- **Resolution:** Enhanced `ClearAction` and `createClearTool` in `BrowserToolProvider` to detect checkbox elements (`type="checkbox"` or `role="checkbox"`), assert editability (`element.shouldBe(Condition.editable)`), and if selected (`element.isSelected()`), click the element to uncheck it (preserving idempotency when already unchecked).
- **Safety Net Added:** Added unit test `testClearActionOnCheckbox` in `SelenideActionPluginsTest` and live/recorded integration test `testClearCheckbox` in `ClearIntegrationTest`.

### [DEF-20260923-05] AgentToolLoopStep Unconditional Exotic Tool Pruning Breaks Clear Cookies Live & Replay
- **Date:** 2026-09-23
- **Component:** `neodymium-core` (`AgentToolLoopStep`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** `ClearCookiesIntegrationTest#testClearCookiesWhenEmpty` failed in `REPLAY_STRICT` (`ConclusiveFailureException: No recorded tool calls found for step 'Clear all cookies'`) and `REPLAY_WITH_HEALING` (`AssertionFailedError: Expected 0 LLM calls, but calls were made. ==> expected: <0> but was: <2>`). Additionally, `testClearCookiesWithActiveSession` failed during initial recording (`FORCE_RECORDING`) because browser cookies were not wiped.
- **Root Cause:** In commit `af748f3b2`, `AgentToolLoopStep#filterTools` introduced an unconditional exclusion of specialized tools (`clear_cookies`, `clear`, `upload_file`, `drag`, `drag_to`, `execute_script`) via `isExoticTool(clean)`. Because `clear_cookies` was unconditionally pruned from `availableTools`, the LLM agent never received the tool definition. In `testClearCookiesWhenEmpty`, the LLM fell back to calling `query_dom("*")` and `complete_step`, recording 0 mutating tool calls in the candidate JSON recording. During replay, `ExecuteActionsStep` threw a `ConclusiveFailureException` on empty tool calls in strict replay, and escalated to LLM healing in healing replay.
- **Detection Gap ("What did we miss?"):** Existing unit tests in `AgentToolLoopStepTest` only tested execution with custom mock tools and mock LLM calls. No unit tests validated tool catalog pruning against step instructions for specialized tools. Mock integration tests bypassed live LLM tool discovery by stubbing legacy JSON responses directly.
- **Resolution:** Enhanced `AgentToolLoopStep#filterTools` to check `isExoticToolDemanded` against instruction and context text (`effectiveInstructionText`), ensuring specialized tools (`clear_cookies`, `clear`, `upload_file`, `drag`, `drag_to`, `execute_script`) are retained whenever the instruction references them.
- **Safety Net Added:** Added unit tests in `AgentToolLoopStepTest` (`testFilterToolsPrunesExoticToolsOnStandardInstruction`, `testFilterToolsPreservesClearCookiesWhenRequested`, `testFilterToolsPreservesUploadFileWhenRequested`, `testFilterToolsPreservesDragWhenRequested`, `testFilterToolsPreservesExecuteScriptWhenRequested`), and validated live integration tests pass across `FORCE_RECORDING`, `REPLAY_STRICT`, and `REPLAY_WITH_HEALING`.

### [DEF-20260923-04] LLM Action Evasion via Uncommanded Assertions and Substitution on Failed Action Steps
- **Date:** 2026-09-23
- **Component:** `neodymium-core` (`AgentToolLoopStep`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** In `CheckIntegrationTest.testCheckDisabledElementFailure` and `testCheckNonExistentElementFailure`, negative tests expecting exceptions (`assertThrows`) failed because the autonomous LLM agent evaded the commanded failing action:
  1. When checking a disabled element (`#disabled-box`) or non-existent element (`#missing-checkbox-target`) failed, the agent called uncommanded assertions (e.g. `assert_element_state(state="disabled")` or `assert_element_state(state="absent")`) or checked an unrelated element (`#newsletter`), claiming the step succeeded via `complete_step`.
  2. In `BrowserToolProvider`, fallback mechanisms (such as clicking parent labels or JavaScript `arguments[0].click()`) bypassed disabled element constraints.
- **Root Cause:**
  1. `BrowserToolProvider.executeElementClick()` and `createCheckTool()` fell back to clicking parent labels or executing JavaScript clicks when standard element clicks threw an exception, bypassing the disabled state of form inputs.
  2. `AgentToolLoopStep` permitted `complete_step` if any effective tool call succeeded, without distinguishing mutating action tools from assertion or discovery tools. When a commanded action failed, successful evasion assertions satisfied the completion check. Furthermore, when challenged, the agent substituted an entirely different, uncommanded checkbox (`#newsletter`) on the page to make an action succeed.
- **Detection Gap ("What did we miss?"):** Prior tests only checked positive happy-path executions. Negative tests expecting interaction failures on disabled or absent elements had not been run with live LLM calls, missing the LLM's goal-seeking behavior to evade failures through uncommanded assertions and target substitutions.
- **Resolution:**
  1. Guarded `BrowserToolProvider.createCheckTool()` and `executeElementClick()` to strictly reject disabled elements (`el.is(Condition.disabled) || !el.is(Condition.enabled)`), throwing without falling back to parent label or JavaScript clicks.
  2. Verified in `createCheckTool()` that `el.isSelected() == targetChecked` post-interaction, returning error on failure.
  3. In `AgentToolLoopStep`, strictly enforce that for interactive action steps (`isInteractiveActionInstruction`), invoking `complete_step` while `hasActionToolFailed` is true throws `ConclusiveFailureException` immediately.
  4. Added target substitution guarding in `AgentToolLoopStep`: if a commanded target selector fails, successful actions on uncommanded elements (e.g. `#newsletter`) cannot clear `hasActionToolFailed`.
  5. Added explicit Operating Rule 3 prompt guidance prohibiting uncommanded assertion substitutions when actions fail.
- **Safety Net Added:** `BrowserToolsTest` assertions 8 and 9, `AgentToolLoopStepTest.testPrematureCompleteStepRejectedWhenActionToolFailed`, `AgentToolLoopStepTest.testCompleteStepRejectedWhenActionToolFailedAndTargetSubstitutionAttempted`, and live integration tests `CheckIntegrationTest.testCheckDisabledElementFailure` and `CheckIntegrationTest.testCheckNonExistentElementFailure`.

### [DEF-20260923-03] Non-Idempotent Check Action Toggling Checkbox State Upon Repeated Invocations
- **Date:** 2026-09-23
- **Component:** `neodymium-core` (`BrowserToolProvider`, `AgentToolLoopStep`, `Action`)
- **Scope:** `Framework`
- **Symptom:** In `CheckIntegrationTest.testCheckIdempotency`, issuing a second check step on an already checked checkbox toggled the checkbox off (unchecking it) instead of keeping it checked, causing idempotency assertions to fail.
- **Root Cause:** In the Unified Tooling Architecture, there was no dedicated `check` browser tool. Autonomous agent actions targeting checkboxes were mapped to the generic `click` tool (and `AgentToolLoopStep.normalizeToolName` collapsed `case "click", "check" -> "click"`). Because standard HTML `<input type="checkbox">` elements toggle state on click, issuing two consecutive check instructions resulted in two clicks (false -> true -> false).
- **Detection Gap ("What did we miss?"):** Prior tests only exercised single-click checking or used Selenide's `setSelected()` helper directly in harness code, without verifying multi-step idempotency inside the autonomous agent tool loop.
- **Resolution:**
  1. Implemented a dedicated, idempotent `check` browser tool in `BrowserToolProvider` that inspects `el.isSelected()` and only clicks if the current state differs from the desired target state (`checked: true/false`, defaulting to `true`), with parent `<label>` and JS fallbacks. Disallowed unchecking individual radio buttons.
  2. Added defensive delegation in `BrowserToolProvider.createSelectTool()` so that radio and checkbox inputs mistakenly targeted with `select` are safely routed to check logic.
  3. Updated `AgentToolLoopStep` system prompt (Operating Rule 3) and `normalizeToolName` to recognize and dispatch `check` / `uncheck` directly.
  4. Updated `Action.toToolCall` and `Action.fromToolCall` to round-trip `CHECK` / `check` actions.
- **Safety Net Added:** `BrowserToolsTest.testBrowserCheckToolSchema` and `CheckIntegrationTest.testCheckIdempotency`.

### [DEF-20260923-02] Duplicate @Test Annotation on @AiPlaybook Methods Triggering ParameterResolutionException
- **Date:** 2026-09-23
- **Component:** `neodymium-core` (`CheckIntegrationTest`, `ClickIntegrationTest`, `HoverIntegrationTest`, `ClearCookiesIntegrationTest`, `RefreshIntegrationTest`, `SelectOptionIntegrationTest`)
- **Scope:** `Test/Harness`
- **Symptom:** Running tests failed with `ParameterResolutionException: No ParameterResolver registered for parameter [org.neodymium.ai.session.AiSession arg0] in method [public void setupProperties(org.neodymium.ai.session.AiSession)]` when `@BeforeEach` or test method expected `AiSession`.
- **Root Cause:** `@AiPlaybook` is meta-annotated with `@TestTemplate`. Adding `@Test` to the same method caused JUnit 5 Jupiter engine to discover and execute the method twice: once as standard `@Test` (where `AiInvocationExtension` is not registered and cannot resolve `AiSession`) and once as `@TestTemplate`. In addition, `SelectOptionIntegrationTest` used `@AiDataSet` without YAML datasets.
- **Detection Gap ("What did we miss?"):** Compilation succeeds because both `@Test` and `@AiPlaybook` are valid annotations; duplicate discovery errors only surface at test execution time.
- **Resolution:** Removed redundant `@Test` annotations and unused imports across affected live integration test classes; removed inapplicable `@AiDataSet` filter from `SelectOptionIntegrationTest`.
- **Safety Net Added:** Clean test execution of `CheckIntegrationTest` and companion live integration suites in both offline replay and live execution modes.

### [DEF-20260923-01] Orphaned endHook in ExecuteActionsStep Overwriting Step Status on Expected Bug with Continue-On-Error
- **Date:** 2026-09-23
- **Component:** `neodymium-core` (`ExecuteActionsStep`, `BugIntegrationTest`)
- **Scope:** `Framework`
- **Symptom:** In tests combining `(bug)` and `(continue-on-error)`, expected step failures are falsely logged as `❌ Expected bug but step succeeded` and persisted in recorded playbooks with status `SUCCESS`. In `BugIntegrationTest.testBugContinueOnError`, the test also failed due to an invalid assertion on non-existent element `#result`.
- **Root Cause:**
  1. `ExecuteActionsStep` pushed an `endHook` lambda onto `ExecutionContext.runStack` prior to `TryCatchStep`. When `StateMachineRunner` handled an expected bug failure on a step with `continue-on-error`, it discarded steps up to `EndTryStep` and continued the loop, popping the orphaned `endHook`. The hook unconditionally set `step.setStatus(SUCCESS)` and `step.setFailed(false)`.
  2. `BugIntegrationTest` asserted `$("#result").shouldHave(exactText("Click Me Triggered!"))` instead of targeting `<span id="click-status">` on `AllActionsTest/test.html`.
- **Detection Gap ("What did we miss?"):** Previous mock tests for `(bug) (continue-on-error)` only validated that the playbook run completed without uncaught exceptions, but did not assert that the failed step retained `PlaybookStepStatus.FAILED` in the recorded companion model.
- **Resolution:** Guarded status assignments and bug validation in `ExecuteActionsStep.endHook` when `step.isFailed()` is true; corrected the element locator and expected text in `BugIntegrationTest.testBugContinueOnError`.
- **Safety Net Added:** `BugIntegrationTest.testBugContinueOnError` verifying end-to-end recording and offline replay, plus assertion on recorded companion step status.

### [DEF-20260922-08] Lack of Retry Mechanism for Transient SessionNotCreatedException During WebDriver Startup
- **Date:** 2026-09-22
- **Component:** `neodymium-core` (`BrowserRunnerHelper`, `NeodymiumConfiguration`)
- **Scope:** `Framework`
- **Symptom:** Tests fail immediately during setup when ChromeDriver or the browser is temporarily unreachable (`org.openqa.selenium.SessionNotCreatedException: Could not start a new session. Response code 500. Message: session not created from chrome not reachable`).
- **Root Cause:** Transient OS process/socket collisions (e.g. DevTools port lingering in TIME_WAIT or Chrome shutdown latency from a previous test) cause ChromeDriver handshake to fail. Neodymium previously lacked a retry loop for driver session instantiation, treating all `SessionNotCreatedException` failures as fatal.
- **Detection Gap ("What did we miss?"):** No test harness resilience for transient process startup race conditions; browser creation assumed 100% determinism.
- **Resolution:** Wrapped `createWebDriverStateContainer` with a single-retry resilience loop with configurable backoff (default 2–10 seconds jitter), ensured automatic cleanup of partial resources (e.g. embedded proxies) before retry, and refreshed remote debugging port probes on retry.
- **Safety Net Added:** `BrowserRunnerHelperTest` validating single retry with backoff, abort behavior on subsequent failure, non-retry for other exceptions, and proxy leak prevention.

### [DEF-20260922-07] Generic RuntimeException Wrapping and Obsolete Schema Version Re-Persistence
- **Date:** 2026-09-22
- **Component:** `neodymium-core` (`IncompatiblePlaybookSchemaException`, `PlaybookToolReplayer`, `ExecuteActionsStep`, `StateMachineRunner`, `AiSession`)
- **Scope:** `Framework`
- **Symptom:** When replaying a playbook with an obsolete or incompatible schema version (e.g., version `3.0`), the runner failed with generic `java.lang.RuntimeException: org.neodymium.ai.pipeline.ConclusiveFailureException` instead of a specialized typed exception with structured version metadata. Additionally, during self-healing runs, `AiSession` copied obsolete schema versions from loaded recorded steps onto active steps, perpetuating outdated schemas.
- **Root Cause:**
  1. Schema version mismatches in `PlaybookToolReplayer` threw `ConclusiveFailureException` directly with a formatted string; `ExecuteActionsStep` wrapped it in `RuntimeException`; and `StateMachineRunner` re-threw the outer `RuntimeException` without unwrapping `PipelineException` causes.
  2. `AiSession.java` unconditionally copied `recorded.getSchemaVersion()` onto the active `PlaybookStep`, overwriting `CURRENT_SCHEMA_VERSION`.
- **Detection Gap ("What did we miss?"):** Absence of negative unit tests asserting the exact exception type and structured fields when loading legacy playbooks.
- **Resolution:**
  1. Introduced `IncompatiblePlaybookSchemaException` extending `PipelineException` with `getRecordedVersion()` and `getExpectedVersion()`.
  2. Updated `PlaybookToolReplayer` to throw `IncompatiblePlaybookSchemaException`.
  3. Updated `ExecuteActionsStep` to throw `IncompatiblePlaybookSchemaException` directly without wrapping in `RuntimeException` and update `schemaVersion` to `CURRENT_SCHEMA_VERSION` on self-healing.
  4. Updated `StateMachineRunner` to unwrap `t.getCause() instanceof PipelineException` to ensure typed exceptions bubble up cleanly.
- **Safety Net Added:** Created `IncompatiblePlaybookSchemaExceptionTest.java` verifying exception hierarchy, version accessors, and `PlaybookToolReplayer` validation.

### [DEF-20260922-06] Multi-Module Classpath Resource Path Resolution Divergence
- **Date:** 2026-09-22
- **Component:** `neodymium-core` (`ClasspathResourceManager`, `ClasspathResourceManagerTest`)
- **Scope:** `Framework`
- **Symptom:** `FORCE_RECORDING` mode in multi-module builds wrote newly generated JSON playbooks into `./src/test/resources/` in the top-level aggregator root directory instead of the submodule's directory (`neodymium-core/src/test/resources/`). Consequently, subsequent test runs executing `REPLAY_STRICT` or `REPLAY_WITH_HEALING` loaded obsolete cached recordings from `neodymium-core/target/test-classes/` that were never updated, leading to schema mismatches (`3.0` vs `4.0`) or missing recordings.
- **Root Cause:** `ClasspathResourceManager.getSourceResourcesRoot()` fell back to `Path.of("src/test/resources")` relative to `System.getProperty("user.dir")` instead of inspecting the active classloader root (`target/test-classes`, `target/classes`, or `bin`) to derive the actual Maven/Gradle submodule source resources directory.
- **Detection Gap ("What did we miss?"):** Unit tests ran in single-module contexts where `user.dir` coincided with the module directory, masking path divergence in multi-module reactor builds.
- **Resolution:** Updated `getSourceResourcesRoot()` to dynamically derive the source resource folder from the active classloader resource URL (`this.classLoader.getResource("")`) by substituting `target/test-classes` with `src/test/resources` before falling back to `user.dir`. Cleaned up 36 orphan playbooks from aggregator root.
- **Safety Net Added:** Added `testSourceResourcesRootResolutionInMultiModule()` in `ClasspathResourceManagerTest.java` and verified live recording in `CanvasClickSandboxMockTest`.

### [DEF-20260922-05] Chained Shorthand Tag Corruption, Missing Test-ID Variants, and Silent Blind Fallthrough in LocatorResolver
- **Date:** 2026-09-22
- **Component:** `neodymium-core` (`LocatorResolver`, `LocatorResolverTest`, `LocatorResolverBrowserTest`)
- **Scope:** `Framework`
- **Symptom:**
  1. Chaining attribute shorthands (e.g. `#modal >> data-testid=save` or `form >> id=submit`) generated invalid XPath queries searching for literal `<data-testid>` and `<id>` HTML tags (`//*[@id='modal']//data-testid` and `//id`).
  2. The `data-test-id=` attribute shorthand was unmapped, falling through to invalid raw CSS and failing at runtime.
  3. Unsupported vendor pseudo-classes and layout selectors (`:visible`, `:hidden`, `:right-of()`, `:left-of()`, `:above()`, `:below()`, `:near()`, `:nth-match()`, `:text-matches()`) silently fell through to `By.cssSelector`, causing deferred browser crashes with cryptic syntax errors and LLM thrashing.
  4. Playwright codegen internal prefixes (`internal:role=...`, `internal:text=...`, etc.) failed to resolve.
- **Root Cause:**
  1. `resolveChainedLocator` only handled `text=` and `role=`; all other segments were passed to `toXPathSegment()` which mistook shorthand keys (`data-testid`, `id`, `placeholder`) for HTML element tags.
  2. `LocatorResolver.resolveLocator` lacked pre-validation for unsupported Playwright/jQuery pseudo-classes before falling through to `By.cssSelector`, and did not recognize `data-test-id=` or `internal:` prefixes.
- **Detection Gap ("What did we miss?"):**
  Unit tests in `LocatorResolverTest` only verified isolated selectors and basic CSS/text chains. No unit or integration tests exercised attribute shorthands inside `>>` chains or checked behavior when unsupported pseudo-classes were submitted.
- **Resolution:**
  1. Expanded `resolveChainedLocator` to recursively map attribute shorthands (`id=`, `data-testid=`, `data-test=`, `data-test-id=`, `placeholder=`, `alt=`, `title=`, `label=`).
  2. Added `data-test-id=` attribute shorthand support.
  3. Added pre-validation in `resolveLocator` to detect unsupported pseudo-classes and spatial layout selectors, throwing an explicit `InvalidSelectorException` with actionable remedies.
  4. Normalized Playwright codegen `internal:*` prefixes and case flags (`[name="Save"i]`).
  5. Expanded pure unit tests in `LocatorResolverTest` and live headless Chrome browser tests in `LocatorResolverBrowserTest`.
- **Safety Net Added:**
  - Unit tests in `LocatorResolverTest`: `testChainedAttributeShorthands`, `testDataTestIdAttribute`, `testPlaywrightCodegenInternalPrefixes`, and `testUnsupportedSelectorsFailFastWithDiagnostics`.
  - Real-browser integration tests in `LocatorResolverBrowserTest`: `testNamedRoleInLiveBrowser`, `testLabelInLiveBrowser`, `testChainedShorthandsInLiveBrowser`, `testDataTestIdInLiveBrowser`, and `testUnsupportedSelectorThrowsInBrowser`.

### [DEF-20260922-04] Selector Parsing Regressions, Unreachable Fallbacks, and Incomplete Element Matching in LocatorResolver
- **Date:** 2026-09-22
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

### [DEF-20260922-03] Root Container Fallback and Incomplete Playwright Selector Support Causing False Element Matching on Hidden Elements
- **Date:** 2026-09-22
- **Component:** `neodymium-core` (`LocatorResolver`, `QualityJudgeToolInterceptor`, `SelenideElementFinder`, `BrowserToolProvider`)
- **Scope:** `Framework`
- **Symptom:** In `AssertIntegrationTest.testAssertVisibility`, asserting that a hidden element is absent (`Assert that the hidden 'Secret Button' is absent`) failed. The selector resolved to `<body>` instead of the absent/hidden element, or failed on `assert_text(negated=true)` with `Expected text/pattern "Secret Button" was still present anywhere on the page within 3000ms.`.
- **Root Cause:**
  1. `QualityJudgeToolInterceptor` called `driver.findElements(By.cssSelector(selector))` directly, throwing `InvalidSelectorException` when Playwright selectors (e.g. `text=Secret Button`, `role=...`, or chained `>>`) were used, silently bypassing quality deliberation.
  2. `SelenideElementFinder` duplicated pseudo-resolution with an ad-hoc XPath expression `contains(normalize-space(.), '...')`. In XPath, `.` matches the string-value of all descendants, matching `<html>` and `<body>`.
  3. When the targeted button was hidden (`display: none`), `findFirstVisible` filtered it out, and Chromium's `visibleEls.find(Condition.focused)` defaulted to `<body>` (since `document.activeElement` is `document.body` when no element has focus), returning `<body>` as the matched element.
  4. In `BrowserToolProvider.matchesElementText`, `el.getAttribute("textContent")` was evaluated unconditionally on `$("body")`, dumping all text in the entire DOM (including hidden `display: none` elements). When the LLM attempted `assert_text(expectedText="Secret Button", negated=true)`, `isTextPresentOnPage` always reported the text present.
- **Detection Gap ("What did we miss?"):**
  1. `LocatorResolver` lacked browserless unit test coverage for Playwright selector extensions (`role=`, chained `>>`, attribute shorthands, test IDs).
  2. Element finding tests focused on finding present and visible elements; negative assertions on hidden/absent elements were not guarded against root container (`html`/`body`) fallback.
  3. Page-level text presence assertions lacked tests verifying that text inside `display: none` elements is correctly recognized as not present on the rendered page.
- **Resolution:**
  1. Expanded `LocatorResolver` into a comprehensive translation layer supporting all Playwright selectors (`role=`, attribute shorthands, test IDs, exact vs substring text matching, chained `>>` combinators, Shadow DOM).
  2. Excluded root containers (`html`, `body`, `head`) from wildcard text match expressions in `LocatorResolver.buildPseudoSelectorXpath`.
  3. Updated `QualityJudgeToolInterceptor` to query live DOM using `LocatorResolver.resolveLocator(selector)`.
  4. Streamlined `SelenideElementFinder.findDirect` to delegate directly to `LocatorResolver`, excised flawed `tryResolvePlaywrightPseudo`, and updated `findFirstVisible` to filter out `html` and `body` unless explicitly requested.
  5. In `BrowserToolProvider`, guarded `matchesElementText` so `textContent` is never evaluated on root containers (`body`/`html`), and filtered for visible inputs in `isTextPresentOnPage`.
- **Safety Net Added:**
  - Added unit test suite `LocatorResolverTest` (10 tests) verifying locator resolution without requiring a browser.
  - End-to-end multi-cycle regression test in `AssertIntegrationTest#testAssertVisibility` covering live recording and strict/healing replay cycles.

### [DEF-20260922-02] Missing Native Negation Support across Assertion Tools Causing Flakiness on Negative Assertions
- **Date:** 2026-09-22
- **Component:** `neodymium-core` (`ai-tool`, `browser-tool-provider`, `ai-executor`, `action-model`)
- **Scope:** `Framework`
- **Symptom:** In `AssertIntegrationTest.testAssertUrl`, step 9 ("There is no '#' in the url") exhibited non-deterministic flakiness: in some runs, the LLM invoked `assert_url({"expectedUrl": "#"})` without negation support, timing out waiting for '#' to appear and failing the step; in other runs, the LLM guessed a complex regex workaround (`^[^#]*$`), passing the step.
- **Root Cause:**
  1. None of Neodymium's browser assertion tools (`assert_url`, `assert_title`, `assert_text`, `assert_attribute`, `assert_count`, `assert_element_state`) exposed a first-class `negated` / `not` property in their schema or runtime execution loops.
  2. The `Action` domain model lacked a `negated` property and corresponding tool serialization/deserialization logic, preventing offline replays and action plugins from preserving or enforcing negative assertions.
  3. `AssertAction` lacked handling for negative assertions on URL, title, text, attribute, and count (`!=`), as well as `ASSERT_UNFOCUSED`.
- **Detection Gap ("What did we miss?"):**
  Assertion tool tests previously verified positive existence or exact matches, but lacked negative asserting suites testing that absence of characters, absent attributes, not-equal counts, and element state inverters (e.g. visible <-> hidden, focused <-> unfocused) work reliably without prompt engineering regexes.
- **Resolution:**
  1. Added `@JsonProperty("negated") private boolean negated = false;` to `Action` with full constructor overloads, with-methods, and JSON serialization/deserialization.
  2. In `Action.toToolCall()`, emit `"negated": true` if set, and map `ASSERT_UNFOCUSED`.
  3. In `Action.fromToolCall()`, parse `negated` (with aliases `not`, `invert`, `inverted`), invert element states, and map `NOT_EQUALS` count assertions to `!=`.
  4. Added `negated` schema property and inverted condition loops across `assert_url`, `assert_title`, `assert_text`, `assert_attribute`, `assert_count`, and `assert_element_state` (including `unfocused`).
  5. Updated `AssertAction` to honor `action.isNegated()` on URLs, titles, text, attributes, count (`!=`), and element focus.
- **Safety Net Added:**
  - `ActionTest#testNegatedJsonRoundTrip`, `ActionTest#testFromToolCallWithNegatedFlag`, `ActionTest#testFromToolCallAssertElementStateInversion`, `ActionTest#testFromToolCallAssertElementStateUnfocused`, `ActionTest#testFromToolCallAssertCountNotEquals`.
  - `BrowserToolsTest#testAssertToolsNegationSchemaProperties`, `BrowserToolsTest#testNormalizeElementStateUnfocused`.
  - `BrowserToolProviderStabilityTest` async polling regression coverage.
  - `AssertActionTest#testNegatedUrlAssertions`, `AssertActionTest#testNegatedTitleAssertions`, `AssertActionTest#testNegatedTextAssertions`, `AssertActionTest#testNegatedAttributeAssertions`, `AssertActionTest#testUnfocusedAndNegatedCountAssertions`.

### [DEF-20260922-01] Missing Replayed Step Count Tracking in ExecuteActionsStep Causing hasAllStepsReplayed Assertion Failure
- **Date:** 2026-09-22
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-replay`, `metrics`)
- **Scope:** `Framework`
- **Symptom:** In `AssertIntegrationTest` and any tests asserting `hasAllStepsReplayed()` on replay, `hasAllStepsReplayed()` failed with `AssertionError: Not all steps were replayed from cache. ==> expected: <N> but was: <0>`.
- **Root Cause:**
  `AiSession.getMetrics()` retrieves `replayedStepCount` from `ExecutionContext.KEY_TOTAL_REPLAYS`. In `ExecuteActionsStep`, replaying steps via `PlaybookToolReplayer.replayStep(...)` or bypassing them via `VisualBaselineGateStep` executed the actions against the SUT but never incremented `ExecutionContext.KEY_TOTAL_REPLAYS`. Consequently, `KEY_TOTAL_REPLAYS` remained 0 across all successful replay steps.
- **Detection Gap ("What did we miss?"):**
  Unit tests for `MetricsAsserter` and `ExecutionMetrics` used manually constructed instances with mocked non-zero `replayedStepCount` values. Pipeline integration tests focused on step completion rather than asserting that the runtime metrics asserter counted real pipeline replay steps.
- **Resolution:**
  1. In `ExecuteActionsStep.java`, increment `ExecutionContext.KEY_TOTAL_REPLAYS` upon successful completion of `PlaybookToolReplayer.replayStep(...)`.
  2. In `ExecuteActionsStep.java`, increment `ExecutionContext.KEY_TOTAL_REPLAYS` when a step is visually verified and bypassed via `VisualBaselineGateStep` in replay mode.
- **Safety Net Added:**
  Regression test `ExecuteActionsStepTest#testReplayedStepCountIncrementedOnReplay` and `AssertIntegrationTest#testAssertUrl` verifying that `hasAllStepsReplayed()` passes with exact step count equality on replay runs.

### [DEF-20260921-03] Missing assert_element_state and assert_attribute Tools Causing False Pass in testAssertReadonlyFailure
- **Date:** 2026-09-21
- **Component:** `neodymium-core` (`ai-tool`, `browser-tool-provider`, `ai-pipeline`, `action-model`)
- **Scope:** `Framework`
- **Symptom:** In `AssertIntegrationTest.testAssertReadonlyFailure`, the step `Assert that the 'readonly-input' field is editable` succeeded unexpectedly, generating an HTML report marked `PASSED` while the test failed in JUnit (`AssertionFailedError: Expected java.lang.Throwable to be thrown, but nothing was thrown`).
- **Root Cause:**
  1. `BrowserToolProvider` registered only text, count, URL, and title assertion tools (`assert_text`, `assert_count`, `assert_url`, `assert_title`), lacking native tools to assert element states (`editable`, `readonly`, `enabled`, `disabled`, `visible`, `hidden`, `checked`, `selected`, etc.) and element attributes (`placeholder`, `value`, `href`, `data-*`).
  2. In `AgentToolLoopStep`, the agent was instructed to call an assertion tool on verification instructions before `complete_step`.
  3. Lacking `assert_element_state`, the LLM inspected `#readonly-input`, observed `value="FixedData"`, and hallucinated/substituted `assert_text({"selector": "#readonly-input", "expectedText": "FixedData"})`. Because "FixedData" was present, `assert_text` succeeded, the LLM called `complete_step`, and the step was marked successful without testing the required editable state.
- **Detection Gap ("What did we miss?"):** `SelenideTargetExecutor` and `AssertAction` had full support for `ASSERT_EDITABLE`, `ASSERT_READONLY`, `ASSERT_ATTRIBUTE`, etc., in playbook replay, but `BrowserToolProvider` (which supplies tools to the live LLM agent loop) had not exposed corresponding tools. Unit tests verified tool loop completion but did not verify state assertion failures during live recording.
- **Resolution:**
  1. Implemented `assert_element_state` in `BrowserToolProvider` supporting `["visible", "hidden", "enabled", "disabled", "editable", "readonly", "checked", "unchecked", "selected", "unselected", "focused", "exists", "absent"]` and throwing `AssertionError` when conditions fail.
  2. Implemented `assert_attribute` in `BrowserToolProvider` supporting exact, substring, and regex attribute assertions and throwing `AssertionError` on mismatch.
  3. Mapped both tools bidirectionally in `Action.java` (`toToolCall()` and `fromToolCall()`).
  4. Updated `AgentToolLoopStep` prompt guidelines and tool normalizations to recognize `assert_element_state` and `assert_attribute`.
- **Safety Net Added:** Unit tests in `BrowserToolProviderStabilityTest`:
  - `testAssertElementStateToolSchema`
  - `testAssertAttributeToolSchema`
  - `testNormalizeElementStateUtility`
  - `testAssertElementStateReadonlySuccessAndEditableFailure` (verifying `readonly` passes and `editable` on a readonly element throws `AssertionError`)
  - `testAssertAttributeSuccessAndFailure` (verifying attribute substring match passes and mismatch throws `AssertionError`)

### [DEF-20260921-02] Soft Error Bypass in assert_text Suppressing AssertionError on Mismatched Selectors
- **Date:** 2026-09-21
- **Component:** `neodymium-core` (`ai-tool`, `browser-tool-provider`, `ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `WikipediaProgrammaticTestDataTest`, Step #5 (`Verify the main heading contains '${searchPhrase}' (bug)`) failed with `ExpectedBugNotReproducedException: Expected bug but step succeeded` instead of catching the headline mismatch defect ("Neodym" vs "Neodymium"). The HTML report displayed an uncommanded second assertion on `#mw-content-subtitle`.
- **Root Cause:**
  1. In `BrowserToolProvider.createAssertTextTool`, when an element selector failed to match the expected text within `Configuration.timeout`, the tool checked `if (!isTextPresentOnPage(...))`. If the expected text existed anywhere else on the page (e.g. in a redirect subtitle, breadcrumb, or footer), it returned `ToolResult.error` instead of throwing `AssertionError`.
  2. Returning `ToolResult.error` bypassed Stop Criterion 2 (immediate termination on assertion failures) in `AgentToolLoopStep`.
  3. The agent loop fed the error back to the LLM in Turn 2, which searched for the text elsewhere in the DOM, asserted on `#mw-content-subtitle` instead of the commanded `#firstHeading`, and called `complete_step`.
  4. This false step success caused `ExecuteActionsStep` to throw `ExpectedBugNotReproducedException` on the expected defect step.
- **Detection Gap ("What did we miss?"):** `BrowserToolProviderStabilityTest` tested page-wide fallbacks and asynchronous polling, but did not assert that an element-specific `assert_text` failure throws `AssertionError` when the text is present in another element on the page.
- **Resolution:**
  1. Removed `isTextPresentOnPage` bypass from element-targeted `assert_text` in `BrowserToolProvider`, throwing `AssertionError` directly when the specified selector does not match the expected text within timeout.
  2. Maintained page-wide text assertions (`isTextPresentOnPage`) only when `selector` is null, blank, `"body"`, or `"html"`.
- **Safety Net Added:** Unit regression test in `BrowserToolProviderStabilityTest#testAssertTextThrowsAssertionErrorWhenSelectorDoesNotMatchEvenIfTextExistsElsewhereOnPage` verifying that `assert_text` on a specific selector throws `AssertionError` when the element text does not match, even if the text exists elsewhere in the document body.

### [DEF-20260920-01] Tool Loop MoveTargetOutOfBoundsException on Coordinates and Malformed Trailing Selector in Hybrid Clicks
- **Date:** 2026-09-20
- **Component:** `neodymium-core` (`ai-tool`, `browser-tool-provider`)
- **Scope:** `Framework`
- **Symptom:** AI tool loop failed with `org.openqa.selenium.interactions.MoveTargetOutOfBoundsException: move target out of bounds: (158, 893) is out of bounds of viewport width (1280) and height (857)` when executing `click({"selector": "article[data-ai=\"xcboo7um\"] button, text:", "x": 158, "y": 893})`.
- **Root Cause:**
  1. In `BrowserToolProvider.createClickTool`, Case 1 (coordinates provided) was prioritized over Case 3 (element selector/text resolution). Even though an element selector was provided, raw coordinates routed execution directly to raw Selenium `new Actions(driver).moveToLocation(x, y).click().perform()`, completely bypassing Selenide's auto-scrolling element click logic.
  2. Selenium's `Actions.moveToLocation(x, y)` operates strictly in viewport coordinates without auto-scrolling and throws `MoveTargetOutOfBoundsException` if coordinates are outside `[0, innerWidth] x [0, innerHeight]`.
  3. LLM generated a malformed trailing selector token (`button, text:`), which caused `document.querySelector` to fail with a `DOMException: SyntaxError` and prevented element bounding rect resolution.
- **Detection Gap ("What did we miss?"):** Tests in `BrowserToolProviderStabilityTest` verified coordinate clicks within bounds and basic selector resolution separately, but did not test hybrid calls where an LLM provides both an element selector and out-of-viewport coordinates, nor did they test selector sanitization or coordinate auto-scrolling.
- **Resolution:**
  1. Added `cleanSelector` utility to sanitize hallucinated trailing markers (such as `, text:`, `, text=`, trailing commas).
  2. In `createClickTool`: Prioritized element-based clicking via Selenide (`el.shouldBe(Condition.visible).click()`) when a selector or text is provided and target is not explicitly `coord:...`. If coordinates are within the element's bounding box (`0 <= x <= width`, `0 <= y <= height`), treated them as an in-element offset via `Actions.moveToElement(el, xOffset, yOffset)`.
  3. Added auto-scrolling and viewport clamping in `performSafeCoordinateClick`: if target coordinates are outside the viewport, `window.scrollBy(...)` is called to scroll the target coordinate into the center of the viewport, coordinate offsets are adjusted, clamped to viewport boundaries, and a JavaScript `document.elementFromPoint(x, y).click()` fallback is executed if `Actions.moveToLocation` fails.
  4. Updated `clickBadgeScript` to scroll the badge into view before querying bounding rect.
  5. Updated `createHoverTool` to call `SelenideElementFinder.scrollIntoViewIfNeeded(el)` before hovering.
- **Safety Net Added:** Unit tests in `BrowserToolProviderStabilityTest` verifying `cleanSelector`, hybrid click with selector prioritizing element click without out-of-bounds error, and coordinate auto-scroll & clamping.

### [DEF-20260919-06] Replay Failure on Compound Step with Coalesced Actions and Missing 'submit' Keyword in partitionToolCallsAndActions
- **Date:** 2026-09-19
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-runner`)
- **Scope:** `Framework`
- **Symptom:** `CartTest.liveNormal` succeeds with an expected bug, but its recorded playbook fails during `CartTest.replayNormal` in `REPLAY_STRICT` mode with: `No recorded tool calls found for step 'Submit the promo code form.' in REPLAY_STRICT mode. Companion JSON recording file is missing or step was not recorded.`
- **Root Cause:**
  1. `matchesSubStep` in `AgentToolLoopStep` checked keywords `click`, `press`, `select`, `choose`, `add`, but omitted `submit`. The submit button click was not matched to 'Submit the promo code form.' and fell back to 'type 'FREEGIFT' into it', leaving the submit sub-step with 0 recorded tool calls.
  2. In `ExecuteActionsStep`, unrolled child sub-steps of a compound step were strictly required to have recorded tool calls in `REPLAY_STRICT` mode, failing on legitimate coalesced sub-steps where a single preceding action (e.g. `fill` clearing and typing) satisfied multiple milestones.
  3. When an assertion failed in `AgentToolLoopStep`, Stop Criterion 2 re-threw `AssertionError` before appending the in-flight tool call to `executedCalls`, preventing the failed assertion from being recorded and partitioned to its sub-step in the companion JSON.
- **Detection Gap ("What did we miss?"):** Existing compound turn group tests only tested 1:1 sub-step-to-tool-call mappings and did not verify replay unrolling of compound steps with coalesced sub-steps or expected defect assertions.
- **Resolution:**
  1. Added `submit` to `matchesSubStep` for `click` in `AgentToolLoopStep`.
  2. Recorded the in-flight assertion `ToolCall` and mapped failed `Action` in `executedCalls` when Stop Criterion 2 triggers in `AgentToolLoopStep`, ensuring failed assertions are captured in companion JSON playbooks.
  3. Updated `ExecuteActionsStep` to allow child sub-steps of a compound parent (`step.getParent() != null`) with 0 tool calls to complete as coalesced no-ops in `REPLAY_STRICT` mode instead of throwing `ConclusiveFailureException`.
- **Safety Net Added:** Unit regression tests in `AgentToolLoopStepTest` (verifying `submit` matching and failed assertion recording) and `ExecuteActionsStepTest` (verifying unrolled compound replay with coalesced sub-steps).

### [DEF-20260919-05] Expected Defect Discovery Loop Death Spiral, Unpartitioned Abortive Exceptions, and Listener Status Fabrication in Compound Steps
- **Date:** 2026-09-19
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-report`)
- **Scope:** `Framework`
- **Symptom:** In `CartTest_livePerfect_perfect_20260919-222644.html`, Step #8 (`Locate the promo code input field:`) failed with `TokenBudgetExceededException: TOTAL budget breached (consumed: 100142, limit: 100000)` across 15 turns. In the HTML report, sub-steps #8.1 through #8.3 were falsely stamped as `SUCCESS` with 0 actions and 0ms duration, while sub-step #8.4 was marked `FAILED` with the parent's `Token budget exceeded` error message.
- **Root Cause:**
  1. **Discovery Loop Death Spiral on Expected Defect:** Step #8 contained 4 compound sub-steps, ending with an expected defect assertion: `Assert that a line item 'Free Bonus Gift' (bug) is added to the cart`. In Verla `perfect`, promo code `FREEGIFT` does not add the bonus line item. The agent checked DOM presence via discovery tool `query_dom({"text": "Free Bonus Gift"})` -> returned 0 matches. Because system prompt instructions strictly prohibited premature `complete_step` before verifying milestones, the agent erroneously deduced that the form submission in turn 2 had not registered, entering a 15-turn death spiral repeatedly re-submitting the promo form until breaching the 100k token limit.
  2. **Unpartitioned Abortive Exceptions:** Action partitioning and duration assignment (`AgentToolLoopStep.partitionToolCallsAndActions`) was only invoked on normal completion (`finishLoop`). When `TokenBudgetExceededException` or timeout aborted the loop, execution bypassed partitioning, leaving all compound child steps with empty actions and 0 duration in the report context.
  3. **Reporting Listener Status Guessing:** `PreliminaryReportListener` lacked handling for abortive infrastructure failures and checked `sub.isBug()`, assuming any failure in a step containing an expected bug was due to the bug, while fabricating `SUCCESS` on preceding sub-steps that had no recorded actions.
- **Detection Gap ("What did we miss?"):** Existing compound turn group tests only tested clean paths where all assertions succeeded, or single-step unrolled failures. None tested an expected bug in a compound step where an assertion milestone failed on an absent element, nor tested that abortive infrastructure exceptions (like token budget limits) properly partition executed actions to the sub-steps executed before the abort.
- **Resolution:**
  1. Updated `AgentToolLoopStep.executeLoop` to wrap the loop execution in `try-finally`, guaranteeing that `finalizeStepExecution` is always executed even on abortive exceptions (`TokenBudgetExceededException`, `StepTimeoutExceededException`), ensuring executed actions and proportional durations are partitioned to child sub-steps. Added idempotency protection (`KEY_STEP_EXECUTION_FINALIZED`) to prevent double-processing.
  2. Refined prompt directives (`systemPrompt`, `turnPrompt`) instructing the LLM that when `query_dom` finds 0 matches for an expected verification milestone, it MUST NOT retry prior form actions, but immediately invoke the commanded assertion tool (`assert_text`, `assert_count`) so expected defects and failures are cleanly asserted and recorded.
  3. Extended `matchesSubStep` in `AgentToolLoopStep` to match `clear`, `empty`, `reset` instructions to `fill`/`clear` tools.
  4. Updated `PreliminaryReportListener` to detect abortive infrastructure failures (`Token budget exceeded`, `timeout`, `Fatal environment`), preventing fabricated `SUCCESS` on unexecuted sub-steps, properly syncing executed actions and statuses from child steps, and attributing the abortive failure to the step active when the abort occurred without falsely blaming expected bugs.
- **Safety Net Added:** Added unit regression tests in `AgentToolLoopStepTest` (`testTokenBudgetExceededInCompoundStepPartitionsExecutedActionsAndSetsDurations`) asserting that when an abortive token exception occurs, executed actions and durations are still partitioned across compound sub-steps, and in `PreliminaryReportListenerTest` (`testAbortiveFailurePreservesSubStepStatusWithoutFabricatedSuccess`) asserting that abortive failures preserve accurate sub-step status and do not fabricate `SUCCESS`.

### [DEF-20260919-04] Quality Judge Container-Hijacking on Compound Steps
- **Date:** 2026-09-19
- **Component:** `neodymium-core` (`QualityJudgePrompt`, `QualityJudgeToolInterceptor`)
- **Scope:** `Framework`
- **Symptom:** `CartTest.liveNormal`, `CartTest.liveBad`, and `CartTest.liveAllDataSets` failed during live recording on the composite add-to-cart step: `liveBad` and `liveAllDataSets` failed with `Expected text/pattern "CART 1" was not found` because the cart item count remained 0; `liveNormal` timed out with `Step timeout of 60s exceeded (elapsed: 60s)`.
- **Root Cause:** In interactive deliberation mode (`QualityJudgeToolInterceptor`), the Judge prompt omitted the active tool action (`Tool: click`) and internal milestones. When evaluating a button click inside a compound step like `Locate the first product card: ... Click its 'Add to Cart' button`, the Judge compared the button locator against the parent header (`Locate the first product card:`) and erroneously refined the locator to the parent card container (`div[data-ai='xcm57t27']` or `article[data-ai='xcboo7um']`). Clicking the container either did not trigger add-to-cart or navigated away to the PDP, causing 15 LLM turns and a 60s timeout.
- **Detection Gap ("What did we miss?"):** Existing unit tests for `QualityJudgePrompt` tested single-line instructions without compound milestones or interactive child buttons inside containers, missing container-hijacking behavior.
- **Resolution:**
  1. Enriched `compileDiscussionRequest` with `Tool: <toolName>`, `Target Locator: <selector>`, and active compound milestones.
  2. Added prompt rules in `quality-judge-discussion-prompt.md` strictly prohibiting the Judge from redirecting interactive element locators (buttons, links, inputs) to parent containers.
  3. Added programmatic safety guardrails (`isContainerHijack`) in `QualityJudgeToolInterceptor` preventing interactive candidates from being replaced by ancestor containers via syntactic CSS hierarchy checks and live DOM Level 3 containment verification.
- **Safety Net Added:** Unit regression tests in `QualityJudgePromptTest` (`testCompileDiscussionRequestWithActionAndMilestones`) and `QualityJudgeToolInterceptorTest` (`testIsContainerHijackDirectDetection`, `testDiscussionRejectsContainerHijackInConsensus`).

### [DEF-20260919-03] Asymmetric Tool Call Cloning across Compound Sub-Steps causing Replay Over-execution and Assertion Desynchronization
- **Date:** 2026-09-19
- **Component:** `neodymium-core` (`AgentToolLoopStep`, `ExecuteActionsStep`)
- **Scope:** `Framework`
- **Symptom:** In `CartTest.replayAllDataSets` (`perfect` and `bad`), replay failed. In `perfect`, Step #4 added an extra item to the cart, causing `AssertionError: Expected "CART 2" but found "CART 3"`. In `bad`, Step #3 clicking Add triggered an HTMX swap of `#cart-btn-wrapper`, leaving `#cart-btn-anchor` detached during action 4 (`assert_text`), causing `ElementNotFound: Element not found {#cart-btn-anchor}`.
- **Root Cause:** When a compound turn group had fewer executed tool calls than sub-steps (e.g. in `bad` Substep 3.4 was a skipped conditional `When this string 'bad' is not equal 'bad', click size 'S'`, resulting in 4 tool calls for 5 sub-steps; in `perfect` Step 4 had 4 tool calls for 5 sub-steps because store was skipped), `AgentToolLoopStep` fallback dumped the full list of tool calls into *every* child sub-step (`child.setToolCalls(sanitizedCalls); child.setActions(actions)`). In replay mode, `ExecuteActionsStep` scheduled each child sub-step sequentially, causing all 4 actions to execute on Substep 1, and all 4 actions to execute again on Substep 2.
- **Detection Gap ("What did we miss?"):** Prior unit tests for compound turn groups only tested 1:1 matching of tool calls to sub-steps or monolithic parent steps, without testing asymmetric counts (skipped conditional sub-steps, skipped store calls) or verifying that individual child sub-steps do not receive duplicate cloned tool lists.
- **Resolution:**
  1. Replaced the cloned fallback in `AgentToolLoopStep` with `partitionToolCallsAndActions`, which sequentially correlates executed tool calls and actions to sub-steps based on instruction keywords and semantic intent, mapping skipped conditional branches to empty tool lists.
  2. Added auto-healing in `ExecuteActionsStep.mapPlaybookStepToPipelineStep` during replay to detect pre-existing playbooks with cloned tool calls (`hasCorruptedClonedCalls`) and dynamically re-partition them on the fly.
- **Safety Net Added:** Added unit regression tests in `ExecuteActionsStepTest` (`testPartitionToolCallsAndActionsAsymmetricMatching`, `testAutoHealsCorruptedClonedToolCallsInReplayMode`), and verified integration replay passes across all datasets.

### [DEF-20260919-02] Ancestor Automation ID Hijacking in Compound Selectors and Missing Sub-Step Activities/Screenshots in Replay
- **Date:** 2026-09-19
- **Component:** `neodymium-core` (`SelenideElementFinder`, `ClickAction`, `ExecuteActionsStep`, `PreliminaryReportListener`, `PlaybookToolReplayer`)
- **Scope:** `Framework`
- **Symptom:** In `CartTest.replayNormal`, Step #3 fails at substep 3.5 with `Expected text/pattern "CART 1" was not found on selector "#cart-btn-anchor" nor anywhere on the page within 3000ms`. Cart badge remains 0 because the size button in the dynamic quick-add dropdown was never clicked. Furthermore, in the HTML report, all sub-steps of the compound step displayed 0 activities and 0 screenshots.
- **Root Cause:**
  1. **Selector Hijacking:** In `SelenideElementFinder.tryResolveAutomationId`, regex matching extracted the first automation ID token in the selector (`xcboo7um`, belonging to the ancestor `<article>`). When the un-stamped dynamic size button failed to match, line 724 queried `[data-ai='xcboo7um']`, returned the visible `<article>`, and bypassed `PageAnalyzer.captureSimplifiedDom`. The replayer clicked the product card container instead of the size button.
  2. **Monolithic Replay Execution:** In `ExecuteActionsStep`, `isLegacySubStepReplay` was guarded by `!parentHasToolCalls`. Because the parent step had recorded tool calls, compound steps were executed as a single monolithic block in replay mode. Sub-steps were never scheduled in the pipeline, so no sub-step lifecycle events (`StepStartedEvent`, `StepFinishedEvent`), sub-step screenshots, or sub-step actions were recorded.
  3. **Missing Sub-Step Action Population:** `PreliminaryReportListener` failed to transfer `childStep.getActions()` into `childEntry` when populating sub-steps.
- **Detection Gap ("What did we miss?"):** Prior unit tests validated compound turn groups in live mode with static instructions, but did not test sequential sub-step unrolling during replay mode, nor did they verify that `ReportStepEntry` sub-steps received their corresponding actions and screenshots.
- **Resolution:**
  1. Updated `SelenideElementFinder.tryResolveAutomationId` to transform all `#xc...` tokens, prioritize `PageAnalyzer.captureSimplifiedDom` on missing elements, and strictly forbid bare `[data-ai='neoId']` fallbacks on compound selectors.
  2. Added `element.shouldBe(Condition.visible)` in `ClickAction` before clicking to ensure dynamic elements are ready for interaction.
  3. Updated `ExecuteActionsStep` to schedule sub-steps in sequence during replay mode (`executionMode.isReplay()`), capturing individual sub-step screenshots, statuses, and durations.
  4. Updated `PlaybookToolReplayer` to dispatch `ActionExecutedEvent` during replay.
  5. Updated `PreliminaryReportListener` to copy actions and screenshots to sub-step report entries.
- **Safety Net Added:** Added regression tests in `SelenideElementFinderTest`, `ExecuteActionsStepTest`, and `PreliminaryReportListenerTest`, and verified `CartTest.replayNormal` passes end-to-end with full sub-step activities and screenshots in the report.

### [DEF-20260919-01] Premature Strict Variable Resolution on Compound Steps with Runtime Placeholders
- **Date:** 2026-09-19
- **Component:** `neodymium-core` (`ai-pipeline`, `playbook-engine`, `ai-runner`)
- **Scope:** `Framework`
- **Symptom:** In `CartTest.liveAllDataSets` (and any playbook step containing dynamic runtime placeholders to be captured on the fly, such as `${lineItemCount}`), step execution fails immediately with `UnresolvableVariableException: Unresolvable variable placeholder '${lineItemCount}' in template: ...` before any browser actions or capture tools can execute.
- **Root Cause:** In `ExecuteActionsStep.mapPlaybookStepToPipelineStep`, compound turn groups and leaf steps invoked strict `contextState.getSessionData().resolveVariables(...)` when resolving instructions and milestones. Because dynamic variables captured during the step (e.g. via `store`) do not exist in `SessionData` at step start, strict resolution threw an `UnresolvableVariableException`. `SessionData.resolveAvailableVariables(...)` was designed specifically to leniently resolve known variables (like `${testId}`) while leaving dynamic placeholders intact, but `ExecuteActionsStep` invoked strict `resolveVariables`. In addition, `StateMachineRunner` used strict resolution in optional and bug step failure logging, risking secondary unhandled exceptions.
- **Detection Gap ("What did we miss?"):** Existing unit tests for compound turn groups in `ExecuteActionsStepTest` (`testCompoundTurnGroupMapsToSingleStepWithMilestonesInLiveMode`) tested instructions with static strings only, without dataset variables or dynamic placeholders.
- **Resolution:**
  1. Updated `ExecuteActionsStep.mapPlaybookStepToPipelineStep` to use `contextState.getSessionData().resolveAvailableVariables(...)` for both the main instruction and internal milestone sub-steps.
  2. Updated `StateMachineRunner` to use `resolveAvailableVariables(...)` when logging bug and optional step failures.
  3. Updated report and linting listeners (`PreliminaryReportListener`, `PostFlightPlaybookLinter`, `PlaybookLinterPrompt`) to use `resolveAvailableVariables(...)` so known variables are resolved cleanly without aborting on uncaptured placeholders.
- **Safety Net Added:** Added unit regression tests in `ExecuteActionsStepTest` (`testCompoundTurnGroupWithRuntimeVariablesPreservesPlaceholdersWithoutFailing`, `testLeafStepWithRuntimeVariablesPreservesPlaceholdersWithoutFailing`) asserting that available variables resolve while runtime placeholders are preserved without throwing.

### [DEF-20260918-05] Visual SSIM Threshold Step Mutation, Missing Parameterized Tag Overrides, and Config Alias Shadowing
- **Date:** 2026-09-18
- **Component:** `neodymium-core` (`ai-model`, `ai-pipeline`, `ai-config`, `ai-junit`)
- **Scope:** `Framework`
- **Symptom:** Inability to override visual assertion SSIM thresholds per step (in YAML playbooks) or per test case via `@AiVisual`. In addition, replay gate execution unconditionally mutated `PlaybookStep.ssimMinScore` from `null` to `0.99`, polluting JSON companion recordings with unintended `ssimMinScore` fields, while static file defaults in `ai.properties` shadowed alias overrides in system properties and thread-local data.
- **Root Cause:**
  1. `VisualBaselineGateStep.execute` and `executePostActionCheck` unconditionally invoked `this.step.setSsimMinScore(minScore)` with the global config score `0.99`. This changed `ssimMinScore` from `null` to `0.99`, causing Jackson (`@JsonInclude(NON_NULL)`) to write `ssimMinScore` into JSON companion files on replay finish, breaking the contract that `(visual)` must use global configuration and not be hardcoded into JSON.
  2. `PlaybookStep` lacked support for comma-separated parameters in visual tags (e.g. `(visual: threshold=0.98)` or `(visual: full,threshold=0.98)`).
  3. `AiConfiguration.getProperty` checked the property file for the primary key (`neodymium.ai.ssim.minScore`) before evaluating fallback aliases, so the default `neodymium.ai.ssim.minScore = 0.99` in `ai.properties` masked dynamic or system property overrides on aliases like `neodymium.ai.visual.threshold`.
- **Detection Gap ("What did we miss?"):** Existing gate tests in `VisualBaselineGateStepTest` only asserted visual matching/divergence behavior, never verifying that `step.getSsimMinScore()` remained `null` for steps tagged with `(visual)`. Configuration tests verified individual properties but did not test alias override precedence against properties file defaults.
- **Resolution:**
  1. Created `@AiVisual` annotation supporting `value()` and `threshold()` attributes for test class and method level SSIM overrides in `NeodymiumAiRunner`.
  2. Updated `PlaybookStep` to support parameterized visual tags: `(visual: threshold=0.98)`, `(visual: full,threshold=0.98)`, percentage formats (`98%`), and `@JsonProperty("threshold")` / `@JsonAlias("threshold")` aliases.
  3. Updated `VisualBaselineGateStep` to evaluate `step.getSsimMinScore()` when present and avoid mutating `step.ssimMinScore` if it was initially `null`.
  4. Updated `AiConfiguration.getVisualSsimMinScore()` to enforce proper multi-tier precedence: thread-local data (`Neodymium.getData()`) -> system properties -> properties files -> default fallback `0.99`.
  5. Updated `PostFlightPlaybookLinter` and `PreliminaryReportListener` to support parameterized visual tags and report accurate thresholds.
- **Safety Net Added:** Added unit regression tests in `PlaybookStepTest` (`testVisualTagVariantsAndThresholdParsing`, `testVisualSerializationExclusionWhenNull`, `testThresholdDeserializationJsonAlias`), `VisualBaselineGateStepTest` (`testReplayWithCustomStepThreshold_passesBelowDefaultThreshold`), `AiConfigurationTest` (`testVisualSsimMinScoreDefaultsAndAliases`), and `NeodymiumAiRunnerTest` (`testAiVisualAnnotationHandling`).

### [DEF-20260918-04] Sub-Step Unrolling Amnesia, Global Scoping Leakage, and Descendant Selector Overscoring in Compound Steps
- **Date:** 2026-09-18
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-util`, `playbook-engine`)
- **Scope:** `Framework`
- **Symptom:** In `CartTest.liveBad`, execution timed out during `Verify that the cart item count is higher than ${lineItemCount}.` with `AssertionError: Cart line item count was not greater than 0 within 3000ms`. The LLM searched for the shopping cart badge inside the product card's DOM fragment and repeatedly evaluated irrelevant child elements.
- **Root Cause:**
  1. **Sub-Step Unrolling & Scoping Leakage:** `ExecuteActionsStep` previously unrolled indented YAML turn groups (e.g. `Locate the first product card:`) into independent pipeline steps, setting each sub-step's parent to the group header. In `AgentToolLoopStep`, this injected `### Scoping Context: Locate the first product card:` into global assertion sub-steps, misleading the agent into searching for global header elements (cart badge) within the scoped product card.
  2. **Conversational Amnesia:** Each unrolled sub-step started a brand new isolated tool loop, causing the agent to lose context of the actions it had just taken in the preceding sub-steps of the group.
  3. **Descendant Selector Overscoring:** `LocatorImprover.scoreLocator` scored any selector containing `#` as a perfect 10/10, even if it contained descendant combinators (e.g. `#prod-info div`). This bypassed Quality Judge deliberation and locked the agent into fragile descendant selectors.
- **Detection Gap ("What did we miss?"):** Existing composite step tests (`CompositeStepTest`) only validated sequential execution order and serialization for replay, but never evaluated live LLM interactions where a turn group combines contextual actions (locating, hovering, clicking) with a global assertion (verifying header cart badge count). `LocatorImproverTest` verified single ID selectors like `#submit-btn`, but lacked assertions ensuring descendant combinators were penalized.
- **Resolution:**
  1. Updated `ExecuteActionsStep.mapPlaybookStepToPipelineStep` to execute turn groups as a single compound `AgentToolLoopStep` with milestones, restricting unrolling strictly to `_include:` files and legacy sub-step replays.
  2. Tightened `LocatorImprover.scoreLocator` to require `!trimmed.contains(" ") && !trimmed.contains(">")` before awarding 10/10 to ID selectors.
  3. Refined `AgentToolLoopStep` multi-turn prompt guidance to instruct the agent to fulfill all milestone actions and commanded verifications before calling `complete_step`.
  4. Updated step completion hooks in `ExecuteActionsStep`, `AgentToolLoopStep`, and `StateMachineRunner` to distribute status, duration, actions, and tool calls to child sub-steps for report fidelity.
- **Safety Net Added:** Added unit regression tests in `ExecuteActionsStepTest` (`testCompoundTurnGroupMapsToSingleStepWithMilestonesInLiveMode`, `testIncludeStepUnrollsSubSteps`) and `LocatorImproverTest` (`testScoreLocatorDescendantCombinatorWithIdNotPerfectScore`).

### [DEF-20260918-03] Multi-Turn DOM Amnesia, Tool Thrashing, and Discovery Tool Action Pollution in Agent Tool Loop
- **Date:** 2026-09-18
- **Component:** `neodymium-core` (`ai-pipeline`, `ai-tool`)
- **Scope:** `Framework`
- **Symptom:** In multi-field form steps (such as filling credit card number, expiry date, and CVV), the agent executed redundant discovery tools (`query_dom`), blind exploratory scrolling (`scroll down`), and re-querying across 7 turns. The read-only discovery tools and blind scrolls were erroneously recorded as persistent test actions in the execution report and playbook (`QUERY_DOM`, `SCROLL`).
- **Root Cause:**
  1. `pruneExpiredDomFromConversation` unconditionally wiped the DOM from Turn 1 without verifying if a replacement DOM was being provided for Turn 2 (`requireDomForNextTurn == false`), completely blinding the LLM of selectors.
  2. The continuation prompt actively nudged the model to call `query_dom`.
  3. `query_dom` used case-sensitive CSS selectors, missing camelCase attributes (`cardExpiry`, `cardCvv`), prompting an uncommanded off-screen scroll.
  4. `finishLoop` failed to filter out discovery/inspection tools (`query_dom`, `inspect`, `request_context`), polluting `PlaybookStep.actions` and `PlaybookStep.toolCalls`.
  5. Strict single-action serialization prevented the agent from proposing cohesive form field inputs in a single turn.
- **Detection Gap ("What did we miss?"):** Tests verified that `pruneExpiredDomFromConversation` pruned messages to save tokens, but did not verify that selector accessibility was maintained across multi-action turns or that discovery tools were excluded from recorded playbook actions.
- **Resolution:**
  1. Updated `pruneExpiredDomFromConversation` to preserve DOM/element selectors when no new DOM snapshot is added for the next turn.
  2. Excluded read-only discovery tools (`query_dom`, `inspect`, `request_context`, `inspect_visual`) from recorded `actions` and `toolCalls` in `finishLoop`.
  3. Enabled case-insensitive attribute fallback in `query_dom` JavaScript.
  4. Allowed cohesive form field action batching for multi-input instructions.
- **Safety Net Added:** Added unit and integration tests verifying that multi-field steps execute cleanly without amnesia, that discovery tools are not recorded as playbook actions, and that `query_dom` handles case-insensitive attribute matching.

### [DEF-20260918-02] Elimination of Fragile Natural Language Text Guessing in SemanticIntent
- **Date:** 2026-09-18
- **Component:** `neodymium-core` (`ai-model`, `ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** `SemanticIntent.inferFromInstruction` attempted to deduce step intent from natural language instruction text using hardcoded English substring patterns (`contains(" says now ")`, `contains(" is shown")`, `contains(" is mentioned")`), introducing fragile keyword-guessing heuristics into core Java logic and violating Neodymium's universal, language-neutral design.
- **Root Cause:** Following the removal of PESAP (which previously supplied LLM-classified intent), an ad-hoc keyword-matching method `inferFromInstruction` was introduced in commit `96b9c315f` to guess intent for unclassified steps, embedding English-specific phrasing assumptions into Java core logic.
- **Detection Gap ("What did we miss?"):** Unit tests only verified that `inferFromInstruction` matched specific predefined English test sentences, without verifying language neutrality or handling diverse phrasings.
- **Resolution:** Removed `SemanticIntent.inferFromInstruction` completely. Reverted `AgentToolLoopStep` to rely strictly on explicitly configured or recorded `SemanticIntent` (or `null` when unspecified), allowing the LLM's system prompt instructions to govern action vs. verification execution without heuristic second-guessing.
- **Safety Net Added:** Cleaned `SemanticIntentTest` to eliminate keyword inference tests; verified that `AgentToolLoopStepTest` executes cleanly without keyword guessing.

### [DEF-20260918-01] Premature Step Completion in Multi-Field Action Instructions Due to Single-Action Prompt Nudge
- **Date:** 2026-09-18
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `GermanCheckoutTest.live` ([GermanCheckoutTest_live_perfect_20260918-002600.html](file:///home/rschwietzke/projects/GIT/neodymium-library/target/ai-results/GermanCheckoutTest_live_perfect_20260918-002600.html)), Step 18 (`Warte bis der Text "Thank you for your purchase!" erscheint.`) timed out with `AssertionError: Expected text/pattern "Thank you for your purchase!" was not found anywhere on the page within 3000ms`.
- **Root Cause:** In Step 16 (`Kartennummer ist '4111 1111 1111 1111', Ablaufdatum '12/29' und CVV ist '111'.`), the agent filled `#cardNumber` in Turn 1. In Turn 2, the post-action turn prompt introduced in DEF-20260917-01 (*"If this was an action instruction, invoke 'complete_step' now without performing uncommanded assertions or anticipating subsequent steps"*) combined with Operating Rule 4 (*"the step goal is completely satisfied once the action executes"*) caused the LLM to call `complete_step` prematurely, leaving `#cardExpiry` and `#cardCvv` empty. Clicking purchase in Step 17 failed client-side validation, so the confirmation page never loaded.
- **Detection Gap ("What did we miss?"):** DEF-20260917-01 tested atomic single-action steps to verify that the agent did not perform uncommanded assertions, but did not test compound action steps requiring multiple sequential `fill` or `click` actions within a single instruction.
- **Resolution:**
  1. Updated `AgentToolLoopStep` post-action turn prompt to state: *"If this was an action instruction and all actions/fields requested in the instruction have been executed, invoke 'complete_step' now without performing uncommanded assertions or anticipating subsequent steps. If the instruction explicitly requested additional fields or actions that have not yet been executed, continue executing the remaining actions."*
  2. Updated Operating Rule 4 in `AgentToolLoopStep` to clarify that multi-action instructions are satisfied only when all explicitly commanded actions/fields are fulfilled.
- **Safety Net Added:** Added regression unit test `AgentToolLoopStepTest#testMultiFieldActionInstructionReceivesRefinedTurnPromptAndAllowsSequentialExecution` verifying that a multi-field fill instruction continues across turns until all requested fields are filled before invoking `complete_step`.

### [DEF-20260917-01] Premature Fast-Break in assert_text Wait Loop and Agent Over-Verification in Action Steps
- **Date:** 2026-09-17
- **Component:** `neodymium-core` (`ai-tool`, `ai-pipeline`, `ai-model`)
- **Scope:** `Framework`
- **Symptom:** During strict replay of recorded playbooks (such as `CheckoutTest.replay` on the `bad` dataset), Step 3 (`Locate the first product card and click its 'Add to Cart' or 'Add' button.`) failed unexpectedly with `Tool execution error in 'assert_text': Expected text "CART 1" was not found on element "#cart-btn-anchor"`, aborting execution before reaching Step 4 (`The mini cart quantity is now 1.`).
- **Root Cause:**
  1. Operating rules in `AgentToolLoopStep` universally commanded *"You MUST invoke an assertion tool before calling 'complete_step'"* without separating action steps from verification steps. During live recording, the LLM felt obliged to assert the cart counter after clicking, anticipating the next step and recording an uncommanded `assert_text` into Step 3's playbook.
  2. In `BrowserToolProvider.assert_text`, the polling retry loop contained a premature `if (isTextPresentOnPage(...)) break;`. When the asynchronous HTMX response (`api/cart/add`) began arriving and updated text anywhere on the page, the tool prematurely broke out of its retry loop rather than polling the target element up to `Configuration.timeout`.
- **Detection Gap ("What did we miss?"):** Existing `assert_text` unit tests verified immediate matches and page-wide fallbacks with mock drivers, but did not test asynchronous DOM mutation scenarios where text appears elsewhere on the page while the target element is still updating.
- **Resolution:**
  1. Removed the premature `isTextPresentOnPage(...) -> break` abort from `BrowserToolProvider.assert_text`, ensuring the tool polls the target element for the full `Configuration.timeout` duration.
  2. Differentiated action vs assertion execution in `AgentToolLoopStep` prompts (with keyword guessing subsequently eliminated in [DEF-20260918-02]).
  3. Restructured `AgentToolLoopStep` operating rules into distinct `Action steps` and `Verification steps` sections and updated post-action turn guidance to complete action steps immediately without uncommanded assertions.
- **Safety Net Added:**
  - `BrowserToolProviderStabilityTest#testAssertTextWaitsForAsyncTargetElementUpdateEvenIfTextIsPresentElsewhereOnPage` ensuring `assert_text` polls continuously until target element updates.

### [DEF-20260916-05] Complete Removal of PESAP Tracing & Reporting and Prompt Cleanliness
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-report`, `ai-runner`, `ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** Reports (HTML, Markdown, JSON) continued to render "PESAP (Pre-Execution Semantic Anchor)" category rows and badge cards with zero calls. StateMachineRunner and AiSession logged empty PESAP metrics in console banners, and AgentToolLoopStep appended conversational "What is your next tool call?" trailing prompts to user turns.
- **Root Cause:**
  1. HTML, Markdown, and JSON report generators retained hardcoded category rows, badges, and JavaScript/CSS handlers for PESAP metrics.
  2. StateMachineRunner, AiSession, and InteractiveStateBuilder retained legacy PESAP token accumulators and banner debug statements.
  3. AgentToolLoopStep retained conversational scaffolding ("What is your next tool call?") originally meant for text-completion agents rather than native function-calling APIs.
- **Detection Gap ("What did we miss?"):** Report unit tests asserted the presence of the PESAP row rather than validating that decoupled/deprecated phases are omitted from user-facing accounting.
- **Resolution:**
  1. Removed "PESAP (Pre-Execution Semantic Anchor)" row and all phase badge/JS logic from HtmlReportGenerator and MarkdownReportGenerator.
  2. Removed PESAP accumulation and console tracing from StateMachineRunner, AiSession, and InteractiveStateBuilder.
  3. Removed "What is your next tool call?" prompts from AgentToolLoopStep turns and updated DOM pruning to be content-boundary-based.
  4. Updated PreliminaryReportListenerTest to assert that PESAP is absent from report tables and JSON metrics.
- **Safety Net Added:** Automated assertions in `PreliminaryReportListenerTest#testReportTokenAccountingAndCategoryBreakdown` verifying absence of "PESAP" in HTML, Markdown, and JSON reports.

### [DEF-20260916-04] Silent Test Pass on Mismatched @AiDataSet Filter
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-junit`)
- **Scope:** `Framework`
- **Symptom:** Running test classes like `SearchGermanTest` resulted in `Tests run: 0, Failures: 0, Errors: 0, Skipped: 0` and reported Maven build success without executing any actual test steps.
- **Root Cause:**
  1. `NeodymiumAiRunner.provideTestTemplateInvocationContexts` filtered datasets against method- and class-level `@AiDataSet` annotations using `shouldIncludeDataSet`.
  2. When `@AiDataSet` specified dataset IDs or includes that did not match any dataset defined in the referenced playbook YAML (e.g. `@AiDataSet("perfect")` on `SearchGermanTest` vs playbook datasets `['US', 'DE', 'FIN']`), `filteredDataSets` resulted in an empty list.
  3. The runner returned an empty stream of `TestTemplateInvocationContext`, which JUnit 5 interpreted as 0 invocations, producing a silent green build.
  4. Multiple test classes (`SearchGermanTest`, `EnglishCheckoutTest`, and `RegisterTest` in `basic` and `full`) carried stale `@AiDataSet` annotations or dead test methods from earlier template copies.
- **Detection Gap ("What did we miss?"):** JUnit 5 `@TestTemplate` test engines do not consider zero invocations as an error by default. Test runners did not validate that explicit user-provided `@AiDataSet` filters matched at least one dataset in the playbook before generating test invocations.
- **Resolution:**
  1. Corrected `@AiDataSet("DE")` in `SearchGermanTest` and `@AiDataSet("canada-fr")` in `EnglishCheckoutTest`.
  2. Pruned dead test methods in basic and full `RegisterTest` that referenced non-existent datasets.
  3. Added fail-fast validation in `NeodymiumAiRunner.provideTestTemplateInvocationContexts` that throws `IllegalArgumentException` with available dataset IDs whenever explicit `@AiDataSet` filters match zero datasets.
- **Safety Net Added:** Unit test `NeodymiumAiRunnerTest#testUnmatchedDataSetThrowsException` verifying that unmatched `@AiDataSet` triggers fail-fast `IllegalArgumentException`.

### [DEF-20260916-03] PESAP Pipeline Decoupling & Direct Agent Execution Migration
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** Upfront PESAP (Pre-Execution Step Analysis & Partitioning) introduced mandatory out-of-band LLM calls prior to action execution, created architectural tight coupling with regex/language heuristics, and constrained instruction autonomy in `AgentToolLoopStep`.
- **Root Cause:** PESAP was originally designed as a speculative pre-classifier that partitioned steps and guessed context levels before seeing actual browser execution results. This architectural separation created redundancy with the agent tool loop and violated language neutrality by encouraging intent keyword sniffing and premature DOM exclusion hacks.
- **Detection Gap ("What did we miss?"):** Early AI test designs tested PESAP in isolation via `PesapPreStepTest`, rather than measuring end-to-end latency, multilingual robustness, and tool-loop adaptability without upfront classification overhead.
- **Resolution:**
  1. Decoupled `PesapPreStep` from `ExecuteActionsStep.mapPlaybookStepToPipelineStep()`, enabling direct tool loop execution for live recording steps.
  2. Deprecated `PesapPreStep`, `PesapPrompt`, and `isPesapEnabled()`, defaulting PESAP configuration to `false`.
  3. Streamlined `AgentToolLoopStep` Turn 1 SUT capture to universal `ContextLevel.LEAN` baseline and refined completion/verification prompt guidance.
- **Safety Net Added:** Comprehensive test suite execution across `AgentToolLoopStepTest` (38 tests) and `ExecuteActionsStepTest` (9 tests) ensuring direct agent autonomous execution succeeds without upfront PESAP calls.

### [DEF-20260916-02] Pre-Action Visual Baseline Check Prematurely Aborted Replay for Visual Steps with Mutating Actions
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `SearchTest_German_testSearchDeReplayDe_DE`, step 11 (`Auf der linken Seite wird eine Box mit Kategorien, Farben, Preis und Angebot angezeigt (visual).`) failed during replay with `DivergenceException: Visual SSIM score below threshold (score: 0.5352 < 0.99)` because the page was not scrolled down.
- **Root Cause:**
  1. During live recording, the LLM executed an interactive/viewport action `scroll(direction: "down", yOffset: 300)`. `VerifyOutcomeStep` captured the post-action screenshot at `scrollY = 300` and saved its SSIM matrix as `step.screenshotHash`.
  2. During replay, `VisualBaselineGateStep.executeGate()` was invoked before replaying step actions (at `scrollY = 0`).
  3. `VisualBaselineGateStep` assumed all visual steps (`step.isVisualStep()`) were pure verification steps and evaluated live pre-action screen state against `step.screenshotHash`. When the pre-action screen did not match the post-action baseline (SSIM 0.5352 < 0.99), line 235 threw a `DivergenceException` immediately, preventing `PlaybookToolReplayer.replayStep()` from ever executing the recorded `SCROLL` action.
- **Detection Gap ("What did we miss?"):**
  - Existing unit tests for `VisualBaselineGateStep` only tested visual steps with zero actions or with `ASSERT` actions (`Action("ASSERT", ...)`); none tested steps containing mutating or viewport actions like `SCROLL` or `CLICK`.
  - The pipeline assumed `step.isVisualStep()` implied a pure verification step with no mutating actions, ignoring cases where an LLM calls non-mutating tools or scroll actions during visual assertions.
- **Resolution:**
  1. Added `VisualBaselineGateStep.isPureVerification()` to distinguish steps that only contain assertions/NO-OPs from steps that contain recorded mutating actions or tool calls (`SCROLL`, `CLICK`, etc.).
  2. Updated `VisualBaselineGateStep.executeGate()` to defer visual baseline comparison when `!isPureVerification() && coordinateTarget == null`, allowing recorded actions to execute first without throwing pre-action divergence.
  3. Added `VisualBaselineGateStep.executePostActionCheck()` and wired it into `ExecuteActionsStep`'s `standardFlow` after action replay and post-step state settling/capture to verify the post-action visual outcome against the recorded baseline.
- **Safety Net Added:**
  - Added 4 unit tests in `VisualBaselineGateStepTest`:
    - `testReplayWithMutatingAction_preActionGateDoesNotThrowDivergence()`
    - `testReplayWithMutatingAction_postActionCheckWithMatchingBaseline_succeeds()`
    - `testReplayWithMutatingAction_postActionCheckWithDivergentBaseline_throwsDivergenceException()`
    - `testReplayWithMutatingAction_postActionCheck_supportsHealing_throwsHealingRequiredException()`
  - Added 2 integration tests in `ExecuteActionsStepTest`:
    - `testReplayVisualStepWithActionExecutesActionThenVerifiesPostActionVisualBaseline()`
    - `testReplayVisualStepWithActionThrowsDivergenceWhenPostActionVisualBaselineDiffers()`

### [DEF-20260916-01] Full-Page Visual Assertion Baseline Recorded As Viewport Capture During Multi-Turn Tool Loops
- **Date:** 2026-09-16
- **Component:** `neodymium-core` (`ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** In `VerlaGuestCheckout_Pl_Polish_testCheckoutReplayPerfect`, step 19 (`Na środku ekranu znajduje się zielony znacznik wyboru (visual: full).`) failed during replay with SSIM 0.6878 against the recorded baseline (required >= 0.99), causing a `DivergenceException`.
- **Root Cause:**
  1. In `AgentToolLoopStep`, for visual assertions, `activeContextLevel` is `ContextLevel.VISUAL`, where `ContextLevel.isFullPageScreenshot()` evaluates to `false`. When an agent loop required multiple turns (e.g. recovering from an invalid tool call), subsequent turns refreshed page state using `executor.captureState(activeContextLevel, activeContextLevel.isFullPageScreenshot())`. This captured a 1500x857 viewport screenshot and overwrote `KEY_LAST_STATE`.
  2. In `VerifyOutcomeStep`, for steps without mutating DOM actions (`step.getActions().isEmpty()`), the pipeline previously discarded `KEY_POST_ACTION_STATE` (which had been freshly captured as full-page by `ExecuteActionsStep`) and fell back to `KEY_LAST_STATE` (overwritten with the viewport capture). It hashed that viewport capture while marking `step.setFullPage(true)`.
  3. During replay, `VisualBaselineGateStep` checked `step.isFullPageVisualStep()`, correctly captured a 1500x1122 full-page screenshot, and compared it against the recorded 1500x857 viewport hash, causing SSIM 0.6878 < 0.99 (`DivergenceException`).
- **Detection Gap ("What did we miss?"):**
  - Existing test `testVisualStepDoesNotConsumeStalePostActionState` in `VerifyOutcomeStepTest` specifically tested that viewport visual steps without actions discard `KEY_POST_ACTION_STATE` in favor of viewport `KEY_LAST_STATE`, but did not test full-page visual steps (`(visual: full)` or `step.isFullPageVisualStep()`).
  - Unit tests for `AgentToolLoopStep` did not verify the `isFullPage` flag passed to `captureState` during intermediate turns.
- **Resolution:**
  1. Updated `AgentToolLoopStep` lines 357, 1086, and 1195 so that initial, intermediate, and visual observation state captures evaluate `isFullPage` considering `(step != null && step.isFullPageVisualStep()) || Boolean.TRUE.equals(context.getTransientData().get("KEY_IS_FULL_PAGE_SCREENSHOT")) || activeContextLevel.isFullPageScreenshot()`.
  2. Updated `VerifyOutcomeStep` lines 116-124 to preserve and consume `KEY_POST_ACTION_STATE` whenever `isFullPageReq` is true, even when `step.getActions()` is empty.
- **Safety Net Added:**
  - Added unit test `testVisualStepWithFullPagePreservesPostActionState` in `VerifyOutcomeStepTest`.
  - Added unit test `testMultiTurnFullPageVisualStepPreservesFullPageCapture` in `AgentToolLoopStepTest`.
  - Added full-page capture tracking (`capturedFullPageFlags`) in `MockTargetExecutor`.
