## ADDED Requirements

### Requirement: Model-Specific Prompt Customization
The prompt generation subsystem MUST format prompts differently based on the active provider's model family (e.g. formatting system prompts as separate fields for Gemini vs inline messages for Mistral) using a registered `PromptBuilderService`.

#### Scenario: Optimized prompt for Gemini model
- **WHEN** the session executes a Gemini provider
- **THEN** the prompt compiler queries the `PromptBuilderService` and sets the system prompt in the provider's dedicated configuration header.

### Requirement: Multi-Stage Response Repairing
The prompt subsystem MUST repair malformed model JSON responses using a multi-stage parser (string normalization, JSON Element validation, and model object overrides) before returning the target object.

#### Scenario: Stripping markdown ticks from JSON
- **WHEN** the LLM response contains ` ```json { "actions": [] } ``` `
- **THEN** the raw string stage repairer strips the ticks and parses the clean JSON body successfully.
