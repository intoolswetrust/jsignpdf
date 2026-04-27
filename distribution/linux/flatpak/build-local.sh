#!/usr/bin/env bash
# Build a JSignPdf Flatpak bundle locally for testing.
#
# Mirrors what .github/workflows/do-release.yml (linux-flatpak job) and
# .github/workflows/refresh-flatpak-deps.yml do, but on the host using
# native flatpak / flatpak-builder. Produces a .flatpak bundle.
#
# Caches that survive across runs:
#   ~/.local/share/flatpak/                 SDK + runtime + openjdk21 extension
#   distribution/linux/flatpak/build/state  flatpak-builder downloads + ccache
#   distribution/linux/flatpak/build/repo   OSTree repo accumulating builds
#   ~/.m2/repository                        Maven (release mode, host build)
#
# Usage: ./build-local.sh [--release|--devel] [--skip-maven] [--keep-build]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

MODE="release"
SKIP_MVN=0
KEEP_BUILD=0

usage() {
  cat <<EOF
Usage: $(basename "$0") [--release|--devel] [--skip-maven] [--keep-build] [-h]

  --release     (default) Build the release manifest. Runs 'mvn install' on
                the host, then flatpak-builder repackages the local zip.
                Fastest iteration.

  --devel       Build the Devel manifest from the working tree using the
                offline maven-dependencies.json. Slower (Maven runs inside
                the SDK) but exercises the offline manifest end-to-end.

  --skip-maven  Reuse an existing distribution/target/jsignpdf-<v>.zip
                (release mode only). The script falls back to running mvn
                if no matching zip is found.

  --keep-build  Keep the flatpak-builder build-dir for inspection.

Output bundle: distribution/linux/flatpak/build/JSignPdf*-<version>-linux-x86_64.flatpak
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --release) MODE="release"; shift ;;
    --devel)   MODE="devel"; shift ;;
    --skip-maven) SKIP_MVN=1; shift ;;
    --keep-build) KEEP_BUILD=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage; exit 2 ;;
  esac
done

require() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "error: '$1' not found." >&2
    echo "       Install with your package manager, e.g.:" >&2
    echo "         sudo apt install flatpak flatpak-builder    # Debian/Ubuntu" >&2
    echo "         sudo dnf install flatpak flatpak-builder    # Fedora" >&2
    echo "         sudo pacman -S flatpak flatpak-builder      # Arch" >&2
    exit 1
  }
}
require flatpak
require flatpak-builder

# Pull the runtime version from the release manifest so this stays in sync
# with whatever is committed.
RELEASE_MANIFEST="$SCRIPT_DIR/io.github.intoolswetrust.JSignPdf.yaml"
DEVEL_MANIFEST="$SCRIPT_DIR/io.github.intoolswetrust.JSignPdf.Devel.yaml"
RUNTIME_VER=$(awk -F"'" '/^runtime-version:/ {print $2; exit}' "$RELEASE_MANIFEST")
[[ -n "$RUNTIME_VER" ]] || { echo "could not parse runtime-version from $RELEASE_MANIFEST" >&2; exit 1; }

# Project version from the root pom (first <version> outside any <parent>).
VERSION=$(awk '
  /<\/parent>/ { in_parent=0; next }
  /<parent>/   { in_parent=1 }
  !in_parent && /<version>/ {
    sub(/.*<version>/, ""); sub(/<\/version>.*/, "")
    print; exit
  }
' "$REPO_ROOT/pom.xml")
[[ -n "$VERSION" ]] || { echo "could not parse project version from pom.xml" >&2; exit 1; }

BUILD_DIR="$SCRIPT_DIR/build"
STATE_DIR="$BUILD_DIR/state"
REPO_DIR="$BUILD_DIR/repo"
WORK_DIR="$BUILD_DIR/work"
STAGE_DIR="$BUILD_DIR/stage"
mkdir -p "$STATE_DIR" "$REPO_DIR" "$STAGE_DIR"
rm -rf "$STAGE_DIR"/*

echo "==> Ensuring Flathub remote and SDK ${RUNTIME_VER} are installed (--user)"
flatpak remote-add --user --if-not-exists flathub \
  https://flathub.org/repo/flathub.flatpakrepo
flatpak install --user -y --noninteractive flathub \
  "org.freedesktop.Platform//${RUNTIME_VER}" \
  "org.freedesktop.Sdk//${RUNTIME_VER}" \
  "org.freedesktop.Sdk.Extension.openjdk21//${RUNTIME_VER}"

DESKTOP_ABS="$REPO_ROOT/distribution/linux/jsignpdf.desktop"
METAINFO_ABS="$REPO_ROOT/distribution/linux/io.github.intoolswetrust.JSignPdf.metainfo.xml"

case "$MODE" in
  release)
    APP_ID="io.github.intoolswetrust.JSignPdf"
    ZIP_NAME="jsignpdf-${VERSION}.zip"
    ZIP_PATH="$REPO_ROOT/distribution/target/$ZIP_NAME"

    if (( SKIP_MVN == 0 )) || [[ ! -f "$ZIP_PATH" ]]; then
      echo "==> Building host zip via mvn (skip with --skip-maven once it exists)"
      ( cd "$REPO_ROOT" && mvn -B -DskipTests clean install )
    else
      echo "==> Reusing $ZIP_PATH"
    fi
    [[ -f "$ZIP_PATH" ]] || { echo "missing zip: $ZIP_PATH" >&2; exit 1; }

    SHA256=$(sha256sum "$ZIP_PATH" | awk '{print $1}')
    cp "$ZIP_PATH"                          "$STAGE_DIR/"
    cp "$SCRIPT_DIR/jsignpdf-flatpak.in"    "$STAGE_DIR/"
    cp "$SCRIPT_DIR/jsignpdf.png"           "$STAGE_DIR/"

    STAGED="$STAGE_DIR/${APP_ID}.local.yaml"
    sed \
      -e "s|        url: https://downloads\\.sourceforge\\.net.*\\.zip|        path: ${ZIP_NAME}|" \
      -e "s|sha256: [a-f0-9]\\{64\\}|sha256: ${SHA256}|" \
      -e "s|path: \\.\\./jsignpdf\\.desktop|path: ${DESKTOP_ABS}|" \
      -e "s|path: \\.\\./io\\.github\\.intoolswetrust\\.JSignPdf\\.metainfo\\.xml|path: ${METAINFO_ABS}|" \
      "$RELEASE_MANIFEST" > "$STAGED"
    grep -q "path: ${ZIP_NAME}" "$STAGED"  || { echo "patch failed (zip path)" >&2; exit 1; }
    grep -q "sha256: ${SHA256}" "$STAGED"  || { echo "patch failed (sha256)" >&2; exit 1; }
    BUNDLE="$BUILD_DIR/JSignPdf-${VERSION}-linux-x86_64.flatpak"
    ;;

  devel)
    APP_ID="io.github.intoolswetrust.JSignPdf.Devel"
    cp "$SCRIPT_DIR/maven-dependencies.json" "$STAGE_DIR/"
    cp "$SCRIPT_DIR/jsignpdf-flatpak.in"     "$STAGE_DIR/"
    cp "$SCRIPT_DIR/jsignpdf.png"            "$STAGE_DIR/"

    STAGED="$STAGE_DIR/${APP_ID}.local.yaml"
    # Replace the 3-line `type: git` block with a 2-line `type: dir` pointing at
    # the local working tree, and rewrite the parent-relative file paths to
    # absolute paths so the manifest is location-independent.
    awk -v root="$REPO_ROOT" -v desktop="$DESKTOP_ABS" -v metainfo="$METAINFO_ABS" '
      /^      - type: git$/ {
        print "      - type: dir"
        print "        path: " root
        skip = 2
        next
      }
      skip > 0 { skip--; next }
      {
        sub(/path: \.\.\/jsignpdf\.desktop/, "path: " desktop)
        sub(/path: \.\.\/io\.github\.intoolswetrust\.JSignPdf\.metainfo\.xml/, "path: " metainfo)
        print
      }
    ' "$DEVEL_MANIFEST" > "$STAGED"
    grep -q "type: dir" "$STAGED" || { echo "patch failed (dir source)" >&2; exit 1; }
    BUNDLE="$BUILD_DIR/JSignPdf-Devel-${VERSION}-linux-x86_64.flatpak"
    ;;
esac

echo "==> Running flatpak-builder ($MODE)"
( cd "$STAGE_DIR" && flatpak-builder \
    --user \
    --state-dir="$STATE_DIR" \
    --repo="$REPO_DIR" \
    --ccache \
    --force-clean \
    --disable-rofiles-fuse \
    "$WORK_DIR" \
    "$STAGED"
)

echo "==> Bundling $(basename "$BUNDLE")"
flatpak build-bundle "$REPO_DIR" "$BUNDLE" "$APP_ID"

if (( KEEP_BUILD == 0 )); then
  rm -rf "$WORK_DIR"
fi

cat <<EOF

Done.
  Bundle:  $BUNDLE
  Install: flatpak install --user --bundle "$BUNDLE"
  Run:     flatpak run $APP_ID
  Remove:  flatpak uninstall --user $APP_ID
EOF
