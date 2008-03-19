[Code]
function MyAppVersion(Param: String) : String;
begin
  Result := '0.5';
end;

[Files]
Source: dist\*; DestDir: {app}; Flags: recursesubdirs; Components: base
Source: doc\JSignPdf.pdf; DestDir: {app}\docs; Components: base
Source: licenses\*.txt; DestDir: {app}\docs; Components: base
Source: C:\install\Java & Eclipse\JDK\jre-6u5-windows-i586-p.exe; DestDir: {tmp}; Flags: deleteafterinstall; Components: java

[Components]
Name: base; Description: Program files; Flags: fixed; Types: custom compact full
Name: ooo; Description: OpenOffice.org Add-On; Types: full custom
Name: java; Description: Java Runtime Environment 1.6; Types: full

[Run]
Filename: {tmp}\jre-6u5-windows-i586-p.exe; StatusMsg: Installing Java Runtime Environment; Components: java
;Filename: {reg:HKLM\SOFTWARE\OpenOffice.org\UNO\InstallPath,}\unopkg.exe; Parameters: " add {app}\JSignPdf.oxt"; Components: ooo; StatusMsg: Enabling OpenOffice.org Add-On
Filename: {app}\JSignPdf.oxt; Components: ooo; StatusMsg: Enabling OpenOffice.org Add-On; Flags: shellexec

[Setup]
AppName=JSignPdf
AppVerName=JSignPdf {code:MyAppVersion}
DefaultDirName={pf}\JSignPdf
;SourceDir=c:\TEMP\JSignPdf_0_2
DefaultGroupName=JSignPdf
LicenseFile=licenses\MPL-1.1.txt
;OutputDir=C:\TEMP
OutputBaseFilename=JSignPdf_setup_{code:MyAppVersion}
VersionInfoVersion=0.5.0.0
VersionInfoCompany=Josef Cacek
VersionInfoDescription=JSignPdf adds digital signatures to PDF documents
AppPublisher=Josef Cacek
AppSupportURL=http://jsignpdf.sourceforge.net/
AppVersion={code:MyAppVersion}

[Icons]
Name: {group}\JSignPdf {code:MyAppVersion}; Filename: {app}\JSignPdf.exe; Components: ; WorkingDir: {app}
Name: {group}\JSignPdf Guide; Filename: {app}\docs\JSignPdf.pdf; Components: 
Name: {group}\Uninstall; Filename: {uninstallexe}; Components: 

[UninstallDelete]
Name: {%USERPROFILE}\.JSignPdf; Type: files; Components: 
