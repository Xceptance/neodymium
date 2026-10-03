# [IDEA-20260807] IntelliJ IDEA & VSCode Visual Playbook Editor

- **Status:** `Proposed`
- **Proposed:** 2026-08-07
- **Resolved:** Pending
- **Component:** `aura-manager, ide-extensions`
- **Category:** `Tooling & DX`
- **Author:** Neodymium Core Team

---

While JSON Playbooks are human-readable, modifying complex paths or tracking dynamic changes in large suites can be tedious.
* **Concept:** Create a dedicated visual IDE extension for IntelliJ and VSCode that parses the cached `ai-playbooks` directory.
* **Capabilities:**
  * **Interactive Step Tracer:** Double-click any instruction in a `.java` or `.yaml` file to view its compiled playbook step.
  * **Visual Diff Inspector:** If a visual Hamming distance match fails (distance > 15), show a split visual diff of the recorded dHash screenshot vs the current SUT failure state.
  * **One-Click Sync:** Accept self-healed locators visually and merge them back to the Git working branch with a single button.
  * **Linter Diagnostics & One-Click Auto-Fixing:** Integrate the `PlaybookLinter` diagnostics engine directly into the editor language server (LSP). When the linter flags sequential steps with un-grouped pronouns or cross-step dependencies (e.g., *"Click its 'Add to Cart' button"* immediately following *"Hover over card"*), the editor displays inline diagnostic squiggles and provides a one-click Quick-Fix (`Alt+Enter`) to automatically indent the child instructions under an executable goal step, converting them into a coherent turn group.
