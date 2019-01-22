COMPILE:
from chainspacecore/
mvn package assembly:single



RUN CONSOLE CLIENT:
from chainspacecore/

java -cp lib/bft-smart-1.2.1-UCL.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.ConsoleClient ChainSpaceClientConfig/config.txt



RUN SERVERS:
from chainsapce/

contrib/core-tools/easystart.sh

