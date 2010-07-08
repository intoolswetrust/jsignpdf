REM use with cmd "/V:ON"

set FILES=
for %%i in (%1\*.pdf) do set FILES=!FILES! "%%i"
java -Duser.language=en -jar JSignPdf.jar -kst WINDOWS-MY -d "%1" %FILES%
