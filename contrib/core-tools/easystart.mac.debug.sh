#!/bin/bash

#Starts a simplified setup with only 1 shard and 1 node
# Starts nodes 0-0 and 0-1 in debug mode so you can attach to them from your ide.
rm -rf chainspacecore-*

cp -r chainspacecore chainspacecore-0-0
cd chainspacecore-0-0
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 0\nthisReplica 0" > ChainSpaceConfig/config.txt
cp config/hosts.config.0 config/hosts.config
screen -dmSL s0n0 java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044 -cp lib/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
sleep 1


cp -r chainspacecore chainspacecore-0-1
cd chainspacecore-0-1
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 0\nthisReplica 1" > ChainSpaceConfig/config.txt
cp config/hosts.config.0 config/hosts.config
screen -dmSL s0n1 java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1045 -cp lib/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
sleep 1

cp -r chainspacecore chainspacecore-0-2
cd chainspacecore-0-2
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 0\nthisReplica 2" > ChainSpaceConfig/config.txt
cp config/hosts.config.0 config/hosts.config
screen -dmSL s0n2 java -cp lib/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
sleep 1

cp -r chainspacecore chainspacecore-0-3
cd chainspacecore-0-3
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 0\nthisReplica 3" > ChainSpaceConfig/config.txt
cp config/hosts.config.0 config/hosts.config
screen -dmSL s0n3 java -cp lib/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
sleep 1

cp -r chainspacecore chainspacecore-1-0
cd chainspacecore-1-0
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 1\nthisReplica 0" > ChainSpaceConfig/config.txt
cp config/hosts.config.1 config/hosts.config
screen -dmSL s1n0 java -cp lib/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
sleep 1

cp -r chainspacecore chainspacecore-1-1
cd chainspacecore-1-1
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 1\nthisReplica 1" > ChainSpaceConfig/config.txt
cp config/hosts.config.1 config/hosts.config
screen -dmSL s1n1 java -cp lib/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
sleep 1

cp -r chainspacecore chainspacecore-1-2
cd chainspacecore-1-2
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 1\nthisReplica 2" > ChainSpaceConfig/config.txt
cp config/hosts.config.1 config/hosts.config
screen -dmSL s1n2 java -cp lib/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
sleep 1

cp -r chainspacecore chainspacecore-1-3
cd chainspacecore-1-3
printf "shardConfigFile ChainSpaceConfig/shardConfig.txt\nthisShard 1\nthisReplica 3" > ChainSpaceConfig/config.txt
cp config/hosts.config.1 config/hosts.config
screen -dmSL s1n3 java -cp lib/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.TreeMapServer ChainSpaceConfig/config.txt
cd ../
