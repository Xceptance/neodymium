@echo off
rem AI-generated: Gemini 3.8 Flash
rem Xceptance GmbH 2026
rem Compiles the project and executes the Neodymium AI Manager (dual-mode).

echo ========================================================================
echo   Starting Neodymium AI Manager...
echo ========================================================================

if exist aura-manager (
    call mvn spring-boot:run -pl aura-manager -am %*
) else (
    call mvn test-compile exec:java -Dexec.mainClass="org.neodymium.manager.NeodymiumManagerLauncher" -Dexec.classpathScope="test" %*
)
