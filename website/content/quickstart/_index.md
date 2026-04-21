---
linkTitle: "Quickstart"
title: Quickstart
type: docs
---

## Quick start

### 1. Install

- Download the latest release from the [GitHub releases page](https://github.com/intoolswetrust/jsignpdf/releases/latest).
- On Windows 64-bit, pick one of the `win-x64` artifacts — the **EXE** or **MSI** installer, or the **portable ZIP**. All three bundle their own Java runtime.
- On Linux, macOS or 32-bit Windows, download `jsignpdf-*.zip` (the platform-independent archive) and install Java 21 or newer, e.g. from [Eclipse Adoptium](https://adoptium.net/).
- Extract the archive if you chose a ZIP.

### 2. Get a keystore

To sign a PDF you need a private key in a keystore file (`.p12`, `.pfx`, `.jks`, ...). For production, get a PKCS#12 file from a trusted Certificate Authority. For a quick test, generate a self-signed key with `keytool`:

```shell
keytool -genkeypair -alias mykey -keyalg RSA -keysize 2048 \
    -keystore keystore.p12 -storetype PKCS12
```

A demo keystore (`jsmith.p12`, password `123456`) and a sample PDF are included in the `demo/` folder of the distribution.

### 3. Launch the GUI

From the folder where `JSignPdf.jar` lives:

```shell
java -jar JSignPdf.jar
```

Then:

1. Open your PDF via _File > Open_ or drag-and-drop.
2. In the _Certificate_ panel, choose the keystore file and enter its password.
3. (Optional) Configure appearance, timestamp, or encryption in the other panels.
4. Click _Sign_ — the signed file is written next to the input with a `_signed` suffix.

### 4. Or sign from the command line

```shell
java -jar JSignPdf.jar \
    -kst PKCS12 -ksf keystore.p12 -ksp mypassword \
    mydocument.pdf
```

Add `-ts https://freetsa.org/tsr -ha SHA256` to attach a trusted timestamp, or `-V -llx 50 -lly 50 -urx 250 -ury 120` to place a visible signature.

## Full documentation

For every option, the JavaFX and Swing interfaces, hardware tokens, timestamping, encryption and troubleshooting, see the **[JSignPdf Guide](../docs/)**.
