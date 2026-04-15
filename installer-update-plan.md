# Replace InnoSetup+launch4j+Ant with jpackage for the Windows installer

## Context

Today the Windows installer is produced by a Docker image (`kwart/innosetup`) that
runs Ant + Launch4j to wrap the shaded jar into `JSignPdf.exe` / `JSignPdfC.exe` /
`InstallCert.exe`, copies bundled JREs from `/opt/jre32` and `/opt/jre64`, and then
runs `iscc` (InnoSetup) under Wine to produce a single fat `.exe` installer that
selects 32- or 64-bit at install time. The orchestrator is
`distribution/windows/create-jsignpdf-installer.sh`, invoked from
`.github/workflows/do-release.yml`.

This setup is brittle, depends on a custom Docker image, on Wine, on Ant, on a
prebuilt 32-bit JRE in `/opt/jre32`, and produces only a single combined `.exe`.
We want to replace it with `jpackage`, run natively on `windows-latest`, and
produce three artifact types (EXE, MSI, ZIP) for x64. 32-bit Windows is dropped
entirely — Temurin no longer ships 32-bit Windows JDKs and 32-bit Windows is
effectively EOL — but the existing cross-arch no-JRE distribution zip
(`jsignpdf-${version}.zip`) is kept so that 32-bit users (and Linux/Mac users)
can still run JSignPdf manually with their own JRE.

The jpackage call must reproduce the four launchers that ship today
(JSignPdf JavaFX GUI, JSignPdf-swing Swing GUI, JSignPdfC console, InstallCert),
the JVM options currently in `launch4j-template.l4j.ini`, the MPL-2.0 license
page that InnoSetup currently shows, and the icons in `distribution/doc/icon/`.

## Final artifact set per release

Produced by the existing ubuntu Maven job (unchanged):

- `jsignpdf-${VERSION}.zip` — cross-arch, no JRE (existing assembly)

Produced by the new windows-latest jpackage job:

- `JSignPdf-${VERSION}-win-x64.exe` — jpackage EXE installer (WiX-based) with license page
- `JSignPdf-${VERSION}-win-x64.msi` — jpackage MSI installer (WiX-based) with license page
- `JSignPdf-${VERSION}-win-x64.zip` — zipped jpackage app-image (portable, bundled JRE)

All four are uploaded to the same GitHub Release tag and (for the existing zip)
to SourceForge.

## File changes

### 1. New: `distribution/jpackage/` (add-launcher properties)

The current `launch4j-template.l4j.ini` JVM args become the main launcher's
`--java-options`. Each add-launcher gets its own `.properties` file; jpackage
properties replace (not merge with) the main launcher's values, so each file
must repeat the JVM options where applicable.

Common JVM options string (used in main launcher and add-launchers that need it):

```
-Xms1g -Xmx1g
--add-exports jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED
--add-exports jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED
--add-exports java.base/sun.security.action=ALL-UNNAMED
--add-exports java.base/sun.security.rsa=ALL-UNNAMED
--add-opens java.base/sun.security.util=ALL-UNNAMED
```

Note: the legacy `-Djsignpdf.home="%EXEDIR%"` is intentionally dropped — jpackage
sets `user.dir`/working directory correctly and the app already resolves resources
relative to its own classpath; if a regression is found during verification, it
will be added back via `--java-options "-Djsignpdf.home=$APPDIR"` (jpackage substitutes
`$APPDIR` to the runtime app directory).

Files to create:

- `distribution/jpackage/JSignPdf-swing.properties` — Swing GUI add-launcher
  ```
  win-console=false
  java-options=-Djsignpdf.swing=true -Xms1g -Xmx1g --add-exports jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED --add-exports jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED --add-exports java.base/sun.security.action=ALL-UNNAMED --add-exports java.base/sun.security.rsa=ALL-UNNAMED --add-opens java.base/sun.security.util=ALL-UNNAMED
  ```

- `distribution/jpackage/JSignPdfC.properties` — console add-launcher
  ```
  win-console=true
  java-options=-Xms1g -Xmx1g --add-exports jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED --add-exports jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED --add-exports java.base/sun.security.action=ALL-UNNAMED --add-exports java.base/sun.security.rsa=ALL-UNNAMED --add-opens java.base/sun.security.util=ALL-UNNAMED
  ```

- `distribution/jpackage/InstallCert.properties` — InstallCert add-launcher (separate jar, separate main class)
  ```
  win-console=true
  main-jar=InstallCert.jar
  main-class=net.sf.jsignpdf.InstallCert
  ```

### 2. New: `distribution/windows/build-windows-installers.ps1`

PowerShell script invoked by the workflow. Lives in the repo (not inline in the
YAML) so it can be tested locally on a Windows box. Responsibilities:

1. Take `$VERSION` as parameter.
2. Build an input staging directory containing `JSignPdf.jar` and `InstallCert.jar`
   pulled out of `distribution/target/jsignpdf-${VERSION}.zip` (the assembly zip
   already produced by the ubuntu job — but in the windows job we'll just rebuild
   via `mvn -B package -DskipTests` since checking out the tag is cheaper than
   passing artifacts cross-job).
3. Run jpackage `--type app-image` to produce `jpackage-out/JSignPdf/`.
4. Zip that directory → `JSignPdf-${VERSION}-win-x64.zip`.
5. Run jpackage `--type exe --app-image jpackage-out/JSignPdf ...` → `.exe`.
6. Run jpackage `--type msi --app-image jpackage-out/JSignPdf ...` → `.msi`.
7. Move all three artifacts to `distribution/target/upload/`.

Skeleton (final version goes in the file):

```powershell
param([Parameter(Mandatory=$true)][string]$Version)
$ErrorActionPreference = "Stop"
$root = (Resolve-Path "$PSScriptRoot/../..").Path
$target = "$root/distribution/target"
$staging = "$target/jpackage-staging"
$out = "$target/jpackage-out"
$upload = "$target/upload"

Remove-Item -Recurse -Force $staging,$out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $staging,$out,$upload | Out-Null

# Pull jars straight from module targets (no need to re-extract the assembly zip)
Copy-Item "$root/jsignpdf/target/jsignpdf-$Version-jar-with-dependencies.jar" "$staging/JSignPdf.jar"
Copy-Item "$root/installcert/target/installcert-$Version.jar" "$staging/InstallCert.jar"

$jvmOpts = @(
  '--java-options','-Xms1g',
  '--java-options','-Xmx1g',
  '--java-options','--add-exports=jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED',
  '--java-options','--add-exports=jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED',
  '--java-options','--add-exports=java.base/sun.security.action=ALL-UNNAMED',
  '--java-options','--add-exports=java.base/sun.security.rsa=ALL-UNNAMED',
  '--java-options','--add-opens=java.base/sun.security.util=ALL-UNNAMED'
)

# Step 1: app-image (one runtime, four launchers)
& jpackage `
  --type app-image `
  --input $staging `
  --main-jar JSignPdf.jar `
  --main-class net.sf.jsignpdf.Signer `
  --name JSignPdf `
  --app-version $Version `
  --vendor "Josef Cacek" `
  --copyright "Josef Cacek" `
  --description "JSignPdf adds digital signatures to PDF documents" `
  --icon "$root/distribution/doc/icon/icons.ico" `
  --dest $out `
  @jvmOpts `
  --add-launcher "JSignPdf-swing=$root/distribution/jpackage/JSignPdf-swing.properties" `
  --add-launcher "JSignPdfC=$root/distribution/jpackage/JSignPdfC.properties" `
  --add-launcher "InstallCert=$root/distribution/jpackage/InstallCert.properties"
if ($LASTEXITCODE -ne 0) { throw "jpackage app-image failed" }

# Step 2: zip the app-image
Compress-Archive -Path "$out/JSignPdf" -DestinationPath "$upload/JSignPdf-$Version-win-x64.zip" -Force

# Steps 3 & 4: build installers from the app-image (shared installer args)
$installerArgs = @(
  '--app-image',"$out/JSignPdf",
  '--name','JSignPdf',
  '--app-version',$Version,
  '--vendor','Josef Cacek',
  '--copyright','Josef Cacek',
  '--description','JSignPdf adds digital signatures to PDF documents',
  '--license-file',"$root/distribution/licenses/MPL-2.0.txt",
  '--about-url','https://intoolswetrust.github.io/jsignpdf/',
  '--win-menu',
  '--win-menu-group','JSignPdf',
  '--win-shortcut',
  '--win-dir-chooser',
  '--win-upgrade-uuid','7b3d6e4c-9a51-4a8b-9b1c-7e8c1a4d2f10',
  '--dest',$out
)

& jpackage --type exe @installerArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage exe failed" }
& jpackage --type msi @installerArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage msi failed" }

Move-Item "$out/JSignPdf-$Version.exe" "$upload/JSignPdf-$Version-win-x64.exe"
Move-Item "$out/JSignPdf-$Version.msi" "$upload/JSignPdf-$Version-win-x64.msi"
```

About `--win-upgrade-uuid`: this UUID must remain stable across releases so MSI
upgrades work. The literal value above is generated once for JSignPdf and
committed as part of this change. Do NOT regenerate it on each release.

About WiX: jpackage on JDK 17+ uses WiX 3 for both EXE and MSI on Windows.
GitHub `windows-latest` runners ship WiX 3 in `C:\Program Files (x86)\WiX Toolset v3.x\bin`,
which the runner automatically puts on `PATH`. If a future runner image drops
WiX, fall back to `choco install wixtoolset --version=3.14.0 -y` before invoking
jpackage.

### 3. Modified: `.github/workflows/do-release.yml`

Restructure into two jobs. Outputs from job 1 (`tag`, `version`) feed job 2.

- **Job `do-release`** (ubuntu-latest, Java 11) — unchanged Maven release flow,
  with these edits:
  - Drop the `docker run … kwart/innosetup …` line and the `*.exe` move.
  - Still uploads the existing `jsignpdf-${VERSION}.zip` to SourceForge and to
    the GitHub release.
  - Adds `outputs: { tag: …, version: … }` so job 2 can consume them.
  - Still runs the `svenstaro/upload-release-action` step for the cross-arch zip.

- **New job `windows-installers`** (`needs: do-release`, `runs-on: windows-latest`):
  ```yaml
  windows-installers:
    needs: do-release
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v6
        with:
          ref: ${{ needs.do-release.outputs.tag }}
      - uses: actions/setup-java@v5
        with:
          java-version: 21
          distribution: temurin
          cache: maven
      - name: Build jars
        run: mvn -B -DskipTests -pl jsignpdf,installcert -am package
      - name: Build Windows installers
        shell: pwsh
        run: ./distribution/windows/build-windows-installers.ps1 -Version ${{ needs.do-release.outputs.version }}
      - name: Upload to GitHub Release
        uses: svenstaro/upload-release-action@29e53e917877a24fad85510ded594ab3c9ca12de
        with:
          file: distribution/target/upload/*
          file_glob: true
          tag: ${{ needs.do-release.outputs.tag }}
          overwrite: true
  ```

Note we deliberately do not `mvn install` the entire reactor in job 2 — only
`jsignpdf` and `installcert` are needed to produce the two jars jpackage consumes.
This keeps the Windows job fast and avoids re-running asciidoctor on Windows.

### 4. Files to delete

- `distribution/windows/JSignPdf.iss`
- `distribution/windows/ant-build-create-launchers.xml`
- `distribution/windows/create-jsignpdf-installer.sh`
- `distribution/windows/launch4j-template.l4j.ini`
- `distribution/windows/JSignPdf-swing.l4j.ini`
- `distribution/doc/icon/splash08.bmp`
- `distribution/doc/icon/splash08.xcf`
- `distribution/doc/icon/splash_1.0.0.bmp`
- `distribution/doc/icon/splash_1.0.0.xcf`

Kept: `distribution/doc/icon/icons.ico` (now used by jpackage `--icon`),
`distribution/doc/icon/Certificate.ico` (still ships in source tree for
historical reference; no longer wired into the build but harmless).

`distribution/pom.xml` requires no changes — it never wired to launch4j or
InnoSetup; the assembly zip and asciidoctor PDF stay as-is.

## Critical files referenced by the change

| Purpose | Path |
|---|---|
| Existing release workflow (to edit) | `.github/workflows/do-release.yml` |
| Existing assembly zip (kept) | `distribution/src/assembly/assembly.xml` |
| Distribution module pom (no change) | `distribution/pom.xml` |
| Shaded JAR producer (no change) | `jsignpdf/pom.xml` (shade plugin, line 69) |
| Main class | `net.sf.jsignpdf.Signer` (`jsignpdf/pom.xml:16`) |
| InstallCert main class | `net.sf.jsignpdf.InstallCert` (`installcert/pom.xml:16`) |
| License file for installer page | `distribution/licenses/MPL-2.0.txt` |
| Installer icon | `distribution/doc/icon/icons.ico` |

## Verification

1. **Local Windows smoke test** (run on a Windows machine or VM with JDK 21 and WiX 3):
   ```
   mvn -B -DskipTests -pl jsignpdf,installcert -am package
   pwsh ./distribution/windows/build-windows-installers.ps1 -Version 3.0.0
   ```
   Confirm `distribution/target/upload/` contains `JSignPdf-3.0.0-win-x64.{exe,msi,zip}`.

2. **Install + launch test**:
   - Run the EXE installer end-to-end, verify the MPL-2.0 license page appears,
     start menu shortcut works, JSignPdf launches.
   - Repeat with the MSI.
   - Unzip the ZIP into `%USERPROFILE%\Apps\JSignPdf` and run `JSignPdf.exe`,
     `JSignPdf-swing.exe`, `JSignPdfC.exe --help`, `InstallCert.exe`.
   - Verify each launcher: signing a sample PDF works (validates JavaFX runtime
     + bouncycastle + the dropped `-Djsignpdf.home` is not a regression).

3. **Upgrade test**: install version X, then install version X+0.0.1 over the top
   using the same MSI flow, confirm the upgrade UUID handles it (no duplicate
   entries in Add/Remove Programs).

4. **Workflow dry run** (optional): create a dummy tag on a branch and trigger
   `do-release` against a private fork pointing at a test Maven repo, to confirm
   the two-job wiring and `needs.do-release.outputs.*` pass through correctly.
   Easier alternative: temporarily add a `pull_request` trigger to the
   `windows-installers` job alone for one PR, then revert.

5. **Artifact set on GitHub Release**: confirm the release page shows
   `jsignpdf-${VERSION}.zip` (cross-arch) plus the three Windows artifacts.

## Out of scope

- 32-bit Windows installers.
- macOS / Linux installer packaging (jpackage can do them; not requested).
- Code-signing the produced EXE/MSI (no certificate set up; can be added later
  via `--win-app-version` is unrelated; signing would be a separate
  `signtool sign` step in the workflow).
