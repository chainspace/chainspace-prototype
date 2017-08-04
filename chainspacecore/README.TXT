COMPILE:
from chainspacecore/
mvn package assembly:single



RUN CONSOLE CLIENT:
from chainspacecore/

java -cp lib/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.ConsoleClient ChainSpaceClientConfig/config.txt



RUN SERVERS:
from chainsapce/

contrib/core-tools/easystart.sh

