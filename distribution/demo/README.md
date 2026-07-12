# JSignPdf demo files

Sample material for trying out JSignPdf from the command line.

| File | Description |
| --- | --- |
| `service-agreeement.pdf` | Sample unsigned PDF document. |
| `jsmith.p12` | Demo PKCS#12 keystore with a self-signed certificate (CN=John Smith). Keystore and key password: `123456`. |

> The keystore is for demonstration only &mdash; do **not** use it to sign real documents.

## Command-line examples

The commands below assume you run them from this `demo` directory inside an extracted cross-platform ZIP (`bin/` and `lib/` sit at the parent level). On Windows substitute `..\bin\jsignpdf.cmd` for `../bin/jsignpdf.sh`. If you installed via the native installer (MSI / DEB / RPM / DMG / Flatpak), replace the launcher path with `jsignpdf` (which lives on `PATH`).

### Basic signature

```shell
../bin/jsignpdf.sh \
    -kst PKCS12 \
    -ksf jsmith.p12 \
    -ksp 123456 \
    service-agreeement.pdf
```

The signed file is written next to the input as `service-agreeement_signed.pdf`.

### Signature with reason, location and contact

```shell
../bin/jsignpdf.sh \
    -kst PKCS12 -ksf jsmith.p12 -ksp 123456 \
    -r "I agree with the content" \
    -l "Prague, CZ" \
    -c "jsmith@example.com" \
    service-agreeement.pdf
```

### Visible signature on page 1

```shell
../bin/jsignpdf.sh \
    -kst PKCS12 -ksf jsmith.p12 -ksp 123456 \
    -V -pg 1 \
    -llx 50 -lly 50 -urx 250 -ury 120 \
    --render-mode DESCRIPTION_ONLY \
    service-agreeement.pdf
```

### Timestamped signature (FreeTSA)

[FreeTSA](https://freetsa.org/) provides a free, public RFC 3161 timestamping authority. No authentication is required.

```shell
../bin/jsignpdf.sh \
    -kst PKCS12 -ksf jsmith.p12 -ksp 123456 \
    -ha SHA256 \
    -ts https://freetsa.org/tsr \
    -ta NONE \
    service-agreeement.pdf
```

If FreeTSA's root certificate is not trusted by your Java runtime, import it into the `cacerts` truststore first (see the _Untrusted TSA certificate_ section of the JSignPdf guide).

### PAdES LT/LTA with the self-signed demo certificate (permissive trust)

The demo certificate is self-signed, so it has no issuing CA and no revocation service — the requirements a conformant PAdES **LT/LTA** signature is built around. The DSS (PAdES) engine can still produce an LTA-level signature if you enable **permissive trust**, which downgrades the trust and revocation checks to warnings:

```shell
../bin/jsignpdf.sh \
    -eng dss -pl LTA \
    -kst PKCS12 -ksf jsmith.p12 -ksp 123456 -ka jsmith \
    -ha SHA256 -tsh SHA256 \
    -ts https://freetsa.org/tsr -ta NONE \
    -o engine.dss.online.enabled=true \
    -o engine.dss.trust.allowUntrusted=true \
    service-agreement.pdf
```

> **Testing only — not a conformant long-term signature.** The output has the LT/LTA *structure* but no real revocation data, so strict validators will not accept it as LT/LTA. `engine.dss.trust.allowUntrusted` exists for private-PKI / testing scenarios only.

Notes:

* `engine.dss.online.enabled=true` is required for any `LT`/`LTA` signing.
* For a genuine `LT`/`LTA` signature, use a certificate issued by a CA that publishes a reachable CRL/OCSP endpoint and trust that CA — see the _Self-signed and private-PKI certificates_ section of the JSignPdf guide.

### Append a signature to an already-signed document

```shell
../bin/jsignpdf.sh \
    -kst PKCS12 -ksf jsmith.p12 -ksp 123456 \
    -a \
    service-agreeement_signed.pdf
```

### List keys in the demo keystore

```shell
../bin/jsignpdf.sh \
    -kst PKCS12 -ksf jsmith.p12 -ksp 123456 \
    -lk
```
