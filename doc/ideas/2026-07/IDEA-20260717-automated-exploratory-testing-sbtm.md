# [IDEA-20260717] Automated Exploratory Testing (SBTM)

- **Status:** `Rejected` (Superseded)
- **Proposed:** 2026-07-17
- **Resolved:** 2026-09-13
- **Component:** `neodymium-core (AutonomousExplorer)`
- **Category:** `Autonomous QA`
- **Author:** Neodymium Core Team

> [!NOTE]
> **Superseded by Comprehensive Autonomous Exploration Specification:**
> This early concept draft has been incorporated into and superseded by [IDEA-20260913-autonomous-exploratory-mode-executionmode-autonomous-exploration.md](file:///home/rschwietzke/projects/GIT/neodymium-library/doc/ideas/2026-09/IDEA-20260913-autonomous-exploratory-mode-executionmode-autonomous-exploration.md), which specifies the full architecture for `ExecutionMode.AUTONOMOUS_EXPLORATION` with SBTM charters, dynamic state tracking, automated playbook synthesis, and CI/CD replay.


---

Traditional automated testing executes static, predefined script paths. While effective, it misses visual regressions, logic gaps, or edge cases that occur off the beaten path. By combining LLM-based web page interaction with Session-Based Test Management (SBTM) principles, we can run autonomous exploratory test sessions guided by high-level human ideas.

---

### Proposal
Implement an automated exploratory testing engine in Neodymium:
* **Charter-Driven Exploration:** The engine accepts a natural-language SBTM Charter (e.g., *"Explore the shopping cart under high latency, adding/removing items rapidly, to identify race conditions or UI breakdowns"*).
* **Autonomous Interaction:** The LLM determines and executes interaction sequences on the SUT dynamically, balancing goal-oriented navigation (following the charter) with random path exploration.
* **Findings Recording & Session Protocol:** Automatically track all interactions, page transitions, visual states, and console logs during the session. At the end of the run, compile a comprehensive SBTM session protocol containing findings, suspected bugs, coverage metrics, and step-by-step reproduction logs.
