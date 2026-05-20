#!/usr/bin/env bash
#
# Builds macOS installers for JSignPdf using jpackage.
#
# Produces two artifacts in distribution/target/upload/:
#   jsignpdf-<version>-macos-<arch>.dmg  - disk image (UNSIGNED in 3.1 — see design doc)
#   jsignpdf-<version>-macos-<arch>.zip  - portable zip of the jpackage app-image
#
# <arch> is auto-detected: x86_64 -> x64, arm64/aarch64 -> aarch64. The
# release pipeline only invokes this on the macos-14 (Apple Silicon) runner;
# Intel macOS is no longer built (see design-doc/3.1-separate-release-steps.md).
# The arch auto-detection is kept for local Intel-mac developer builds.
#
# Prerequisites on the build machine:
#   * Azul Zulu+FX 21 (or another JDK 21 bundle that ships the JavaFX modules)
#     on PATH and pointed at by JAVA_HOME.
#   * Xcode command line tools (provides hdiutil for the DMG).
#   * The distribution module already packaged
#     (mvn -B -DskipTests -pl distribution -am package). This produces
#     distribution/target/appassembler/lib with every runtime jar, including
#     the bootstrap and installcert jars.
#
# Usage: build-macos-installers.sh <version>
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 <version>" >&2
  exit 64
fi
VERSION="$1"

SCRIPT_DIR="$(cd -P "$(dirname "$0")" >/dev/null 2>&1 && pwd -P)"
ROOT="$(cd -P "$SCRIPT_DIR/../.." >/dev/null 2>&1 && pwd -P)"
TARGET="$ROOT/distribution/target"
# Runtime jar dir. Defaults to the locally-built appassembler staging; the
# package-release workflow overrides it with JSIGNPDF_LIB_DIR pointing at the
# lib/ unpacked from the full ZIP downloaded from Maven Central.
APPASSEMBLY="${JSIGNPDF_LIB_DIR:-$TARGET/appassembler/lib}"
STAGING="$TARGET/jpackage-staging"
OUT="$TARGET/jpackage-out"
UPLOAD="$TARGET/upload"
GENERATED="$TARGET/jpackage-generated"
JPKG_CFG="$ROOT/distribution/jpackage"
ICONS_DIR="$ROOT/distribution/doc/icon"

case "$(uname -m)" in
  x86_64|amd64)   ARCH_LABEL="x64" ;;
  aarch64|arm64)  ARCH_LABEL="aarch64" ;;
  *) echo "Unsupported architecture: $(uname -m)" >&2; exit 65 ;;
esac

APP_VERSION="$(printf '%s\n' "$VERSION" | sed -E 's/[^0-9.].*//' | awk -F. '{
  out="";
  for (i=1; i<=NF && i<=3; i++) {
    if ($i ~ /^[0-9]+$/) {
      out = (out ? out "." $i : $i);
    } else {
      break;
    }
  }
  print out;
}')"
if [[ -z "$APP_VERSION" ]]; then
  echo "Cannot derive jpackage --app-version from '$VERSION'." >&2
  exit 66
fi

echo "JSignPdf jpackage build (macOS $ARCH_LABEL)"
echo "  version    : $VERSION"
echo "  appVersion : $APP_VERSION"
echo "  root       : $ROOT"

if [[ ! -d "$APPASSEMBLY" ]]; then
  echo "Missing distribution staging at $APPASSEMBLY" >&2
  echo "Run 'mvn -B -DskipTests -pl distribution -am package' first." >&2
  exit 67
fi

rm -rf "$STAGING" "$OUT" "$GENERATED"
mkdir -p "$STAGING" "$OUT" "$GENERATED" "$UPLOAD"

find "$UPLOAD" -maxdepth 1 -type f -name "jsignpdf-*-macos-${ARCH_LABEL}.*" -delete 2>/dev/null || true

# Skip every JavaFX jar — JFX is supplied by the bundled Zulu+FX runtime as
# named modules. Keeping any JFX jar on the classpath causes split-package
# conflicts with the boot module copy.
shopt -s nullglob
for jar in "$APPASSEMBLY"/*.jar; do
  base="$(basename "$jar")"
  case "$base" in
    javafx-*) continue ;;
    *) cp "$jar" "$STAGING/$base" ;;
  esac
done
shopt -u nullglob

MAIN_JAR=""
for jar in "$STAGING"/jsignpdf-*.jar; do
  base="$(basename "$jar")"
  case "$base" in
    jsignpdf-bootstrap-*|jsignpdf-jpedal*) continue ;;
    *) MAIN_JAR="$base"; break ;;
  esac
done
if [[ -z "$MAIN_JAR" ]]; then
  echo "Could not locate the jsignpdf main jar inside $STAGING." >&2
  exit 69
fi

INSTALL_CERT_JAR=""
for jar in "$STAGING"/installcert-*.jar; do
  INSTALL_CERT_JAR="$(basename "$jar")"; break
done
if [[ -z "$INSTALL_CERT_JAR" ]]; then
  echo "Could not locate installcert-*.jar inside $STAGING." >&2
  exit 70
fi

echo "  main jar   : $MAIN_JAR"
echo "  cert jar   : $INSTALL_CERT_JAR"

GUIDE_SRC="$TARGET/pdf-guide"
GUIDE_CONTENT_ROOT="$TARGET/jpackage-app-content"
GUIDE_CONTENT_DOCS="$GUIDE_CONTENT_ROOT/docs"
rm -rf "$GUIDE_CONTENT_ROOT"
GUIDE_FOUND=0
if [[ -d "$GUIDE_SRC" ]]; then
  guide_pdf="$(find "$GUIDE_SRC" -maxdepth 1 -type f -name '*.pdf' -print -quit 2>/dev/null || true)"
  if [[ -n "$guide_pdf" ]]; then
    mkdir -p "$GUIDE_CONTENT_DOCS"
    cp "$guide_pdf" "$GUIDE_CONTENT_DOCS/JSignPdf.pdf"
    GUIDE_FOUND=1
    echo "  guide      : $(basename "$guide_pdf") -> app/docs/JSignPdf.pdf"
  fi
fi
if [[ "$GUIDE_FOUND" -eq 0 ]]; then
  echo "  guide      : (not present — no PDF guide will be bundled)"
fi

DEMO_SRC="$ROOT/distribution/demo"
DEMO_FOUND=0
if [[ -d "$DEMO_SRC" ]]; then
  DEMO_FOUND=1
  echo "  demo       : $DEMO_SRC -> app/demo/"
fi

COMMON_JVM_OPTS_FILE="$JPKG_CFG/common-jvm-options.txt"
if [[ ! -f "$COMMON_JVM_OPTS_FILE" ]]; then
  echo "Missing shared JVM options file: $COMMON_JVM_OPTS_FILE" >&2
  exit 71
fi
COMMON_JVM_OPTS=()
while IFS= read -r line; do
  line="${line#"${line%%[![:space:]]*}"}"
  line="${line%"${line##*[![:space:]]}"}"
  [[ -z "$line" ]] && continue
  [[ "${line:0:1}" == "#" ]] && continue
  COMMON_JVM_OPTS+=("$line")
done < "$COMMON_JVM_OPTS_FILE"

JFX_ADD_MODULES='--add-modules=javafx.controls,javafx.fxml,javafx.swing'
has_jfx=0
for opt in "${COMMON_JVM_OPTS[@]}"; do
  [[ "$opt" == "$JFX_ADD_MODULES" ]] && has_jfx=1
done
if [[ "$has_jfx" -eq 0 ]]; then
  COMMON_JVM_OPTS=("$JFX_ADD_MODULES" "${COMMON_JVM_OPTS[@]}")
fi

MAIN_JVM_OPTS=()
for opt in "${COMMON_JVM_OPTS[@]}"; do
  MAIN_JVM_OPTS+=(--java-options "$opt")
done

# Swing-only launcher properties. macOS launchers can read/write the same
# add-launcher .properties shape as Linux; mac-shortcut is a no-op on console
# launchers and an extra .app shortcut on GUI ones.
SWING_PROPS="$GENERATED/JSignPdf-swing.properties"
{
  echo '# Generated by build-macos-installers.sh — do not edit by hand.'
  printf 'java-options=-Djsignpdf.swing=true %s\n' "${COMMON_JVM_OPTS[*]}"
} > "$SWING_PROPS"

INSTALL_CERT_PROPS="$GENERATED/InstallCert.properties"
{
  echo '# Generated by build-macos-installers.sh — do not edit by hand.'
  echo "main-jar=$INSTALL_CERT_JAR"
  echo 'main-class=net.sf.jsignpdf.InstallCert'
} > "$INSTALL_CERT_PROPS"

CONSOLE_PROPS="$GENERATED/JSignPdfC.properties"
{
  echo '# Generated by build-macos-installers.sh — do not edit by hand.'
} > "$CONSOLE_PROPS"

echo "==> jpackage --type app-image"
APP_IMAGE_ARGS=(
  --type app-image
  --input "$STAGING"
  --main-jar "$MAIN_JAR"
  --main-class net.sf.jsignpdf.Signer
  --name JSignPdf
  --app-version "$APP_VERSION"
  --vendor 'Josef Cacek'
  --copyright 'Josef Cacek'
  --description 'JSignPdf adds digital signatures to PDF documents'
  --add-modules javafx.controls,javafx.fxml,javafx.swing
  --dest "$OUT"
  "${MAIN_JVM_OPTS[@]}"
  --add-launcher "JSignPdf-swing=$SWING_PROPS"
  --add-launcher "JSignPdfC=$CONSOLE_PROPS"
  --add-launcher "InstallCert=$INSTALL_CERT_PROPS"
)
# jpackage on macOS needs an .icns. Skip --icon if we don't have one; the
# default jpackage icon is fine for an unsigned dev build.
if [[ -f "$ICONS_DIR/icons.icns" ]]; then
  APP_IMAGE_ARGS+=(--icon "$ICONS_DIR/icons.icns")
fi
if [[ "$GUIDE_FOUND" -eq 1 ]]; then
  APP_IMAGE_ARGS+=(--app-content "$GUIDE_CONTENT_DOCS")
fi
if [[ "$DEMO_FOUND" -eq 1 ]]; then
  APP_IMAGE_ARGS+=(--app-content "$DEMO_SRC")
fi
jpackage "${APP_IMAGE_ARGS[@]}"

APP_IMAGE="$OUT/JSignPdf.app"
if [[ ! -d "$APP_IMAGE" ]]; then
  echo "Expected app-image directory not found: $APP_IMAGE" >&2
  exit 72
fi

ZIP_PATH="$UPLOAD/jsignpdf-${VERSION}-macos-${ARCH_LABEL}.zip"
echo "==> creating $ZIP_PATH"
rm -f "$ZIP_PATH"
( cd "$OUT" && zip -qry "$ZIP_PATH" "JSignPdf.app" )

echo "==> jpackage --type dmg"
DMG_ARGS=(
  --app-image "$APP_IMAGE"
  --name JSignPdf
  --app-version "$APP_VERSION"
  --vendor 'Josef Cacek'
  --copyright 'Josef Cacek'
  --description 'JSignPdf adds digital signatures to PDF documents'
  --license-file "$ROOT/distribution/licenses/MPL-2.0.txt"
  --about-url 'https://jsignpdf.eu/'
  --dest "$OUT"
)
jpackage --type dmg "${DMG_ARGS[@]}"

mv "$OUT"/JSignPdf-*.dmg "$UPLOAD/jsignpdf-${VERSION}-macos-${ARCH_LABEL}.dmg"

echo
echo "Done. Artifacts (NOT codesigned — Gatekeeper warnings expected; see design doc 3.1):"
ls -1 "$UPLOAD" | grep -E "macos-${ARCH_LABEL}\.(dmg|zip)\$" | sed 's/^/  /'
