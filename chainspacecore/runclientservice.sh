#!/bin/bash



BFT_SMART_LIB=lib/bft-smart-1.2-UCL.jar
CLIENT_API_DB=chainspacecore-0-0/database

rm config/currentView
java -Dclient.api.database=${CLIENT_API_DB} -cp ${BFT_SMART_LIB}:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.Client ChainSpaceClientConfig/config.txt
