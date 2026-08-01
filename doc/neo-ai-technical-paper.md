# Human Specs, Machine Execution: Rethinking Test Automation with the Help of AI

**Author:** René Schwietzke (Managing Director & Co-Founder, Xceptance)  
**Date:** July 2026 | **Document Version:** 2.0  

---

## Executive Summary

Historically, web test automation operated by driving browser protocols directly against dynamic Document Object Model (DOM) structures. Code-based frameworks (like Selenium or Playwright) brought execution speed, and BDD tools (like Cucumber) offered business-readable specs. However, both shared primary operational bottlenecks: **reliance on handwritten DOM selectors** and **fragile framework boilerplate**.

Artificial intelligence offers an alternative model by replacing explicit DOM queries with intent-driven automation. Modern AI tools often attempt to solve this by generating test code—but why introduce an intermediate code layer when no one intends to maintain that code manually anyway? When UI changes occur, generated code inevitably breaks, forcing teams into continuous manual regeneration and reevaluation cycles. Conversely, querying live LLMs on every single test step introduces recurring API token costs, high CI/CD execution latency (often 10x slower than native drivers), external API dependencies, and potential non-determinism during exact arithmetic checks.

This paper describes an open-source approach to AI-assisted test automation centered on **Intent Compilation with Zero-Cost Replay**. By keeping human-language specs as the single source of truth, test intent compiles once into a local file-based playbook. Later, test suites replay these playbooks 100% offline via native browser drivers (WebDriver, Playwright, Selenide) at native speed with zero token costs, triggering LLM self-healing only when UI selectors fail.

We also present our key learnings from programming with AI—not just a few lines, but almost the entire codebase.

---

## 1. Problem Statement & The Test Automation Spectrum

### 1.1 The Test Automation Spectrum

| Paradigm | Primary Advantages | Key Drawbacks |
| :--- | :--- | :--- |
| **Code-Based Frameworks** *(Selenium, Playwright, Selenide)* | Fast CI/CD execution, zero runtime token cost | High Page Object Model code overhead, brittle CSS/XPath selectors |
| **BDD Specifications** *(Cucumber, Gherkin)* | Business-readable plain-text specifications | Dual-layer maintenance (glue code + step bindings), selector fragility |
| **Live-AI Drivers** *(Per-step LLM querying)* | Plain-language authoring, dynamic locator resolution | High token costs & slow execution, risk of hallucinations, model changes may alter outcomes |
| **Vision & Autonomous Agents** *(Visual pixel navigation)* | No DOM dependency, visual UI traversal | Uncapped VLM compute costs, non-deterministic execution paths |

### 1.2 The Core Idea

Neo AI addresses these trade-offs by separating **human test authoring** from **machine execution artifacts**:

* **Why Explicit Test Case Definitions Matter (Reproducibility & Governance):** Explicit test case definitions are essential for **reproducibility**, **auditability**, and human governance. Fully autonomous agents that infer intent dynamically risk non-determinism, hallucinations, and unconstrained action paths. Explicit natural-language test definitions anchor AI agents to deterministic objectives—ensuring every execution is 100% reproducible, human-auditable, and strictly governed.
* **Human Specs as the Single Source of Truth:** Engineers write plain-language test scenarios without writing code, Page Object Models, or glue scripts.
* **Transforming Specs to Executable Actions:** The underlying LLM parses natural human test steps and maps them to precise, deterministic browser actions.
* **Efficient Browser Control Mechanics:** Utilizes compact DOM snapshots, accessibility trees (`AXTREE`), and protocol drivers (CDP, WebDriver, Playwright) to inspect and control the browser with minimal context footprint.
* **Deterministic Playbooks & Zero-Cost Replay:** On initial execution (Creation Mode), Neo AI compiles steps into file-based playbooks stored locally on disk. In CI/CD, playbooks replay 100% offline at native driver speed with zero token consumption.
* **Localized Self-Healing:** When UI updates invalidate a replayed locator, Neo AI's localized self-healing pipeline re-analyzes the live DOM, updates the local playbook file, and resumes execution.
* **Beyond Explicit Verification (Automated Visual & System Health Checks):** Captures visual page baselines during compilation to deliver automated visual regression testing without extra test steps, while applying automatic console/network error checks and data sanitization.

---

## 2. Platform Architecture & Core Features

### 2.1 Natural Language Input & Zero-Cost Offline Replay via Playbooks

The primary input to Neo AI is a **natural-language test case definition**—authored in plain human language as a sequence of test steps formatted in clean YAML (`steps: | ...`).

When executing a natural-language test case for the first time (**Creation Mode**), Neo AI parses these step instructions, inspects the page DOM, determines locator strategies and action sequences, compiles and executes the test intent into a structured **playbook** saved locally and versioned normally:

```yaml
# Natural Language Authoring in YAML (e.g., Guest Checkout Scenario)
steps: |
  Open ${verla.url}/verla-${quality}/index.html
  Validate that United States is selected as country.
  Locate the first product card and click its 'Add to Cart' or 'Add' button.
  When this string '${quality}' is not equal to 'bad', click the size 'L' button.
  The mini cart quantity is now 1.
  Go to the cart using the mini cart.
  Click 'Checkout'

  The headline now says 'Checkout'
  There are data input forms on the left and an order summary block on the right (visual).
  Select 'United States' as country.
  Enter 'Mario' as first name, 'Meier' as last name, and the email address 'foo@varmail.net'.
  Enter '123 Main St' as street address, 'Manchester' as city, and '12345' as postcode.
  Enter state 'MA'.
  Card number is '4111 1111 1111 1111', expiry date '12/29', and CVV is '111'.
  Click 'Purchase'.
  Wait for text "Thank you for your purchase!" to appear.
  Scroll target 'body' to 'top'.
  An order number is shown in the form 'V-[0-9]+-US'.
  The total is mentioned and in USD.
  The zip code says '12345'.
  The text "Thank you for your purchase!" is shown.
  There is a green checkmark in the middle of the screen (visual).
```

In subsequent regression runs (**Replay Mode**), the runners execute native driver calls (WebDriver, Playwright, Selenide) directly from the local playbook 100% offline at native speed with zero LLM API token costs.

### 2.2 Localized Self-Healing & Escalating Context System

If an application UI update alters an element target (e.g., `#btn-submit-v1` to `#btn-submit-v2`), the replayed action fails and engages the localized self-healing pipeline via a 6-tier **Escalating Context System**:

| Level | Name | Description & Token Footprint |
| :--- | :--- | :--- |
| 1 | `HINT` | Zero DOM elements. Activated when inline locator hints `(hint: ...)` are present. |
| 2 | `AXTREE` | Browser-native Accessibility Tree outline (~90% smaller than raw HTML). |
| 3 | `LEAN` | Interactive DOM elements only (buttons, inputs, links, selects). |
| 4 | `STANDARD` | Full interactive DOM plus visible text nodes for text assertions. |
| 5 | `VISUAL_LEAN` | Interactive DOM + compressed page screenshot. Used for explicit `(visual)` tags. |
| 6 | `VISUAL` | Full DOM + viewport screenshot. Reserved for complex canvas, SVG, or shadow-DOM elements. |

**Smart Escalation Jumps:** If an instruction requires visual inspection, the runner jumps directly to `VISUAL` context. Once healed, the updated locator and context level are saved directly to the local playbook file on disk for subsequent offline runs.

### 2.3 Programmatic Extensions & Deterministic Guardrails

To prevent LLM validation hallucinations during exact arithmetic or complex data formatting, Neo AI delegates precise computations to native programmatic helper functions:
* **Exact Precision & Calculations:** Delegates financial logic and exact mathematical verifications to deterministic programming operations rather than probabilistic LLM reasoning.
* **Data Formatting & Normalization:** Standardizes localized currency strings (e.g., `14,96 €` -> `14.96`) and custom string transformations before/after step execution.
* **Explicit Guardrails:** Ensures only explicitly exposed helper methods can be invoked by the test execution pipeline.

### 2.4 Automated Visual Assertions & Soft Warning Gates

* **Perceptual dHash Verification:** During compilation, a dHash fingerprint of the target element or viewport screenshot is generated and saved. Replays evaluate the Hamming distance locally without LLM calls, delivering also visual regression testing at zero cost.
* **Secret Sanitization:** Automatically masks passwords, tokens, and PII before transmitting DOM data to LLMs or saving playbooks to disk.

---

## 3. Engineering Lessons Learned & Challenges

Developing Neo AI provided key engineering insights into building AI-driven quality engineering tools:

1. **The 5-Iteration Modular Evolution:** Decoupling browser interaction drivers from LLM parsing logic was essential to keep test execution deterministic and independent of model vendor changes.
2. **Defensive Response Parsing:** Switching LLM providers (Gemini, Claude, local open-source models) introduces Markdown fences, conversational commentary, or malformed output syntax. A multi-stage defensive parser isolates and repairs structured model responses before execution.
3. **Context Reduction & `AXTREE` Efficiency:** Early prototypes sending raw HTML DOM trees suffered from prompt bloat and locator confusion. Switching to native Accessibility Trees (`AXTREE`) reduced prompt payloads by ~90% while sharpening element resolution accuracy.
4. **Cross-Model Vendor Dynamics & Task Routing:** Changing LLM families alters locator selection choices. Task-specific model routing directs heavy visual tasks to specialized VLMs while executing routine DOM parsing through fast local LLMs.
5. **Differentiating Self-Healing from Real UI Bugs:** A critical engineering challenge was tuning self-healing thresholds to ensure the LLM repairs locator drift on valid UI updates without falsely "healing" through genuine application regression bugs.
6. **Complex Algorithmic Control Flow Bounds:** Natural-language steps excel at linear interaction sequences. Complex programmatic loops (e.g., *"repeatedly remove cart items until subtotal is under €50"*) remain best handled via explicit programmatic helper functions.
7. **Building AI Tools with AI Assistance:** While AI coding assistants accelerated initial prototyping, generated code frequently introduced unnecessary abstraction layers. Continuous human oversight and aggressive refactoring were essential to keep the framework core simple, minimal, and fast.

Bad web practices stay bad practices and make AI-based testing more expensive and less predictable. Having a magic machine does not render engineering concepts void.

---

## 4. Research Agenda & Long-Term Roadmap

* **Direct Native Test Spec Execution:** Expanding the engine's long-term vision to directly read, parse, and execute native Markdown/text test case specifications without requiring intermediate formatting, mapping human documentation directly into executable machine intent.
* **Post-Execution AI Verification ("Second Opinion"):** Implementing a secondary AI auditor that evaluates recorded execution traces against initial test intent to classify outcomes (`SUCCESS`, `ACCEPTABLE_DEVIATION`, `UNEXPECTED_SIDE_EFFECT`) before, during, or after execution.
* **Low-Cost Comprehensive Evaluation:** Leveraging general-purpose LLMs/VLMs to evaluate layout consistency, brand color palettes, WCAG accessibility, and overall application health in a single pass without adding specialized tooling.
* **No-Code Custom Extensions:** Ability to add extensions for more verification without the need for programming, e.g., language and terminology checks.

---

## 5. Conclusion

Neo AI combines natural-language test authoring with native execution speed, zero token costs in CI/CD, and explicit human control. By compiling plain-language intent into local playbooks, teams eliminate selector maintenance while guaranteeing **reproducibility** and keeping human experts firmly **in the loop**.

* **Framework Repository (`develop` branch):** [Neodymium Library on GitHub (`develop` branch)](https://github.com/Xceptance/neodymium-library/tree/develop)
* **Technical Documentation & Examples (`develop` branch):** [`neodymium-library/doc`](https://github.com/Xceptance/neodymium-library/tree/develop/doc) and sample playbooks under [`src/test/resources/playbooks/`](https://github.com/Xceptance/neodymium-library/tree/develop/src/test/resources/playbooks/)
