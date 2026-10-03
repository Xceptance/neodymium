# [DEF-20260930-13] High Latency in AI Test Execution Due to Turn 1 Context Starvation, Intercepted Click Retries, and Unchecked Quality Judge

- **Status:** Resolved
- **Opened:** 2026-09-30
- **Closed:** 2026-09-30
- **Component:** AI Engine (`AgentToolLoopStep`, `BrowserToolProvider`, `QualityJudgeToolInterceptor`)
- **Scope:** `Framework`
- **Symptom:** E-commerce test runs (e.g. `AddToCartTest`) take 324s (~5.4 min) for 24 steps; verification steps require 5–8 LLM turns (15s–26s each), and element clicks suffer 12s–23s in browser retry timeouts.
- **Root Cause:**
  1. `AgentToolLoopStep` defaulted Turn 1 context to `ContextLevel.LEAN`, stripping static text leaves (`div`, `span`, `p`, `td`) needed for price/subtotal/total verifications, forcing the LLM into expensive multi-turn `query_dom` / `inspect` exploratory loops.
  2. `QualityJudgeToolInterceptor` defaulted `this.enabled = true` and did not check `config.isJudgeEnabled()`, triggering WebDriver queries, attribute scans, and candidate scoring even when disabled.
  3. `BrowserToolProvider.executeElementClick` incurred Selenide's full retry timeout and disk report attachments upon `ElementClickInterceptedException` before falling back to JavaScript click.
- **Detection Gap ("What did we miss?"):** Existing unit tests mocked LLM tool calls with pre-canned selectors and did not evaluate turn efficiency on static text assertions or measure real-browser timeout cascading on intercepted clicks.
- **Resolution:**
  1. Updated `AgentToolLoopStep` to resolve initial context level via `AiConfiguration.getContextLevel()` (configured to `STANDARD`), ensuring all text content is visible on Turn 1 in a universal, language-agnostic manner.
  2. Bypassed all DOM queries and scoring in `QualityJudgeToolInterceptor.intercept()` when `!config.isJudgeEnabled()`.
  3. Fast-pathed intercepted clicks in `BrowserToolProvider.executeElementClick` directly to JavaScript click.
- **Safety Net Added:** Unit tests asserting `ContextLevel.STANDARD` propagation from configuration, zero DOM queries when Quality Judge is disabled, and fast JavaScript click execution on intercepted elements.
