#!/bin/bash
# Produces maven-dependencies.json (+ optionally maven-dependencies.tar.gz) for the from-git manifest.
#
# Default: Maven runs inside org.freedesktop.Sdk + openjdk21 extension
# (same toolchain as flatpak-builder) — independent of Toolbx / host Fedora Maven.
#
# First-time setup:
#   flatpak install flathub org.freedesktop.Sdk//25.08 \
#     org.freedesktop.Sdk.Extension.openjdk21//25.08
#
# Optional: USE_HOST_MVN=1 — use system mvn (debugging / CI).

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INNER="${SCRIPT_DIR}/prepare-maven-deps-inner.sh"

# Must match runtime-version in io.github.intoolswetrust.JSignPdf.from-git.yaml
SDK_REF="${FLATPAK_SDK_REF:-org.freedesktop.Sdk//25.08}"
SDK_EXT="${FLATPAK_SDK_EXT:-openjdk21}"

REPO_URL="${REPO_URL:-https://github.com/intoolswetrust/jsignpdf.git}"
BRANCH="${BRANCH:-master}"
export PREPARE_ROOT="${SCRIPT_DIR}"
export REPO_URL
export BRANCH

if [ ! -f "$INNER" ]; then
  echo "Missing file: $INNER" >&2
  exit 1
fi

if [ "${USE_HOST_MVN:-0}" = 1 ]; then
  echo "==> USE_HOST_MVN=1 (system Maven)"
  if ! command -v mvn >/dev/null 2>&1; then
    echo "Error: mvn not on PATH." >&2
    exit 1
  fi
  exec bash "$INNER"
fi

if ! command -v flatpak >/dev/null 2>&1; then
  echo "Error: flatpak command not found." >&2
  exit 1
fi

if ! flatpak info "$SDK_REF" &>/dev/null; then
  echo "Error: ${SDK_REF} is not installed" >&2
  echo "  flatpak install flathub ${SDK_REF}" >&2
  exit 1
fi

EXT_REF="org.freedesktop.Sdk.Extension.${SDK_EXT}//${SDK_REF##*/}"
if ! flatpak info "$EXT_REF" &>/dev/null; then
  echo "Error: extension ${EXT_REF} is not installed" >&2
  echo "  flatpak install flathub ${EXT_REF}" >&2
  exit 1
fi

echo "==> Running prepare inside Flatpak Sdk (${SDK_REF} + ${SDK_EXT})"
# Sdk default command is bash — do not prefix a second `bash` or you get "bash bash script"
# and the second token is treated as a file to read → /usr/bin/bash errors.
exec env FLATPAK_ENABLE_SDK_EXT="${SDK_EXT}" flatpak run \
  --share=network \
  --filesystem="${SCRIPT_DIR}:rw" \
  --env=PREPARE_ROOT="${PREPARE_ROOT}" \
  --env=REPO_URL="${REPO_URL}" \
  --env=BRANCH="${BRANCH}" \
  "$SDK_REF" \
  "$INNER"
