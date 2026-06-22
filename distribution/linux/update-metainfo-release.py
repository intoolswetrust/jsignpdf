#!/usr/bin/env python3
"""Insert an AppStream <release> entry into the JSignPdf metainfo file.

Reads a release-notes Markdown file (see
distribution/doc/release-notes/README.md for the required format) and
prepends a matching <release> element to the <releases> list in the
metainfo XML, so Linux app catalogs (Flathub, distros) show the new
version. Run at release time, before the tag is cut.

The metainfo file is edited as text, not reparsed, so existing comments,
formatting, and unrelated entries are left byte-for-byte untouched.

Behaviour:
  * Pre-release versions (ALPHA/BETA/RC/MILESTONE) are skipped (exit 0,
    no change) -- they get a GitHub release but no AppStream entry.
  * If an entry for the version already exists, it is left as-is
    (idempotent re-runs).

Usage:
  update-metainfo-release.py --version 3.1.0 \
      --notes distribution/doc/release-notes/3.1.0.md \
      --metainfo distribution/linux/io.github.intoolswetrust.JSignPdf.metainfo.xml \
      [--date 2026-06-21]
"""

import argparse
import datetime
import html
import re
import sys

PRERELEASE_RE = re.compile(r"(ALPHA|BETA|RC|MILESTONE)", re.IGNORECASE)
# A code span `...` or a bold run **...**, matched left to right.
INLINE_RE = re.compile(r"`([^`]+)`|\*\*(.+?)\*\*")


def fail(msg):
    sys.stderr.write("::error::%s\n" % msg)
    sys.exit(1)


def inline(text):
    """Convert the allowed Markdown inline markup to AppStream markup.

    `code` -> <code>, **bold** -> <em>. All other text is XML-escaped.
    Bold runs are processed recursively so a stray code span inside bold
    still escapes correctly.
    """
    out = []
    pos = 0
    for m in INLINE_RE.finditer(text):
        out.append(html.escape(text[pos:m.start()], quote=False))
        if m.group(1) is not None:
            out.append("<code>%s</code>" % html.escape(m.group(1), quote=False))
        else:
            out.append("<em>%s</em>" % inline(m.group(2)))
        pos = m.end()
    out.append(html.escape(text[pos:], quote=False))
    return "".join(out)


def parse_notes(text):
    """Return (intro_paragraphs, bullets) from a release-notes file."""
    intro = []          # list of paragraph strings
    bullets = []        # list of bullet strings
    cur_para = []       # words of the paragraph being built
    in_list = False

    def flush_para():
        if cur_para:
            intro.append(" ".join(cur_para))
            cur_para.clear()

    for raw in text.splitlines():
        line = raw.strip()
        if line.startswith("# "):
            continue  # H1 title
        if line.startswith("## "):
            fail("'## headings' are not allowed in release notes "
                 "(see distribution/doc/release-notes/README.md): %r" % line)
        if line.startswith("- "):
            in_list = True
            flush_para()
            bullets.append(line[2:].strip())
        elif line == "":
            if not in_list:
                flush_para()
        else:
            if in_list:
                # continuation of the current bullet
                if not bullets:
                    fail("dangling continuation line before any bullet: %r" % line)
                bullets[-1] += " " + line
            else:
                cur_para.append(line)
    flush_para()

    if not intro:
        fail("no intro paragraph found in release notes")
    if not bullets:
        fail("no bullet list found in release notes")
    return intro, bullets


def build_entry(version, date, intro, bullets):
    tag = "JSignPdf_" + version.replace(".", "_")
    url = "https://github.com/intoolswetrust/jsignpdf/releases/tag/" + tag
    lines = []
    lines.append('    <release version="%s" date="%s">' % (version, date))
    lines.append('      <url type="details">%s</url>' % url)
    lines.append('      <description>')
    lines.append('        <p>Version %s</p>' % version)
    for para in intro:
        lines.append('        <p>%s</p>' % inline(para))
    lines.append('        <ul>')
    for b in bullets:
        lines.append('          <li>%s</li>' % inline(b))
    lines.append('        </ul>')
    lines.append('      </description>')
    lines.append('    </release>')
    return "\n".join(lines) + "\n"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--version", required=True)
    ap.add_argument("--notes", required=True)
    ap.add_argument("--metainfo", required=True)
    ap.add_argument("--date", default=datetime.date.today().isoformat(),
                    help="release date YYYY-MM-DD (default: today, UTC runner)")
    args = ap.parse_args()

    if PRERELEASE_RE.search(args.version):
        print("Pre-release %s: skipping AppStream metainfo update." % args.version)
        return

    with open(args.metainfo, encoding="utf-8") as f:
        xml = f.read()

    if ('version="%s"' % args.version) in xml:
        print("metainfo already has a <release> for %s: nothing to do."
              % args.version)
        return

    with open(args.notes, encoding="utf-8") as f:
        intro, bullets = parse_notes(f.read())

    entry = build_entry(args.version, args.date, intro, bullets)

    # Insert right after the <releases> opening tag, keeping newest first.
    new_xml, n = re.subn(r"(^[ \t]*<releases>[ \t]*\n)",
                         lambda m: m.group(1) + entry, xml,
                         count=1, flags=re.MULTILINE)
    if n != 1:
        fail("could not find a <releases> opening tag in %s" % args.metainfo)

    with open(args.metainfo, "w", encoding="utf-8") as f:
        f.write(new_xml)
    print("Added <release> for %s to %s" % (args.version, args.metainfo))


if __name__ == "__main__":
    main()
