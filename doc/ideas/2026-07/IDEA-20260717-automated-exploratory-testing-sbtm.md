# [IDEA-20260717] Automated Exploratory Testing (SBTM)

- **Status:** `Proposed`
- **Proposed:** 2026-07-17
- **Resolved:** Pending
- **Component:** `neodymium-core (AutonomousExplorer)`
- **Category:** `Autonomous QA`
- **Author:** Neodymium Core Team

---

Traditional automated testing executes static, predefined script paths. While effective, it misses visual regressions, logic gaps, or edge cases that occur off the beaten path. By combining LLM-based web page interaction with Session-Based Test Management (SBTM) principles, we can run autonomous exploratory test sessions guided by high-level human ideas.

---

### Proposal
Implement an automated exploratory testing engine in Neodymium:
* **Charter-Driven Exploration:** The engine accepts a natural-language SBTM Charter (e.g., *"Explore the shopping cart under high latency, adding/removing items rapidly, to identify race conditions or UI breakdowns"*).
* **Autonomous Interaction:** The LLM determines and executes interaction sequences on the SUT dynamically, balancing goal-oriented navigation (following the charter) with random path exploration.
* **Findings Recording & Session Protocol:** Automatically track all interactions, page transitions, visual states, and console logs during the session. At the end of the run, compile a comprehensive SBTM session protocol containing findings, suspected bugs, coverage metrics, and step-by-step reproduction logs.
