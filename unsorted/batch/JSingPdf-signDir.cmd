@echo off
REM use with cmd "/V:ON"

set FILES=
for %%i in (%1\*.pdf) do set FILES=!FILES! "%%i"
SHIFT
echo JSignPdfC.exe %FILES% %1 %2 %3 %4 %5 %6 %7 %8
JSignPdfC.exe %FILES% %1 %2 %3 %4 %5 %6 %7 %8
