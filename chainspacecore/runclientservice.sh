#!/bin/bash



BFT_SMART_LIB=lib/bft-smart-1.2-UCL.jar


rm config/currentView
java -cp ${BFT_SMART_LIB}:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.Client ChainSpaceClientConfig/config.txt
