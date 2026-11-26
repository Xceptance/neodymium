# [IDEA-20261010-1322] Named Sub-Loop Delegation with Tool Namespacing and Targeted Intel Ingestion

- **Status:** `Proposed`
- **Proposed:** 2026-10-10 13:22
- **Resolved:** `Pending`
- **Component:** `neodymium-core` (`AgentToolLoopStep`, `ExecuteActionsStep`, `ToolRegistry`, `AiPipeline`)
- **Category:** `Architecture & Core`
- **Author:** AI-generated: Gemini 3.8 Flash & Xceptance GmbH 2026

## 1. Problem Statement & Motivation
Currently, Neodymium's agent execution (`AgentToolLoopStep.java`) operates as a single monolithic ReAct loop where all tools, system rules, and interaction types are bundled together. This presents two key architectural challenges:

1. **Prefix Cache Invalidation from Dynamic Tool Shuffling:**
   In `AgentToolLoopStep.java` (L2259–L2296), available tools are dynamically pruned per-step based on runtime heuristics (`alertPresent`, `hasMultipleTabs`, `isVisualAssertion`, `isExoticToolDemanded`). Furthermore, system prompts conditionally append visual instructions (`isVisual`). Because tool definitions and system rules sit in the prompt prefix header, any alteration mutates the prefix token hash and invalidates provider-side prompt caching (Gemini, Claude, OpenAI), causing redundant token recalculation across steps.

2. **Monolithic Tool Loop Congestion & Scope Contamination:**
   A single loop currently handles functional actions (`click`, `fill`), verifications (`assert_text`), visual audits, and quality judgment. Presenting 30+ mixed mutating and assertion tools simultaneously increases the risk of models executing accidental mutating actions during assertions. Furthermore, introducing new specialized test concerns (such as accessibility keyboard audits, security header/input probing, or layout contrast checks) into this single loop causes massive prompt clutter, context dilution, and combinatorial maintenance issues.

## 2. Proposed Architecture & Design

### Text-Based Architecture Flow:
```text
+---------------------------------------------------------------------------------------+
|                 Java Step Pipeline (Main Controller — No LLM Pre-Reasoning)           |
+---------------------------------------------------------------------------------------+
|  1. Parse Step & Resolve Bound Sub-Loops                                              |
|     Reads step instruction, resolves annotations like "(a11y)", or global policies    |
|                                                                                       |
|  2. Execute Primary Interaction / Assertion ('default' Sub-Loop)                      |
|     --> Intel Injected: [Instruction, SessionVars, SUT State]                         |
|     --> Tool Namespace: browser.* (Full 39-tool suite, static schema)                 |
|     --> Failure Policy: FATAL BREAK (halts test if action/assertion fails)            |
|                                                                                       |
|  3. SUT State Settle Boundary                                                         |
|     --> Deterministic pause: waits for DOM settle, AJAX/network idle, and animations  |
|                                                                                       |
|  4. Sequential Specialized Sub-Loop Dispatch with Targeted Intel                      |
|     [a] 'judge'    --> Intel: [Planned vs Executed, Before/After] | Tools: judge.*    |
|     [b] 'a11y'     --> Intel: [Settled SUT State, Target Scope]   | Tools: a11y.*     |
|     [c] 'security' --> Intel: [Settled SUT State, Network Logs]   | Tools: security.* |
|     --> All auxiliary audits default to: NON-BREAKING NOTICE                          |
|                                                                                       |
|  5. Findings Aggregator (Author-Managed State)                                        |
|     --> State transitions are owned by the test author (no synthetic resets)          |
|     --> Non-fatal notices aggregated into report (Allure / HTML dashboard)            |
+---------------------------------------------------------------------------------------+
```

### Core Architecture Components & Principles:

1. **Java Orchestrates, Sub-Loops Interact (No PESAP):**
   - The main loop is deterministic Java (`ExecuteActionsStep`). It does not interact with the browser directly.
   - It avoids PESAP-style speculative LLM pre-reasoning: sub-loop dispatch is determined statically by configuration and step annotations, incurring zero upfront LLM round-trips ([DEF-20260916-03](doc/defects/2026-09/DEF-20260916-03-pesap-pipeline-decoupling-direct-agent-execution.md)).

2. **Interactive Sub-Loops:**
   - Sub-loops are the sole entities that interact with the live WebDriver.
   - Sub-loops are active and interactive: an `a11y` sub-loop sends keyboard Tab events and checks focus; a `security` sub-loop probes form inputs; an `action` sub-loop clicks and types.

3. **Tool Namespacing (`namespace.*`):**
   - Instead of brittle tool-by-tool enumerations, sub-loops bind toolsets using namespace patterns (e.g. `browser.*`, `a11y.*`, `security.*`, `judge.*`).
   - The `default` sub-loop carries the complete standard 39-tool browser suite under `browser.*` with a completely static, unvarying schema that eliminates runtime pruning.

4. **Targeted Initial Intel Ingestion:**
   Rather than dumping entire conversation histories or unneeded retry logs across boundaries, Java injects a tailored initial intel slice into each sub-loop:

   | Sub-Loop Name | Ingested Intel (Context Slice) | Tool Namespace | Failure Policy |
   | :--- | :--- | :--- | :--- |
   | **`default`** | Active instruction, session variables, initial SUT state | `browser.*` (39 tools) | **Fatal** (halts test) |
   | **`judge`** | Planned instruction, executed tool calls, before & after states | `judge.*` | **Notice** (non-fatal) |
   | **`outcome`** | Target outcome criteria, settled page state | `outcome.*` | **Notice** (configurable) |
   | **`a11y`** | Settled page state, active target scope (no action retry history) | `a11y.*` | **Notice** (non-fatal) |
   | **`security`** | Active form fields, endpoints, network logs (no action retry history)| `security.*` | **Notice** (non-fatal) |
   | **`layout-check`**| Viewport dimensions, target bounding box | `layout.*` | **Notice** (non-fatal) |

5. **Reusable Declarations & Natural Language Invocation:**
   - Sub-loops can be defined upfront in YAML headers, Java annotations, or imported from reusable include files under human-readable names:
     ```yaml
     _subloops:
       a11y:
         description: "Evaluate keyboard navigation, focus indicators, and WCAG AA contrast"
         tools: "a11y.*"
         intel: [sut_state, target_scope]
         failOnError: false

       security:
         description: "Probe input sanitization, check CSP directives and sensitive cookie flags"
         tools: "security.*"
         intel: [sut_state, network_logs]
         failOnError: false
     ```
   - **Default Behavior:** Any step without annotations runs the standard `default` interaction/assertion sub-loop.
   - **Per-Step Invocation:** Test authors invoke specialized sub-loops using natural parenthetical annotations:
     ```yaml
     # 1. Regular default interaction step (zero overhead, standard execution)
     - instruction: Click "Add to Cart"

     # 2. Regular assertion with an auxiliary layout & a11y sub-loop
     - instruction: Verify mini-cart drawer displays item (a11y, layout-check)

     # 3. Dedicated specialized audit step
     - instruction: Audit checkout payment form attributes (security)
     ```
   - **Global Invocation:** Configured via class/method annotations (e.g. `@AiJudge`, `@AiOutcomeVerification`, `@AiSubLoop(name = "a11y")`).

6. **Decoupled Failure Semantics:**
   - **Fatal Breaks:** Primary interaction or assertion failures halt the test immediately (`ConclusiveFailureException`).
   - **Non-Breaking Notices:** Specialized audits (`a11y`, `security`, `judge`) record diagnostic notices to Allure/HTML reports without breaking the functional flow, unless configured as strict quality gates (`failOnError = true`).

7. **Author-Owned State Management:**
   - The framework does not attempt artificial or synthetic DOM resets or blur gymnastics between sub-loops. Managing test state and accounting for interaction side-effects is the explicit responsibility of the test author.

## 3. Expected Impact & Trade-offs
- **Benefits:**
  - **100% Prefix Cache Stability:** Because tool profiles per sub-loop are strictly static, provider-side prompt caching remains warm across all steps.
  - **Zero PESAP Latency:** Eliminates speculative pre-reasoning; sub-loop sequencing is handled in Java.
  - **Architectural Unification:** Replaces ad-hoc interceptors (`QualityJudge`, `OutcomeVerification`) with a single extensible sub-loop primitive.
  - **Clean Reporting:** Distinguishes functional test breaks from auxiliary diagnostic notices.
  - **Context Hygiene:** Sub-loops receive only the specific intel slice required for their role, avoiding prompt dilution from retry scratchpads.
- **Risks & Trade-offs:**
  - **Execution Latency on Indiscriminate Auditing:** Running interactive audits on every step would inflate test duration; mitigated by explicit step targeting and selective milestone triggers.
  - **SUT State Sharing:** Interactive audits that press keys or open menus leave real browser side-effects; test authors must design step sequences accordingly.

## 4. Open Questions & Alternatives Considered
- *Alternative 1: LLM-Driven Step Coordinator (PESAP-style).* Evaluated and rejected because upfront reasoning about sub-loop dispatch re-introduces the latency and fragility of PESAP ([DEF-20260916-03](doc/defects/2026-09/DEF-20260916-03-pesap-pipeline-decoupling-direct-agent-execution.md)).
- *Alternative 2: Read-Only Audit Sub-Loops.* Evaluated and rejected because thorough accessibility evaluation requires keyboard tabbing and ARIA interaction, and security testing requires input probing.
- *Alternative 3: Automatic Synthetic State Rollback.* Evaluated and rejected to avoid fragile framework-level heuristics that mask real application behavior.
- *Open Question:* Should sub-loop registration be exposed via a plugin SPI (`SubLoopProvider`) for third-party extensions, or kept internal to `neodymium-core` in the initial milestone?
