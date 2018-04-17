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
RUN apt-get -y install build-essential libssl-dev libffi-dev python-dev
RUN easy_install pip
WORKDIR /app
RUN virtualenv .chainspace.env
RUN . .chainspace.env/bin/activate && pip install -U setuptools
RUN . .chainspace.env/bin/activate && pip install -e ./chainspaceapi
RUN . .chainspace.env/bin/activate && pip install -e ./chainspacecontract
RUN . .chainspace.env/bin/activate && pip install petlib
CMD make start-nodes && make start-client-api

