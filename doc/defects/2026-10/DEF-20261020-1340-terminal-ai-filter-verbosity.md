# [DEF-20261020-1340] Aura Manager Terminal "AI Only" Filter Verbosity and Text Wall Regression

- **Status:** `Resolved`
- **Opened:** 2026-10-20 13:40
- **Closed:** 2026-10-20 16:25
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** When running tests in Aura Manager with the "AI Only" filter enabled, terminal output displays walls of text containing Maven compiler logs, JVM Unsafe warnings, Surefire discovery noise, 150-line tool schema dumps, and raw HTTP wire request bodies (e.g. `      "text" : "You are an autonomous web testing agent..."` and JSON parameters), while crucial agent actions (such as modern native tool calls, tool results, thoughts, and goal achievements) were previously stripped and hidden.
- **Root Cause:**
  1. Overly broad keyword matching on `'aura'` and `'step'` matched package names (`com.xceptance.neodymium.aura.sandbox`), file paths, and pipeline steps in Maven/Surefire output.
  2. Character class regex defect `/[... \?]/` caused literal question marks `?` in URLs, regexes, and version strings to match every line containing `?`.
  3. Modern AI emojis (`💭`, `🔧`, `📥`, `🎯`, `🚀`, `⚖️`, `🏁`, `📊`, `🛑`, `🔄`, `🚨`, `💡`, `🧬`, `🚫`) and keywords (`thought:`, `tool call:`, `result [`, `goal accomplished`) were omitted from `isAiLogLine()`.
  4. Multi-line indented blocks were evaluated statelessly, stripping headers and exposing disconnected continuation lines.
  5. `isMavenLog` only suppressed 6 specific strings, leaving compiler logs, build summaries, and dependency warnings unfiltered.
  6. **Continuation Leakage & Raw Wire Logs:** `Agent Turn #` set `lastLineWasAi = true`, and the continuation rule allowed any line starting with whitespace (`line.startsWith('   ')`). When LangChain4j or LLM client wire logging dumped indented JSON HTTP request/response payloads (`"text" : "You are an autonomous web testing agent..."`), every line was treated as an AI continuation block, producing massive text walls.
  7. `dev.langchain4j` lacked an explicit warning threshold in `log4j2.properties`, allowing default LangChain4j `LoggingHttpClient` to dump HTTP bodies to STDOUT console.
- **Detection Gap ("What did we miss?"):** `StaticResourceSyntaxTest` initially did not exercise continuation filtering against raw JSON key-value properties or HTTP wire dumps following `Agent Turn #` headers.
- **Resolution:**
  1. Replaced broad `'aura'` and `'step'` substrings with exact structured patterns (`Step [`, `Step 🧠`, `Thought:`, `Tool Call:`, `Result [`, `Goal Accomplished:`, etc.).
  2. Removed `\?` literal matching from `emojiPattern` and added the full modern AI emoji set (`[🤖🧠💭🔧📥🎯🚀🎬🏁🏆👣🔮⚙✨🔍▶✅❌🛑⚠️⚖🔄📊🐞🐛🚨📸💾📜📂⏱🎟⚡💬📌💡🧬🚫•]`).
  3. Expanded `MAVEN_EXCLUDE_PREFIXES` to suppress compilation, project scanning, Surefire discovery, Unsafe warnings, and SLF4J binding warnings.
  4. Added `AI_SUPPRESS_CONTAINS` and `AI_SUPPRESS_PATTERNS` to eliminate `Available Native Tools` schema dumps, raw conversation context, JSON object properties (`/^\s*"[a-zA-Z0-9_-]+"\s*:/`), JSON punctuation (`/^[{}[\],":\s]+$/`), and HTTP wire headers (`request:`, `response:`, `LoggingHttpClient`, `dev.langchain4j`, `Content-Type:`, etc.).
  5. Implemented `allowsAiContinuation(line)` so single-line headlines (`Agent Turn #`, `Goal Accomplished:`, `Step:`) never open arbitrary indented continuation blocks, while genuine multiline blocks (`Thought:`, `Tool Call:`, `Result [`, `•`, `║`) properly maintain indented continuation.
  6. Configured `logger.dev_langchain4j.level = warn` in `neodymium-core/src/main/resources/log4j2.properties` to suppress HTTP client wire dumps from STDOUT.
  7. Mirrored all changes across `aura-manager/src/main/resources/static/dashboard-runner.js`, `neodymium-core/src/main/resources/com/xceptance/neodymium/aura/dashboard-runner.js`, and `neodymium-core/src/main/resources/com/xceptance/neodymium/aura/dashboard-history.js`.
- **Safety Net Added:** Added automated regression assertions in `StaticResourceSyntaxTest.java` evaluating `isAiLogLine` against indented JSON properties (`"text" : ...`, `"role" : ...`), JSON delimiters (`{`, `}`), and HTTP client wire logs, asserting they evaluate to `false` even when `previousWasAi=true`.
