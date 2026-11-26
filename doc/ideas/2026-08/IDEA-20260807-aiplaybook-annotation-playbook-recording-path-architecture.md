# [IDEA-20260807] `@AiPlaybook` Annotation & Playbook Recording Path Architecture

- **Status:** `Proposed`
- **Proposed:** 2026-08-07
- **Resolved:** Pending
- **Component:** `neodymium-core (NeodymiumAiRunner, @AiPlaybook)`
- **Category:** `Architecture & Core`
- **Author:** Neodymium Core Team

---

The current `@AiPlaybook` annotation handles basic YAML playbook loading and deterministic replay file binding (via `recordingMethod` and `recordingFileName`). However, as test suites scale, managing companion recording paths across multiple execution environments, datasets, and cross-class scenarios requires a dedicated architecture review.
* **Key Areas to Revisit & Explore:**
  * **Recording Directory Scoping:** Allow configuring custom output directories for recorded companion JSON files (e.g., separating integration baseline recordings from staging/prod baselines via annotation or properties).
  * **First-Class Cross-Class Replay Reference:** Support referencing recordings from different test classes directly via annotation (e.g., `recordingClass = LoginTest.class, recordingMethod = "testLogin"`).
  * **Explicit Shared Recording Registries:** Create a central registry/catalog for shared test workflows (e.g. login, guest checkout setup) where multiple test classes can bind to a single canonical baseline recording without duplicating JSON companion files.
  * **Annotation Syntax Streamlining:** Evaluate combining `@AiPlaybook`, `@AiMode`, and `@AiDataSet` into unified meta-annotations or composable test annotations to reduce boilerplate on test methods.
