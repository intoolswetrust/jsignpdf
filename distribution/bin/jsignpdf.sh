#!/bin/bash

DIRNAME=$(dirname "$(readlink -e "$0")")
DIR=$(cd "$DIRNAME" || exit 112; pwd)

[ "$OSTYPE" = "cygwin" ] && DIR="$( cygpath -m "$DIR" )"

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
