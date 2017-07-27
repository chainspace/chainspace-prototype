#!/bin/bash

cp -r ../src/ ~/Desktop/shard0/TreeMapServer0/
cd ~/Desktop/shard0/TreeMapServer0/
mvn package assembly:single
find . -name "currentView" -type f -delete
java -cp target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar:bin/BFT-SMaRt.jar foo.gettingstarted.server.TreeMapServer ChainSpaceConfig/config.txt