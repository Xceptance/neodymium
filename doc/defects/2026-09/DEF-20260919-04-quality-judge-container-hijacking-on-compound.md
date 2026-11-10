# [DEF-20260919-04] Quality Judge Container-Hijacking on Compound Steps

- **Status:** Resolved
- **Opened:** 2026-09-19
- **Closed:** 2026-09-19
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
