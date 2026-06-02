## ADDED Requirements

### Requirement: Dynamic Multi-Action Step Splitting
The AI execution engine SHALL support a `SPLIT` action type that allows a compound, multi-stage instruction to be divided into two sequential steps at runtime when the second part of the instruction cannot be reliably executed or identified yet. 

Upon encountering a `SPLIT` action in the action list of the current step, the engine SHALL:
1. Execute all actions that precede the `SPLIT` action in the list.
2. Mark the current step as successfully completed and update the playbook with the successfully executed actions.
3. Dynamically insert a new step with the remaining instruction text (provided as the `value` or `target` of the `SPLIT` action) directly after the current step in both the in-memory execution queue (`stepsList`) and the playbook step list.
4. End the current step execution loop immediately and continue with the next step.

#### Scenario: Dynamic splitting of a multi-action step
- **WHEN** the agent receives an action list containing `Action(type="CLICK", target="#login-btn")` followed by `Action(type="SPLIT", value="type 'user@example.com' into the username field")`
- **THEN** the engine SHALL execute the `CLICK` action, persist only the `CLICK` action to the current playbook step, and dynamically insert the step `"type 'user@example.com' into the username field"` immediately following the current step for subsequent execution.

#### Scenario: Splitting a dropdown navigation compound step
- **WHEN** the agent is executing the step `"Click the profile icon at the top right, then click 'Create Account' below the welcome text in the just opened drop down."` and the dropdown elements are not visible yet
- **THEN** the engine SHALL receive a `SPLIT` action with `value="click 'Create Account' below the welcome text in the just opened drop down"`, execute the click action on the profile icon, mark the first step as done, and dynamically insert the step `"click 'Create Account' below the welcome text in the just opened drop down"` immediately following the current step.

