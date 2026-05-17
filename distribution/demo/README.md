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
