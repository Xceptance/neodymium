## Context

The system serializes DOM elements to be sent as context to the LLM. Currently, this serialization process ignores the `value` attribute of input elements, which means the LLM cannot see what the user has currently typed or selected.

## Goals / Non-Goals

**Goals:**
- Include the `value` attribute in the serialized DOM element representation sent to the LLM.
- Ensure the `value` attribute is correctly read for elements where it's relevant (e.g., `<input>`, `<textarea>`, `<select>`).

**Non-Goals:**
- Do not serialize all properties or full element state beyond the `value` attribute unless already done.
- Do not modify how the LLM processes this information, only how it is provided.

## Decisions

- **Extracting Value Attribute**: We will modify the DOM serialization logic to explicitly extract the `value` property/attribute and include it alongside other serialized attributes. This approach is direct and targets the specific missing context without unnecessarily expanding the serialization scope.

## Risks / Trade-offs

- [Payload Size] → The payload size to the LLM will slightly increase. Mitigation: The `value` attribute is generally short, so this is an acceptable trade-off for the improved context.
