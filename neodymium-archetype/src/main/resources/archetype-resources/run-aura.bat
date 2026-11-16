@echo off
rem Compiles the project and executes Neodymium Aura Manager.

echo ========================================================================
echo   Starting Neodymium Aura Manager...
echo ========================================================================

call mvn -U test-compile exec:java -Dexec.mainClass="com.xceptance.aura.AuraLauncher" -Dexec.classpathScope="test"
