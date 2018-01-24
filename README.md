# JSignPDF

Project home-page: [jsignpdf.sourceforge.net](http://jsignpdf.sourceforge.net)

JSignPdf is a Java application which adds digital signatures to PDF documents. 
It can be used as a standalone application or as an Add-On in OpenOffice.org. 
The application uses the jsignpdf-itxt library for PDF manipulations 
(based on iText library in version 2.1.7 with LGPL/MPL license). 
JSignPdf is open-source software and can be freely used in both private and business sectors.

## Build/Release process

* install `genisoimage`
* install InnoSetup installer (use `wine`) in version 5.4.x (http://files.jrsoftware.org/is/5/)
  * check if `unsorted/iscc` script contains correct path
* update files in `docs`:
  * ChangeLog.txt
  * ReleaseNotes.txt
  * JSignPdf.odt (version field)
* update build.properties
* build the `jsignpdf` (the `jsignpdf-itxt` is built automatically)
```
ant clean all
```

The result bits are located in the `Output` directory.
