# [DEF-20261019-0957] Interactive Console HTTP HEAD Probe Hang and Silent Mock Fallback

- **Status:** `Resolved`
- **Opened:** 2026-10-19 09:57
- **Closed:** 2026-10-19 10:02
- **Component:** `neodymium-core` & `aura-manager`
- **Scope:** `Framework`
- **Symptom:** On starting interactive mode, the console intermittently displays the static demo test `testCheckoutFlow` (`Checkout-092`) with a fake failed step ("Enter '123 Test Street' into the Street input field. Element is obscured by overlay") and enters "Heal" mode instead of displaying the actual active test run.
- **Root Cause:** 
  1. `interactive_console.js` issued a `fetch('/api/console/events', { method: 'HEAD' })` probe before establishing SSE.
  2. In `AuraInteractiveController`, `@GetMapping` on `/api/console/events` returned an infinite `StreamingResponseBody` without a dedicated `HEAD` handler, causing Spring MVC to hang indefinitely on HEAD requests.
  3. When the hanging probe was aborted by iframe navigation, runner initialization, or network timeouts, the rejection handler called `tryStaticLoad()`, which fetched and rendered `run_example.json` from the classpath, leaving the UI permanently detached from the live SSE session.
- **Detection Gap ("What did we miss?"):**
  1. `AuraInteractiveControllerTest.testSseEventStreaming` only tested `GET /api/console/events`, never verifying `HEAD /api/console/events`.
  2. Standalone tests used `InteractiveConsoleEngine.SseHandler`, which explicitly handled `HEAD` (`sendResponseHeaders(200, -1)`), masking the failure that occurred when running under Spring Boot Aura Manager.
- **Resolution:**
  1. Added an explicit `@RequestMapping(value = "/api/console/events", method = RequestMethod.HEAD)` endpoint in `AuraInteractiveController` returning immediate 200 OK with SSE headers.
  2. In `interactive_console.js`, eliminated the preflight `fetch HEAD` probe on live initialization and called `connectSSE()` directly. `EventSource` natively manages connections, errors, and retries.
  3. In `tryStaticLoad()`, removed the automatic fallback to `run_example.json` so failed loads present a clear connection/error indicator instead of mock test failures.
- **Safety Net Added:**
  1. `AuraInteractiveControllerTest.testConsoleEventsHeadRequestReturnsImmediately` asserting `mockMvc.perform(head("/api/console/events"))` returns 200 OK instantly with correct headers.
  2. `InteractiveConsoleServerTest.testInteractiveConsoleDirectSseConnectionAndGuardedStaticLoad` verifying direct SSE connection and removal of silent example JSON fallback.
