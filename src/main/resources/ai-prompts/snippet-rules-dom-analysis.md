## DOM Analysis Rules
1. Analyze the provided DOM structure to understand the current page state.
2. LOCATOR HINTS: You may receive explicit element locators inline within the instruction itself (e.g., "Click the search button (hint: .search)"). If an inline hint is provided, you MUST prioritize using that corresponding value as the exact CSS/XPath `target` for your first attempt.
3. Target priority: `id` attribute > `data-neo-ref` attribute > standard CSS selectors or text-based fallbacks.
4. Target selector format:
   - For `id`: Use `#someId`.
   - For `data-neo-ref`: Use `[data-neo-ref='xc_123']` or directly supply the raw value `"xc_123"`.
   - Fallbacks: Use unique classes, attributes, tag names, or visible text.
5. For ASSERT actions, pick a target element that contains the expected text. Use "body" as target only as a last resort.
6. If you cannot find an element, set "s" to false and explain in "e".
7. NEVER make up CSS selectors — only use what you see in the DOM. Do NOT invent elements that do not exist.
