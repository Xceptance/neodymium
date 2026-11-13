#!/bin/bash
# AI-generated: Gemini 3.8 Flash
# Xceptance GmbH 2026
# Compiles the project and executes the Neodymium AI Manager (dual-mode).

echo "========================================================================"
echo "  Starting Neodymium AI Manager..."
echo "========================================================================"

if [ -d "aura-manager" ]; then
    # Running inside Neodymium multi-module repository
    mvn -U spring-boot:run -pl aura-manager -am
else
    # Running inside external test project with AI manager dependency
    mvn -U test-compile exec:java -Dexec.mainClass="com.xceptance.aura.AiLauncher" -Dexec.classpathScope="test"
fi
