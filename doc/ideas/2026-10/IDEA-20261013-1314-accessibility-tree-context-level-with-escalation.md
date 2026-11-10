# [IDEA-20261013-1314] Accessibility Tree Context Level with Replay-Gated Fast Escalation

- **Status:** `Proposed`
- **Proposed:** 2026-10-13 13:14
- **Resolved:** `Pending`
- **Component:** `neodymium-core` (`PageAnalyzer`, `ContextLevel`, `AgentToolLoopStep`, `LocatorImprover`, `BrowserToolProvider`)
- **Category:** `Performance & Cost`
- **Author:** AI-generated: Gemini 3.8 Flash & Xceptance GmbH 2026

## 1. Problem Statement & Motivation

Modern web applications (React, Next.js, Vue, Tailwind, Emotion CSS-in-JS) generate deeply nested DOM trees cluttered with non-semantic styling wrappers. On production e-commerce storefronts, passing even filtered DOM snapshots (`LEAN` or `STANDARD`) consumes 4,000 to 12,000 prompt tokens per turn.

The Chrome Accessibility Tree (`Accessibility.getFullAXTree` via CDP) eliminates layout noise and isolates interactive semantic roles (`button`, `link`, `textbox`, `combobox`, `dialog`), reducing prompt payload size by **60% to 80%**.

### The Core Architectural Dilemma: No-LLM Replay is High Priority
Neodymium's foundational architecture requires that generated tests replay **100% deterministically in CI/CD without calling an LLM**, executing pure Selenide commands recorded in the companion JSON playback book.

Relying naively on the Accessibility Tree threatens no-LLM replay reliability due to four failure modes:
1. **Selector Durability Deficit:** The AX tree strips tag names, IDs, data attributes, and CSS class names. If an LLM selects an element solely by its accessible role/name, the framework must reverse-synthesize a durable CSS/XPath locator. If the SUT lacks unique text anchors, algorithmic reverse-mapping produces brittle positional selectors (`div:nth-child(3) > button`) that break in CI replay.
2. **Container Scoping Collapse:** Product grids and carousels often render 8 to 12 identical buttons (`button "Add to Basket"`). Without container classes (`article.product-card`), the model cannot distinguish between products or record scoped selectors.
3. **Modal & Overlay Occlusion:** Geolocation modals or cookie banners trapping focus (`aria-hidden="true"`) cause the underlying page to disappear entirely from the AX tree.
4. **Missing Resting State Attributes:** Dropdown menus or flyout accordions frequently lack resting `aria-expanded` attributes until hovered, remaining invisible in the AX tree.

To preserve **No-LLM Replay as a strict invariant**, the Accessibility Tree cannot be a static mode; it must feature **Replay-Gated Escalation**, where any threat to replay durability triggers an immediate, zero-turn fallback to DOM context.

## 2. Proposed Architecture & Design

### Replay-Gated Context Flow:
```text
+---------------------------------------------------------------------------------------+
|                       Turn 1: Lightweight AX Tree Ingestion                          |
|  - PageAnalyzer captures Chrome AX Tree via CDP (Token reduction: ~70%)               |
+---------------------------------------------------------------------------------------+
                                           |
                  +------------------------+------------------------+
                  |                                                 |
         [Unambiguous & Valid]                             [Degraded / Ambiguous]
                  |                                                 |
                  v                                                 v
+-----------------------------------+             +-----------------------------------+
| Action Execution & Replay Gating: |             | Fast Escalation (Triggers 1 & 2): |
| - Model identifies target element |             | - Thin tree / modal occlusion     |
| - LocatorImprover evaluates live  |             | - Duplicate identical siblings    |
|   node for replay durability score|             | --> Immediate switch to LEAN DOM  |
+-----------------------------------+             +-----------------------------------+
                  |                                                 |
         +--------+--------+                                        v
         |                 |                      +-----------------------------------+
   [Score >= 0.70]   [Score < 0.70]               | Model receives full DOM with CSS   |
         |         (Brittle Selector)             | classes, IDs & container scoping  |
         |                 |                      +-----------------------------------+
         v                 v                                        |
+-----------------+ +-----------------------------------+           |
| Commit Action   | | Replay Guard Escalation (Trigger 3)|           |
| to Companion    | | - Discards brittle selector       |           |
| JSON Playbook   | | - Escalates to LEAN DOM in-memory |           |
| (100% No-LLM)   | | - Model resolves durable classes  |           |
+-----------------+ +-----------------------------------+           |
         |                         |                                |
         v                         +---------------->---------------+
+-------------------------------------------------------------------+
|                     Proceed to Next Step                          |
+-------------------------------------------------------------------+
```

### 1. Replay Durability as an Active Escalation Trigger:
In `AgentToolLoopStep`, the transition from `ACCESSIBILITY` to `LEAN` is triggered automatically by four deterministic conditions:

1. **Replay Selector Durability Gating (High Priority):**
   - When an action is executed on an element resolved via the AX tree, `LocatorImprover` analyzes candidate selectors on the live DOM.
   - If the candidate score is below threshold (e.g. `< 0.70`, indicating only brittle nth-child or volatile dynamic attributes are available), the framework **rejects the candidate and immediately escalates the turn to `LEAN` DOM**.
   - This ensures a step never records a fragile selector into the companion JSON simply because the model could not see the surrounding CSS classes.
2. **Sibling Ambiguity Collision:**
   - If multiple elements share identical accessible roles and names without distinct parent headings in the AX tree (e.g. 5x `button "Add to Basket"`), the framework escalates immediately to provide container scoping.
3. **Occlusion & Thin-Tree Trigger:**
   - If `captureAccessibilityTree()` yields `< 5` interactive elements or a modal dialog marks the rest of the document `aria-hidden`, the analyzer escalates prior to LLM dispatch.
4. **Action Execution Failure:**
   - Any `ElementNotFoundException` or `ElementClickInterceptedException` against an AX target immediately escalates to `LEAN` without penalizing the step retry budget.

### 2. Context Escalation Ladder (`ContextLevel.java`):
```java
public enum ContextLevel
{
    HINT,
    ACCESSIBILITY, // Fast-path semantic tree (<1,500 tokens)
    MINIMAL,
    LEAN,          // Structural interactive DOM with classes & container tags
    STANDARD,
    RICH,
    VISUAL,
    VISUAL_LEAN,
    VISUAL_RICH;

    public ContextLevel escalate()
    {
        return switch (this)
        {
            case HINT -> ACCESSIBILITY;
            case ACCESSIBILITY -> LEAN; // Direct jump to structural DOM
            case MINIMAL -> LEAN;
            case LEAN -> STANDARD;
            case STANDARD -> RICH;
            case RICH -> VISUAL_RICH;
            case VISUAL -> VISUAL_LEAN;
            case VISUAL_LEAN -> VISUAL_RICH;
            case VISUAL_RICH -> null;
        };
    }
}
```

### 3. Replay Companion JSON Guarantee:
- **No AX References in Replay Books:** The companion JSON playback book strictly stores native Selenide selectors (`button.quick-add-btn`, `#checkout-btn`, `article.card:has(h3:contains("Rosehip")) button`).
- In no-LLM CI replay, the test runner executes these locators directly via Selenide without loading or parsing the AX tree.

## 3. Expected Impact & Trade-offs

- **Benefits:**
  - **Replay Fidelity Guaranteed:** Prioritizes CI/CD execution stability above prompt optimization; no brittle selector is allowed into the playback book.
  - **60–80% Token Reduction on Clean Apps:** When storefronts have robust semantic ARIA (e.g. clear landmark regions, accessible form controls), steps execute with minimal token footprint.
  - **Zero Replay Performance Overhead:** Replay speed in CI remains identical (pure Selenide execution).
- **Risks & Trade-offs:**
  - **In-Flight Dual Synthesis:** The framework must evaluate live DOM selectors even when interacting via AX tree nodes.
  - **Escalation Turn Overhead:** When escalation triggers, a second prompt with `LEAN` DOM is dispatched, incurring a single turn latency penalty for that specific step.

## 4. Open Questions & Alternatives Considered

- *Alternative 1: Static AX Tree without Replay Guard.* Evaluated and strongly rejected; recording brittle nth-child selectors breaks Neodymium's primary goal of deterministic no-LLM replay.
- *Alternative 2: AX Tree Only for Assertions.* Evaluated, but interaction steps (`click`, `fill`) consume the bulk of tokens where compression is most valuable.
- *Open Question:* What should be the minimum `LocatorImprover` score threshold required before accepting an AX-derived selector into the companion JSON (default: 0.70)?
