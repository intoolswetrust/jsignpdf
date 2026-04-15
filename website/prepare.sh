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
ROOT_POM="${HERE}/../pom.xml"

if [ ! -f "${SRC_ADOC}" ]; then
  echo "ERROR: ${SRC_ADOC} not found" >&2
  exit 1
fi

# Resolve the JSignPdf version. Prefer an explicit JSIGNPDF_VERSION override
# (handy for CI or dry-runs), otherwise read it from the root pom.xml so the
# website stays in lock-step with the Maven build's ${project.version}.
if [ -n "${JSIGNPDF_VERSION:-}" ]; then
  VERSION="${JSIGNPDF_VERSION}"
elif [ -f "${ROOT_POM}" ]; then
  if ! command -v python3 >/dev/null 2>&1; then
    echo "ERROR: python3 not found — set JSIGNPDF_VERSION to bypass pom.xml parsing" >&2
    exit 1
  fi
  # Parse the root <version> (direct child of <project>) with a real XML
  # parser so namespaces and stray <version> tags in dependencies/plugins
  # don't trip us up.
  VERSION=$(python3 - "${ROOT_POM}" <<'PY'
import sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
ns = root.tag[1:root.tag.index('}')] if root.tag.startswith('{') else ''
elem = root.find('{%s}version' % ns) if ns else root.find('version')
if elem is None or not (elem.text or '').strip():
    sys.exit("could not find /project/version")
print(elem.text.strip())
PY
)
else
  echo "ERROR: JSIGNPDF_VERSION not set and ${ROOT_POM} not found" >&2
  exit 1
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
  printf -- 'title: "JSignPdf Quick Start Guide"\n'
  printf -- 'linkTitle: "Guide"\n'
  printf -- 'toc: false\n'
  printf -- 'excludeSearch: true\n'
  printf -- 'sidebar:\n'
  printf -- '  open: true\n'
  printf -- '---\n'
  sed "s|{jsignpdf-version}|${VERSION}|g" "${SRC_ADOC}"
} > "${DEST_GUIDE}/index.adoc"
rm -rf "${DEST_GUIDE}/img"
cp -r  "${SRC_GUIDE_IMG}" "${DEST_GUIDE}/img"

echo "Prepared ${DEST_GUIDE} (jsignpdf-version=${VERSION})"
