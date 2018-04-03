#!/usr/bin/env bash

DIST=chainspacecore/target/dist

NODE_0_0=${DIST}/node_0_0

UBER_JAR=`ls chainspacecore/target/chainspace*-with-dependencies.jar`
BFT_JAR=`ls chainspacecore/lib/bft-smart*-UCL.jar`
DIST_TEMPLATE=contrib/core-tools/node-dist-template
CONTRACT_DIR=chainspacecore/contracts

echo -e "\nGoing to build a distribution zip file in [${DIST}] ...\n"

rm -rf ${DIST}
mkdir -p ${DIST}
mkdir =p ${NODE_0_0}

echo "Copying Jar file from [${UBER_JAR}]"

cp ${UBER_JAR} ${NODE_0_0}

mkdir -p ${NODE_0_0}/lib


cp ${BFT_JAR} ${NODE_0_0}/lib
cp -r ${CONTRACT_DIR} ${NODE_0_0}
cp -r ${DIST_TEMPLATE}/* ${NODE_0_0}

echo -e "\nCreating client-api\n"

CLIENT_API=${DIST}/client-api
mkdir -p ${CLIENT_API}
cp -r ${NODE_0_0}/* ${CLIENT_API}/
rm -r ${CLIENT_API}/config/node
rm -r {NODE_0_0}/contracts
rm ${CLIENT_API}/start_node.sh



echo -e "\nCreating multiple nodes...\n"

rm ${NODE_0_0}/start_client_api.sh
rm -r ${NODE_0_0}/config/client-api

NODE_0_1=${DIST}/node_0_1
mkdir -p ${NODE_0_1}

cp -r ${NODE_0_0}/* ${NODE_0_1}/

sed -e "s/REPLICA_ID/0/g" ${NODE_0_0}/config/node/config.txt >> ${NODE_0_0}/config/node/config.txt
sed -e "s/REPLICA_ID/1/g" ${NODE_0_1}/config/node/config.txt >> ${NODE_0_1}/config/node/config.txt


echo -e "\nCreating client...\n"

echo -e "\ntree ${DIST}\n"
tree ${DIST}


echo ""