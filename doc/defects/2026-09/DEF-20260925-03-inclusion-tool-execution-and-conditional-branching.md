# [DEF-20260925-03] Inclusion Tool Execution and Conditional Branching Fallback in AgentToolLoopStep

- **Status:** Resolved
- **Opened:** 2026-09-25
- **Closed:** 2026-09-25
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
