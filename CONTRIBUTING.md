# Contributing to JSignPdf

## Questions and support

Ask in the [JSignPdf Google Group](https://groups.google.com/g/jsignpdf). The issue tracker is for bugs and
feature requests, not usage questions — check the [user guide](https://jsignpdf.eu/docs/) first.

## Bugs and feature requests

Open an [issue](https://github.com/intoolswetrust/jsignpdf/issues). For a bug report, include:

- JSignPdf version and how it was installed (MSI, DEB, RPM, DMG, Flatpak, Homebrew, ZIP)
- operating system, and `java -version` when running from a cross-platform ZIP
- the exact command line, or the steps taken in the UI
- the full error message or the Output console content
- a sample PDF when the document itself is the problem — only one you can share publicly

Do not report security vulnerabilities in a public issue; email <josef@cacek.cz> instead.

## Pull requests

- Branch off `master`, one topic per pull request.
- `mvn clean install` must pass — it runs Checkstyle and the unit tests.
- Follow the surrounding code style; no unrelated reformatting.
- Update [`website/docs/JSignPdf.adoc`](website/docs/JSignPdf.adoc) when behaviour or command-line options change.
- [AGENTS.md](AGENTS.md) describes the module layout and test commands.

## Translations

Translations are maintained on [Weblate](https://hosted.weblate.org/projects/jsignpdf/messages/), which commits
to this repository directly. Edit them there rather than sending a pull request against
`engines/api/src/main/resources/net/sf/jsignpdf/translations/`. English source strings (`messages.properties`)
are the exception — those change with the code.
