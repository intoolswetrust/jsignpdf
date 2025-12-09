#!/bin/bash
set -euo pipefail

# Follow symlinks until we get the real file.
SOURCE="$0"
while [ -L "$SOURCE" ]; do
  # directory containing the symlink
  DIR="$(cd -P "$(dirname "$SOURCE")" >/dev/null 2>&1 && pwd -P)"
  # readlink returns the link target (may be relative)
  TARGET="$(readlink "$SOURCE")"
  # if target is relative, make it absolute relative to the symlink dir
  case "$TARGET" in
    /*) SOURCE="$TARGET" ;;
    *)  SOURCE="$DIR/$TARGET" ;;
  esac
done
# Now SOURCE is the real script path. Get its parent directory (script lives in bin/ so go up one)
DIR="$(cd -P "$(dirname "$SOURCE")" >/dev/null 2>&1 && pwd -P)"

[ "$OSTYPE" = "cygwin" ] && DIR="$( cygpath -m "$DIR" )"

JAVA_HOME="${JAVA_HOME-}"
JAVA_OPTS="${JAVA_OPTS-}"

JAVA=java
if [ -n "$JAVA_HOME" ]; then
  JAVA="$JAVA_HOME/bin/java"
fi

JAVA_VERSION=$("$JAVA" -cp "$DIR/JSignPdf.jar" net.sf.jsignpdf.JavaVersion)
if [ "$JAVA_VERSION" -gt "8" ]; then
  JAVA_OPTS="$JAVA_OPTS \
  --add-exports jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED \
  --add-exports jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED \
  --add-exports java.base/sun.security.action=ALL-UNNAMED \
  --add-exports java.base/sun.security.rsa=ALL-UNNAMED \
  --add-opens java.base/java.security=ALL-UNNAMED \
  --add-opens java.base/sun.security.util=ALL-UNNAMED"
fi

"$JAVA" $JAVA_OPTS "-Djsignpdf.home=$DIR" -jar "$DIR/JSignPdf.jar" "$@"
