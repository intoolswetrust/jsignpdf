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

$root    = (Resolve-Path "$PSScriptRoot/../..").Path
$target  = Join-Path $root 'distribution/target'
$staging = Join-Path $target 'jpackage-staging'
$out     = Join-Path $target 'jpackage-out'
$upload  = Join-Path $target 'upload'
$jpkgCfg = Join-Path $root 'distribution/jpackage'

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

foreach ($d in @($staging, $out)) {
    if (Test-Path $d) { Remove-Item -Recurse -Force $d }
}
New-Item -ItemType Directory -Force -Path $staging,$out,$upload | Out-Null

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

$mainJvmOptions = @(
    '--java-options','-Xms1g',
    '--java-options','-Xmx1g',
    '--java-options','--add-exports=jdk.crypto.cryptoki/sun.security.pkcs11=ALL-UNNAMED',
    '--java-options','--add-exports=jdk.crypto.cryptoki/sun.security.pkcs11.wrapper=ALL-UNNAMED',
    '--java-options','--add-exports=java.base/sun.security.action=ALL-UNNAMED',
    '--java-options','--add-exports=java.base/sun.security.rsa=ALL-UNNAMED',
    '--java-options','--add-opens=java.base/sun.security.util=ALL-UNNAMED'
)

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
    '--add-launcher',"JSignPdf-swing=$jpkgCfg/JSignPdf-swing.properties",
    '--add-launcher',"JSignPdfC=$jpkgCfg/JSignPdfC.properties",
    '--add-launcher',"InstallCert=$jpkgCfg/InstallCert.properties"
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
