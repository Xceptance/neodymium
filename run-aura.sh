#!/bin/bash
# AI-generated: Gemini 3.8 Flash
# Xceptance GmbH 2026
# Deprecated wrapper for run-ai.sh

echo "[DEPRECATED] run-aura.sh has been renamed to run-ai.sh. Forwarding..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "${SCRIPT_DIR}/run-ai.sh" "$@"
