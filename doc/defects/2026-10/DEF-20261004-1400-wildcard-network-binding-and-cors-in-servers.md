# [DEF-20261004-1400] Wildcard Network Binding and Permissive CORS in Interactive Console and Test Servers

- **Status:** `Resolved`
- **Opened:** 2026-10-04 14:00
- **Closed:** 2026-10-04 14:00
- **Component:** `neodymium-core`, `neodymium-test-server`
- **Scope:** `Framework & Infra/Build`
- **Symptom:** `InteractiveConsoleServer` and `EmbeddedHtmlServer` bound to wildcard `0.0.0.0` across all host network interfaces. Furthermore, `InteractiveConsoleServer` and `InteractiveConsoleEngine` (SSE and Action handlers) returned `Access-Control-Allow-Origin: *` unconditionally, permitting arbitrary external web origins to interact with the local debugging console and execute actions on the SUT or trigger process stops.
- **Root Cause:** In `InteractiveConsoleServer.createServer()`, `HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0)` was hardcoded. In `EmbeddedHtmlServer.createHttpServerWithFallback()` and `createHttpsServerWithFallback()`, `new InetSocketAddress(port)` defaulted to wildcard listening. Both HTTP handlers hardcoded `Access-Control-Allow-Origin: *` without validating the request `Origin` header.
- **Detection Gap ("What did we miss?"):** Unit tests verified that the servers started and responded to local HTTP requests, but never inspected `address.getAddress().isLoopbackAddress()` or tested malicious cross-origin requests from external web origins like `https://evil.com`.
- **Resolution:**
  1. Defaulted `InteractiveConsoleServer` to bind to `127.0.0.1` (configurable via `neodymium.ai.console.bindAddress`).
  2. Implemented `InteractiveConsoleServer.applyCorsHeaders` and `isLocalhostOrigin` restricting `Access-Control-Allow-Origin` strictly to `http://localhost:*`, `http://127.0.0.1:*`, or `"null"`, rejecting external web domains. Applied this helper across `InteractiveConsoleServer` handlers and `InteractiveConsoleEngine` (`SseHandler`, `ActionHandler`).
  3. Bound `EmbeddedHtmlServer` HTTP and HTTPS fallback servers strictly to `127.0.0.1`.
- **Safety Net Added:** Added permanent automated unit test `com.xceptance.neodymium.ai.console.InteractiveConsoleServerTest` asserting default loopback binding and origin validation, and `org.neodymium.ai.util.EmbeddedHtmlServerTest.testLoopbackBinding` verifying `EmbeddedHtmlServer` loopback isolation.
