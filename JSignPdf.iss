[Files]
Source: {#DistDir}\*; DestDir: {app}; Flags: recursesubdirs; Components: base
Source: {#JreInstaller}; DestDir: {tmp}; DestName: java_setup.exe; Flags: deleteafterinstall; Components: java

[Components]
Name: base; Description: Program files; Flags: fixed; Types: custom compact full
Name: ooo; Description: OpenOffice.org Add-On; Types: full custom
Name: java; Description: Java Runtime Environment 6; Types: full

[Run]
Filename: {tmp}\java_setup.exe; StatusMsg: Installing Java Runtime Environment; Components: java
Filename: {app}\{#MyAppFilename}.oxt; Components: ooo; StatusMsg: Enabling OpenOffice.org Add-On; Flags: shellexec

[Setup]
AppName={#MyAppName}
AppId={#MyAppId}
AppVerName={#MyAppName} {#MyAppVersion}
DefaultDirName={pf}\{#MyAppName}
DefaultGroupName={#MyAppName}
LicenseFile=licenses\MPL-1.1.txt
OutputBaseFilename={#MyAppName}_setup_{#MyAppVersion}_wjre
VersionInfoVersion={#MyAppVersionWin}
VersionInfoCompany=Josef Cacek
VersionInfoDescription=JSignPdf adds digital signatures to PDF documents
AppPublisher=Josef Cacek
AppSupportURL=http://jsignpdf.sourceforge.net/
AppVersion={#MyAppVersion}

[Icons]
Name: {group}\JSignPdf {#MyAppVersion}; Filename: {app}\JSignPdf.exe; Components: ; WorkingDir: {app}
Name: {group}\JSignPdf Guide; Filename: {app}\docs\JSignPdf_signed.pdf; Components: 
Name: {group}\Uninstall {#MyAppName}; Filename: {uninstallexe}; Components: 

[UninstallDelete]
Name: {%USERPROFILE}\.JSignPdf; Type: files; Components: 

[Code]
//********** Check if application is already installed
function MyAppInstalled: Boolean;
begin
  Result := RegKeyExists(HKEY_LOCAL_MACHINE,
	'SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{#MyAppID}_is1');
end;

//********** If app already installed, uninstall it before setup.
function InitializeSetup(): Boolean;
var
  uninstaller: String;
  oldVersion: String;
  ErrorCode: Integer;
begin
  if not MyAppInstalled then begin
    Result := True;
    Exit;
  end;
  RegQueryStringValue(HKEY_LOCAL_MACHINE,
	'SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{#MyAppID}_is1',
	'DisplayName', oldVersion);
  if (MsgBox(oldVersion + ' is already installed, it has to be uninstalled before installation. Continue?',
	  mbConfirmation, MB_YESNO) = IDNO) then begin
	Result := False;
	Exit;
  end;

  RegQueryStringValue(HKEY_LOCAL_MACHINE,
	'SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{#MyAppID}_is1',
	'QuietUninstallString', uninstaller);
  Exec('>', uninstaller, '', SW_SHOW, ewWaitUntilTerminated, ErrorCode);
  if (ErrorCode <> 0) then begin
	MsgBox('Failed to uninstall previous version. . Please run {#MyAppName} uninstaller manually from Start menu or Control Panel and then run installer again.',
	 mbError, MB_OK );
	Result := False;
	Exit;
  end;

  Result := True;
end;
