## Why

Neodymium's AI features (such as exploratory test generation and playbook healing) rely on structured JSON payloads exchanged with LLMs. Currently, these payloads use verbose, long keys (e.g., `"success"`, `"actions"`, `"targetContext"`), which consume significant input/output tokens and inflate API execution costs. Using a highly compact JSON schema with 1- or 2-letter keys will reduce token usage and cost while retaining full semantic equivalence.

## What Changes

- **Update System Prompt Templates**: Update system prompt templates (e.g., `system-prompt.txt`, `system-healing-prompt.txt`, `system-exploration-prompt.txt`, `v2-system-exploration-prompt.txt`, `v2-extraction-prompt.txt`) to instruct the LLM to output the new compact JSON format with short keys.
- **Strict Response Parser**: Update `ActionParser.java` and related parsers to strictly parse the new short keys from LLM responses (legacy keys do not need to be supported in the response parser per user guidance).
- **Playbook Preservation**: Recorded playbook files saved to or loaded from disk remain strictly in their standard long-key format (e.g. `"type"`, `"target"`, `"value"`) for readability and compatibility.
- **Pretty-Printed Response Logging**: Logging of LLM responses is updated to pretty-print the JSON response under the updated header `   📄 --- LLM Response (Pretty-Printed) ---`.

## Capabilities

### New Capabilities

- `compact-json-response`: Introduction of a compact, token-optimized JSON payload format for LLM communication.

### Modified Capabilities

## Impact

- `com.xceptance.neodymium.ai.action.ActionParser`: Parser logic updated to strictly parse compact keys.
- `com.xceptance.neodymium.ai.generator.AiPromptGenerator`: Prompt formatting and parsing updated to utilize the new key definitions.
- All prompt resources under `src/main/resources/ai-prompts/`.
- Unit and integration tests verifying parsing compatibility under the compact format.
