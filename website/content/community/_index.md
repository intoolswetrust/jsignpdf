---
linkTitle: "Community"
title: Community
type: docs
sidebar:
  hide: true
---

JSignPdf is free software maintained by volunteers. There is no support desk — everything happens
in the open, and help is always welcome.

## Getting help

- [JSignPdf Google Group](https://groups.google.com/g/jsignpdf) — questions about signing,
  keystores, timestamps and troubleshooting. Worth searching before asking: most problems have
  come up before.
- [User guide](../docs/) — the reference for every GUI panel and command-line option.
- [Release notes](../releases/) — what changed in each version.

## Reporting bugs and requesting features

Use the [issue tracker](https://github.com/intoolswetrust/jsignpdf/issues). A good bug report has:

- the JSignPdf version and how it was installed (MSI, DEB, RPM, DMG, Flatpak, Homebrew, ZIP)
- the operating system, and `java -version` when running from a cross-platform ZIP
- the exact command line, or the steps taken in the UI
- the full error message or the Output console content
- a sample PDF when the document itself is the problem — only one you can share publicly

Usage questions belong in the Google Group rather than the tracker.

**Security vulnerabilities** are the exception: report them privately through
[GitHub security advisories](https://github.com/intoolswetrust/jsignpdf/security/advisories/new)
rather than a public issue. The
[security policy](https://github.com/intoolswetrust/jsignpdf/blob/master/SECURITY.md) says what to
include and what is in scope.

## Contributing

- **Code** — pull requests are welcome. Branch off `master`, keep one topic per pull request, and
  make sure `mvn clean install` passes; it runs Checkstyle and the unit tests. See
  [CONTRIBUTING.md](https://github.com/intoolswetrust/jsignpdf/blob/master/CONTRIBUTING.md).
- **Translations** — JSignPdf ships in 20 languages, maintained on
  [Weblate](https://hosted.weblate.org/projects/jsignpdf/messages/). No development environment
  needed, and new languages are always welcome.
- **Documentation** — every page on this site has an _Edit this page_ link that opens the source
  file on GitHub.
- **Spread the word** — starring the [repository](https://github.com/intoolswetrust/jsignpdf) and
  writing about how you use JSignPdf both help people find it.
