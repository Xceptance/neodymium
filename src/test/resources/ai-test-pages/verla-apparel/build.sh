#!/usr/bin/env bash
#
# Regenerates ../shared/verla-apparel.css from tailwind.src.css.
#
# ============================ DEVELOPMENT ONLY ============================
# Nothing in the test or runtime path needs Node.js. The generated stylesheet
# is committed to the repository and EmbeddedHtmlServer serves it as a plain
# static file. Run this script only after changing the markup or the design
# tokens, then commit the regenerated CSS.
# ==========================================================================
#
# Usage:  ./build.sh
#
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_ROOT="$(cd "${HERE}/../../.." && pwd)"          # src/test
OUT="${HERE}/../shared/verla-apparel.css"
SCANNED_JAVA="${TEST_ROOT}/java/org/neodymium/ai/util/EmbeddedHtmlServer.java"

if ! command -v npx >/dev/null 2>&1; then
    echo "npx not found. Node.js is required to REGENERATE the stylesheet," >&2
    echo "but NOT to run the tests -- ${OUT} is committed." >&2
    exit 1
fi

# The Tailwind CLI resolves '@import "tailwindcss"' relative to the input file,
# so the build runs from a throwaway workspace that owns the dependency. The
# @source globs are rewritten to absolute paths so they still point back here.
WORK="$(mktemp -d)"
trap 'rm -rf "${WORK}"' EXIT

echo "Installing Tailwind CLI into ${WORK} ..."
( cd "${WORK}" && npm init -y >/dev/null 2>&1 && npm install --silent tailwindcss @tailwindcss/cli >/dev/null 2>&1 )

sed -e "s#@source \"\./\*\.html\";#@source \"${HERE}/*.html\";#" \
    -e "s#@source \"\.\./\.\./\.\./java/org/neodymium/ai/util/EmbeddedHtmlServer\.java\";#@source \"${SCANNED_JAVA}\";#" \
    "${HERE}/tailwind.src.css" > "${WORK}/input.css"

echo "Building Tailwind CSS -> ${OUT}"
( cd "${WORK}" && ./node_modules/.bin/tailwindcss --input input.css --output "${OUT}" --minify )

echo "Done: $(wc -c < "${OUT}") bytes"
