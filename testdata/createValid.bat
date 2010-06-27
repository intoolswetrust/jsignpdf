SET KEY_ALG=RSA
SET KEY_SIZE=1024

call genTestKey.bat

SET KEY_SIZE=2048

call genTestKey.bat

SET KEY_SIZE=4096

call genTestKey.bat

SET KEY_ALG=DSA
SET KEY_SIZE=1024

call genTestKey.bat

rem expired
rem keytool -genkeypair -keyalg RSA -keysize 2048 -dname "cn=Expired Key, ou=PDF support, o=JSignPdf s.r.o., c=CZ" -alias expired -keypass expiredpass -keystore test-keystore.jks -storepass keystorepass -validity 90
pause