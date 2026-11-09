# [DEF-20261109-1022] Prevent Indefinite Socket Deadlock on Gemini LLM HTTP Calls

- **Status:** `Resolved`
- **Opened:** 2026-11-09 09:38
- **Closed:** 2026-11-09 10:28
- **Component:** `neodymium-core`
- **Scope:** `Framework & AI/Prompt`
- **Symptom:** During test runs executing live LLM steps (such as `mvn test -Pai-live-tests`), test execution froze indefinitely (~5 hours and 53 minutes) parked inside `jdk.internal.net.http.HttpClientImpl.send()`, locking test execution and build pipelines without timing out, failing, or retrying.
- **Root Cause:**
  1. OpenJDK's `HttpClient.send(HttpRequest, BodyHandler)` is implemented as `sendAsync(...).get()`. In OpenJDK HTTP/2 connections, if a connection drops silently or an HTTP/2 stream stalls without TCP FIN/RST or frame error, OpenJDK's internal CompletableFuture fails to complete exceptionally. Because the synchronous `.send()` uses an untimed `.get()`, the calling thread blocks forever.
  2. `GeminiLlmProvider.createDecoratedHttpClientBuilder()` previously delegated requests synchronously to `delegate.execute(repairedRequest)` without a watchdog deadline.
  3. `LlmRetryHelper.executeWithRetry` only retries when exceptions are thrown by the callable; because the thread was deadlocked inside OpenJDK's `HttpClientImpl.send()`, the retry loop was completely locked out. Additionally, `isTransientError()` only evaluated `t.getMessage().contains("timeout")`, missing typed timeout exceptions when messages are `null` or phrased differently.
- **Detection Gap ("What did we miss?"):** Existing unit tests used in-memory mocks that complete immediately, never simulating dropped HTTP/2 TCP streams or stalled OpenJDK futures.
- **Resolution:**
  1. In `GeminiLlmProvider.createDecoratedHttpClientBuilder()`, wrapped HTTP executions in an asynchronous watchdog: calls `delegate.executeAsync(repairedRequest)` and awaits with a strict deadline `future.get(watchdogDeadline, MILLISECONDS)`.
  2. Configured the watchdog deadline to dynamically resolve the configured request timeout (`neodymium.ai.<role>.timeoutSeconds` or `neodymium.ai.timeoutSeconds`, default 180s) plus a configurable buffer (`neodymium.ai.llm.watchdogBufferSeconds`, default 5s).
  3. On watchdog timeout, immediately invoke `future.cancel(true)` to abort the underlying OpenJDK HTTP/2 stream and close the stalled socket, before throwing `dev.langchain4j.exception.RetriableException`. In `executeAsync()`, attached a cancellation hook so asynchronous callers also forcibly abort underlying sockets on timeout.
  4. In `LlmRetryHelper.isTransientError()`, added explicit recognition for `TimeoutException`, `HttpTimeoutException`, `SocketTimeoutException`, `RetriableException`, `ConnectException`, and `SocketException`, as well as `"timed out"`, `"connection reset"`, `"connection refused"`, and `"broken pipe"`. This allows the existing `LlmRetryHelper` exponential backoff retry loop to catch the timeout and execute a normal retry on a fresh connection.
- **Safety Net Added:**
  - `GeminiLlmProviderTest#testWatchdogTimeoutAbortsStalledSocket`
  - `GeminiLlmProviderTest#testWatchdogSuccessfulExecutionWithinDeadline`
  - `LlmRetryHelperTest#testExecuteWithRetry_succeedsAfterTimeout`
  - `LlmRetryHelperTest#testIsTransientError`
