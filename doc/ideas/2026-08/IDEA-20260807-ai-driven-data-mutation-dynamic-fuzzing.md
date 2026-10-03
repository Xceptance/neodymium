# [IDEA-20260807] AI-Driven Data Mutation & Dynamic Fuzzing

- **Status:** `Proposed`
- **Proposed:** 2026-08-07
- **Resolved:** Pending
- **Component:** `neodymium-core (AiDataMutator)`
- **Category:** `Autonomous QA`
- **Author:** Neodymium Core Team

---

Natural language steps often require complex test data. Instead of hardcoding edge cases, let the AI generate them dynamically at runtime.
* **Concept:** Integrate dynamic fuzzing directly into steps using dynamic AI dataset generation.
* **Example Instruction:**
  ```yaml
  steps: |
    Type a dynamically generated invalid email address into the input field.
    Verify that the validation error 'Invalid email format' is visible.
  ```
* **Execution:** The agent detects the semantic request for dynamic data, asks the LLM (or a local generator) to generate mutated synthetic strings designed to trigger validation rules, and injects them dynamically during SUT execution.
