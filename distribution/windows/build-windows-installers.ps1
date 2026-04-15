#requires -Version 5.1
<#
Builds Windows installers for JSignPdf using jpackage.

Produces three artifacts in distribution/target/upload/:
  JSignPdf-<version>-win-x64.exe  - WiX-based EXE installer with license page
  JSignPdf-<version>-win-x64.msi  - WiX-based MSI installer with license page
  JSignPdf-<version>-win-x64.zip  - portable zip of the jpackage app-image

Prerequisites on the build machine:
  * JDK 17+ on PATH (provides jpackage)
  * WiX Toolset 3.x on PATH (jpackage uses light.exe / candle.exe)
  * jsignpdf and installcert modules already built
    (mvn -B -DskipTests -pl jsignpdf,installcert -am package)
#>
param(
    [Parameter(Mandatory=$true)]
    [string]$Version
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$root      = (Resolve-Path "$PSScriptRoot/../..").Path
$target    = Join-Path $root 'distribution/target'
$staging   = Join-Path $target 'jpackage-staging'
$out       = Join-Path $target 'jpackage-out'
$upload    = Join-Path $target 'upload'
$generated = Join-Path $target 'jpackage-generated'
$jpkgCfg   = Join-Path $root 'distribution/jpackage'
$iconsDir  = Join-Path $root 'distribution/doc/icon'

# jpackage --app-version accepts only numeric MAJOR[.MINOR[.MICRO]]. Strip any
# non-numeric suffix (e.g. "-RC1", ".Final", "-SNAPSHOT") and keep up to three
# numeric components. $Version itself is preserved for release-tag-aligned
# filenames; $appVersion is what we hand to jpackage.
$numericParts = @(
    (($Version -split '[^0-9.]', 2)[0]) -split '\.' |
        Where-Object { $_ -match '^\d+$' } |
        Select-Object -First 3
)
if ($numericParts.Count -eq 0) {
    throw "Cannot derive jpackage --app-version from '$Version' (no numeric component)."
}
$appVersion = $numericParts -join '.'

Write-Host "JSignPdf jpackage build"
Write-Host "  version   : $Version"
Write-Host "  appVersion: $appVersion"
Write-Host "  root      : $root"

# WiX 3.x is preinstalled on GitHub windows-latest runners; fall back to
# chocolatey if a future image drops it or if running on a clean dev box.
if (-not (Get-Command light.exe -ErrorAction SilentlyContinue)) {
    Write-Warning "WiX Toolset (light.exe) not on PATH; attempting 'choco install wixtoolset'..."
    & choco install wixtoolset --version=3.14.0 -y
    if ($LASTEXITCODE -ne 0) { throw "Failed to install WiX Toolset via chocolatey." }
    foreach ($wixBin in @(
        'C:\Program Files (x86)\WiX Toolset v3.14\bin',
        'C:\Program Files (x86)\WiX Toolset v3.11\bin'
    )) {
        if (Test-Path $wixBin) { $env:Path = "$wixBin;$env:Path"; break }
    }
    if (-not (Get-Command light.exe -ErrorAction SilentlyContinue)) {
        throw "WiX Toolset installed but light.exe is still not on PATH."
    }
}

foreach ($d in @($staging, $out, $generated)) {
    if (Test-Path $d) { Remove-Item -Recurse -Force $d }
}
New-Item -ItemType Directory -Force -Path $staging,$out,$generated,$upload | Out-Null

# Drop old artifacts from repeat local runs so a stale build doesn't get
# uploaded alongside the new one. CI runners are fresh so this is a no-op.
Get-ChildItem -Path $upload -Filter 'JSignPdf-*-win-x64.*' -File -ErrorAction SilentlyContinue |
    Remove-Item -Force

$shadedJar = Join-Path $root "jsignpdf/target/jsignpdf-$Version-jar-with-dependencies.jar"
$installCertJar = Join-Path $root "installcert/target/installcert-$Version.jar"

if (-not (Test-Path $shadedJar)) {
    throw "Missing shaded jar: $shadedJar`nRun 'mvn -B -DskipTests -pl jsignpdf,installcert -am package' first."
}
if (-not (Test-Path $installCertJar)) {
    throw "Missing installcert jar: $installCertJar"
}

Copy-Item $shadedJar      (Join-Path $staging 'JSignPdf.jar')
Copy-Item $installCertJar (Join-Path $staging 'InstallCert.jar')

# Optional: bundle the JSignPdf user guide PDF alongside the app payload.
# The CI workflow downloads it from a workflow artifact into
# distribution/target/pdf-guide/. Local builds may leave it absent — in that
# case we skip the --app-content entry and log a warning.
$guideSrcDir = Join-Path $target 'pdf-guide'
$guideContentRoot = Join-Path $target 'jpackage-app-content'
$guideContentDocs = Join-Path $guideContentRoot 'docs'
Remove-Item -Recurse -Force $guideContentRoot -ErrorAction SilentlyContinue
$guideFound = $false
if (Test-Path $guideSrcDir) {
    $guidePdf = Get-ChildItem -Path $guideSrcDir -Filter '*.pdf' -File -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($guidePdf) {
        New-Item -ItemType Directory -Force -Path $guideContentDocs | Out-Null
        Copy-Item $guidePdf.FullName (Join-Path $guideContentDocs 'JSignPdf.pdf')
        $guideFound = $true
        Write-Host "  guide   : $($guidePdf.Name) -> app/docs/JSignPdf.pdf"
    }
}
if (-not $guideFound) {
    Write-Warning "No PDF guide found under $guideSrcDir; app-image will not include docs/JSignPdf.pdf"
}

# Load shared JVM options from the single source of truth. The main launcher
# gets them as --java-options on the jpackage command line; JSignPdfC and
# InstallCert inherit them (their .properties files leave java-options unset);
# JSignPdf-swing gets a generated .properties file below that concatenates
# these options with '-Djsignpdf.swing=true'.
$commonJvmOptionsFile = Join-Path $jpkgCfg 'common-jvm-options.txt'
if (-not (Test-Path $commonJvmOptionsFile)) {
    throw "Missing shared JVM options file: $commonJvmOptionsFile"
}
$commonJvmOptions = @(
    Get-Content $commonJvmOptionsFile |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -and -not $_.StartsWith('#') }
)

$mainJvmOptions = @()
foreach ($opt in $commonJvmOptions) {
    $mainJvmOptions += @('--java-options', $opt)
}

# Generate JSignPdf-swing.properties at build time so the common JVM options
# never get duplicated. jpackage add-launcher properties replace (not merge
# with) the main launcher's java-options, so the Swing launcher must repeat
# the common set plus its own -Djsignpdf.swing=true.
$swingLauncherProps = Join-Path $generated 'JSignPdf-swing.properties'
$swingJavaOptions = (@('-Djsignpdf.swing=true') + $commonJvmOptions) -join ' '
@(
    '# Generated by build-windows-installers.ps1 — do not edit by hand.',
    'win-console=false',
    "java-options=$swingJavaOptions"
) | Set-Content -Path $swingLauncherProps -Encoding ASCII

# Generate InstallCert.properties at build time so it can carry an absolute
# path to Certificate.ico (jpackage resolves add-launcher icon paths relative
# to the current working directory, so the value is build-time-dependent).
$installCertLauncherProps = Join-Path $generated 'InstallCert.properties'
$certIconPath = (Join-Path $iconsDir 'Certificate.ico') -replace '\\','/'
@(
    '# Generated by build-windows-installers.ps1 — do not edit by hand.',
    'win-console=true',
    'main-jar=InstallCert.jar',
    'main-class=net.sf.jsignpdf.InstallCert',
    "icon=$certIconPath"
) | Set-Content -Path $installCertLauncherProps -Encoding ASCII

# 1. Build the app-image (one runtime, four launchers)
Write-Host "==> jpackage --type app-image"
$appImageArgs = @(
    '--type','app-image',
    '--input',$staging,
    '--main-jar','JSignPdf.jar',
    '--main-class','net.sf.jsignpdf.Signer',
    '--name','JSignPdf',
    '--app-version',$appVersion,
    '--vendor','Josef Cacek',
    '--copyright','Josef Cacek',
    '--description','JSignPdf adds digital signatures to PDF documents',
    '--icon',(Join-Path $root 'distribution/doc/icon/icons.ico'),
    '--dest',$out
) + $mainJvmOptions + @(
    '--add-launcher',"JSignPdf-swing=$swingLauncherProps",
    '--add-launcher',"JSignPdfC=$jpkgCfg/JSignPdfC.properties",
    '--add-launcher',"InstallCert=$installCertLauncherProps"
)
if ($guideFound) {
    $appImageArgs += @('--app-content', $guideContentDocs)
}
& jpackage @appImageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage app-image failed with exit code $LASTEXITCODE" }

$appImage = Join-Path $out 'JSignPdf'
if (-not (Test-Path $appImage)) { throw "Expected app-image directory not found: $appImage" }

# 2. Zip the app-image (portable distribution)
$zipPath = Join-Path $upload "JSignPdf-$Version-win-x64.zip"
Write-Host "==> Compress-Archive -> $zipPath"
if (Test-Path $zipPath) { Remove-Item -Force $zipPath }
Compress-Archive -Path $appImage -DestinationPath $zipPath

# 3. & 4. Build EXE and MSI installers from the app-image
$installerArgs = @(
    '--app-image',$appImage,
    '--name','JSignPdf',
    '--app-version',$appVersion,
    '--vendor','Josef Cacek',
    '--copyright','Josef Cacek',
    '--description','JSignPdf adds digital signatures to PDF documents',
    '--license-file',(Join-Path $root 'distribution/licenses/MPL-2.0.txt'),
    '--about-url','https://intoolswetrust.github.io/jsignpdf/',
    '--win-menu',
    '--win-menu-group','JSignPdf',
    '--win-shortcut',
    '--win-dir-chooser',
    '--win-upgrade-uuid','7b3d6e4c-9a51-4a8b-9b1c-7e8c1a4d2f10',
    '--dest',$out
)

Write-Host "==> jpackage --type exe"
& jpackage --type exe @installerArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage exe failed with exit code $LASTEXITCODE" }

Write-Host "==> jpackage --type msi"
& jpackage --type msi @installerArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage msi failed with exit code $LASTEXITCODE" }

Move-Item (Join-Path $out "JSignPdf-$appVersion.exe") (Join-Path $upload "JSignPdf-$Version-win-x64.exe") -Force
Move-Item (Join-Path $out "JSignPdf-$appVersion.msi") (Join-Path $upload "JSignPdf-$Version-win-x64.msi") -Force

Write-Host ""
Write-Host "Done. Artifacts:"
Get-ChildItem $upload | ForEach-Object { Write-Host "  $($_.Name)" }
