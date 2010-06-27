keytool -delete -alias %KEY_ALG%%KEY_SIZE% -keypass %KEY_ALG%%KEY_SIZE%pass -keystore test-keystore.jks -storepass keystorepass
keytool -genkeypair -keyalg %KEY_ALG% -keysize %KEY_SIZE% -dname "cn=%KEY_ALG% %KEY_SIZE%, ou=PDF support, o=JSignPdf s.r.o., c=CZ" -alias %KEY_ALG%%KEY_SIZE% -keypass %KEY_ALG%%KEY_SIZE%pass -keystore test-keystore.jks -storepass keystorepass -validity 10000
