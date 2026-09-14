#!/usr/bin/env bash
# Regenerates the JavaFX documentation screenshots *with window decorations*, by running the real UI on an X
# display and grabbing it with system tools.
#
# The counterpart to `mvn -pl jsignpdf -am -Pscreenshots test`, which renders the same states off-screen and is
# faster and fully reproducible, but cannot produce a title bar: there is no window manager to draw one.
# Use this script when the images should show the app as a user sees it on their desktop.
#
# Two sources of pixels:
#
#   harness mode (default)  DecoratedScreenshotRunner drives the UI from the build's test classes and, at every
#                           state, drops a `NNN.ready` marker and blocks. This script grabs the named window,
#                           writes the PNG and answers with `NNN.done`. Java owns the application state, the
#                           shell owns the pixels - which is how states needing a loaded document, a keystore
#                           and a real signing pass get captured at all.
#
#   --app <path>            Runs an installed JSignPdf instead. There is no handshake, so only the states
#                           reachable from a cold start are captured: the main window and the Preferences
#                           dialog (Ctrl+, - an accelerator, so this works in any UI language). Works against
#                           older releases too: nothing here depends on a command-line flag the build in this
#                           tree happens to have.
#
# Examples:
#   ./capture-screenshots-x11.sh                        # full set, inside a private Xvfb display
#   ./capture-screenshots-x11.sh --no-xvfb              # on the current desktop, using its real theme
#   ./capture-screenshots-x11.sh --locales all          # plus one main window per bundled translation
#   ./capture-screenshots-x11.sh --app /opt/jsignpdf/jsignpdf.sh --locales cs,de
#   ./capture-screenshots-x11.sh --version 3.2.0              # title bar reads that version
#   ./capture-screenshots-x11.sh --locales all --javafx-version 23.0.2
#
# Fonts: OpenJFX 21 puts no CJK font into its fontconfig fallback chain, so the ja / zh-CN / zh-TW windows
# show empty boxes when the harness runs on the build's JavaFX 21. OpenJFX 23 picks Noto Sans CJK (or
# whatever fontconfig prefers for the language) - use --javafx-version, or --app against a package that
# bundles a newer JavaFX runtime.
#   ./capture-screenshots-x11.sh --check                # only verify the required tools are installed
#
# Requires: xdotool, xprop, xwininfo, ImageMagick (import), a window manager, and Xvfb unless --no-xvfb.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "${HERE}/.." && pwd)"

GUIDE_DIR="${HERE}/docs/img/javafx"
SITE_DIR="${HERE}/static/img/screenshots"
DEMO_DIR="${REPO}/distribution/demo"
GEOMETRY="1920x1200x24"
EXEC_PLUGIN_VERSION="3.5.1"
USE_XVFB=1
DISPLAY_NUM=99
WM=""
CHECK_ONLY=0
APP=""
LOCALES=""
CAPTURE_PREFS=0
APP_VERSION=""
JAVAFX_VERSION=""
MVN_EXTRA=()

# Guide image -> the name the website serves it under. The third website card,
# jsignpdf-javafx-signed.png, is captured directly because it has the confirmation dialog on top.
SITE_COPIES=(
  "document-loaded.png:jsignpdf-javafx-main.png"
  "visible-signature-placement.png:jsignpdf-javafx-visible-sig.png"
)

# Every bundled translation, mirroring net.sf.jsignpdf.utils.SupportedLanguages.
ALL_LOCALES="en cs de el es fr hr hu hy it ja nb pl pt pt-BR ru sk ta zh-CN zh-TW"

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
    --app)       APP="$2"; shift ;;
    --locales)   LOCALES="$2"; shift ;;
    --version)   APP_VERSION="$2"; shift ;;
    --javafx-version) JAVAFX_VERSION="$2"; shift ;;
    --out-dir)   GUIDE_DIR="$2"; shift ;;
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

case "$LOCALES" in
  "")    LOCALE_TAGS=() ;;
  all)   read -r -a LOCALE_TAGS <<<"$ALL_LOCALES" ;;
  *)     IFS=, read -r -a LOCALE_TAGS <<<"$LOCALES"
         for tag in "${LOCALE_TAGS[@]+"${LOCALE_TAGS[@]}"}"; do
           case " $ALL_LOCALES " in
             *" $tag "*) ;;
             *) die "no bundled translation for '$tag'; known tags: $ALL_LOCALES" ;;
           esac
         done ;;
esac

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
  local missing=() tool
  for tool in xdotool xprop xwininfo; do
    command -v "$tool" >/dev/null 2>&1 || missing+=("$tool")
  done
  if [ -z "$APP" ]; then
    for tool in mvn java; do
      command -v "$tool" >/dev/null 2>&1 || missing+=("$tool")
    done
  fi
  resolve_import
  [ ${#IMPORT[@]} -gt 0 ] || missing+=("imagemagick (import)")

  if [ "$USE_XVFB" -eq 1 ]; then
    command -v Xvfb >/dev/null 2>&1 || missing+=("Xvfb")
    if [ -z "$WM" ]; then
      local candidate
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

[ -z "$APP" ] || [ -x "$APP" ] || die "--app $APP is not an executable"
[ -z "$JAVAFX_VERSION" ] || MVN_EXTRA+=("-Dopenjfx.version=$JAVAFX_VERSION")
for dir in "$GUIDE_DIR" "$SITE_DIR" "$DEMO_DIR"; do
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
  local pid
  for pid in "$RUNNER_PID" "$WM_PID" "$XVFB_PID"; do
    [ -n "$pid" ] && kill "$pid" 2>/dev/null || true
  done
  rm -rf "$WORK"
}
trap cleanup EXIT

# --- Display -----------------------------------------------------------------

start_display() {
  if [ "$USE_XVFB" -eq 1 ]; then
    export DISPLAY=":$DISPLAY_NUM"
    log "starting Xvfb on $DISPLAY ($GEOMETRY)"
    Xvfb "$DISPLAY" -screen 0 "$GEOMETRY" -nolisten tcp >"$WORK/xvfb.log" 2>&1 &
    XVFB_PID=$!
    local _
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
}

# --- Geometry and grabbing ---------------------------------------------------

# Client area in root coordinates. xwininfo reports it unambiguously, which xdotool's own geometry does not
# across versions (some report the frame origin, some the client origin).
read_client_geometry() { # window-id
  eval "$(xwininfo -id "$1" | awk '
    /Absolute upper-left X/ { printf "CX=%s\n", $4 }
    /Absolute upper-left Y/ { printf "CY=%s\n", $4 }
    /^  Width:/             { printf "CW=%s\n", $2 }
    /^  Height:/            { printf "CH=%s\n", $2 }')"
}

# Decoration thickness the window manager added, as left/right/top/bottom.
read_frame_extents() { # window-id
  local raw
  FE_LEFT=0; FE_RIGHT=0; FE_TOP=0; FE_BOTTOM=0
  raw="$(xprop -id "$1" _NET_FRAME_EXTENTS 2>/dev/null || true)"
  case "$raw" in
    *=*) read -r FE_LEFT FE_RIGHT FE_TOP FE_BOTTOM <<<"$(echo "${raw#*= }" | tr -d ',')" ;;
  esac
}

grab_rect() { # width height x y outfile
  mkdir -p "$(dirname "$5")"
  "${IMPORT[@]}" -silent -window root -crop "${1}x${2}+${3}+${4}" +repage "png:$5"
}

grab_window() { # window-id outfile
  read_client_geometry "$1"
  read_frame_extents "$1"
  grab_rect "$((CW + FE_LEFT + FE_RIGHT))" "$((CH + FE_TOP + FE_BOTTOM))" \
            "$((CX - FE_LEFT))" "$((CY - FE_TOP))" "$2"
  log "wrote $2"
}

grab_region() { # window-id x y w h outfile
  read_client_geometry "$1"
  grab_rect "$4" "$5" "$((CX + $2))" "$((CY + $3))" "$6"
  log "wrote $6"
}

# Where a `guide/...` or `site/...` path from the runner lands on disk.
resolve_path() { # rooted-path
  case "$1" in
    guide/*) printf '%s/%s' "$GUIDE_DIR" "${1#guide/}" ;;
    site/*)  printf '%s/%s' "$SITE_DIR" "${1#site/}" ;;
    *)       die "unknown image root in '$1'" ;;
  esac
}

# xdotool matches titles as regexes, so anything the app puts in a title has to be escaped.
escape_regex() {
  printf '%s' "$1" | sed 's/[][\\^$.*+?(){}|]/\\&/g'
}

find_window_by_title() { # title
  timeout 60 xdotool search --sync --onlyvisible --name "^$(escape_regex "$1")\$" 2>/dev/null | tail -1
}

find_window_by_title_prefix() { # title-prefix
  timeout 120 xdotool search --sync --onlyvisible --name "^$(escape_regex "$1")" 2>/dev/null | tail -1
}

list_windows() {
  xdotool search --onlyvisible --name '.*' 2>/dev/null | sort
}

# --- Harness mode ------------------------------------------------------------

# Constants.VERSION reads the Maven descriptor that only exists inside the packaged jar, so a run from
# target/classes would title every window "JSignPdf [UNKNOWN]" - and with decorations, that title is in the
# picture. Write the descriptor by hand instead of packaging a jar for it. --version overrides the project
# version, for capturing the images of a release before its version is set.
stamp_version() {
  local dir="$REPO/jsignpdf/target/classes/META-INF/maven/com.github.kwart.jsign/jsignpdf"
  if [ -z "$APP_VERSION" ]; then
    APP_VERSION="$(mvn -q -pl jsignpdf -f "$REPO/pom.xml" help:evaluate -Dexpression=project.version -DforceStdout)"
  fi
  mkdir -p "$dir"
  printf 'groupId=com.github.kwart.jsign\nartifactId=jsignpdf\nversion=%s\n' "$APP_VERSION" >"$dir/pom.properties"
  log "window title version: $APP_VERSION"
}

# JVM options that start a JVM in the language of a BCP-47 tag.
locale_jvm_opts() { # tag
  local opts="-Duser.language=${1%%-*}"
  case "$1" in *-*) opts="$opts -Duser.country=${1##*-}" ;; esac
  printf '%s' "$opts"
}

# exec:exec forks a real JVM with the module's *test* class path, which is what carries the runner and the
# signing engines. (dependency:build-classpath silently drops test-scope artifacts here, and exec:java would
# run inside Maven's own JVM, where overriding user.home breaks ~/.m2 resolution.)
launch_runner() { # extra JVM options
  rm -rf "$HANDSHAKE"
  mkdir -p "$HANDSHAKE"
  JSIGNPDF_CONFIG_DIR="$RUN_HOME/config" mvn -q -pl jsignpdf -f "$REPO/pom.xml" ${MVN_EXTRA[@]+"${MVN_EXTRA[@]}"} \
    "org.codehaus.mojo:exec-maven-plugin:${EXEC_PLUGIN_VERSION}:exec" \
    -Dexec.executable=java -Dexec.classpathScope=test \
    -Dexec.args="-Duser.home=$RUN_HOME -Djsignpdf.screenshot.demoDir=$DEMO_DIR -Djsignpdf.screenshot.handshakeDir=$HANDSHAKE $1 -cp %classpath net.sf.jsignpdf.fx.screenshot.DecoratedScreenshotRunner" \
    >>"$WORK/runner.log" 2>&1 &
  RUNNER_PID=$!
}

# Answers the runner's markers until it reports `finished`.
serve_handshake() {
  local deadline ready id path region window wid
  deadline=$(( $(date +%s) + 1800 ))
  while true; do
    if [ -f "$HANDSHAKE/error" ]; then
      cat "$HANDSHAKE/error" >&2
      die "the UI runner failed; see $WORK/runner.log"
    fi
    [ -f "$HANDSHAKE/finished" ] && break
    if ! kill -0 "$RUNNER_PID" 2>/dev/null; then
      cat "$WORK/runner.log" >&2
      die "the UI runner exited before finishing"
    fi
    [ "$(date +%s)" -gt "$deadline" ] && die "timed out waiting for the UI runner"

    shopt -s nullglob
    for ready in "$HANDSHAKE"/*.ready; do
      id="$(basename "$ready" .ready)"
      path="$(sed -n 's/^path=//p' "$ready")"
      region="$(sed -n 's/^region=//p' "$ready")"
      window="$(sed -n 's/^window=//p' "$ready")"

      wid="$(find_window_by_title "$window")"
      [ -n "$wid" ] || { cat "$WORK/runner.log" >&2; die "no window titled '$window'"; }
      xdotool windowactivate --sync "$wid" || true
      xdotool windowraise "$wid" || true
      sleep 0.4

      # shellcheck disable=SC2086 # region is a deliberately split "crop x y w h"
      set -- $region
      if [ "$1" = "full" ]; then
        grab_window "$wid" "$(resolve_path "$path")"
      else
        grab_region "$wid" "$2" "$3" "$4" "$5" "$(resolve_path "$path")"
      fi

      rm -f "$ready"
      : >"$HANDSHAKE/$id.done"
    done
    shopt -u nullglob
    sleep 0.2
  done

  wait "$RUNNER_PID" || true
  RUNNER_PID=""
}

run_harness() {
  log "compiling test classes"
  mvn -q -pl jsignpdf -am -DskipTests test-compile -f "$REPO/pom.xml" ${MVN_EXTRA[@]+"${MVN_EXTRA[@]}"}
  stamp_version
  [ -z "$JAVAFX_VERSION" ] || log "JavaFX version: $JAVAFX_VERSION"

  log "launching the UI"
  launch_runner ""
  serve_handshake

  local copy
  for copy in "${SITE_COPIES[@]}"; do
    cp "$GUIDE_DIR/${copy%%:*}" "$SITE_DIR/${copy##*:}"
    log "wrote $SITE_DIR/${copy##*:}"
  done

  # One JVM per translation, started in that language: JavaFX resolves its fontconfig fallback chain once per
  # JVM, ordered by the startup language - what a user starting the app in that language gets.
  local tag
  for tag in "${LOCALE_TAGS[@]+"${LOCALE_TAGS[@]}"}"; do
    log "launching the UI in $tag"
    launch_runner "$(locale_jvm_opts "$tag") -Djsignpdf.screenshot.galleryLocale=$tag"
    serve_handshake
  done
}

# --- Installed-application mode ----------------------------------------------

# One cold start: the main window, and on the first pass the Preferences dialog too.
capture_installed() { # locale-tag ("" for the app's own default) main-image-path
  local tag="$1" target="$2" wid before after prefs jvm_opts

  # A throwaway config directory is not enough on its own: the JVM takes user.home from the password entry,
  # not from $HOME, so the app would still find the real ~/.JSignPdf, migrate it, and put that user's
  # keystore path and recent files into the images. JAVA_TOOL_OPTIONS reaches the JVM through any launcher.
  # The UI language rides along the same way - user.language is what every release reads, whereas the
  # -o ui.language=<tag> override only exists from 3.2.0 on.
  jvm_opts="-Duser.home=$RUN_HOME"
  [ -z "$tag" ] || jvm_opts="$jvm_opts $(locale_jvm_opts "$tag")"

  log "launching $APP${tag:+ (${tag})}"
  # setsid puts the launcher and the JVM it starts in one process group, so the fallback kill reaches both.
  setsid env HOME="$RUN_HOME" JSIGNPDF_CONFIG_DIR="$RUN_HOME/config" JAVA_TOOL_OPTIONS="$jvm_opts" \
    "$APP" >>"$WORK/app.log" 2>&1 &
  RUNNER_PID=$!

  wid="$(find_window_by_title_prefix 'JSignPdf')"
  [ -n "$wid" ] || { cat "$WORK/app.log" >&2; die "the application window never appeared"; }
  xdotool windowmove "$wid" 40 40 || true
  xdotool windowactivate --sync "$wid" || true
  sleep 1.5
  grab_window "$wid" "$(resolve_path "$target")"

  if [ "$CAPTURE_PREFS" -eq 1 ]; then
    CAPTURE_PREFS=0
    before="$(list_windows)"
    # Ctrl+, is the Preferences accelerator, so this does not depend on menu labels or the UI language.
    # Sent through XTEST (no --window): JavaFX ignores the synthetic XSendEvent that --window produces.
    xdotool windowactivate --sync "$wid" || true
    xdotool key --clearmodifiers ctrl+comma
    sleep 3
    after="$(list_windows)"
    prefs="$(comm -13 <(echo "$before") <(echo "$after") | head -1)"
    if [ -n "$prefs" ]; then
      xdotool windowactivate --sync "$prefs" || true
      sleep 0.5
      grab_window "$prefs" "$(resolve_path guide/preferences-general.png)"
      xdotool key --clearmodifiers Escape || true
      sleep 0.5
    else
      log "WARNING: the Preferences dialog did not open; skipping preferences-general.png"
    fi
  fi

  close_app "$wid"
}

# The next locale needs a cold start, so the previous instance has to be gone - window and process both,
# or the window search would immediately match the one still on screen.
close_app() { # window-id
  local _
  # Ctrl+Q is the Exit accelerator: a clean shutdown, and one a launcher script cannot swallow the way it
  # can swallow a signal aimed at the JVM it started.
  xdotool windowactivate --sync "$1" >/dev/null 2>&1 || true
  xdotool key --clearmodifiers ctrl+q || true

  for _ in $(seq 1 30); do
    if [ -z "$(xdotool search --onlyvisible --name '^JSignPdf' 2>/dev/null)" ]; then
      RUNNER_PID=""
      return 0
    fi
    sleep 0.5
  done

  log "WARNING: the application ignored Ctrl+Q; killing it"
  xdotool windowkill "$1" 2>/dev/null || true
  kill -- "-$RUNNER_PID" 2>/dev/null || kill "$RUNNER_PID" 2>/dev/null || true
  RUNNER_PID=""
  sleep 2
}

run_installed() {
  CAPTURE_PREFS=1
  capture_installed "" guide/main-window-empty.png
  local tag
  for tag in "${LOCALE_TAGS[@]+"${LOCALE_TAGS[@]}"}"; do
    capture_installed "$tag" "site/locales/main-window-$tag.png"
  done
  log "note: --app captures only the cold-start states; the document, signing and visible-signature"
  log "      images need the harness (run without --app)"
}

# --- Go ----------------------------------------------------------------------

start_display
if [ -n "$APP" ]; then
  run_installed
else
  run_harness
fi
log "done"
