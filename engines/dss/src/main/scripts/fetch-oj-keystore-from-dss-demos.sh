#!/usr/bin/env bash
# copy the OJ/LOTL trust keystore from the dss-demonstrations repo,
# matching our DSS version, into the engine resources. Password convention: dss-password.
set -euo pipefail

TAG="${1:-6.4}"                       # dss-demonstrations tag == DSS version
PASS="dss-password"
DEST="engines/dss/src/main/resources/net/sf/jsignpdf/engine/dss/eu-oj-keystore.p12"
REPO="https://github.com/esig/dss-demonstrations.git"

[ -f pom.xml ] || { echo "Run from the jsignpdf repo root." >&2; exit 1; }

tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
echo ">> Cloning $REPO @ $TAG ..."
git clone --depth 1 --branch "$TAG" "$REPO" "$tmp" 2>/dev/null \
  || { echo ">> Tag $TAG not found, falling back to default branch."; git clone --depth 1 "$REPO" "$tmp"; }

# Find candidate keystores that (a) open with dss-password and (b) contain EU trusted-list signer certs.
echo ">> Searching for the OJ/LOTL keystore ..."
candidate=""
while IFS= read -r ks; do
  if keytool -list -keystore "$ks" -storetype PKCS12 -storepass "$PASS" >/dev/null 2>&1; then
    if keytool -list -v -keystore "$ks" -storetype PKCS12 -storepass "$PASS" 2>/dev/null \
        | grep -qiE "European Commission|Trusted List|C=EU|LOTL"; then
      candidate="$ks"; break
    fi
    [ -z "$candidate" ] && candidate="$ks"   # remember a dss-password p12 even if the heuristic misses
  fi
done < <(find "$tmp" -name '*.p12' | sort)

[ -n "$candidate" ] || { echo "No dss-password PKCS12 keystore found in the repo." >&2; exit 1; }

echo ">> Using: ${candidate#$tmp/}"
cp "$candidate" "$DEST"
echo ">> Copied to $DEST"
echo ">> Contents:"
keytool -list -v -keystore "$DEST" -storetype PKCS12 -storepass "$PASS" \
  | grep -E "Alias name:|Owner:|Valid from:" || true
echo ">> Done. Review the certificate owners/validity above before committing."
