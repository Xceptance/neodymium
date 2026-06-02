## Why

Multi-action instructions (e.g., "Click Login, then verify dashboard") are difficult to execute robustly in a single step because subsequent actions often depend on page loads, animations, or state transitions that have not yet occurred or completed when the first action is being planned. When the second part of a multi-action cannot be identified reliably (due to missing elements, pending page transitions, or requiring higher context levels), the agent must currently either guess, escalate prematurely, or fail the entire step. This change introduces a mechanism to split a multi-action step into two separate, sequential steps dynamically during execution.

## What Changes

- **SPLIT Action Type**: Introduce a new core `Action` type called `SPLIT` (or `SPLIT_STEP`).
- **Dynamic Step Splitting**: When the LLM returns a `SPLIT` action, the execution engine will execute the preceding actions, mark the current step as completed, and dynamically insert a new step containing the remaining natural language instruction directly after the current step in the execution list.
- **LLM System Prompt Updates**: Update the system prompts to teach the LLM about the `SPLIT` action type and instruct it to use this action type when it cannot reliably identify or execute the second part of a multi-stage instruction yet.

## Capabilities

### New Capabilities
- `split-multi-action`: Allows the AI agent to dynamically split a multi-action instruction into two sequential steps when the second part cannot be reliably identified or executed immediately.

### Modified Capabilities

## Impact

- **Affected Code**: 
  - `com.xceptance.neodymium.ai.action.Action` (to support the new action type/fields)
  - `com.xceptance.neodymium.ai.core.AiAgent` (to intercept `SPLIT` actions and dynamically insert the new steps)
  - `com.xceptance.neodymium.ai.core.AiAgentPrompts` (system prompts to document and instruct the LLM on `SPLIT` actions)
- **API Impact**: Internal AI engine capability, no public API signature changes.
