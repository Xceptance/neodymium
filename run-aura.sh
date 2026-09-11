#!/bin/bash
# AI-generated: Gemini 3.8 Flash
# Xceptance GmbH 2026
# Compiles the project and executes the Neodymium Aura Manager (dual-mode).

echo "========================================================================"
echo "  Starting Neodymium Aura Manager..."
echo "========================================================================"

if [ -d "aura-manager" ]; then
    # Running inside Neodymium multi-module repository
    mvn -U spring-boot:run -pl aura-manager -am
else
    # Running inside external test project with aura-manager dependency
    mvn -U test-compile exec:java -Dexec.mainClass="com.xceptance.aura.AuraManagerApplication" -Dexec.classpathScope="test"
fi
