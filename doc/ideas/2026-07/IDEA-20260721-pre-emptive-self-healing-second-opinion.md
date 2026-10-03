# [IDEA-20260721] Pre-emptive Self-Healing & "Second Opinion" Thresholds

- **Status:** `Proposed`
- **Proposed:** 2026-07-21
- **Resolved:** Pending
- **Component:** `neodymium-core (AgentToolLoopStep, LocatorResolver)`
- **Category:** `AI & VLM`
- **Author:** Neodymium Core Team

---

When an agentic step or locator execution fails or exhibits low confidence (low token probabilities, high complexity, or repeated execution exceptions), we should explore triggering an adaptive reflection phase.
* **Reflective Re-framing:** The agent pauses, steps back, and reflects on the current DOM state, its overall approach, and previous context to "heal" its strategy or request a second opinion under a different persona/prompt structure.
* **Check Existing Mechanics:** We need to review how Neodymium currently handles locator self-healing under the hood (heuristics, triggers, alternate DOM lookups) to ensure any future reasoning-level healing aligns with or expands upon this foundation.
