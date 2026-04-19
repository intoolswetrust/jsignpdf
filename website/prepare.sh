#!/bin/sh
# Copies the AsciiDoc guide and its images from website/docs/ into the
# Hugo content tree as a leaf bundle. The destination paths are gitignored,
# so website/docs/JSignPdf.adoc remains the single source of truth (also
# consumed by the Maven asciidoctor-pdf build in distribution/).
#
# Run before `hugo serve` or `hugo build`. The CI workflow runs it too.
# POSIX sh — runs in busybox/alpine containers without bash.
set -eu

HERE="$(cd "$(dirname "$0")" && pwd)"
SRC_ADOC="${HERE}/docs/JSignPdf.adoc"
SRC_GUIDE_IMG="${HERE}/docs/img"
DEST_GUIDE="${HERE}/content/docs/guide"

if [ ! -f "${SRC_ADOC}" ]; then
  echo "ERROR: ${SRC_ADOC} not found" >&2
  exit 1
fi

# Resolve the JSignPdf version to show in the guide.
#   1. $JSIGNPDF_VERSION — explicit override (offline dev, tight edit loops)
#   2. GitHub Releases API — tag_name of the latest published release
# The Releases API is authoritative: do-release.yml uploads the built
# binaries to that release, so "latest release" is exactly what users can
# download. Reading pom.xml would leak the in-progress -SNAPSHOT instead.
# If $GITHUB_TOKEN is set (CI), it bumps the rate limit from 60/hr to
# 1000/hr — not required for correctness.
if [ -n "${JSIGNPDF_VERSION:-}" ]; then
  VERSION="${JSIGNPDF_VERSION}"
else
  if ! command -v python3 >/dev/null 2>&1; then
    echo "ERROR: python3 not found — set JSIGNPDF_VERSION to bypass the GitHub API lookup" >&2
    exit 1
  fi
  if ! VERSION=$(python3 - <<'PY'
import json, os, sys, urllib.request
url = "https://api.github.com/repos/intoolswetrust/jsignpdf/releases/latest"
headers = {
    "Accept": "application/vnd.github+json",
    "User-Agent": "jsignpdf-prepare",
}
token = os.environ.get("GITHUB_TOKEN")
if token:
    headers["Authorization"] = "Bearer " + token
try:
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=10) as resp:
        tag = json.load(resp)["tag_name"]
except Exception as e:
    sys.exit("failed to fetch latest release: %s" % e)
# Release tags look like "JSignPdf_2_3_3" — normalize to "2.3.3".
if tag.startswith("JSignPdf_"):
    tag = tag[len("JSignPdf_"):]
print(tag.replace("_", "."))
PY
  ); then
    echo "ERROR: could not resolve version from GitHub — set JSIGNPDF_VERSION to override" >&2
    exit 1
  fi
fi

if [ -z "${VERSION}" ]; then
  echo "ERROR: could not determine JSignPdf version" >&2
  exit 1
fi

# Guide page bundle: index.adoc + img/ resources.
# Prepend a Hugo YAML front-matter block so the page gets a stable URL
# slug ("guide") and so we can opt out of Hextra's sidebar TOC — Hugo's
# Page.Fragments isn't populated for asciidoc-rendered content, which
# trips up Hextra's TOC partial.
mkdir -p "${DEST_GUIDE}"
{
  printf -- '---\n'
  printf -- 'title: "JSignPdf User Guide"\n'
  printf -- 'linkTitle: "Guide"\n'
  printf -- 'excludeSearch: true\n'
  printf -- 'sidebar:\n'
  printf -- '  open: true\n'
  printf -- '---\n'
  sed "s|{jsignpdf-version}|${VERSION}|g" "${SRC_ADOC}"
} > "${DEST_GUIDE}/index.adoc"
rm -rf "${DEST_GUIDE}/img"
cp -r  "${SRC_GUIDE_IMG}" "${DEST_GUIDE}/img"

echo "Prepared ${DEST_GUIDE} (jsignpdf-version=${VERSION})"
