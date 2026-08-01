# Human Specs, Machine Execution: Rethinking Test Automation with the Help of AI

**Speaker:** René Schwietzke (Managing Director & Co-Founder, Xceptance)  
**Format:** Keynote Address or Main Track Technical Session  
**Track / Topics:** Quality Engineering, Test Automation, AI in Software Engineering, Open-Source Testing  

---

## Short Abstract

Fast release cycles constantly break UI test automation, and generating code with AI just leaves teams with fragile code that breaks when UIs change. Querying live LLMs on every step is equally impractical for CI/CD. This talk presents a pragmatic open-source alternative: **Intent Compilation with Zero-Cost Replay**. By keeping human-language specs as the single source of truth, an LLM compiles test intent once into an internal machine playbook. In CI/CD, test suites replay this playbook 100% offline at native browser speed with zero token costs, while automated self-healing updates it under the hood when UI layouts evolve.

---

## Extended Abstract / Proposal Description

For decades, software test automation forced teams to write and maintain complex code wrappers. Modern AI tools often attempt to solve this by generating test code—but why introduce an intermediate code layer when no one intends to maintain that code manually anyway? Furthermore, when UI changes occur, generated code inevitably breaks, forcing teams into continuous manual re-generation and re-evaluation cycles. On the other hand, querying live LLMs on every single test step creates slow, expensive, and unpredictable test runs.

Rather than presenting a commercial product, this session takes you on an engineering journey—sharing what we learned, where we struggled, and how we tackled core automation challenges by starting our own open-source AI test automation project. We believe test automation shouldn't generate code or rely on live AI guesswork—**your human-language test case should remain the single source of truth.**

This session shares our engineering findings, architecture, and practical demonstrations:

1. **Automation Challenges & AI Pitfalls:** Real-world lessons learned when using AI for test automation, addressing non-determinism, DOM complexity, and state management.
2. **Transforming Specs to Executable Actions:** How LLMs parse natural human test steps into precise, deterministic browser actions.
3. **Browser Control Mechanics:** Practical strategies for controlling the browser efficiently using DOM snapshots, accessibility trees, and protocol drivers.
4. **Machine Playbooks & Offline Replay:** Compiling test intent into cached machine playbooks for 100% offline CI/CD execution at native browser speed with zero token costs.
5. **Automated Machine Healing:** Keeping test suites maintenance-free by updating machine playbooks automatically under the hood when UI layouts drift.
6. **Beyond Explicit Verification (Sanitization & Guardrails):** Adding automatic console/network error checks, data sanitization, and visual regression testing as free side effects beyond explicit test steps.

Rather than showcasing a finished product, this talk offers a practical learning journey. Attendees will gain actionable insights into the mechanics of translating human language into automation, equipping them with valuable knowledge to evaluate, design, or critique any AI testing concept or tool.

---

## Target Audience & Prerequisites

- **Audience:** QA Engineers, Test Automation Architects, Software Developers, Engineering Managers, and DevOps Engineers.
- **Prerequisites:** Familiarity with basic web testing concepts (WebDriver, Selenium, Playwright) and CI/CD automation pipelines.

---

## Key Takeaways

1. **Single Source of Truth:** Authoring and maintaining test scenarios in natural human language without generating or maintaining code.
2. **Zero-Cost Offline Replay:** Compiling human test intent into machine playbooks that run 100% offline in CI/CD at native speed with zero API token costs.
3. **Transparent Machine Healing:** Keeping test maintenance hands-free by updating internal machine playbooks automatically during UI changes.
4. **Free Visual Assertions & Verification:** Gaining visual regression testing as a free side effect of playbook execution, backed by explicit outcome verification steps and summary reporting.

---

## Talk Outline

```
00:00 - 08:00 | 1. The Dilemma: Automation Challenges & Generated Code Overhead
08:00 - 18:00 | 2. Architecture: Step Translation, Browser Control, & Machine Playbooks
18:00 - 32:00 | 3. Live Demos: Human Specs, Playbook Compilation, Self-Healing, & Visual Assertions
32:00 - 40:00 | 4. Deep Dive: Beyond Explicit Steps (Sanitization, Guardrails, & Verification)
40:00 - 45:00 | 5. Conclusion & Q&A
```

---

## Speaker Biography

**René Schwietzke** is the Managing Director of Xceptance, based in Germany and the US. As co-founder, he has played a major role in shaping the company's profile, including the continuous evaluation and development of their test tools and services.

René Schwietzke is a seasoned IT professional with over two decades of experience in performance tuning and performance measurements. His academic background includes a Master's degree in Computer Science, and he actively shares his knowledge by giving lectures at universities and presenting at international software development conferences. He is also a sought-after speaker for training sessions on Java, performance testing, quality assurance, and testing.

---

## Speaker Pitch

> *"This isn't a product pitch—it's an engineering journey to understand how AI test automation really works under the hood. Generating code with AI creates unmaintained code layers that break on UI changes, while querying live LLMs on every test step is too slow and expensive for CI/CD. To explore and solve this, we started our own open-source AI test automation project. In this session, we share our lessons learned, architecture, and practical concepts—showing how human-language specs can remain the single source of truth through Intent Compilation and zero-cost offline replay."*
