## Context

The `PageAnalyzer` simplifies DOM nodes for AI consumption. Currently, for `<select>` elements, it attempts to extract the `<option>` values into an `options` attribute on the `<select>` node itself or `[form-field]`. However, the current implementation trims spaces (losing `nbsp`) and doesn't quote individual options. This produces an ambiguous list when option texts contain spaces or commas.

## Goals / Non-Goals

**Goals:**
- Provide a lossless and non-ambiguous text representation of `<select>` options.
- Retain exact option text strings including whitespace and special characters like `nbsp`.

**Non-Goals:**
- Changing the analysis logic for other form elements.

## Decisions

**Decision 1: How to represent the options?**

*Alternative A: JSON-like array in `options` attribute (Proposed)*
- Use proper quoting (single or double) around each option.
- Escape inner quotes.
- Example: `options='["A", "B nbsp", "C, D"]'`
- Pros: Keeps the representation compact.
- Cons: Still has to be carefully parsed if attribute parsing logic is simple.

*Alternative B: Separate child elements*
- Do not collapse options into an attribute.
- Render them as separate `<option>` nodes indented below the `<select>`.
- Example:
  ```html
  <select id="time">
    <option>A</option>
    <option>B nbsp</option>
  </select>
  ```
- Pros: Reflects exact DOM structure, extremely robust.
- Cons: Increases the line count of the analyzed page for selects with many options (e.g., country dropdowns).

**Decision:** We need to clarify with the user which alternative they prefer. For now, Alternative A (JSON array in attribute) is the tentative design to maintain compactness, but keeping options as separate elements (Alternative B) is a strong alternative.

## Risks / Trade-offs

- [Risk] Changing the format will break existing AI tests that assert on the exact string `options="yearly, ... "`. → Mitigation: Update the corresponding tests in `AuraGlanceTest` and `PageAnalyzerCheckableTest`.

## Open Questions

- Should we stick with the `options` attribute but format it as a proper JSON string array (e.g. `options='["yearly", "half-yearly"]'`), or should we represent the options as separate `<option>` elements indented below the `<select>`?
