#!/bin/bash
cd TreeMapClient
java -cp bin/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar foo.gettingstarted.client.ConsoleClient ChainSpaceConfig/config.txt
