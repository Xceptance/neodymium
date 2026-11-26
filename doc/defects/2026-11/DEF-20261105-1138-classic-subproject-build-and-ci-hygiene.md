# [DEF-20261105-1138] Neodymium-Classic Subproject Java 21 Bytecode Override and CI Headless Deficiencies

- **Status:** `Resolved`
- **Opened:** 2026-11-05 11:38
- **Closed:** 2026-11-05 11:46
- **Component:** `neodymium-classic`
- **Scope:** `Infra/Build & Config/Environment`
- **Symptom:** `neodymium-classic` compiled into Java 21 bytecode (class major version 65) instead of Java 17, breaking runtime compatibility for Java 17 consumer projects with `UnsupportedClassVersionError`. Additionally, browser tests executed on headless CI runners failed or hung due to missing `selenide.headless=true` property injection, and utility tests were omitted from default Maven test execution.
- **Root Cause:** 
  1. `neodymium-parent` defined `<maven.compiler.release>21</maven.compiler.release>`. Because `neodymium-classic/pom.xml` only specified `source`/`target` 17, the Maven compiler plugin inherited `--release 21`.
  2. `neodymium-classic/config/browser.properties` commented out `browserprofile.global.headless`, and `neodymium-classic/pom.xml` did not supply `<selenide.headless>true</selenide.headless>` in `maven-surefire-plugin`'s `systemPropertyVariables`.
  3. Surefire `<includes>` only pointed to `junit5/tests` and `common` (which contained 0 test classes), omitting all `com.xceptance.neodymium.util` tests.
- **Detection Gap ("What did we miss?"):** The multi-module build compiled successfully on a Java 21 JDK without checking classfile major versions (`javap -v`) or verifying default Surefire test execution properties.
- **Resolution:**
  1. Added `<maven.compiler.release>17</maven.compiler.release>` to `neodymium-classic/pom.xml` and configured `maven-compiler-plugin` with `<release>${maven.compiler.release}</release>`.
  2. Updated `maven-javadoc-plugin` `<source>` to `17`.
  3. Added `<selenide.headless>true</selenide.headless>` to `maven-surefire-plugin` in `neodymium-classic/pom.xml`.
  4. Updated Surefire `<includes>` in `neodymium-classic/pom.xml` to include `com/xceptance/neodymium/util/**/*Test.java`.
- **Safety Net Added:** Bytecode version verified via `javap -v` asserting `major version: 61` (Java 17).
