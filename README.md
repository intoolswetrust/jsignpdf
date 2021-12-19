# JSignPdf

Project home-page: [jsignpdf.sourceforge.net](http://jsignpdf.sourceforge.net)

JSignPdf is a Java application which adds digital signatures to PDF documents. 
The application uses the OpenPDF library for PDF manipulations.

## Translations
Help to translate the project on Weblate platform: https://hosted.weblate.org/projects/jsignpdf/messages/

## Build

### ZIP distribution

* update files in `docs`:
  * ChangeLog.txt
  * ReleaseNotes.txt
* build the `jsignpdf`

```bash
mvn clean install
```

Resulting bits are located in the `distribution/target`

### Windows installer

```bash
docker pull kwart/innosetup
docker run -it --rm -v "$(pwd):/mnt" \
  -u $(id -u):$(id -g) kwart/innosetup \
  /mnt/distribution/windows/create-jsignpdf-installer.sh
```

## Deploy/Release

Deploy snapshots

```
mvn clean install deploy
```

Release

```bash
mvn -P release --batch-mode -Dtag=JSignPdf_2_0_0 release:prepare \
                 -DreleaseVersion=2.0.0 \
                 -DdevelopmentVersion=2.1.0-SNAPSHOT

mvn -P release --batch-mode release:perform

# and build the Windows installers too
cd target/checkout
docker pull kwart/innosetup
docker run -it --rm -v "$(pwd):/mnt" \
  -u $(id -u):$(id -g) kwart/innosetup \
  /mnt/distribution/windows/create-jsignpdf-installer.sh

# copy the bits to a new subdirectory in sftp://<user>@frs.sourceforge.net/home/frs/project/jsignpdf
# update version in sftp://<user>@frs.sourceforge.net/home/project-web/jsignpdf/htdocs

```

## Random

### Testing PKCS11 without a card reader

Use NSS keystore

```bash
echo "pass123+" > /tmp/newpass.txt
echo "dsadasdasdasdadasdasdasdasdsadfwerwerjfdksdjfksdlfhjsdk" > /tmp/noise.txt
mkdir /tmp/nssdb
MODUTIL_CMD="modutil -force -dbdir /tmp/nssdb"
$MODUTIL_CMD -create
$MODUTIL_CMD -changepw "NSS Certificate DB" -newpwfile /tmp/newpass.txt
certutil -S -v 240 -k rsa -n "CN=localhost"  -t "u,u,u" -x -s "CN=localhost" -d /tmp/nssdb -f /tmp/newpass.txt -z /tmp/noise.txt

# https://bugzilla.redhat.com/show_bug.cgi?id=1760437
touch /tmp/nssdb/secmod.db

cat <<EOT >conf/pkcs11.cfg
name=testPkcs11
nssLibraryDirectory=/usr/lib/x86_64-linux-gnu
nssSecmodDirectory=/tmp/nssdb
nssModule=keystore
EOT
```
