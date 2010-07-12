@echo off

IF "%1a"=="a" GOTO PrintHelp

cmd "/V:ON" /C JSingPdf-signDir.cmd %1 %2 %3 %4 %5 %6 %7 %8 %9

GOTO End

:PrintHelp
echo "Command line parameter is missing"
echo ""
echo "Usage:"
echo "signPdfDir.bat <C:\Path\To\Pdf\Folder> <argumentForJSignPdf-1> <argumentForJSignPdf-2> ..."


:End

pause