#!/bin/bash
# Compiles the project and executes Neodymium AI Manager.

echo "========================================================================"
echo "  Starting Neodymium AI Manager..."
echo "========================================================================"

set -e

mvn -U test-compile exec:java \
  -Dexec.mainClass="org.neodymium.manager.NeodymiumManagerLauncher" \
  -Dexec.classpathScope="test"
