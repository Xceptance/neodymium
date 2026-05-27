## Why

Currently, the Pre-Execution Static Analysis Phase (PESAP) semantic linter checks playbooks against a standard set of static rules (such as vague actions or missing values). However, different teams, environments, and test projects accumulate specific mistakes, learnings, and custom anti-patterns over time. Adding a way to inject a custom prompt extension allows projects to customize the PESAP rules dynamically based on their specific domain requirements and historical learnings.

## What Changes

- **Custom Prompt Extension Property**: Introduce a new configuration property `neodymium.ai.pesap.custom.file` in `ai.properties` to allow users to specify a custom rules file (resolvable from classpath or filesystem).
- **Custom Rules File Support**: Support loading the configured custom prompt file from the classpath or filesystem.
- **Prompt Injection**: Update `AiAgentPrompts` to dynamically inject the custom rules block into the linter system prompt `AiAgentPrompts.PESAP_LINTER_PROMPT` when defined.
- **Prompt Files Refactoring**: Rename all AI prompt files under `src/main/resources/ai-prompts/` from `.txt` to `.md` to correctly represent their markdown formatting.
- **Core Linter Rules Expansion**: Expand the base linter rules to detect critical quality issues: ambiguous repeating grid interactions, hardcoded transient dynamic IDs, raw technical jargon/selectors, non-observable actions, and total absence of playbook assertions.
- **Logging**: Log when custom rules are loaded and applied during the static analysis phase so users have visibility into active rules.

## Capabilities

### New Capabilities
- `pesap-custom-prompt-extension`: Custom static analysis rules and instructions injected dynamically into the PESAP linter prompt.

### Modified Capabilities
- `pesap-semantic-linter`: Enhanced base rules list inside `pesap-linter-prompt.md` to check for repeating grids, raw selectors, technical jargon, hardcoded dynamic IDs, non-observable steps, and playbook assertion coverage.

## Impact

- `AiConfiguration.java`: Added property for custom rules configuration.
- `AiAgentPrompts.java`: Logic to load and inject custom rules into system prompts, and updated file paths for prompt templates to `.md`.
- `AiAgent.java`: Dynamically pass and log the injected linter prompt when PESAP runs.
- `ai.properties`: Default placeholder settings for custom rules.
- `src/main/resources/ai-prompts/`: Rename all prompt template files from `.txt` to `.md`, and expand the core rules inside `pesap-linter-prompt.md`.
