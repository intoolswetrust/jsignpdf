#!/bin/bash

set -e -x

if [ ! -d /mnt/jsignpdf -o ! -d /mnt/distribution ]; then
  echo "JSignPdf expected to be mounted to /mnt" >&2
  exit 2;
fi

SRCPATH=$(ls /mnt/distribution/target/jsignpdf-*.zip)
DIRNAME=$(dirname "$(readlink -e "$SRCPATH")")
DIR=$(cd "$DIRNAME" || exit 112; pwd)

export PATH=$PATH:/opt/launch4j:/opt/ant/bin

BUILDDIR=/tmp/build

if [ -d ${BUILDDIR} ]; then
  rm -rf ${BUILDDIR}
fi

mkdir -p ${BUILDDIR}
# workaround for running WINE with a custom user
export HOME=${BUILDDIR}
cp -r /root/.wine ${BUILDDIR}

unzip -q "${SRCPATH}" -d ${BUILDDIR}
if [ ! -d ${BUILDDIR}/jsignpdf-* ]; then
  echo "The zip $SRCPATH doesn't contain jsignpdf-* folder" >&2
  exit 3;
fi

DISTNAME=$(basename ${BUILDDIR}/jsignpdf-*)
VERSION=$(echo $DISTNAME | sed s/jsignpdf-[a-zA-Z\-]*//)
WINVERSION=$(echo $VERSION | sed 's/\([0-9\.]*\).*/\1/')
while [ $(echo $WINVERSION |tr '.' '\n'|wc -l) -lt 4 ]; do
  WINVERSION="${WINVERSION}.0"
done

echo "WINVERSION=${WINVERSION}"

TARGET=${BUILDDIR}/jsignpdf
mv "${BUILDDIR}/${DISTNAME}" ${TARGET}

cp -r /opt/jre{32,64} ${TARGET}/
ls -al ${TARGET}

# Use Launch4j as the ant task because if used directly it fails to expand variables properly
ant -f /mnt/distribution/windows/ant-build-create-launchers.xml \
  "-Djsignpdf.version=${VERSION}" \
  "-Djsignpdf.winversion=${WINVERSION}" \
  "-Dbuild.dir=${BUILDDIR}"

isscdistdir=$(winepath -w "${TARGET}")
isscoutputdir=$(winepath -w "${DIR}")
scriptpath=$(winepath -w "/mnt/distribution/windows/JSignPdf.iss")
iscc /O+ "/DDistDir=${isccdistdir}" "/DOutputDir=${isccoutputdir}" \
  /DMyAppName=JSignPdf /DMyAppVersion=${VERSION} /DMyAppVersionWin=${WINVERSION} \
  /DMyAppId=JSignPdf "/DDistDir=${isscdistdir}" "/DOutputDir=${isscoutputdir}" \
  "${scriptpath}"
