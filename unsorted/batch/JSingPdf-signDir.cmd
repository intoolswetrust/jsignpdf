@echo off
REM use with cmd "/V:ON"

set FILES=
for %%i in (%1\*.pdf) do set FILES=!FILES! "%%i"
SHIFT
echo java -Duser.language=en -jar JSignPdf.jar %FILES% %1 %2 %3 %4 %5 %6 %7 %8
java -Duser.language=en -jar JSignPdf.jar %FILES% %1 %2 %3 %4 %5 %6 %7 %8
