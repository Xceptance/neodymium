@echo off
rem AI-generated: Gemini 3.8 Flash
rem Xceptance GmbH 2026
rem Compiles the project and executes the Neodymium AI Manager (dual-mode).

echo ========================================================================
echo   Starting Neodymium AI Manager...
echo ========================================================================

if exist aura-manager (
    mvn -U spring-boot:run -pl aura-manager -am
) else (
    mvn -U test-compile exec:java -Dexec.mainClass="com.xceptance.aura.AiLauncher" -Dexec.classpathScope="test"
)
