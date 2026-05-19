#!/usr/bin/env bash
#
# Builds Linux installers for JSignPdf using jpackage.
#
# Produces three artifacts in distribution/target/upload/:
#   jsignpdf-<version>-linux-<arch>.deb  - Debian / Ubuntu package
#   jsignpdf-<version>-linux-<arch>.rpm  - RPM package for dnf/yum/zypper
#   jsignpdf-<version>-linux-<arch>.zip  - portable zip of the jpackage app-image
#
# <arch> is auto-detected: x86_64/amd64 -> x64, aarch64/arm64 -> aarch64.
#
# Prerequisites on the build machine:
#   * Azul Zulu+FX 21 (or another JDK 21 bundle that ships the JavaFX modules)
#     on PATH and pointed at by JAVA_HOME.
#   * fakeroot (for DEB) and rpm-build (for RPM) installed.
#   * The distribution module already packaged
#     (mvn -B -DskipTests -pl distribution -am package). This produces
#     distribution/target/appassembler/lib with every runtime jar, including
#     the bootstrap and installcert jars.
#
# Usage: build-linux-installers.sh <version>
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

# jpackage --app-version only accepts MAJOR[.MINOR[.MICRO]] with numeric
# components. Strip qualifiers like -SNAPSHOT / -RC1 / .Final and keep the
# first three numeric parts.
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

echo "JSignPdf jpackage build (Linux $ARCH_LABEL)"
echo "  version    : $VERSION"
echo "  appVersion : $APP_VERSION"
echo "  root       : $ROOT"

if [[ ! -d "$APPASSEMBLY" ]]; then
  echo "Missing distribution staging at $APPASSEMBLY" >&2
  echo "Run 'mvn -B -DskipTests -pl distribution -am package' first." >&2
  exit 67
fi

# Sanity check the runner has fakeroot + rpmbuild before we start spending time.
for tool in fakeroot rpmbuild; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "Required tool '$tool' is not on PATH." >&2
    exit 68
  fi
done

rm -rf "$STAGING" "$OUT" "$GENERATED"
mkdir -p "$STAGING" "$OUT" "$GENERATED" "$UPLOAD"

# Drop any stale artifacts from previous local runs.
find "$UPLOAD" -maxdepth 1 -type f -name "jsignpdf-*-linux-${ARCH_LABEL}.*" -delete 2>/dev/null || true

# Copy runtime jars into the staging dir but skip all JavaFX jars — JFX comes
# from the bundled Zulu+FX runtime modules. Keeping any JFX jar on the
# classpath causes split-package conflicts with the boot module copy.
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

# Optional bundled docs/demo, mirroring the Windows installer build.
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

# Common JVM options shared by every launcher (mirrors the Windows script).
COMMON_JVM_OPTS_FILE="$JPKG_CFG/common-jvm-options.txt"
if [[ ! -f "$COMMON_JVM_OPTS_FILE" ]]; then
  echo "Missing shared JVM options file: $COMMON_JVM_OPTS_FILE" >&2
  exit 71
fi

COMMON_JVM_OPTS=()
while IFS= read -r line; do
  line="${line#"${line%%[![:space:]]*}"}"   # ltrim
  line="${line%"${line##*[![:space:]]}"}"   # rtrim
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

# Swing-only launcher properties.
SWING_PROPS="$GENERATED/JSignPdf-swing.properties"
{
  echo '# Generated by build-linux-installers.sh — do not edit by hand.'
  echo 'linux-shortcut=false'
  printf 'java-options=-Djsignpdf.swing=true %s\n' "${COMMON_JVM_OPTS[*]}"
} > "$SWING_PROPS"

# InstallCert launcher properties (console).
INSTALL_CERT_PROPS="$GENERATED/InstallCert.properties"
{
  echo '# Generated by build-linux-installers.sh — do not edit by hand.'
  echo 'linux-shortcut=false'
  echo "main-jar=$INSTALL_CERT_JAR"
  echo 'main-class=net.sf.jsignpdf.InstallCert'
} > "$INSTALL_CERT_PROPS"

# JSignPdfC console launcher properties (inherits java-options from the main launcher).
CONSOLE_PROPS="$GENERATED/JSignPdfC.properties"
{
  echo '# Generated by build-linux-installers.sh — do not edit by hand.'
  echo 'linux-shortcut=false'
} > "$CONSOLE_PROPS"

# 1. Build the app-image.
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
  --icon "$ICONS_DIR/logojsignpdf.png"
  --add-modules javafx.controls,javafx.fxml,javafx.swing
  --dest "$OUT"
  "${MAIN_JVM_OPTS[@]}"
  --add-launcher "JSignPdf-swing=$SWING_PROPS"
  --add-launcher "JSignPdfC=$CONSOLE_PROPS"
  --add-launcher "InstallCert=$INSTALL_CERT_PROPS"
)
if [[ "$GUIDE_FOUND" -eq 1 ]]; then
  APP_IMAGE_ARGS+=(--app-content "$GUIDE_CONTENT_DOCS")
fi
if [[ "$DEMO_FOUND" -eq 1 ]]; then
  APP_IMAGE_ARGS+=(--app-content "$DEMO_SRC")
fi
jpackage "${APP_IMAGE_ARGS[@]}"

APP_IMAGE="$OUT/JSignPdf"
if [[ ! -d "$APP_IMAGE" ]]; then
  echo "Expected app-image directory not found: $APP_IMAGE" >&2
  exit 72
fi

# 2. Zip the app-image (portable distribution).
ZIP_PATH="$UPLOAD/jsignpdf-${VERSION}-linux-${ARCH_LABEL}.zip"
echo "==> creating $ZIP_PATH"
rm -f "$ZIP_PATH"
( cd "$OUT" && zip -qr "$ZIP_PATH" "JSignPdf" )

# 3. & 4. Build the DEB and RPM installers from the app-image.
INSTALLER_ARGS=(
  --app-image "$APP_IMAGE"
  --name JSignPdf
  --app-version "$APP_VERSION"
  --vendor 'Josef Cacek'
  --copyright 'Josef Cacek'
  --description 'JSignPdf adds digital signatures to PDF documents'
  --license-file "$ROOT/distribution/licenses/MPL-2.0.txt"
  --about-url 'https://jsignpdf.eu/'
  --linux-package-name jsignpdf
  --linux-app-category 'office;Office;'
  --linux-menu-group 'Office;'
  --linux-shortcut
  --dest "$OUT"
)

echo "==> jpackage --type deb"
jpackage --type deb "${INSTALLER_ARGS[@]}"

echo "==> jpackage --type rpm"
jpackage --type rpm "${INSTALLER_ARGS[@]}"

# jpackage names DEB / RPM files with its own convention; rename to match the
# release naming scheme. The exact filename pattern depends on the OS and
# jpackage version, so glob.
mv "$OUT"/jsignpdf_*.deb "$UPLOAD/jsignpdf-${VERSION}-linux-${ARCH_LABEL}.deb"
mv "$OUT"/jsignpdf-*.rpm "$UPLOAD/jsignpdf-${VERSION}-linux-${ARCH_LABEL}.rpm"

echo
echo "Done. Artifacts:"
ls -1 "$UPLOAD" | grep -E "linux-${ARCH_LABEL}\.(deb|rpm|zip)\$" | sed 's/^/  /'
