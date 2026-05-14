#!/usr/bin/env bash
set -euo pipefail

# Renders icon-N.png at multiple sizes from logo-mark.svg using Inkscape.
# Run from the repo root:
#     bash plsql-toolkit-app/build-tools/render-icons.sh

SRC="plsql-toolkit-app/src/main/resources/branding/logo-mark.svg"
DST_DIR="plsql-toolkit-app/src/main/resources/branding"

if ! command -v inkscape >/dev/null 2>&1; then
  echo "error: inkscape not found on PATH" >&2
  exit 1
fi

for size in 16 32 64 128 256 512; do
  out="$DST_DIR/icon-${size}.png"
  inkscape --export-type=png \
           --export-width="$size" \
           --export-height="$size" \
           --export-filename="$out" \
           "$SRC"
  echo "wrote $out (${size}x${size})"
done
