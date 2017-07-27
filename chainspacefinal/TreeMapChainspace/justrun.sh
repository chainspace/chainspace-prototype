#!/bin/bash
find . -name "currentView" -type f -delete

cp -r TreeMapServer TreeMapServer0
cd TreeMapServer0
cp config/hosts.config.0 config/hosts.config
screen -dmS s0n0 java -cp bin/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar foo.gettingstarted.server.TreeMapServer ChainSpaceConfig/config-0-0.txt
screen -dmS s0n1 java -cp bin/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar foo.gettingstarted.server.TreeMapServer ChainSpaceConfig/config-0-1.txt
screen -dmS s0n2 java -cp bin/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar foo.gettingstarted.server.TreeMapServer ChainSpaceConfig/config-0-2.txt
screen -dmS s0n3 java -cp bin/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar foo.gettingstarted.server.TreeMapServer ChainSpaceConfig/config-0-3.txt

cd ../

cp -r TreeMapServer TreeMapServer1
cd TreeMapServer1
cp config/hosts.config.1 config/hosts.config
screen -dmS s1n0 java -cp bin/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar foo.gettingstarted.server.TreeMapServer ChainSpaceConfig/config-1-0.txt
screen -dmS s1n1 java -cp bin/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar foo.gettingstarted.server.TreeMapServer ChainSpaceConfig/config-1-1.txt
screen -dmS s1n2 java -cp bin/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar foo.gettingstarted.server.TreeMapServer ChainSpaceConfig/config-1-2.txt
screen -dmS s1n3 java -cp bin/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar foo.gettingstarted.server.TreeMapServer ChainSpaceConfig/config-1-3.txt

cd ../
