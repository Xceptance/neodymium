# [IDEA-20260807] Autonomous Intent-Based QA Explorer

- **Status:** `Proposed`
- **Proposed:** 2026-08-07
- **Resolved:** Pending
- **Component:** `neodymium-core (AiPromptGenerator)`
- **Category:** `Autonomous QA`
- **Author:** Neodymium Core Team

---

Evolve the experimental `AiPromptGenerator` (`@NeodymiumTestGenerator`) into a fully autonomous QA bot.
* **Concept:** Instead of step-by-step instructions, the QA engineer defines high-level goals (e.g., *"Test the checkout flow under 5 different currencies"* or *"Find broken links on the catalog page"*).
* **Execution:** 
  1. The agent autonomously crawls the application under test (SUT) using Large Action Models (LAMs).
  2. It attempts to complete the goal, recording all valid paths and UI states.
  3. Once successful, it automatically generates the standard `.yaml` test files and `.json` playbooks.
  4. It flags unexpected UI changes or error states as bugs during exploration.
