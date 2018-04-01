#!/usr/bin/env bash

TARGET=chainspacecore/target/node-dist

UBER_JAR=`ls chainspacecore/target/chainspace*-with-dependencies.jar`
BFT_JAR=`ls chainspacecore/lib/bft-smart*-UCL.jar`
DIST_TEMPLATE=contrib/core-tools/node-dist-template

echo -e "\nGoing to build a distribution zip file in [${TARGET}] ...\n"

rm -rf ${TARGET}
mkdir -p ${TARGET}

echo "Copying Jar file from [${UBER_JAR}]"

cp ${UBER_JAR} ${TARGET}

mkdir -p ${TARGET}/lib

cp ${BFT_JAR} ${TARGET}/lib
cp -r ${DIST_TEMPLATE}/* ${TARGET}

echo -e "\nls ${TARGET}\n"
tree ${TARGET}

echo ""