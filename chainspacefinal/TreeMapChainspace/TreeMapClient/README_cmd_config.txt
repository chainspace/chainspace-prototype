run:
java -cp target/chainspace-1.0-SNAPSHOT-jar-with-dependencs.jar:bin/BFT-SMaRt.jar foo.gettingstarted.client.ConsoleClient ChainSpaceConfig/config.txt

compile:
mvn package assembly:single