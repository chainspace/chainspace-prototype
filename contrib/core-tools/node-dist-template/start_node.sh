#!/usr/bin/env bash

echo -e "\nStarting Chainspace Node...\n"

BFT_SMART_LIB=lib/bft-smart-1.2-UCL.jar
CHAINSPACE_JAR=chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar
CHECKER_START_PORT=__START_PORT__
MAIN_CLASS=uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer
CONFIG_FILE=config/node/config.txt
PYTHON_BIN=../../../../.chainspace.env/bin/python

java -Dchecker.python.bin=${PYTHON_BIN} -Dchecker.start.port=${CHECKER_START_PORT} -cp ${BFT_SMART_LIB}:${CHAINSPACE_JAR} ${MAIN_CLASS} ${CONFIG_FILE}