## ADDED Requirements

### Requirement: Dynamic Secret Masking
Before sending state context or prompts to the LLM, the `ContextSanitizer` MUST scan the payload for sensitive keys defined in `SessionData` and replace them with format-preserving mock patterns or user-configured stand-in values.

#### Scenario: Sensitive keys are masked in prompt
- **WHEN** a state capture DOM contains the password value `mysecretpass123` defined in `SessionData`
- **THEN** it replaces it with a dummy pattern of equal length and stores a reverse mapping to the variable name.

### Requirement: On-the-Fly Action Parameterization
When an action is executed and recorded, the `ActionSanitizer` MUST immediately scan the action parameters for raw sensitive credentials or transient runtime values, replacing them with variable references before writing them to the `PlaybookRecording`.

#### Scenario: raw value is parameterised
- **WHEN** a `TypeAction` is executed with value `admin_pass_9921` which matches the variable `${userPassword}`
- **THEN** the action is written to the recording with value set to `${userPassword}`.
