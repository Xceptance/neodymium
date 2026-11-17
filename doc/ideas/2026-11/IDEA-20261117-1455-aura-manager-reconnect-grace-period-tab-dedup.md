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

2. **Server-Side Grace Period (`BrowserAutoOpenService`):**
   - Defer automated browser launching until Spring Boot reaches `ApplicationReadyEvent`.
   - Maintain an `autoOpenRequested` flag set only when started via `AiLauncher.main()`, ensuring zero interference or browser launches during `@SpringBootTest` test executions.
   - Intercept incoming client activity via a lightweight servlet filter (`BrowserAutoOpenFilter`).
   - Schedule a 1,500 ms grace window on startup:
     - If an active client connects during the window: log reconnect detection and suppress opening a new window.
     - If no client connects within 1,500 ms: launch the default browser directly to `http://localhost:<port>`.
