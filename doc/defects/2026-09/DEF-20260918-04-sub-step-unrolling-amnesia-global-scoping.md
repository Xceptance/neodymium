# [DEF-20260918-04] Sub-Step Unrolling Amnesia, Global Scoping Leakage, and Descendant Selector Overscoring in Compound Steps

- **Status:** Resolved
- **Opened:** 2026-09-18
- **Closed:** 2026-09-18
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
