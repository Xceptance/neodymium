# [IDEA-20261117-1455] Aura Manager Reconnect Grace Period Tab Deduplication

- **Status:** `Implemented`
- **Proposed:** 2026-11-17 14:55
- **Resolved:** 2026-11-17
- **Component:** `aura-manager`
- **Category:** `Tooling & DX`
- **Author:** Olha Omelianchuk / AI Assistant

## Context & Motivation
Starting the Neodymium AI / Aura Manager server via `./run-ai.sh` previously launched `AiLauncher.openBrowser()` immediately on second 0 using an ephemeral splash HTTP server. 
When developers restart the server during iterative testing, their existing browser tab is already open and running the `dashboard-runner.js` offline heartbeat loop. The premature browser launch forced the operating system to open a new tab on every single server restart, cluttering the developer's desktop with duplicate tabs while the existing tab reconnected independently.

## Proposed Architecture
1. **Accelerated Offline Reconnection Polling (`dashboard-runner.js`):**
   - Reduce the reconnect polling delay from 2,000 ms to 500 ms when the server is offline or unreachable (`isServerOffline || serverFailureCount > 0`).
   - Ensures an existing tab checks in within 500 ms after port 8080 opens.

2. **Pre-Boot Socket Probe & Instant Splash Screen (`AiLauncher`):**
   - At launcher startup before Spring Boot boot-up, execute a 400 ms loopback `ServerSocket` probe on the configured target server port.
   - If an open dashboard tab is polling, it connects to the probe within ~100 ms. The probe responds with `503 Service Unavailable`, records client presence, and `AiLauncher` suppresses browser/splash launch, allowing the existing tab to seamlessly reconnect when Spring Boot finishes booting.
   - If no client connects within 400 ms (cold start), `AiLauncher` starts the ephemeral splash server and immediately launches `loading.html` in the user's browser, providing instant visual feedback (spinner) while Spring Boot initializes in the background.

3. **Fallback Reconnection Grace Period (`BrowserAutoOpenService`):**
   - Retain `BrowserAutoOpenService` and `BrowserAutoOpenFilter` as secondary runtime safety nets for programmatic/headless invocations.
