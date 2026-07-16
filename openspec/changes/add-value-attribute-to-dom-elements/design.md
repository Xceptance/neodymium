## Context

The system serializes DOM elements to be sent as context to the LLM. Currently, this serialization process ignores the `value` attribute of input elements. While omitting this for user-editable fields (like text or password inputs) is desired, it means the LLM cannot see the text on non-editable inputs like buttons (e.g. `<input type="button" value="Click Me">`).

## Goals / Non-Goals

**Goals:**
- Include the `value` attribute in the serialized DOM element representation sent to the LLM ONLY for non user-editable input elements (e.g., `type="button"`, `type="submit"`, `type="reset"`, `type="hidden"`).

**Non-Goals:**
- Do not serialize the `value` attribute for user-editable elements like `<input type="text">` or `<textarea>`.
- Do not serialize all properties or full element state beyond the `value` attribute unless already done.
- Do not modify how the LLM processes this information, only how it is provided.

## Decisions

- **Extracting Value Attribute**: We will modify the DOM serialization logic to explicitly extract the `value` property/attribute and include it alongside other serialized attributes, but conditionally check the element's type to ensure it is not a user-editable field.

## Risks / Trade-offs

- [Payload Size] → The payload size to the LLM will slightly increase. Mitigation: The `value` attribute is generally short and we are only including it for a subset of inputs, so this is an acceptable trade-off for the improved context.
