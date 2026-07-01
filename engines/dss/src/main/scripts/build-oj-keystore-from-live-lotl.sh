#!/usr/bin/env bash
# bootstrap the OJ keystore from the certificate(s) embedded in the live EU LOTL's
# XML signature. TOFU (trust on first use) — cross-check the printed certs against the OJ notice
# before committing. Produces a PKCS12 with trustedCertEntry entries, password dss-password.
set -euo pipefail

LOTL_URL="${1:-https://ec.europa.eu/tools/lotl/eu-lotl.xml}"
PASS="dss-password"
DEST="engines/dss/src/main/resources/net/sf/jsignpdf/engine/dss/eu-oj-keystore.p12"

[ -f pom.xml ] || { echo "Run from the jsignpdf repo root." >&2; exit 1; }
command -v python3 >/dev/null || { echo "python3 required." >&2; exit 1; }

tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
echo ">> Downloading $LOTL_URL ..."
curl -fsSL "$LOTL_URL" -o "$tmp/eu-lotl.xml"

echo ">> Extracting X509 certificate(s) from the LOTL signature ..."
python3 - "$tmp/eu-lotl.xml" "$tmp" <<'PY'
import re, base64, sys, os
xml = open(sys.argv[1], encoding="utf-8").read()
outdir = sys.argv[2]
b64s = re.findall(r'<(?:\w+:)?X509Certificate>\s*([A-Za-z0-9+/=\s]+?)\s*</(?:\w+:)?X509Certificate>', xml)
if not b64s:
    print("No X509Certificate found in the LOTL signature.", file=sys.stderr); sys.exit(1)
seen = set(); n = 0
for b in b64s:
    der = base64.b64decode("".join(b.split()))
    if der in seen:  # de-dup
        continue
    seen.add(der)
    pem = "-----BEGIN CERTIFICATE-----\n" + "\n".join(
        base64.b64encode(der).decode()[i:i+64] for i in range(0, len(base64.b64encode(der)), 64)
    ) + "\n-----END CERTIFICATE-----\n"
    open(os.path.join(outdir, f"oj-{n}.pem"), "w").write(pem)
    n += 1
print(n)
PY

rm -f "$DEST"
i=0
for pem in "$tmp"/oj-*.pem; do
  keytool -importcert -noprompt -trustcacerts -alias "oj-signer-$i" \
    -file "$pem" -keystore "$DEST" -storetype PKCS12 -storepass "$PASS"
  i=$((i+1))
done

echo ">> Built $DEST with $i certificate(s):"
keytool -list -v -keystore "$DEST" -storetype PKCS12 -storepass "$PASS" \
  | grep -E "Alias name:|Owner:|Valid from:" || true
echo ">> CROSS-CHECK the owners/validity above against the OJ notice before committing."
