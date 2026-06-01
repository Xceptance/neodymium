## Context

Currently, Neodymium AI's execution engine in `AiAgent.java` handles multi-stage compound steps using a loop that continues as long as `playbook.isRecording()` and the LLM returns `done: false`. However, if subsequent parts of a compound step cannot be identified reliably yet (e.g., because a modal hasn't fully loaded, or the target element isn't present in the current DOM state), the engine is forced to either fail or escalate context prematurely.

For example, when executing:
`Click the profile icon at the top right, then click 'Create Account' below the welcome text in the just opened drop down.`
The dropdown dropdown elements are not loaded or visible in the DOM when the step starts. When the LLM decides what actions to take, it cannot reliably identify the selector or target for clicking the 'Create Account' link because the dropdown hasn't been opened yet.

This design introduces a `SPLIT` action type, enabling the LLM to cleanly divide a compound step into two sequential steps:
1. Execute `CLICK` on the profile icon, and split the remaining instruction.
2. The remaining instruction (`click 'Create Account' below the welcome text in the just opened drop down`) is dynamically inserted as the next step, allowing the page to update, so the next execution loop can resolve the elements under a new state.


## Goals / Non-Goals

**Goals:**
- Define a new `SPLIT` action type.
- Support dynamic step insertion at runtime in `AiAgent.java`.
- Update system prompts to instruct the LLM on how and when to use the `SPLIT` action type.

**Non-Goals:**
- Framework-side heuristic parsing/splitting of steps without LLM guidance. The decision to split is driven entirely by the LLM.

## Decisions

### 1. Representation of the Split Action
We will define a new action type `"SPLIT"`. The remaining natural language instruction that represents the second part of the step will be stored in the `value` field of the action.

*Alternative Considered*: Using a separate field like `remainingInstruction`.
*Rationale*: Reusing the existing `value` field avoids complicating the `Action` class schema and matches standard JSON serialization of the LLM responses.

### 2. Runtime Injection in `AiAgent.java`
When a `SPLIT` action is encountered:
1. The engine executes all actions preceding the `SPLIT` action in the list.
2. The current step's loop terminates.
3. The engine dynamically inserts the remaining instruction into the active `stepsList` at `i + 1`.
4. The `stepLines` list is padded with `null` at `i + 1` to maintain index alignment.
5. In the `Playbook`, a new `PlaybookStep` with the remaining instruction is created and inserted at `i + 1`.

## Risks / Trade-offs

- **Infinite Splitting**: If the LLM repeatedly returns a `SPLIT` action for the same step, it could lead to an infinite execution loop.
  - *Mitigation*: We will add a threshold or rely on the maximum step execution/retry limits. Since each split creates a new separate step, standard limits apply.
