# Security policy

## Reporting a vulnerability

Report security vulnerabilities privately through GitHub:
[**Report a vulnerability**](https://github.com/intoolswetrust/jsignpdf/security/advisories/new).

Do not open a public issue, and do not post the details to the Google Group — a public report gives
everyone the exploit before there is a fix.

Please include:

- the JSignPdf version, and the signing engine (`openpdf` or `dss`) when it matters
- how it was installed (MSI, DEB, RPM, DMG, Flatpak, Homebrew, ZIP) and the Java version in use
- steps to reproduce, ideally with a sample PDF or keystore you can share publicly
- what an attacker gains — a forged or wrongly-validated signature, a leaked key or password,
  code execution, and so on

If you cannot use GitHub advisories, e-mail <josef@cacek.cz> instead.

JSignPdf is maintained by volunteers, so please allow some time for a reply. You will be credited in
the advisory unless you ask otherwise, and kept informed while a fix is prepared.

## Supported versions

Fixes land in the next release built from `master`; there are no maintenance branches for older
versions. Check the [releases page](https://github.com/intoolswetrust/jsignpdf/releases) before
reporting — the issue may already be fixed.

## Scope

In scope: anything that lets an attacker forge a signature, make JSignPdf accept an invalid one,
expose a private key or keystore password, or execute code through a crafted PDF or configuration.

Out of scope: weaknesses of a certificate or keystore you chose yourself (self-signed certificates,
weak passwords, expired or untrusted CAs), and vulnerabilities in a third-party PDF reader that
happens to render a JSignPdf output.
