@echo off
rem AI-generated: Gemini 3.5 Flash
rem Compiles the project and executes the Neodymium Aura Manager.

echo ========================================================================
echo   Starting Neodymium Aura Manager compilation and startup...
echo ========================================================================

mvn test-compile exec:java -Dexec.mainClass="com.xceptance.neodymium.aura.NeodymiumAuraManager" -Dexec.classpathScope="test"
