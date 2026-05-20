# JSignPdf

[![CI](https://github.com/intoolswetrust/jsignpdf/actions/workflows/pr-builder.yaml/badge.svg)](https://github.com/intoolswetrust/jsignpdf/actions/workflows/pr-builder.yaml)
[![GitHub release](https://img.shields.io/github/v/release/intoolswetrust/jsignpdf?include_prereleases&sort=semver)](https://github.com/intoolswetrust/jsignpdf/releases)
[![SourceForge downloads](https://img.shields.io/sourceforge/dm/jsignpdf.svg)](https://sourceforge.net/projects/jsignpdf/files/latest/download)
[![Translation status](https://hosted.weblate.org/widget/jsignpdf/messages/svg-badge.svg)](https://hosted.weblate.org/projects/jsignpdf/messages/)
[![License: MPL 2.0 / LGPL 2.1](https://img.shields.io/badge/license-MPL%202.0%20%2F%20LGPL%202.1-blue)](License.md)

JSignPdf is a Java desktop application for adding digital signatures to PDF documents.
It supports both software keystores (PKCS#12) and hardware tokens / smartcards (PKCS#11),
RFC 3161 timestamping (TSA), OCSP and CRL embedding for long-term validation (LTV),
drag-to-place visible signatures, and a scriptable command-line / batch mode.

Project home page: [jsignpdf.eu](https://jsignpdf.eu/)

![JSignPdf JavaFX UI](https://raw.githubusercontent.com/intoolswetrust/jsignpdf/master/website/static/img/screenshots/jsignpdf-javafx-main.png)

## Features

- **JavaFX desktop UI** — PDF preview, drag-to-place visible signatures, zoom / page navigation, drag-and-drop document loading, collapsible options panel. Legacy Swing UI remains available via `-Djsignpdf.swing=true`.
- **Command-line / batch mode** — sign many PDFs non-interactively; full CLI parity with the GUI.
- **Keystores**: PKCS#12, Java keystore, and PKCS#11 (hardware tokens, smartcards, HSMs via `<config-dir>/pkcs11.cfg`, edited from the Preferences dialog).
- **Timestamping**: RFC 3161 TSA with optional user/password authentication.
- **Revocation info**: CRL and OCSP embedding for LTV workflows.
- **Visible signatures**: customizable layout with `${signer}`, `${timestamp}`, and other placeholders.
- **Internationalization**: maintained via [Weblate](https://hosted.weblate.org/projects/jsignpdf/messages/) (15+ languages).

## Install

Native installers bundle their own Java 21 runtime. Cross-platform ZIPs need a **Java 21 (or newer) JRE** on `PATH` or pointed at by `JAVA_HOME`.

All artifacts are published on [GitHub Releases](https://github.com/intoolswetrust/jsignpdf/releases) and mirrored on [SourceForge](https://sourceforge.net/projects/jsignpdf/files/latest/download).

| Platform | Recommended | Portable fallback |
|---|---|---|
| **Windows x64** | `jsignpdf-<version>-windows-x64.msi` (SignPath-signed) | `jsignpdf-<version>-windows-x64.zip` |
| **Linux x64 (Debian / Ubuntu)** | `jsignpdf-<version>-linux-x64.deb` | `jsignpdf-<version>-linux-x64.zip` |
| **Linux x64 (Fedora / RHEL / openSUSE)** | `jsignpdf-<version>-linux-x64.rpm` | `jsignpdf-<version>-linux-x64.zip` |
| **Linux aarch64 (ARM servers / SBCs)** | `jsignpdf-<version>-linux-aarch64.{deb,rpm}` | `jsignpdf-<version>-linux-aarch64.zip` |
| **Linux (any distro)** | `jsignpdf-<version>-linux-<arch>.flatpak` (Flathub submission tracked in [#307](https://github.com/intoolswetrust/jsignpdf/issues/307)) | — |
| **macOS Apple Silicon** | `jsignpdf-<version>-macos-aarch64.dmg` (unsigned in 3.1) | `jsignpdf-<version>-macos-aarch64.zip` |
| **Any OS with Java 21** | `jsignpdf-<version>-full.zip` (JFX bundled for every supported OS/arch) | `jsignpdf-<version>-minimal.zip` (no JFX, Swing fallback only — for CLI users and packagers) |

`jsignpdf-<version>-SHA256SUMS.txt` covers every artifact above.


Maven-Central-published artifacts (for embedding the signing engine in your own project) live under `com.github.kwart.jsign`.

## Documentation

- User guide: <https://jsignpdf.eu/docs/>
- Release notes: [`distribution/doc/release-notes/`](distribution/doc/release-notes/)
- Developer / architecture guide: [AGENTS.md](AGENTS.md)
- Design notes: [`design-doc/`](design-doc/)
- Issue tracker: <https://github.com/intoolswetrust/jsignpdf/issues>

## Translations

Help translate JSignPdf on Weblate: <https://hosted.weblate.org/projects/jsignpdf/messages/>

## Build from source

Requires **Java 21** and Apache Maven.

```bash
mvn clean install
```

The resulting artifacts are produced under `distribution/target/`. See [AGENTS.md](AGENTS.md) for module layout, source-tree overview, and test commands.

### Native installers

Native installers for every supported platform are built with `jpackage` as
part of the release workflow (`.github/workflows/do-release.yml`):

- Windows: `distribution/windows/build-windows-installers.ps1` → MSI + ZIP
- Linux  : `distribution/linux/build-linux-installers.sh` → DEB + RPM + ZIP (auto-detects x64 / aarch64)
- macOS  : `distribution/macos/build-macos-installers.sh` → DMG + ZIP (auto-detects x64 / aarch64)

Each script consumes the staging tree assembled by the `distribution` module
(`mvn -B -DskipTests -pl distribution -am package`) and assumes Azul Zulu+FX 21
is on `PATH` / `JAVA_HOME` so the JavaFX modules are available to `jlink`.

For a local experiment on a different platform, cross-building is not
supported by `jpackage` — the GitHub Actions release workflow runs each script
on a matching runner.

### Flatpak

Flatpak packaging lives under [`distribution/linux/flatpak/`](distribution/linux/flatpak/).
Maven dependencies for the Flatpak build are captured in `maven-dependencies.json`
and regenerated by `generate-dependencies.sh` (run by the
`refresh-flatpak-deps.yml` workflow).

## Release process

Releases are workflow-driven via [`.github/workflows/do-release.yml`](.github/workflows/do-release.yml):

1. Add release notes for the new version as
   `distribution/doc/release-notes/<version>.md`. The release workflow uses
   this file as both the bundled README and the GitHub Release body.
2. Trigger the `do-release` workflow manually, supplying the target version.
3. SNAPSHOT builds are published to Maven Central on every push to `master`
   via [`push-snapshots.yaml`](.github/workflows/push-snapshots.yaml).

## License

Dual-licensed under **MPL 2.0** and **LGPL 2.1** — see [License.md](License.md).

---

## Appendix: Testing PKCS#11 without a card reader

A minimal way to exercise the PKCS#11 code path without real hardware is to
back `SunPKCS11` with a software NSS database.

> **Note:** this only works with the `PKCS11` keystore type. `JSignPKCS11`
> does not support NSS keystores.

```bash
sudo apt install -y libnss3-tools

echo "pass123+" > /tmp/newpass.txt
echo "dsadasdasdasdadasdasdasdasdsadfwerwerjfdksdjfksdlfhjsdk" > /tmp/noise.txt
mkdir /tmp/nssdb

MODUTIL_CMD="modutil -force -dbdir /tmp/nssdb"
$MODUTIL_CMD -create
$MODUTIL_CMD -changepw "NSS Certificate DB" -newpwfile /tmp/newpass.txt

certutil -S -v 240 -k rsa -n "CN=localhost" -t "u,u,u" -x \
         -s "CN=localhost" -d /tmp/nssdb \
         -f /tmp/newpass.txt -z /tmp/noise.txt

# Workaround for https://bugzilla.redhat.com/show_bug.cgi?id=1760437
touch /tmp/nssdb/secmod.db

mkdir -p ~/.config/jsignpdf

cat <<EOT >~/.config/jsignpdf/pkcs11.cfg 
name=testPkcs11
nssLibraryDirectory=/usr/lib/x86_64-linux-gnu
nssSecmodDirectory=/tmp/nssdb
nssModule=keystore
EOT
```

Then select the `PKCS11` keystore type in JSignPdf and use `pass123+` as the token PIN.
