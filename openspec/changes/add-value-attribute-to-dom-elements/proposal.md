## Why

When sending DOM elements to the LLM, the `value` attribute is currently omitted. For non-editable interactive elements (like `<input type="button">`, `<input type="submit">`, or `<input type="hidden">`), the `value` attribute contains critical context, such as the text displayed on the button. The values of user-editable inputs (like text fields or textareas) should remain omitted, but non-editable inputs should have their `value` included so the LLM can properly understand the page.

## What Changes

- Update the DOM element serialization logic to include the `value` attribute ONLY for non user-editable inputs (e.g., `<input type="button">`, `submit`, `hidden`) when converting DOM elements into the format sent to the LLM.
- Ensure that the attribute is safely serialized alongside existing attributes like `id`, `class`, etc.

## Capabilities

### New Capabilities
- `dom-element-serialization`: Enhances the serialization of DOM elements to include the `value` attribute for non user-editable inputs, providing the LLM with necessary context (like button labels).

### Modified Capabilities

## Impact

- **Affected Code**: The component responsible for reading and serializing DOM elements to be sent to the LLM.
- **System Impact**: This will slightly increase the payload size sent to the LLM, but will improve the context provided for non-editable form elements.
