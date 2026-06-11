#!/bin/bash
# AI-generated: Gemini 3.5 Flash
# Compiles the project and executes the Neodymium Aura Manager.

echo "========================================================================"
echo "  Starting Neodymium Aura Manager compilation & startup..."
echo "========================================================================"

mvn test-compile exec:java -Dexec.mainClass="com.xceptance.neodymium.aura.NeodymiumAuraManager" -Dexec.classpathScope="test"
