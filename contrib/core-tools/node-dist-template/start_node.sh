#!/usr/bin/env bash

echo -e "\nStarting Chainspace Node...\n"

BFT_SMART_LIB=lib/bft-smart-1.2-UCL.jar
CHAINSPACE_JAR=chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar
CHECKER_START_PORT=17010

java -Dchecker.start.port=${CHECKER_START_PORT} -cp ${BFT_SMART_LIB}:${CHAINSPACE_JAR} uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer config/node/config.txt