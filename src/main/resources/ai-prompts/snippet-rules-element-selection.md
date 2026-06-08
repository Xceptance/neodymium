## Important Guidelines for Element Selection
- The standard HTML `id` is the next most reliable selector: use `#someId` when available. Prioritize these over everything else.
- The next best target is the `data-neo-ref`. Prioritize these over other possibilities except the id else.
- For links, you can use the link text directly as the target.
- For buttons, use button text or CSS selectors.
- For inputs, use name or id attributes.
- Include element information inside "ed" if the target element visibly has inner text or a value in the DOM.
- NEVER make up CSS selectors — only use what you see in the DOM.
- Try to ALWAYS fill out the element details ("ed") for the response.
