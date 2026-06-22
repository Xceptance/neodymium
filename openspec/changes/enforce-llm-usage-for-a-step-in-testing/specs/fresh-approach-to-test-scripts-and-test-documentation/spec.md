## MODIFIED Requirements

### Requirement: List-Based Steps & Standard Playbook Lifecycle
The framework SHALL support defining test steps as a standard YAML list under the `_steps` key.
The framework SHALL support marking individual steps to enforce live LLM evaluation by prefixing the step string with the `!(llm)` marker. 
To make playbooks highly organized, the framework SHALL support the standard modern execution lifecycle blocks:
- **`_beforeAll` (Static Scope):** Setup steps executed once before any tests start.
- **`_beforeEach` (Iteration Scope):** Setup steps executed before each dataset iteration.
- **`_steps` (Iteration Scope):** The main execution sequence.
- **`_onSuccessEach` (Iteration Scope):** Executed right after `_steps` of an iteration succeeds, before `_afterEach`.
- **`_onFailureEach` (Iteration Scope):** Executed right after `_steps` of an iteration fails, before `_afterEach`.
- **`_afterEach` (Iteration Scope):** Teardown steps executed after each dataset iteration (always runs, like a `finally` block).
- **`_onSuccessAll` (Static Scope):** Executed once after all dataset iterations succeed, before `_afterAll`.
- **`_onFailureAll` (Static Scope):** Executed once if any part of the playbook fails, before `_afterAll`.
- **`_afterAll` (Static Scope):** Teardown steps executed once after all dataset iterations complete (always runs, like a `finally` block).

To allow free-text natural comments, the parser SHALL tolerate and ignore any unrecognized keys at the root level or dataset maps that do not start with a leading underscore (Human Space).
To prevent human typos in framework directives, the parser SHALL validate all keys starting with a leading underscore (Machine Space). If an underscored key is not in the registered keys (`_meta`, `_properties`, `_context`, `_data`, `_beforeAll`, `_beforeEach`, `_steps`, `_onSuccessEach`, `_onFailureEach`, `_afterEach`, `_onSuccessAll`, `_onFailureAll`, `_afterAll`, `_include`, `_id`), the parser SHALL throw a `MalformedPlaybookException`.

#### Scenario: Valid lifecycle blocks and comments
- **WHEN** the playbook contains modernized lifecycle lists and arbitrary un-underscored keys
- **THEN** the parser successfully extracts the steps and ignores the un-underscored keys

#### Scenario: Typo in framework key
- **WHEN** the playbook contains an invalid underscored key (e.g. `_beforeeach:`)
- **THEN** the parser throws a `MalformedPlaybookException` indicating the unrecognized key

#### Scenario: Step string prefixed with LLM enforcement marker
- **WHEN** a playbook step string starts with the `!(llm)` marker
- **THEN** the parser successfully extracts the step, trims the marker from the text, and flags the step internally to enforce LLM execution
