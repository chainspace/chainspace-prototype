FROM java:8-jre
COPY chainspacecore /app/chainspacecore
COPY contrib /app/contrib
RUN apt-get update
RUN apt-get install screen
WORKDIR /app
CMD ./contrib/core-tools/easystart.mac.sh && cd chainspacecore && java -cp lib/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.Client ChainSpaceClientConfig/config.txt
