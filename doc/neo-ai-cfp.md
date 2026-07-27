# Neo AI: Intent-Driven Test Automation with Zero-Cost Replay and Multi-Dimensional Governance

**Format:** Technical Presentation / Call for Papers (CFP) Summary  
**Duration:** 45 Minutes  
**Authors:** Neodymium Core Team / Xceptance GmbH  
**Date:** July 2026  
**Target Length:** ~3 DIN A4 Pages (Condensed Technical Overview)

---

> [!NOTE]
> **Open Source Research & Non-Commercial Commitment**: This session is strictly focused on open engineering research, architectural discoveries, and advancing quality engineering standards. Neo AI is developed as a 100% free, community-accessible open-source framework (`GNU AGPLv3 / MIT`). We are not pitching a product or selling commercial SaaS subscriptions; our goal is sharing empirical learnings to help the software testing ecosystem evaluate and understand AI-driven test automation effectively.

Software test automation has reached a critical inflection point. For decades, teams relied on code-based frameworks (Selenium, Playwright, Cypress, Selenide) or BDD tools (Cucumber). While fast in execution, all traditional tools share a common bottleneck: **unforgiving reliance on explicit DOM locators** (CSS, XPath, IDs) and **strict, rigid framework syntax**. In modern Single-Page Applications (React, Next.js, Vue, Angular), component hydration, generated utility classes, and layout shifts across deployments regularly break locators, forcing QA engineers to spend significant effort repairing brittle step definitions or begging developers for custom `data-testid` attributes.

Recent AI approaches—such as live-LLM browser wrappers—attempt to solve locator fragility by querying LLMs live on *every single test step*. However, querying cloud APIs at runtime introduces steep monthly token bills, 10x CI/CD execution latency, external API dependencies, and validation hallucinations during financial or arithmetic checks.

**Neo AI** (Neodymium Aura AI) solves this dilemma through **Intent Compilation into File-Based Playbooks with Zero-Cost Offline Replay**:

```
+-----------------------------------------------------------------------------------------------------------------------------+
|                                              THE TEST AUTOMATION SPECTRUM                                                   |
+-----------------------------------------------------------------------------------------------------------------------------+
|                                                                                                                             |
|  CODE-BASED FRAMEWORKS            BDD SPECIFICATIONS              LIVE-AI DRIVERS                 VISION & AUTONOMOUS AGENTS|
|  (Native Language Automation)     (Human-Readable Specs)          (Per-Step LLM Querying)         (Visual Pixel Navigation) |
|                                                                                                                             |
|  [+] Fast CI/CD execution         [+] Business-readable text      [+] Plain-language authoring     [+] No DOM dependency     |
|  [+] Zero runtime token cost      [+] Structured specifications   [+] Dynamic locator resolution   [+] Autonomous discovery  |
|  [-] High POM code overhead       [-] Dual-layer maintenance      [-] High monthly token costs     [-] Uncapped VLM costs    |
|  [-] Brittle CSS/XPath selectors  [-] Glue-code & locator binding [-] 10x slower CI/CD latency    [-] Non-deterministic     |
|  [-] Significant developer toil   [-] Selector fragility remains  [-] Hallucinations in math/rules [-] High compute overhead |
|                                                                                                                             |
+-----------------------------------------------------------------------------------------------------------------------------+
                                                              |
                                                              v
+-----------------------------------------------------------------------------------------------------------------------------+
|                                                    THE NEO AI SOLUTION                                                      |
+-----------------------------------------------------------------------------------------------------------------------------+
|  [+] Multi-Lingual Natural Authoring (English, German, French, Spanish, or mixed steps in YAML format)                      |
|  [+] On-The-Fly Dynamic UI Translation (LLM dynamically maps natural steps across localized UI variants)                    |
|  [+] Zero-Cost Offline Replay (100% Offline in CI/CD via file-based JSON Playbooks)                                         |
|  [+] Local Self-Healing (LLM heals locators in local playbook files when UI breaks)                                         |
|  [+] Programmatic Extensions (JAVA_METHOD for custom logic, formatting, and precision)                                      |
|  [+] Soft Warning Gates (Non-blocking (soft) and (optional) step tags)                                                      |
+-----------------------------------------------------------------------------------------------------------------------------+
```

Neo AI occupies the **"Sweet Spot" of Quality Engineering**: positioned strictly between rigid legacy scripts (which break on minor DOM locator updates) and unpredictable autonomous agent guesswork (which infer unconstrained intent). Neo AI maintains explicit natural-language step definitions while executing them with relaxed locator flexibility and deterministic replay.

---

## 2. Core Architectural Pillars & Mechanisms

The 45-minute technical presentation walks through the six engineering pillars powering Neo AI:

```
+-----------------------------------------------------------------------------------+
|                                 NEO AI PLATFORM                                   |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  1. TEST AUTHORING & LINTING                                                      |
|     Multi-Lingual Natural Language (YAML) --> [ PESAP Static Analysis & Predictor ]|
|                                                                                   |
|  2. PLAYBOOK COMPILATION                                                          |
|     Creation Mode (LLM) --> Compiles to [ Local File-Based JSON Playbook ]        |
|                                                                                   |
|  3. CI/CD OFFLINE REPLAY & SELF-HEALING                                           |
|     CI Execution --> Replays JSON Playbook via Native WebDriver (0 Tokens, Fast)  |
|                         |                                                         |
|                         +--> [ Locator Fails? ]                                   |
|                                    |                                              |
|                                    v (Self-Healing Loop)                          |
|                             [ Escalating Context System ]                         |
|                             [ LLM Heals Element Locator ]                         |
|                             [ Updates Local Playbook File ]                       |
|                                                                                   |
|  4. GUARDRAILS & OBSERVABILITY                                                    |
|     - Programmatic Extensions: JAVA_METHOD / Formatting / BigDecimal / JShell     |
|     - Perceptual Verification: Microsecond Local dHash & Hamming Distance          |
|     - Soft Warning Gates: Non-Blocking (soft) / (optional) Step Tags              |
|     - Decoupled State Machine: Thread-Isolated Web Browser Engine                 |
|     - Reporting & Diagnostics: Aura Server & Interactive Trace Viewer             |
+-----------------------------------------------------------------------------------+
```

### 2.1 Multi-Lingual Natural Language Authoring & AST Synchronization
Engineers and domain experts author test scenarios in plain human language formatted in YAML—without writing Gherkin step bindings or code abstractions:

```yaml
# Multi-Lingual Natural Language Test Scenario (YAML Authoring)
- step: "Click the checkout button and proceed to payment"        # English
- step: "Klicke auf 'In den Warenkorb' und prüfe die Gesamtsumme" # German
- step: "Cliquez sur 'Commander' et vérifiez le montant"          # French
```

YAML acts as an intermediate serialization format representing an underlying **Test Step Abstract Syntax Tree (AST)**, establishing 1-to-1 bidirectional synchronization between human definitions and compiled machine playbooks.

### 2.2 Creation Mode vs. Zero-Cost Offline Replay
* **Creation Mode**: On first execution, Neo AI queries the LLM to inspect the DOM, determine locator strategies, and compile them into a local file-based **JSON Playbook** (`src/test/resources/ai-playbooks`).
* **Replay Mode**: In CI/CD pipelines, Neo AI replays native WebDriver/Selenide actions directly from the local JSON Playbook file **100% offline at native speed with zero LLM token costs**.

### 2.3 Localized Self-Healing & Git-Driven Governance
When a front-end deployment changes DOM elements (e.g. changing `#btn-submit-v1` to `#btn-submit-v2`), offline replay fails. Neo AI activates localized self-healing:
1. Queries the LLM to re-analyze the live DOM.
2. Heals the broken locator strategy and executes the step.
3. Updates the local JSON Playbook file on disk.

Because Playbooks reside in project directories, locator self-healing updates are tracked in Git, allowing engineers to review selector diffs during normal Pull Requests before merging into production pipelines.

### 2.4 Token Economics: Escalating Context & PESAP
To avoid transmitting full HTML DOMs to LLMs on every query, Neo AI employs a **6-Tier Escalating Context System**:
1. `HINT`: Zero DOM elements (inline hints).
2. `AXTREE`: Browser Accessibility Tree outline (~90% smaller than HTML).
3. `LEAN`: Interactive DOM elements only (buttons, inputs, links).
4. `STANDARD`: Full interactive DOM + body text nodes.
5. `VISUAL_LEAN`: Interactive DOM + compressed screenshot.
6. `VISUAL`: Full DOM + viewport screenshot (Canvas/Shadow DOM).

Combined with the **Pre-Execution Static Analysis Phase (PESAP)**—which predicts initial context tiers, lints ambiguous instructions, and splits compound actions—Neo AI reduces prompt token payloads by 85–95%.

### 2.5 Programmatic Java Extensions (`JAVA_METHOD`)
To prevent LLM calculation or formatting errors during assertions, complex data transformations, or specialized evaluations, Neo AI delegates execution to native Java code via `@AiMethod` annotations. `JAVA_METHOD` provides reflection security, exact `BigDecimal` financial precision, localized price normalization (`14,96 €` $\rightarrow$ `14.96`), and custom evaluation hooks.

### 2.6 Perceptual Hash Verification & Soft Warning Gates
* **dHash Fingerprinting**: Caches 64-bit perceptual image hashes in Playbooks. Replays evaluate Hamming distances in microseconds to confirm visual stability without remote VLM calls.
* **Soft Warning Gates (`(soft)` / `(optional)`)**: Steps or assertions tagged `(soft)` (e.g., `Observe promo banner (soft)`) issue warnings when secondary UI elements differ across environments without failing the test run.

---

## 3. Engineering Retrospective: Meta-AI & Architectural Iterations

Developing Neo AI yielded four major empirical takeaways regarding LLM integration and framework craftsmanship:

1. **Building AI with AI (Meta-Development)**: Neo AI was developed using AI pair-programming tools for rapid component prototyping. However, unconstrained feature additions can cause codebase accretion, requiring active human architectural governance.
2. **The 5-Iteration Architectural Evolution**: Neo AI underwent **five major internal refactorings**. Iteration 5 rebuilt the framework into a clean, decoupled modular engine (with abstract interfaces like `TargetExecutor`, `SutState`, `StateMachineRunner`, and `ActionSanitizer`) to allow new context tiers and interaction modes to be plugged in cleanly.
3. **Elimination of Hallucinations**: Early prototypes encountered LLM hallucinations (inventing invalid element attributes or unparseable XPath selectors). Strict JSON schemas, PESAP analysis, and multi-stage defensive parsing (`ModelResponseParser`) achieved near 100% output determinism with zero hallucinations during playbook compilation.
4. **Task-Specific Model Routing & Behavioral Dynamics**: Iteration 5 routes tasks to specialized models (heavy screenshot analysis to external VLMs, fast text reasoning to local/lightweight LLMs). Empirical testing demonstrates that **changing models changes behavior**, requiring model-family prompt tuning to normalize locator selection choices across LLM providers.

---

## 4. Key Paradigm Comparison

| Dimension | **Neo AI (Intent Playbooks)** | **Live-LLM Drivers** | **AI Code Generators** | **Vision / Coordinate Agents** | **Selector Proxies (e.g., Healenium)** |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Core Paradigm** | **Native Language + Playbook Caching** | Live Per-Step LLM Prompting | Static Code Generation | Screenshot $(x,y)$ Visual Clicks | Proxy Selector Distance Heuristic |
| **CI/CD Replay** | **100% Offline (0 Tokens, Native Speed)** | Requires Cloud API per step | Fast (once compiled) | High VLM Compute per step | DB lookup latency |
| **Self-Healing** | **Local File-Based Playbook Updates** | SaaS Cloud Updates | Manual Code Review | Vision Replanning | Postgres DB Selector Cache |
| **Math & Logic** | **`JAVA_METHOD` / JShell / BigDecimal** | LLM Prompt Inference | Generated Code Assertions | VLM Prompt Evaluation | Traditional Code Assertions |
| **Token Optimization**| **Escalating Context + Smart Jumps** | Full Page Sent to Cloud | N/A (Build-Time Only) | Full Viewport Screenshot | N/A (ML Selector Model) |
| **Visual & Soft** | **Perceptual dHash + Soft Tags (`(soft)`)** | Cloud Vision API | Visual Baseline Extensions | Cloud VLM | Pixel-by-pixel comparisons |
| **License** | **Open Source (GNU AGPLv3 / MIT)** | Proprietary SaaS / API | IDE / Model API dependent | Proprietary / Model API | Open Source (Apache-2.0) |

---

## 5. Long-Term Roadmap & "Implicit Evaluation" Vision

Long-term development directions for Neo AI focus on three major frontiers:

1. **Next-Generation Aura Server & Second-Opinion Verification**:
   * **Real-Time Streaming Trace Viewer**: Streaming live execution timelines, DOM snapshots, network headers, and LLM reasoning steps to `localhost:8080`.
   * **AI Verification Verdicts ("Second Opinion")**: A post-execution secondary AI auditor evaluates recorded action traces against initial test intent, issuing formal verdicts (`SUCCESS`, `ACCEPTABLE_DEVIATION`, `UNCERTAIN_GOAL`, `UNEXPECTED_SIDE_EFFECT`).
   * **Multi-Model Consensus**: Querying multiple distinct model families (Gemini + Claude + local LLMs) in parallel to establish consensus verdicts.
2. **Multi-Tier Agentic Execution Modes ("Spend More to Get More")**:
   * *Default Deterministic Mode*: Maximum token efficiency, 0-token offline CI/CD replay, deterministic intent execution.
   * *Exploratory / Aggressive Agentic Mode*: An interactive problem-solving tier where the agent engages in multi-turn reasoning and autonomous exploratory recovery for complex unscripted UI failures.
3. **Human Co-Worker Ergonomics & "Implicit Multi-Dimensional Evaluation"**:
   * **Co-Worker Ergonomics**: Authoring test scenarios feels like delegating tasks to a human co-worker in natural language without writing explicit verification code for every UI property.
   * **Implicit Verification "For Free"**: By leveraging general-purpose LLMs/VLMs directly within the framework, multi-dimensional quality checks (**visual layout, accessibility WCAG compliance, color palette consistency, and runtime health**) come **implicitly for free** as a native capability of model inference without purchasing single-purpose SaaS suites.

---

### Presentation Resources & Links

* **Framework Repository**: [Neodymium Library on GitHub](https://github.com/Xceptance/neodymium-library)
* **Full Technical Paper**: [Neo AI Architecture Paper Version 2.0](https://github.com/Xceptance/neodymium-library/blob/master/doc/neo-ai-technical-paper.md)
* **Sample Scenarios**: Natural-language Playbooks under `src/test/resources/playbooks/`
