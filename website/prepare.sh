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
  cat "${SRC_ADOC}"
} > "${DEST_GUIDE}/index.adoc"
rm -rf "${DEST_GUIDE}/img"
cp -r  "${SRC_GUIDE_IMG}" "${DEST_GUIDE}/img"

echo "Prepared ${DEST_GUIDE}"
