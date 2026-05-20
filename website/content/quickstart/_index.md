---
linkTitle: "Quickstart"
title: Quickstart
type: docs
sidebar:
  hide: true
---

## Quick start

### 1. Install

Download the latest release from the [GitHub releases page](https://github.com/intoolswetrust/jsignpdf/releases/latest). Native installers bundle their own Java 21 runtime; cross-platform ZIPs need Java 21+ from e.g. [Eclipse Adoptium](https://adoptium.net/).

- **Windows x64** — `jsignpdf-<version>-windows-x64.msi` (signed) or `jsignpdf-<version>-windows-x64.zip` (portable).
- **Linux x64 / aarch64** — `jsignpdf-<version>-linux-<arch>.deb`, `jsignpdf-<version>-linux-<arch>.rpm`, or the matching `.zip`. Flatpak bundles are also published per arch.
- **macOS Apple Silicon** — `jsignpdf-<version>-macos-aarch64.dmg` (unsigned in 3.1 — expect a Gatekeeper prompt), or the matching `.zip`. Intel Macs: run the Apple Silicon build under Rosetta 2, or use `jsignpdf-<version>-full.zip` with a locally installed Java 21.
- **Any OS with Java 21** — `jsignpdf-<version>-full.zip` (JavaFX bundled) or `jsignpdf-<version>-minimal.zip` (CLI / Swing fallback only).

If you grabbed a ZIP, extract it and run `bin/jsignpdf.sh` (POSIX) or `bin\jsignpdf.cmd` (Windows).

### 2. Get a keystore

To sign a PDF you need a private key in a keystore file (`.p12`, `.pfx`, `.jks`, ...). For production, get a PKCS#12 file from a trusted Certificate Authority. For a quick test, generate a self-signed key with `keytool`:

```shell
keytool -genkeypair -alias mykey -keyalg RSA -keysize 2048 \
    -keystore keystore.p12 -storetype PKCS12
```

A demo keystore (`jsmith.p12`, password `123456`) and a sample PDF are included in the `demo/` folder of the distribution.

### 3. Launch the GUI

If you installed via DEB / RPM / MSI / DMG / Flatpak, launch JSignPdf from the application menu.

If you extracted a cross-platform ZIP, run the launcher from the extracted folder:

```shell
bin/jsignpdf.sh       # POSIX
bin\jsignpdf.cmd      # Windows
```

Then:

1. Open your PDF via _File > Open_ or drag-and-drop.
2. In the _Certificate_ panel, choose the keystore file and enter its password.
3. (Optional) Configure appearance, timestamp, or encryption in the other panels.
4. Click _Sign_ — the signed file is written next to the input with a `_signed` suffix.

### 4. Or sign from the command line

```shell
bin/jsignpdf.sh \
    -kst PKCS12 -ksf keystore.p12 -ksp mypassword \
    mydocument.pdf
```

Add `-ts https://freetsa.org/tsr -ha SHA256` to attach a trusted timestamp, or `-V -llx 50 -lly 50 -urx 250 -ury 120` to place a visible signature.

## Full documentation

For every option, the JavaFX and Swing interfaces, hardware tokens, timestamping, encryption and troubleshooting, see the **[JSignPdf Guide](../docs/)**.
