# [DEF-20260919-03] Asymmetric Tool Call Cloning across Compound Sub-Steps causing Replay Over-execution and Assertion Desynchronization

- **Status:** Resolved
- **Opened:** 2026-09-19
- **Closed:** 2026-09-19
- **Component:** `neodymium-core` (`AgentToolLoopStep`, `ExecuteActionsStep`)
- **Scope:** `Framework`
- **Symptom:** In `CartTest.replayAllDataSets` (`perfect` and `bad`), replay failed. In `perfect`, Step #4 added an extra item to the cart, causing `AssertionError: Expected "CART 2" but found "CART 3"`. In `bad`, Step #3 clicking Add triggered an HTMX swap of `#cart-btn-wrapper`, leaving `#cart-btn-anchor` detached during action 4 (`assert_text`), causing `ElementNotFound: Element not found {#cart-btn-anchor}`.
- **Root Cause:** When a compound turn group had fewer executed tool calls than sub-steps (e.g. in `bad` Substep 3.4 was a skipped conditional `When this string 'bad' is not equal 'bad', click size 'S'`, resulting in 4 tool calls for 5 sub-steps; in `perfect` Step 4 had 4 tool calls for 5 sub-steps because store was skipped), `AgentToolLoopStep` fallback dumped the full list of tool calls into *every* child sub-step (`child.setToolCalls(sanitizedCalls); child.setActions(actions)`). In replay mode, `ExecuteActionsStep` scheduled each child sub-step sequentially, causing all 4 actions to execute on Substep 1, and all 4 actions to execute again on Substep 2.
- **Detection Gap ("What did we miss?"):** Prior unit tests for compound turn groups only tested 1:1 matching of tool calls to sub-steps or monolithic parent steps, without testing asymmetric counts (skipped conditional sub-steps, skipped store calls) or verifying that individual child sub-steps do not receive duplicate cloned tool lists.
- **Resolution:**
  1. Replaced the cloned fallback in `AgentToolLoopStep` with `partitionToolCallsAndActions`, which sequentially correlates executed tool calls and actions to sub-steps based on instruction keywords and semantic intent, mapping skipped conditional branches to empty tool lists.
  2. Added auto-healing in `ExecuteActionsStep.mapPlaybookStepToPipelineStep` during replay to detect pre-existing playbooks with cloned tool calls (`hasCorruptedClonedCalls`) and dynamically re-partition them on the fly.
- **Safety Net Added:** Added unit regression tests in `ExecuteActionsStepTest` (`testPartitionToolCallsAndActionsAsymmetricMatching`, `testAutoHealsCorruptedClonedToolCallsInReplayMode`), and verified integration replay passes across all datasets.
