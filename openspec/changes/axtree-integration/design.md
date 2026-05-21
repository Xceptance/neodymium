## Context

In AI-driven web automation, prompt size (token usage) directly translates to execution latency and operational API cost.
Neodymium currently simplifies DOM elements using a custom Javascript parser to reduce prompt sizes (`ContextLevel.LEAN`). While efficient, this still consumes around 15 tokens per element and relies on heuristically matching labels to input fields.
By leveraging Chrome DevTools Protocol (CDP), we can directly extract the accessibility tree (AXTree) compiled natively by Chrome's rendering engine for screen readers. The AXTree flattens Shadow DOMs, resolves semantic names natively, and contains only structural/interactive components, achieving an incredibly low 5 tokens per element without losing action accuracy.

## Goals / Non-Goals

**Goals:**
- Add `ContextLevel.AXTREE` as the primary fallback element-level stage (Tier 2).
- Native, robust, version-neutral retrieval of Chromium accessibility trees using Selenium 4's `executeCdpCommand`.
- Dynamically stamp elements with `data-neo-ref` attributes inside the browser via CDP `Runtime.callFunctionOn` to integrate seamlessly with the framework's existing locator logic.
- 100% transparent and graceful fallback to JS `LEAN` extraction on Firefox, Safari, or remote grids where CDP is unavailable.

**Non-Goals:**
- Supporting native AXTree on non-Chromium browsers (we fall back to standard `LEAN` instead).
- Compiling visual screenshot layouts for `AXTREE` (screenshots remain isolated to `VISUAL` contexts).

## Decisions

### Decision 1: Version-Neutral CDP Command Execution
We will use `ChromiumDriver.executeCdpCommand(String, Map)` rather than the strongly-typed `org.openqa.selenium.devtools` classes (e.g. `org.openqa.selenium.devtools.v131...`).
- **Rationale**: Selenium's devtools modules are bound to specific Chrome major versions. Hardcoding a specific package makes the library extremely brittle and prone to runtime compilation/linkage errors when the browser version changes. `executeCdpCommand` is version-agnostic and fully compatible across all Selenium 4 releases.
- **Alternatives Considered**: Using Selenium's typed `Accessibility.getFullAXTree` which was rejected due to strict version dependencies and compilation fragility.

### Decision 2: Stamping refs in browser via Runtime.callFunctionOn
To interact with elements from the AXTree, we need to map the tree's virtual `backendDOMNodeId` back to physical DOM elements that the `ActionExecutor` can select.
We will resolve the `backendDOMNodeId` to a remote V8 object ID via `DOM.resolveNode`, then execute a lightweight JS function on it via `Runtime.callFunctionOn` to stamp the element with `data-neo-ref` dynamically.
- **Rationale**: This bypasses complex Selenium element lookup, executes entirely within Chrome's native engine, and keeps standard playbooks and selectors 100% compatible.
- **Alternatives Considered**: Resolving the node to a `WebElement` and calling `element.setAttribute` via `executeScript`, which is much slower and prone to stale element exceptions.

### Decision 3: Compact Text Outlining
The raw JSON returned by `Accessibility.getFullAXTree` contains massive metadata structures (role values, state lists, parent links). We will parse and serialize this JSON locally into a highly compressed, structured text outline (e.g., `[button] data-neo-ref='xc_ax_41' name='Submit' disabled='true'`).
- **Rationale**: Direct JSON passage to the LLM defeats the token-saving goal. A custom clean text outline guarantees maximum token compression.

## Risks / Trade-offs

- **[Risk]** CDP commands fail on older or restricted Selenium grid nodes.
  - **Mitigation** → Wrap all CDP calls in a safe try-catch block. On any failure, log the error, and immediately fall back to the JS-based `LEAN` context extraction.
- **[Risk]** Stale `backendDOMNodeId` reference if the DOM changes dynamically.
  - **Mitigation** → Re-fetch the AXTree on each attempt and re-assign fresh references before sending the context payload to the LLM.
