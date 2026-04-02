#!/bin/bash
# Generates maven-dependencies.json for Flatpak offline builds.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Must match runtime-version in io.github.intoolswetrust.JSignPdf.Devel.yaml
SDK_REF="${FLATPAK_SDK_REF:-org.freedesktop.Sdk//25.08}"
SDK_EXT="${FLATPAK_SDK_EXT:-openjdk21}"

REPO_URL="${REPO_URL:-https://github.com/intoolswetrust/jsignpdf.git}"
BRANCH="${BRANCH:-master}"

# Verify flatpak is installed
if ! command -v flatpak >/dev/null 2>&1; then
  echo "Error: flatpak command not found." >&2
  exit 1
fi

# Verify SDK is installed
if ! flatpak info "$SDK_REF" &>/dev/null; then
  echo "Error: ${SDK_REF} is not installed" >&2
  echo "  flatpak install flathub ${SDK_REF}" >&2
  exit 1
fi

# Verify OpenJDK extension is installed
EXT_REF="org.freedesktop.Sdk.Extension.${SDK_EXT}//${SDK_REF##*/}"
if ! flatpak info "$EXT_REF" &>/dev/null; then
  echo "Error: extension ${EXT_REF} is not installed" >&2
  echo "  flatpak install flathub ${EXT_REF}" >&2
  exit 1
fi

echo "==> Generating dependencies in Flatpak SDK (${SDK_REF} + ${SDK_EXT})"

# Run the dependency generation inside the Flatpak SDK
# All the work is done in a single command pipeline
cat << SCRIPT_EOF | env FLATPAK_ENABLE_SDK_EXT="${SDK_EXT}" SCRIPT_DIR="${SCRIPT_DIR}" flatpak run \
  --share=network \
  --filesystem="${SCRIPT_DIR}:rw" \
  --env=SCRIPT_DIR="${SCRIPT_DIR}" \
  --devel \
  "$SDK_REF"

set -euo pipefail

# Enable OpenJDK
source /usr/lib/sdk/openjdk21/enable.sh

# Set up directories (use the mounted SCRIPT_DIR)
WORK_DIR="${SCRIPT_DIR}"
BUILD_DIR="\${WORK_DIR}/jsignpdf-build"
M2_DIR="\${WORK_DIR}/m2deps-flatpak"
OUT_JSON="\${WORK_DIR}/maven-dependencies.json"

echo "==> Cloning jsignpdf repository"
rm -rf "\$BUILD_DIR"
git clone --depth 1 --branch "${BRANCH}" "${REPO_URL}" "\$BUILD_DIR"

echo "==> Running Maven to download dependencies"
cd "\$BUILD_DIR"
rm -rf "\$M2_DIR"
mkdir -p "\$M2_DIR"
mvn clean install \
  -Dmaven.repo.local="\$M2_DIR" \
  -DskipTests \
  -Dmaven.javadoc.skip=true \
  -Dmaven.source.skip=true \
  -Dasciidoctor.skip=true

echo "==> Generating maven-dependencies.json"
cd "\$WORK_DIR"

# Create temp file for JSON entries
TEMP_JSON="/tmp/maven-deps-\$\$.json"

# Find all .jar, .pom, .aar files and generate JSON entries
find "\$M2_DIR" -type f \( -name "*.jar" -o -name "*.pom" -o -name "*.aar" \) | sort | while read -r file; do
  # Get relative path from M2_DIR
  rel_path="\${file#\$M2_DIR/}"

  # Convert path to Maven Central URL
  url="https://repo1.maven.org/maven2/\${rel_path}"

  # Compute SHA256
  sha256=\$(sha256sum "\$file" | awk '{print \$1}')

  # Get destination directory (parent directory in m2local)
  dest_dir="m2local/\$(dirname "\$rel_path")"

  # Get filename
  filename=\$(basename "\$file")

  # Write compact JSON entry on single line for easier comma handling
  echo "{\"type\":\"file\",\"url\":\"\$url\",\"sha256\":\"\$sha256\",\"dest\":\"\$dest_dir\",\"dest-filename\":\"\$filename\"}"
done > "\$TEMP_JSON"

# Assemble final JSON with proper formatting and comma separators
echo "[" > "\$OUT_JSON"
# Add commas between entries and format with jq if available, else use sed
if command -v jq >/dev/null 2>&1; then
  jq -s '.' "\$TEMP_JSON" | tail -n +2 | head -n -1 >> "\$OUT_JSON"
else
  sed '1s/^/  /; 1!s/^/,  /' "\$TEMP_JSON" >> "\$OUT_JSON"
fi
echo "]" >> "\$OUT_JSON"

# Clean up temp file
rm -f "\$TEMP_JSON"

echo "==> Generated \$(grep -c '"type": "file"' "\$OUT_JSON" || echo 0) dependency entries"

echo "==> Cleaning up"
rm -rf "\$BUILD_DIR" "\$M2_DIR"

SCRIPT_EOF

echo ""
echo "Done. Use maven-dependencies.json in your Flatpak manifest."
