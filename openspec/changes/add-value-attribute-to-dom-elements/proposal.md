## Why

When sending DOM elements to the LLM, the `value` attribute is currently omitted. For many interactive elements (like input fields, selects, or textareas), the `value` attribute contains critical context about the current state of the page. Without it, the LLM cannot fully understand the user's input state or effectively interact with forms. 

## What Changes

- Update the DOM element serialization logic to include the `value` attribute when converting DOM elements into the format sent to the LLM.
- Ensure that the attribute is safely serialized alongside existing attributes like `id`, `class`, etc.

## Capabilities

### New Capabilities
- `dom-element-serialization`: Enhances the serialization of DOM elements to include the `value` attribute, providing the LLM with full context of input values on the page.

### Modified Capabilities

## Impact

- **Affected Code**: The component responsible for reading and serializing DOM elements to be sent to the LLM.
- **System Impact**: This will slightly increase the payload size sent to the LLM, but will significantly improve the context provided for form elements and user inputs.
