#!/usr/bin/env bash
# Regenerates the JavaFX documentation screenshots *with window decorations*, by running the real UI on an X
# display and grabbing it with system tools.
#
# The counterpart to `mvn -pl jsignpdf -am -Pscreenshots test`, which renders the same states off-screen and is
# faster and fully reproducible, but cannot produce a title bar: there is no window manager to draw one.
# Use this script when the images should show the app as a user sees it on their desktop.
#
# How it works: DecoratedScreenshotRunner (test sources) drives the UI and, at every state, drops a
# `<image>.ready` marker and blocks. This script grabs the window, writes the PNG and answers with `<image>.done`.
# The Java side owns the application state, the shell side owns the pixels.
#
#   ./capture-screenshots-x11.sh              # inside a private Xvfb display (reproducible)
#   ./capture-screenshots-x11.sh --no-xvfb    # on the current desktop, using its real theme
#   ./capture-screenshots-x11.sh --check      # only verify the required tools are installed
#
# Requires: xdotool, xprop, xwininfo, ImageMagick (import), a window manager, and Xvfb unless --no-xvfb.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "${HERE}/.." && pwd)"

OUT_DIR="${HERE}/docs/img/javafx"
SITE_DIR="${HERE}/static/img/screenshots"
DEMO_DIR="${REPO}/distribution/demo"
GEOMETRY="1920x1200x24"
EXEC_PLUGIN_VERSION="3.5.1"
USE_XVFB=1
DISPLAY_NUM=99
WM=""
CHECK_ONLY=0

# Guide image -> the name the website serves it under. The third website card,
# jsignpdf-javafx-signed.png, is captured directly because it has the confirmation dialog on top.
SITE_COPIES=(
  "document-loaded.png:jsignpdf-javafx-main.png"
  "visible-signature-placement.png:jsignpdf-javafx-visible-sig.png"
)

# Window managers worth trying, most desktop-typical first - the decorations in the images are whatever this
# one draws, so a plain title bar beats an exotic theme.
WM_CANDIDATES=(marco metacity xfwm4 mutter openbox fluxbox)

usage() {
  awk 'NR > 1 && /^#/ { sub(/^# ?/, ""); print; next } NR > 1 { exit }' "$0"
}

while [ $# -gt 0 ]; do
  case "$1" in
    --no-xvfb)   USE_XVFB=0 ;;
    --display)   DISPLAY_NUM="${2#:}"; shift ;;
    --wm)        WM="$2"; shift ;;
    --out-dir)   OUT_DIR="$2"; shift ;;
    --site-dir)  SITE_DIR="$2"; shift ;;
    --geometry)  GEOMETRY="$2"; shift ;;
    --check)     CHECK_ONLY=1 ;;
    -h|--help)   usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 2 ;;
  esac
  shift
done

log() { printf '[capture] %s\n' "$*"; }
die() { printf '[capture] ERROR: %s\n' "$*" >&2; exit 1; }

# --- Tooling ----------------------------------------------------------------

IMPORT=()
resolve_import() {
  if command -v magick >/dev/null 2>&1; then
    IMPORT=(magick import)
  elif command -v import >/dev/null 2>&1; then
    IMPORT=(import)
  fi
}

check_tools() {
  local missing=()
  for tool in xdotool xprop xwininfo mvn java; do
    command -v "$tool" >/dev/null 2>&1 || missing+=("$tool")
  done
  resolve_import
  [ ${#IMPORT[@]} -gt 0 ] || missing+=("imagemagick (import)")

  if [ "$USE_XVFB" -eq 1 ]; then
    command -v Xvfb >/dev/null 2>&1 || missing+=("Xvfb")
    if [ -z "$WM" ]; then
      for candidate in "${WM_CANDIDATES[@]}"; do
        if command -v "$candidate" >/dev/null 2>&1; then WM="$candidate"; break; fi
      done
      [ -n "$WM" ] || missing+=("a window manager (any of: ${WM_CANDIDATES[*]})")
    fi
  fi

  if [ ${#missing[@]} -gt 0 ]; then
    printf '[capture] Missing: %s\n' "${missing[*]}" >&2
    printf '[capture] On Debian/Ubuntu: sudo apt install xdotool x11-utils imagemagick xvfb marco\n' >&2
    exit 1
  fi
  log "tools OK (grab: ${IMPORT[*]}${WM:+, wm: $WM})"
}

check_tools
if [ "$CHECK_ONLY" -eq 1 ]; then
  exit 0
fi

for dir in "$OUT_DIR" "$SITE_DIR" "$DEMO_DIR"; do
  [ -d "$dir" ] || die "$dir is not a directory"
done

# --- Scratch space and cleanup ----------------------------------------------

WORK="$(mktemp -d -t jsignpdf-capture-XXXXXX)"
HANDSHAKE="$WORK/handshake"
RUN_HOME="$WORK/home"
mkdir -p "$HANDSHAKE" "$RUN_HOME/config"

XVFB_PID=""
WM_PID=""
RUNNER_PID=""

cleanup() {
  for pid in "$RUNNER_PID" "$WM_PID" "$XVFB_PID"; do
    [ -n "$pid" ] && kill "$pid" 2>/dev/null || true
  done
  rm -rf "$WORK"
}
trap cleanup EXIT

# --- Build --------------------------------------------------------------------

log "compiling test classes"
mvn -q -pl jsignpdf -am -DskipTests test-compile -f "$REPO/pom.xml"

# --- Display -----------------------------------------------------------------

if [ "$USE_XVFB" -eq 1 ]; then
  export DISPLAY=":$DISPLAY_NUM"
  log "starting Xvfb on $DISPLAY ($GEOMETRY)"
  Xvfb "$DISPLAY" -screen 0 "$GEOMETRY" -nolisten tcp >"$WORK/xvfb.log" 2>&1 &
  XVFB_PID=$!
  for _ in $(seq 1 50); do
    xdotool getdisplaygeometry >/dev/null 2>&1 && break
    sleep 0.2
  done
  xdotool getdisplaygeometry >/dev/null 2>&1 || die "Xvfb did not come up; see $WORK/xvfb.log"

  log "starting window manager: $WM"
  "$WM" >"$WORK/wm.log" 2>&1 &
  WM_PID=$!
  sleep 2
else
  [ -n "${DISPLAY:-}" ] || die "--no-xvfb needs DISPLAY to be set"
  log "using the current display $DISPLAY - keep the window unobstructed"
fi

# --- Run the scenario --------------------------------------------------------

# exec:exec forks a real JVM with the module's *test* class path, which is what carries the runner and the
# signing engines. (dependency:build-classpath silently drops test-scope artifacts here, and exec:java would
# run inside Maven's own JVM, where overriding user.home breaks ~/.m2 resolution.)
log "launching the UI"
JSIGNPDF_CONFIG_DIR="$RUN_HOME/config" mvn -q -pl jsignpdf -f "$REPO/pom.xml" \
  "org.codehaus.mojo:exec-maven-plugin:${EXEC_PLUGIN_VERSION}:exec" \
  -Dexec.executable=java -Dexec.classpathScope=test \
  -Dexec.args="-Duser.home=$RUN_HOME -Djsignpdf.screenshot.demoDir=$DEMO_DIR -Djsignpdf.screenshot.handshakeDir=$HANDSHAKE -cp %classpath net.sf.jsignpdf.fx.screenshot.DecoratedScreenshotRunner" \
  >"$WORK/runner.log" 2>&1 &
RUNNER_PID=$!

log "waiting for the main window"
# --sync blocks until a match exists, so it needs an outer timeout of its own.
WID="$(timeout 180 xdotool search --sync --onlyvisible --name '^JSignPdf' | head -1)" \
  || { cat "$WORK/runner.log" >&2; die "the main window never appeared; see $WORK/runner.log"; }
[ -n "$WID" ] || { cat "$WORK/runner.log" >&2; die "no window matched '^JSignPdf'"; }
xdotool windowmove "$WID" 40 40
xdotool windowactivate --sync "$WID"
xdotool windowraise "$WID"
sleep 1

# --- Geometry ----------------------------------------------------------------

# Client area in root coordinates. xwininfo reports it unambiguously, which xdotool's own geometry does not
# across versions (some report the frame origin, some the client origin).
read_client_geometry() {
  eval "$(xwininfo -id "$WID" | awk '
    /Absolute upper-left X/ { printf "CX=%s\n", $4 }
    /Absolute upper-left Y/ { printf "CY=%s\n", $4 }
    /^  Width:/             { printf "CW=%s\n", $2 }
    /^  Height:/            { printf "CH=%s\n", $2 }')"
}

# Decoration thickness the window manager added, as left/right/top/bottom.
read_frame_extents() {
  local raw
  FE_LEFT=0; FE_RIGHT=0; FE_TOP=0; FE_BOTTOM=0
  raw="$(xprop -id "$WID" _NET_FRAME_EXTENTS 2>/dev/null || true)"
  case "$raw" in
    *=*) read -r FE_LEFT FE_RIGHT FE_TOP FE_BOTTOM <<<"$(echo "${raw#*= }" | tr -d ',')" ;;
  esac
}

grab_rect() { # width height x y outfile
  "${IMPORT[@]}" -silent -window root -crop "${1}x${2}+${3}+${4}" +repage "png:$5"
}

# --- Capture loop ------------------------------------------------------------

capture() { # image-name region-line
  local name="$1" region="$2" target
  case "$name" in
    jsignpdf-*) target="$SITE_DIR/$name" ;;
    *)          target="$OUT_DIR/$name" ;;
  esac

  read_client_geometry
  read_frame_extents

  # shellcheck disable=SC2086 # region is a deliberately split "crop x y w h"
  set -- $region
  if [ "$1" = "full" ]; then
    grab_rect "$((CW + FE_LEFT + FE_RIGHT))" "$((CH + FE_TOP + FE_BOTTOM))" \
              "$((CX - FE_LEFT))" "$((CY - FE_TOP))" "$target"
  else
    grab_rect "$4" "$5" "$((CX + $2))" "$((CY + $3))" "$target"
  fi
  log "wrote $target"
}

deadline=$(( $(date +%s) + 600 ))
while true; do
  if [ -f "$HANDSHAKE/error" ]; then
    cat "$HANDSHAKE/error" >&2
    die "the UI runner failed; see $WORK/runner.log"
  fi
  if [ -f "$HANDSHAKE/finished" ]; then
    break
  fi
  if ! kill -0 "$RUNNER_PID" 2>/dev/null; then
    cat "$WORK/runner.log" >&2
    die "the UI runner exited before finishing"
  fi
  if [ "$(date +%s)" -gt "$deadline" ]; then
    die "timed out waiting for the UI runner"
  fi

  shopt -s nullglob
  for ready in "$HANDSHAKE"/*.ready; do
    name="$(basename "$ready" .ready)"
    capture "$name" "$(cat "$ready")"
    rm -f "$ready"
    : >"$HANDSHAKE/$name.done"
  done
  shopt -u nullglob
  sleep 0.2
done

wait "$RUNNER_PID" || true
RUNNER_PID=""

# --- Mirror into the website tree -------------------------------------------

for copy in "${SITE_COPIES[@]}"; do
  cp "$OUT_DIR/${copy%%:*}" "$SITE_DIR/${copy##*:}"
  log "wrote $SITE_DIR/${copy##*:}"
done

log "done"
