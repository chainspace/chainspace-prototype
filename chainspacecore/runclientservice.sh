#!/bin/bash

rm config/currentView
java -cp lib/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.Client ChainSpaceClientConfig/config.txt
