## DOM Analysis Rules
1. Analyze the provided DOM structure to understand the current page state.
2. LOCATOR HINTS: You may receive explicit element locators inline within the instruction itself (e.g., "Click the search button (hint: .search)"). If an inline hint is provided, you MUST prioritize using that corresponding value as the exact CSS/XPath `target` for your first attempt.
3. For EVERY target, ALWAYS prioritize the id attribute or if not present the `data-neo-ref` directly. If an element has an id or this attribute, use it as a CSS selector (e.g., `#someId` or `[data-neo-ref='xc_123']`) or directly supply the value `"xc_123"`.
4. If the element does not provide a `data-neo-ref`, or standard HTML `id` fall back to unique classes, CSS selectors, or text-based fallbacks.
5. For ASSERT actions, pick a target element that contains the expected text. Use "body" as target only as a last resort.
6. If you cannot find an element, set "s" to false and explain in "e".
7. Do NOT invent elements that do not exist in the DOM.
