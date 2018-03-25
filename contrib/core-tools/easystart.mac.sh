#!/bin/bash
rm -rf chainspacecore-*

BFT_SMART_LIB=lib/bft-smart-1.2-UCL.jar

cp -r chainspacecore chainspacecore-0-0
cd chainspacecore-0-0
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 0\nthisReplica 0" > ChainSpaceConfig/config.txt
cp config/hosts.config.0 config/hosts.config
screen -dmSL s0n0 java -Dchecker.start.port=13010 -cp ${BFT_SMART_LIB}:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
sleep 1

cp -r chainspacecore chainspacecore-0-1
cd chainspacecore-0-1
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 0\nthisReplica 1" > ChainSpaceConfig/config.txt
cp config/hosts.config.0 config/hosts.config
screen -dmSL s0n1 java -Dchecker.start.port=14010 -cp ${BFT_SMART_LIB}:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
sleep 1

cp -r chainspacecore chainspacecore-0-2
cd chainspacecore-0-2
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 0\nthisReplica 2" > ChainSpaceConfig/config.txt
cp config/hosts.config.0 config/hosts.config
screen -dmSL s0n2 java -Dchecker.start.port=15010 -cp ${BFT_SMART_LIB}:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
sleep 1

cp -r chainspacecore chainspacecore-0-3
cd chainspacecore-0-3
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 0\nthisReplica 3" > ChainSpaceConfig/config.txt
cp config/hosts.config.0 config/hosts.config
screen -dmSL s0n3 java -Dchecker.start.port=16010 -cp ${BFT_SMART_LIB}:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
sleep 1

cp -r chainspacecore chainspacecore-1-0
cd chainspacecore-1-0
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 1\nthisReplica 0" > ChainSpaceConfig/config.txt
cp config/hosts.config.1 config/hosts.config
screen -dmSL s1n0 java -Dchecker.start.port=17010 -cp ${BFT_SMART_LIB}:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
sleep 1

cp -r chainspacecore chainspacecore-1-1
cd chainspacecore-1-1
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 1\nthisReplica 1" > ChainSpaceConfig/config.txt
cp config/hosts.config.1 config/hosts.config
screen -dmSL s1n1 java -Dchecker.start.port=18010 -cp ${BFT_SMART_LIB}:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
sleep 1

cp -r chainspacecore chainspacecore-1-2
cd chainspacecore-1-2
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 1\nthisReplica 2" > ChainSpaceConfig/config.txt
cp config/hosts.config.1 config/hosts.config
screen -dmSL s1n2 java -Dchecker.start.port=19010 -cp ${BFT_SMART_LIB}:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
sleep 1

cp -r chainspacecore chainspacecore-1-3
cd chainspacecore-1-3
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 1\nthisReplica 3" > ChainSpaceConfig/config.txt
cp config/hosts.config.1 config/hosts.config
screen -dmSL s1n3 java -Dchecker.start.port=20010 -cp ${BFT_SMART_LIB}:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
