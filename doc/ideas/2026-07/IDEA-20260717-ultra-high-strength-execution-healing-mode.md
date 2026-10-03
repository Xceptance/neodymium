# [IDEA-20260717] "Ultra" High-Strength Execution & Healing Mode

- **Status:** `Rejected` (Superseded)
- **Proposed:** 2026-07-17
- **Resolved:** 2026-10-01
- **Component:** `neodymium-core (LocatorCascadeResolver, ExecutionMode)`
- **Category:** `Architecture & Core`
- **Author:** Neodymium Core Team

> [!NOTE]
> **Superseded by Deterministic Fast-Path Replay and Targeted Cascade:**
> Runaway multi-model healing and unbounded reflection loops introduced 15–30s latency overhead and non-deterministic behavior during regression runs. As resolved in DEF-20261001-05, Neodymium adopted strict zero-token fast-path replay (`ExecutionMode.REPLAY_STRICT`) with a targeted 5-tier fallback cascade rather than an unbounded heavy-weight Ultra healing mode.


---

During test execution or self-healing, when standard action extraction or verification fails, the default reasoning prompt or condensed DOM structure may not contain enough semantic clues to succeed. In these situations, we need a high-strength fallback mode that commits more resources (tokens, models, and inputs) to recover the step and prevent test failure.

---

### Proposal
Introduce a configurable "Ultra Mode" for execution and self-healing:
* **Enriched DOM & Context Representation:** Bypass normal token-saving truncations to feed more complete DOM details, sibling attributes, or full CSS state to the LLM.
* **Multimodal & Visual Reasoning:** Dynamically capture and inject full screenshots, region crops, or visual bounding box overlays (e.g. Set-of-Mark prompting) to let the model visually locate elements.
* **Divergent Prompts & High-Reasoning Models:** Query the LLM using distinct prompt templates tailored for hard cases (e.g., using Chain-of-Thought reasoning). Automatically route these queries to high-tier reasoning models (such as Gemini Pro or Ultra equivalent) with optimized temperature and sampling settings.
* **Self-Reflection & Verification Loops:** Run a multi-turn reasoning loop where the model first proposes an action, evaluates its own proposal against the visual state, and refines it before final execution.
* **Cost & Resource Transparency:** Explicitly track and report the increased token usage, execution time, and estimated financial cost in the test report when Ultra Mode is triggered, ensuring full awareness of the overhead.
* **Visual-Only Coordinate Interaction:** Introduce support for pure visual-only interactions where the model identifies target elements solely from screenshots and interacts with the page via direct coordinate-based mouse clicks and inputs, bypassing DOM-based locators entirely (e.g., for interacting with canvas elements or non-standard custom graphics).
* **Hybrid DOM-Coordinate Interaction:** Retrieve target element locations/boundaries via the DOM, but perform the actual interactions (hovering, clicking, typing) by calculating screen coordinates and simulating mouse trajectories.
* **Human-like Input Simulation:** Emulate realistic mouse movements using curved trajectories (e.g., Bézier curves) and dynamic speed profiles (Fitts's Law) dispatched via low-level CDP (Chrome DevTools Protocol) events to bypass bot-detection mechanisms.
* **Post-Execution Session Audit & Drift Review:** Option to submit the entire execution history (the complete sequence of steps, screenshots, actions, and inputs) to a post-run review process (either inline or via a separate LLM evaluation step). This checks for gradual state drift, circular loops, or silent failures that might have caused the agent to wander off-track over a longer session, appending this audit analysis to the final test report.
