# [IDEA-20260721] Pre-emptive Self-Healing & "Second Opinion" Thresholds

- **Status:** `Rejected` (Superseded)
- **Proposed:** 2026-07-21
- **Resolved:** 2026-10-03
- **Component:** `neodymium-core (AgentToolLoopStep, LocatorResolver)`
- **Category:** `AI & VLM`
- **Author:** Neodymium Core Team

> [!NOTE]
> **Superseded by In-Loop Quality Judge and Post-Mortem Visual RCA:**
> Adaptive reflection is addressed directly by `QualityJudgeToolInterceptor` during active agent loops and the dedicated rubric-driven `VisualRcaStep` with screenshot diffing upon conclusive test failure (DEF-20261002-18), avoiding artificial pause-and-reflect latency overhead during normal execution.


---

When an agentic step or locator execution fails or exhibits low confidence (low token probabilities, high complexity, or repeated execution exceptions), we should explore triggering an adaptive reflection phase.
* **Reflective Re-framing:** The agent pauses, steps back, and reflects on the current DOM state, its overall approach, and previous context to "heal" its strategy or request a second opinion under a different persona/prompt structure.
* **Check Existing Mechanics:** We need to review how Neodymium currently handles locator self-healing under the hood (heuristics, triggers, alternate DOM lookups) to ensure any future reasoning-level healing aligns with or expands upon this foundation.
