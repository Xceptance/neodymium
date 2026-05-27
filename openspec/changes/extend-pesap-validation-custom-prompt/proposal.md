## Why

Currently, the Pre-Execution Static Analysis Phase (PESAP) semantic linter checks playbooks against a standard set of static rules (such as vague actions or missing values). However, different teams, environments, and test projects accumulate specific mistakes, learnings, and custom anti-patterns over time. Adding a way to inject a custom prompt extension allows projects to customize the PESAP rules dynamically based on their specific domain requirements and historical learnings.

## What Changes

- **Custom Prompt Extension Property**: Introduce a new configuration property `neodymium.ai.pesap.customRules` (or similar) in `ai.properties` to allow users to specify custom instructions directly in the configuration.
- **Custom Rules File Support**: Support loading a custom prompt file from the classpath or filesystem (e.g., `ai-prompts/pesap-custom-rules.txt` or `config/pesap-custom-rules.txt`).
- **Prompt Injection**: Update `AiAgentPrompts` to dynamically inject the custom rules block into the linter system prompt `AiAgentPrompts.PESAP_LINTER_PROMPT` (and potentially classification prompts) when defined.
- **Logging**: Log when custom rules are loaded and applied during the static analysis phase so users have visibility into active rules.

## Capabilities

### New Capabilities
- `pesap-custom-prompt-extension`: Custom static analysis rules and instructions injected dynamically into the PESAP linter prompt.

### Modified Capabilities

## Impact

- `AiConfiguration.java`: Added property for custom rules configuration.
- `AiAgentPrompts.java`: Logic to load and inject custom rules into system prompts.
- `AiAgent.java`: Dynamically pass and log the injected linter prompt when PESAP runs.
- `ai.properties`: Default placeholder settings for custom rules.
