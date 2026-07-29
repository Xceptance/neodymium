# Intent-Driven Test Automation with Zero-Cost Replay

**Speaker:** René Schwietzke (Managing Director & Co-Founder, Xceptance)  
**Format:** Keynote Address or Main Track Technical Session  
**Track / Topics:** Quality Engineering, Test Automation, AI in Software Engineering, Open-Source Testing  

---

## Short Abstract

Software projects with fast release cycles constantly break UI test automation through changing locators and script maintenance. Generating test code doesn't solve this—it just leaves engineers with generated code they still have to maintain. This talk demonstrates a pragmatic alternative: **Intent Compilation with Zero-Cost Replay**. Instead of generating code or querying LLMs live on every test step, your human-language test case remains the **single source of truth**. On initial execution, an LLM parses the human test definition to build an internal machine replay playbook—a cached execution artifact not meant for human consumption. In CI/CD, test suites replay this playbook 100% offline at native browser speed with zero API costs, while automated LLM healing updates the machine playbook when UI layouts change—without ever cluttering your human test specs with code or selectors.

---

## Extended Abstract / Proposal Description

For decades, software test automation forced teams to write and maintain complex code wrappers. Modern AI tools often attempt to solve this by generating test code—but generated code is still code, creating a secondary artifact that engineers must compile, debug, and maintain. On the other hand, querying live LLMs on every single test step creates slow, expensive, and unpredictable test runs.

We believe test automation shouldn't generate code or rely on live AI guesswork. **Your human-language test case should remain the single source of truth.**

This session presents an open-source approach demonstrating **Intent Compilation**:
1. **Single Source of Truth:** You write and maintain regular human-language test definitions.
2. **No Code Generation:** No code, Page Objects, or glue scripts are generated or maintained.
3. **Machine Replay Playbooks:** An LLM processes the human test definition once to compile an internal machine playbook (a cached execution payload, not intended for human consumption).
4. **Zero-Cost Offline Replay:** CI/CD runners execute the machine playbook 100% offline at native browser speed with zero LLM token costs.
5. **Transparent Self-Healing:** When UI layouts drift, the LLM re-evaluates the page and updates the machine playbook automatically under the hood.

Key topics covered with practical demonstrations:
1. **Human-Language Test Specs:** Authoring test scenarios in plain human language as the sole source of truth.
2. **Playbook Compilation vs. Replay:** How initial LLM compilation creates machine playbooks for 100% offline CI/CD execution at zero API cost.
3. **Automated Machine Healing:** Updating machine playbooks automatically during UI changes without touching human test definitions.
4. **Smart Context Optimization:** Slicing prompt payloads during compilation using accessibility trees and DOM analysis.
5. **Programmatic Extensions & Guardrails:** Pairing natural steps with custom hooks for precise math, data formatting, and visual baselines.
6. **Outcome Verification & Summary Reporting:** Double-checking action outcomes through explicit verification steps and comprehensive summary reports to guarantee intent fulfillment.

Attendees will gain a practical blueprint for automating tests directly from human-language specs—without writing code, incurring recurring API costs, or maintaining brittle scripts.

---

## Target Audience & Prerequisites

- **Audience:** QA Engineers, Test Automation Architects, Software Developers, Engineering Managers, and DevOps Engineers.
- **Prerequisites:** Familiarity with basic web testing concepts (WebDriver, Selenium, Playwright) and CI/CD automation pipelines.

---

## Key Takeaways

1. **Single Source of Truth:** Authoring and maintaining test scenarios in natural human language without generating or maintaining code.
2. **Zero-Cost Offline Replay:** Compiling human test intent into machine playbooks that run 100% offline in CI/CD at native speed with zero API token costs.
3. **Transparent Machine Healing:** Keeping test maintenance hands-free by updating internal machine playbooks automatically during UI changes.
4. **Outcome Verification & Summary Reporting:** Double-checking AI action results via explicit verification steps, programmatic guardrails, visual baselines, and actionable summary reporting.

---

## Talk Outline

```
00:00 - 08:00 | 1. The Dilemma: Maintenance Fatigue & Generated Code Overhead
08:00 - 18:00 | 2. Concept: Single Source of Truth, Machine Playbooks, & Zero-Cost Offline Replay
18:00 - 32:00 | 3. Live Demos: Human Specs, Playbook Compilation, Self-Healing, & Verification Hooks
32:00 - 40:00 | 4. Deep Dive: Context Optimization, Outcome Verification, & Summary Reporting
40:00 - 45:00 | 5. Conclusion & Q&A
```

---

## Speaker Biography

**René Schwietzke** is the Managing Director of Xceptance, based in Germany and the US. As co-founder, he has played a major role in shaping the company's profile, including the continuous evaluation and development of their test tools and services.

René Schwietzke is a seasoned IT professional with over two decades of experience in performance tuning and performance measurements. His academic background includes a Master's degree in Computer Science, and he actively shares his knowledge by giving lectures at universities and presenting at international software development conferences. He is also a sought-after speaker for training sessions on Java, performance testing, quality assurance, and testing.

---

## Speaker Pitch

> *"Generating test code doesn't solve maintenance—it just leaves you with generated code to maintain. And querying live LLMs on every test step is too slow and expensive for CI/CD. In this session, we demonstrate how your human-language test case stays the single source of truth. By using an LLM to compile human intent into an internal machine playbook—a cached artifact for offline execution, not human consumption—teams get 100% offline CI/CD replay with zero API costs and automated self-healing, without ever writing or maintaining automation code. Everyone can automate."*
