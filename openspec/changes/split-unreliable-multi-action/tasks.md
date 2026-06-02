## 1. Action Class Support

- [ ] 1.1 Document and verify the `Action` representation for the `SPLIT` action type.

## 2. AiAgent Prompts Update

- [ ] 2.1 Update system prompts in `AiAgentPrompts.java` to define the `SPLIT` action type.
- [ ] 2.2 Add instruction examples in `AiAgentPrompts.java` showing when the LLM should return a `SPLIT` action (using the dropdown profile navigation example).

## 3. AiAgent Execution Loop Modification

- [ ] 3.1 Update the execution loop in `AiAgent.java` to detect `SPLIT` actions.
- [ ] 3.2 Implement execution of preceding actions when a `SPLIT` action is encountered.
- [ ] 3.3 Implement dynamic insertion of the remaining instruction into the active `stepsList` and `stepLines` lists.
- [ ] 3.4 Implement dynamic insertion of the new `PlaybookStep` in the `Playbook` steps list.
- [ ] 3.5 Terminate the compound step execution loop upon handling the `SPLIT` action to proceed to the next (newly inserted) step.

## 4. Verification & Testing

- [ ] 4.1 Write a unit/integration test verifying the dynamic step splitting logic.
- [ ] 4.2 Run tests locally to ensure full functionality.
