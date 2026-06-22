## Why

Large Language Models (LLMs) used in AI-driven test automation struggle with context window bloat, high execution latency, and semantic element resolution flakiness when operating on raw or heuristically-filtered HTML.
While Neodymium's current `LEAN` mode optimizes element retrieval using a custom Javascript extraction, it still consumes approximately 15 tokens per element, lacks deterministic shadow-DOM piercing, and struggles to resolve implicit semantic label mappings natively (e.g. associating `<input>` with sibling `<label>`s or resolving complex `aria-*` tags).

By integrating **The Chrome Accessibility Tree (AXTree)** via Chrome DevTools Protocol (CDP), Neodymium can leverage the browser's native accessibility engine (originally built to support screen readers). This enables:
1. **Ultra-Low Token Usage**: Reducing element representation from ~15 tokens to ~5 tokens (a **65%+ cost savings**).
2. **Deterministic Resolution**: Direct browser-native mapping via `backendDOMNodeId` to assign unique references and execute actions safely.
3. **Perfect Semantic Grounding**: Automatic native resolution of `aria-label`, `aria-labelledby`, form labels, and element states.
4. **Seamless Shadow-DOM & IFrame Traversal**: Native flat-tree representation that pierces web components automatically.
5. **Cross-Browser Safety**: An elegant, transparent fallback to standard JS `LEAN` extraction on non-Chromium drivers (Firefox, Safari) or older Selenium grid endpoints.

## What Changes

1. **Context Escalation Framework Refactoring**: Add `ContextLevel.AXTREE` into the escalating fallback cascade, placing it as Tier 2 (the first element-carrying tier) directly after `HINT` (Tier 1) and preceding `LEAN` (Tier 3).
2. **PageAnalyzer Extension**: Implement `fetchAXTree()` and CDP node labeling using Selenium 4's `HasCdp` interface and the version-neutral `executeCdpCommand("Accessibility.getFullAXTree", ...)` API.
3. **Deterministic ID Stamping**: Resolve `backendDOMNodeId` to live elements and stamp them with dynamic `data-neo-ref` attributes via version-neutral CDP `Runtime.callFunctionOn` executions, preventing any Selenium WebElement locator flakiness.
4. **Transparent Graceful Fallback**: Automatically detect driver compatibility at runtime (checking `driver instanceof HasCdp`) and transparently fall back to JS-based `LEAN` extraction if CDP is unavailable.
5. **Upgraded Context Ladder Documentation**: Update the user manuals, `AI-README.md`, and system prompts to reflect the upgraded cascading ladder.

## Capabilities

### New Capabilities

- `axtree-cdp-extractor`: Native, version-neutral retrieval of Chromium's accessibility nodes and metadata using CDP commands `Accessibility.getFullAXTree`, `DOM.resolveNode`, and `Runtime.callFunctionOn`.
- `axtree-node-serializer`: A compact, token-optimized text serializer that formats accessibility trees into high-semantic, ultra-low-token lists (e.g., `link [12] "Login"`).

### Modified Capabilities

- `reporting`: Add representation details in execution logs and Aura streaming/Playbook metadata to show when AXTree was successfully utilized.
- `healing`: Support healing pipelines when resolving actions using AXTree coordinates and cached locators.

## Impact

- **Affected Classes**:
  - `com.xceptance.neodymium.ai.core.ContextLevel`: Add `AXTREE` to the enum and refactor `escalate()` to follow: `HINT` -> `AXTREE` -> `LEAN` -> `STANDARD` -> `VISUAL`.
  - `com.xceptance.neodymium.ai.core.PageAnalyzer`: Integrate the CDP extractor and fallback mechanism inside `captureSimplifiedDom()`.
  - `com.xceptance.neodymium.ai.core.AiAgent`: Align `getInitialContextLevel` to start at `AXTREE` by default (except for explicitly tagged hints or visual requests).
- **Dependency Impact**: Zero new dependencies. Uses standard Selenium 4 API capabilities (`HasCdp`) and Gson/Maps which are already fully present.
