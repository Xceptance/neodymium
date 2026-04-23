# Neodymium AI Module

The Neodymium framework includes an integrated AI agent capable of intelligent UI exploration, test generation, and script healing. To provide maximum flexibility and allow testing strategies to be customized per project, the core LLM instructions and prompt templates have been externalized.

## Prompt Overriding

By default, the framework loads its prompt templates from the `neodymium.jar` classpath at `ai-prompts/`. 

External projects that include Neodymium (via Maven/Gradle) can easily override any of these system prompts. To do so, simply create a file with the exact same name in your own project's `src/main/resources/ai-prompts/` directory.

Because the Java classloader prioritizes your project's `target/classes` over dependencies, the framework will automatically discover and use your customized prompt file instead of the default one.

### Example Override

If you want to alter the strict data binding instructions in V2 generation, you would create this file in your consuming project:

```text
src/main/resources/ai-prompts/v2-system-exploration-prompt.txt
```

*(You can copy the default file contents directly from the Neodymium source repository to serve as a starting template).*

## Available AI Prompts

Here is a breakdown of the available templates that can be overridden:

### 1. Test Generation (V2 Mode)
These prompts control the V2 test generation pipeline, which emphasizes robust forward-exploration and a subsequent extraction phase to filter out mistakes.

- **`v2-system-exploration-prompt.txt`**: The overarching system manual for the AI. Defines its persona, JSON formatting requirements, data parameterization rules, and instructions for recovering from UI errors.
- **`v2-exploration-prompt-template.txt`**: The user-facing prompt injected at each step. It interpolates the current DOM state, high-level intent, previously attempted actions, and any known data bindings.
- **`v2-extraction-prompt.txt`**: Instructions for the secondary LLM pass. It tells the AI how to analyze the messy chronological playbook and extract only the successful, linear steps.
- **`v2-extraction-retry-prompt.txt`**: A small retry template used if the AI fails to output the extraction array in the correct JSON format.

### 2. Test Generation (Legacy / V1 Mode)
These prompts govern the original test generation pipeline, which allows the AI to conceptually backtrack and delete steps mid-exploration using `dropLastNActions`.

- **`system-exploration-prompt.txt`**: The system instructions for the V1 exploratory agent, including backtracking rules.
- **`exploration-prompt-template.txt`**: The user-facing prompt template for the V1 generation loop.

### 3. Playbook Healing & Validation
These prompts are utilized during the execution of a pre-recorded Playbook when a step fails (e.g., due to a changed locator or moved button).

- **`system-healing-prompt.txt`**: Instructs the AI on how to act as a self-healing agent, asking it to determine if a failure is a genuine application bug or just a UI change that can be fixed.
- **`healing-prompt-template.txt`**: The user-facing template that provides the broken instruction, original element context, the thrown exception, and the current DOM state to diagnose the issue.

### 4. Basic Agent Execution (Single-Shot)
These prompts are used for direct, single-action commands when utilizing the `AiAgent` for simple instructions outside of a continuous exploration loop.

- **`system-prompt.txt`**: The core system persona for basic test automation, describing available browser capabilities (`CLICK`, `TYPE`, `ASSERT`, etc.).
- **`user-prompt-template.txt`**: The user-facing template combining the human instruction and current DOM state.

### 5. Retries & Error Handling
Templates used to feed error context back into the LLM if an action fails or the model hallucinates an invalid response format.

- **`retry-prompt-template.txt`**: Used when a chosen action fails (e.g., ElementNotInteractableException). It provides the exception and tells the AI to pick a new approach.
- **`no-actions-retry-prompt-template.txt`**: Used when the AI successfully returns JSON but hallucinates an empty actions array. Instructs the AI that it must output at least one action.
