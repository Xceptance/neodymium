# [DEF-20261020-1340] Aura Manager Terminal "AI Only" Filter Verbosity and Text Wall Regression

- **Status:** `Resolved`
- **Opened:** 2026-10-20 13:40
- **Closed:** 2026-10-20 13:55
- **Component:** `aura-manager`, `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** When running tests in Aura Manager with the "AI Only" filter enabled, terminal output displays walls of text containing Maven compiler logs, JVM Unsafe warnings, Surefire discovery noise, and 150-line tool schema dumps, while crucial agent actions (such as modern native tool calls, tool results, thoughts, and goal achievements) are stripped and hidden.
- **Root Cause:**
  1. Overly broad keyword matching on `'aura'` and `'step'` matched package names (`com.xceptance.neodymium.aura.sandbox`), file paths, and pipeline steps in Maven/Surefire output.
  2. Character class regex defect `/[... \?]/` caused literal question marks `?` in URLs, regexes, and version strings to match every line containing `?`.
  3. Modern AI emojis (`💭`, `🔧`, `📥`, `🎯`, `🚀`, `⚖️`, `🏁`, `📊`, `🛑`, `🔄`, `🚨`) and keywords (`thought:`, `tool call:`, `result [`, `goal accomplished`) were omitted from `isAiLogLine()`.
  4. Multi-line indented blocks were evaluated statelessly, stripping headers and exposing disconnected continuation lines.
  5. `isMavenLog` only suppressed 6 specific strings, leaving compiler logs, build summaries, and dependency warnings unfiltered.
- **Detection Gap ("What did we miss?"):** `StaticResourceSyntaxTest` only performed syntax compilation checks (`node -c`) without exercising log stream classification against realistic Maven subprocess output and modern agent execution logs.
- **Resolution:**
  1. Replaced broad `'aura'` and `'step'` substrings with exact structured patterns (`Step [`, `Step 🧠`, `Thought:`, `Tool Call:`, `Result [`, `Goal Accomplished:`, etc.).
  2. Removed `\?` literal matching from `emojiPattern` and added the full modern AI emoji set (`[🤖🧠💭🔧📥🎯🚀🎬🏁🏆👣🔮⚙✨🔍▶✅❌🛑⚠️⚖🔄📊🐞🐛🚨📸💾📜📂⏱🎟⚡💬📌•]`).
  3. Expanded `MAVEN_EXCLUDE_PREFIXES` to suppress compilation, project scanning, Surefire discovery, Unsafe warnings, and SLF4J binding warnings.
  4. Added `AI_SUPPRESS_CONTAINS` to eliminate `Available Native Tools` schema dumps and raw conversation context.
  5. Implemented stateful multi-line continuation tracking (`lastLineWasAi`) so indented thoughts and tool call parameters remain intact with their headers while non-AI continuation blocks stay hidden.
  6. Mirrored all changes across `aura-manager/src/main/resources/static/dashboard-runner.js`, `neodymium-core/src/main/resources/com/xceptance/neodymium/aura/dashboard-runner.js`, and `neodymium-core/src/main/resources/com/xceptance/neodymium/aura/dashboard-history.js`.
- **Safety Net Added:** Added automated regression test `testDashboardRunnerAiLogFilter` in `com.xceptance.aura.report.staticassets.StaticResourceSyntaxTest` executing Node.js against real modern AI logs and noise benchmark streams.
