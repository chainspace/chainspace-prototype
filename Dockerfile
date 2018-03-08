FROM java:8-jre
COPY chainspaceapi /app/chainspaceapi
COPY chainspacecontract /app/chainspacecontract
COPY chainspacecore /app/chainspacecore
COPY contrib /app/contrib
COPY Makefile /app/
RUN apt-get update
RUN apt-get install screen
RUN apt-get -y install virtualenv
RUN apt-get -y install python
RUN apt-get -y install python-setuptools
RUN easy_install pip
RUN virtualenv .chainspace.env
RUN . .chainspace.env/bin/activate
RUN pip install -e ./app/chainspaceapi
RUN pip install -e ./app/chainspacecontract
WORKDIR /app
CMD ./contrib/core-tools/easystart.mac.sh && cd chainspacecore && java -cp lib/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.Client ChainSpaceClientConfig/config.txt cd chainspacecore && java -cp lib/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.Client ChainSpaceClientConfig/config.txt

