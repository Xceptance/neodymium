# [DEF-20261016-1902] AuraLauncher Address In Use Port Collision on 8081 with Spring Boot DevTools

- **Status:** `Resolved`
- **Opened:** 2026-10-16 19:02
- **Closed:** 2026-10-16 19:05
- **Component:** `aura-manager`
- **Scope:** `Framework & Infra/Build`
- **Symptom:** Running `./run-aura.sh` fails with `java.net.BindException: Address already in use` at `com.xceptance.aura.AuraLauncher.main(AuraLauncher.java:20)` even though port 8080 is completely free.
- **Root Cause:** `AuraLauncher` hardcoded port 8081 for its lightweight splash HTTP server and lacked re-entrancy protection. When executed with `spring-boot-devtools` on the runtime classpath, DevTools spawned the `restartedMain` thread to execute the application under its `RestartClassLoader` and re-invoked `AuraLauncher.main(args)`. This caused `restartedMain` to attempt binding port 8081 while the initial `main` thread was still holding it. Additionally, hardcoding port 8081 created vulnerability against any unrelated process occupying 8081.
- **Detection Gap ("What did we miss?"):** `AuraLauncher` was executed without unit tests asserting dynamic port selection and DevTools restart thread bypass behavior.
- **Resolution:** Refactored `AuraLauncher` to bind to dynamic ephemeral loopback port `0` (`new InetSocketAddress(InetAddress.getLoopbackAddress(), 0)`), dynamically resolved the allocated port via `server.getAddress().getPort()`, and added re-entrancy protection that detects `restartedMain` or an active splash flag and delegates directly to `AuraManagerApplication.main(args)`.
- **Safety Net Added:** Added `AuraLauncherTest` verifying dynamic ephemeral port allocation, loading resource delivery, and DevTools bypass guard recognition.
