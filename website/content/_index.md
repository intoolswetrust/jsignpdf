---
title: JSignPdf
layout: hextra-home
toc: false
---

{{< hextra/hero-badge >}}
  <div class="hx:w-2 hx:h-2 hx:rounded-full hx:bg-primary-400"></div>
  <span>Free · Open source · Java · Cross-platform</span>
{{< /hextra/hero-badge >}}

<div class="hx:mt-6 hx:mb-6">
{{< hextra/hero-headline >}}
  Sign your PDFs.&nbsp;<br class="sm:hx:block hx:hidden" />Free. Forever.
{{< /hextra/hero-headline >}}
</div>

<div class="hx:mb-12">
{{< hextra/hero-subtitle >}}
  A desktop GUI and command-line tool for digital PDF signatures.&nbsp;<br class="sm:hx:block hx:hidden" />
  No subscription. No telemetry. No vendor lock-in.
{{< /hextra/hero-subtitle >}}
</div>

<div class="hx:flex hx:flex-wrap hx:justify-center hx:items-center hx:gap-4 hx:mb-6">
  <a class="jsp-download-btn" href="https://github.com/intoolswetrust/jsignpdf/releases/latest" target="_blank" rel="noreferrer">
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" aria-hidden="true">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 0 0 3 3h10a3 3 0 0 0 3-3v-1m-4-4-4 4m0 0-4-4m4 4V4"/>
    </svg>
    Download latest release
  </a>
  <a class="jsp-secondary-btn" href="docs/guide/">
    Read the Guide
  </a>
</div>

<p class="jsp-meta">
  Runs on Windows, macOS and Linux · Java 11+ · LGPL / MPL licensed
</p>

<div class="jsp-hero-showcase">
  <img src="img/screenshots/jsignpdf-javafx-main.png" alt="JSignPdf JavaFX application — document loaded with signing options"/>
</div>

<h2 class="jsp-section-heading">Built for real-world signing</h2>
<p class="jsp-section-sub">Everything you need to sign professional PDF documents — nothing you don't.</p>

{{< hextra/feature-grid >}}
  {{< hextra/feature-card
      icon="eye"
      title="Visible signatures"
      subtitle="Place a signature block on any page with a custom image and text layout." >}}
  {{< hextra/feature-card
      icon="clock"
      title="Timestamps & revocation"
      subtitle="TSA support plus CRL and OCSP revocation checks built in." >}}
  {{< hextra/feature-card
      icon="shield-check"
      title="Certification levels"
      subtitle="Control what changes are permitted after the document has been signed." >}}
  {{< hextra/feature-card
      icon="lock-closed"
      title="PDF encryption"
      subtitle="Set access rights and encrypt the output with industry-standard algorithms." >}}
  {{< hextra/feature-card
      icon="key"
      title="Hardware tokens"
      subtitle="Sign with PKCS#11 smart cards, HSMs and USB tokens — not just soft keys." >}}
  {{< hextra/feature-card
      icon="sparkles"
      title="Free and open source"
      subtitle="LGPL / MPL licensed. Free for personal and commercial use, no strings attached." >}}
{{< /hextra/feature-grid >}}

<h2 class="jsp-section-heading">Two ways to use it</h2>
<p class="jsp-section-sub">Pick whichever fits your workflow — or use both.</p>

<div class="jsp-ways">
  <div class="jsp-way">
    <h3 class="jsp-way-title">
      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17 9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 0 0 2-2V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2Z"/>
      </svg>
      Desktop application
    </h3>
    <p>Launch the Java GUI, pick a keystore, drop a PDF in and click Sign. Switch to the advanced view when you need timestamps, certification levels or visible signatures.</p>
    <a class="jsp-way-link" href="docs/guide/#launching">Start with the GUI →</a>
  </div>

  <div class="jsp-way">
    <h3 class="jsp-way-title">
      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m7 8 4 4-4 4m6 0h5M5 4h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z"/>
      </svg>
      Command line
    </h3>
    <p>Automate signing in scripts, CI pipelines, or keep a hot folder watched for incoming PDFs.</p>
<pre><code>jsignpdf -kst PKCS12 -ksf my.p12 \
         -ksp "$PASS" -d output/ \
         contract.pdf</code></pre>
    <a class="jsp-way-link" href="docs/guide/#command-line-batch-mode">CLI reference →</a>
  </div>
</div>

<div class="jsp-closing-cta">
  <a class="jsp-download-btn" href="https://github.com/intoolswetrust/jsignpdf/releases/latest" target="_blank" rel="noreferrer">
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" aria-hidden="true">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 0 0 3 3h10a3 3 0 0 0 3-3v-1m-4-4-4 4m0 0-4-4m4 4V4"/>
    </svg>
    Download from GitHub Releases
  </a>
  <small>Binaries and installers for every platform</small>
</div>
