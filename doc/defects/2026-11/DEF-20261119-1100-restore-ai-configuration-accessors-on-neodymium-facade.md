# [DEF-20261119-1100] Missing reloadAiConfiguration and aiConfiguration Accessors on Neodymium Facade

- **Status:** `Resolved`
- **Opened:** 2026-11-19 11:00
- **Closed:** 2026-11-19 11:10
- **Component:** `neodymium-core`
- **Scope:** `Framework`
- **Symptom:** Downstream test projects (`neodymium-ai-testcase-hub`) failed compilation with `cannot find symbol: method reloadAiConfiguration()` on `com.xceptance.neodymium.util.Neodymium` in `AbstractLogodataTest.java` and `AbstractStokkeTest.java`.
- **Root Cause:** When consolidating AI capabilities back into `neodymium-core` under the GNU AGPLv3 + Commercial dual license, the public static accessors `reloadAiConfiguration()` and `aiConfiguration()` (along with the internal member field `aiConfiguration`) that were stripped during the earlier experimental module split (commit `08fe821fb`) were not restored to `org.neodymium.util.Neodymium`. Because `com.xceptance.neodymium.util.Neodymium` extends `org.neodymium.util.Neodymium`, both facades lacked these accessors.
- **Detection Gap ("What did we miss?"):** Neodymium repo's internal unit tests tested `AiConfiguration.getInstance()` directly or used `Neodymium.reloadConfiguration()`, but did not assert the presence and behavior of `Neodymium.reloadAiConfiguration()` and `Neodymium.aiConfiguration()` on the facade class.
- **Resolution:** Restored `aiConfiguration` member field, `Neodymium.aiConfiguration()`, and `Neodymium.reloadAiConfiguration()` in `org.neodymium.util.Neodymium`. Re-installed `neodymium-core` 6.0.0-BETA to local repository and verified clean compilation of `neodymium-ai-testcase-hub`.
- **Safety Net Added:** Added permanent automated unit tests in `org.neodymium.util.NeodymiumAiConfigurationApiTest` verifying `Neodymium.aiConfiguration()`, `Neodymium.reloadAiConfiguration()`, and inheritance through `com.xceptance.neodymium.util.Neodymium`.
