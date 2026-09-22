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
