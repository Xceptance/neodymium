# [DEF-20260918-02] Elimination of Fragile Natural Language Text Guessing in SemanticIntent

- **Status:** Resolved
- **Opened:** 2026-09-18
- **Closed:** 2026-09-18
- **Component:** `neodymium-core` (`ai-model`, `ai-pipeline`)
- **Scope:** `Framework`
- **Symptom:** `SemanticIntent.inferFromInstruction` attempted to deduce step intent from natural language instruction text using hardcoded English substring patterns (`contains(" says now ")`, `contains(" is shown")`, `contains(" is mentioned")`), introducing fragile keyword-guessing heuristics into core Java logic and violating Neodymium's universal, language-neutral design.
- **Root Cause:** Following the removal of PESAP (which previously supplied LLM-classified intent), an ad-hoc keyword-matching method `inferFromInstruction` was introduced in commit `96b9c315f` to guess intent for unclassified steps, embedding English-specific phrasing assumptions into Java core logic.
- **Detection Gap ("What did we miss?"):** Unit tests only verified that `inferFromInstruction` matched specific predefined English test sentences, without verifying language neutrality or handling diverse phrasings.
- **Resolution:** Removed `SemanticIntent.inferFromInstruction` completely. Reverted `AgentToolLoopStep` to rely strictly on explicitly configured or recorded `SemanticIntent` (or `null` when unspecified), allowing the LLM's system prompt instructions to govern action vs. verification execution without heuristic second-guessing.
- **Safety Net Added:** Cleaned `SemanticIntentTest` to eliminate keyword inference tests; verified that `AgentToolLoopStepTest` executes cleanly without keyword guessing.
