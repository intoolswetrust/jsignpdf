# Release notes

One Markdown file per release, named `<base-version>.md` (the version
without any `-BETA`/`-RC` suffix), e.g. `3.1.0.md`. Each file is the
single source of truth for that release and is consumed three ways:

1. **GitHub Release body** — passed to `gh release create --notes-file`.
2. **`README.md` shipped inside the release assets.**
3. **AppStream `<release>` entry** in
   `distribution/linux/io.github.intoolswetrust.JSignPdf.metainfo.xml`,
   generated at release time by
   [`distribution/linux/update-metainfo-release.py`](../../linux/update-metainfo-release.py).

Sink 3 is the strict one: AppStream `<description>` markup only allows
`<p>`, `<ul>`/`<ol>`/`<li>`, `<em>`, and `<code>`, and `appstreamcli
validate` (run in the release workflow and again by Flathub) rejects
anything else. To keep one file usable for all three sinks, follow the
format below.

## Format

```markdown
# Version X.Y.Z

One short intro paragraph summarising the release. Plain prose, no links.
(A blank line starts a second intro paragraph if you really need one.)

- **Bold lead-in** followed by a sentence or two describing the change.
- Another item. Inline `code` and **bold** are the only markup allowed.
- Items may wrap across multiple source lines; a new item starts with `- `.
```

Rules that keep the file convertible to AppStream:

- **H1 title**, then one (or more) intro paragraphs, then a single flat
  bullet list. Nothing after the list.
- **No links** — neither `[text](url)` nor bare URLs. AppStream forbids
  them in descriptions. Point to the GitHub release/issues from prose
  instead (e.g. "see issue 307") without a hyperlink.
- **No `##` headings** and **no nested / indented sub-bullets** — the
  list must be flat (an AppStream `<li>` cannot contain a sub-list).
- **Inline markup is limited to `**bold**` and `` `code` ``.** Do not
  wrap a code span in bold (`**`like this`**`); pick one.
- Bold renders as `<em>` in AppStream (AppStream has no bold), and
  `` `code` `` renders as `<code>`. `<`, `>`, and `&` in the text are
  XML-escaped automatically — write them literally (e.g.
  `jsignpdf-<version>.zip`).

## What the generator adds

The `version`, `date`, and the `<url type="details">` link to the
GitHub release tag are injected by the workflow — do **not** put them in
the Markdown. Pre-release versions (`-ALPHA`/`-BETA`/`-RC`/`-MILESTONE`)
are skipped: they get a GitHub release but no AppStream entry.
