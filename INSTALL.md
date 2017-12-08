- recommended virtualenv
- from /chainspace/chainspacecore:
	- mvn package assembly:single
- from /chainspace
	- contrib/core-tools/easystart.mac.sh
	- from /chainspace/chainspacecore: java -cp lib/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.ConsoleClient ChainSpaceClientConfig/config.txt




macOS from scartch
 - pip install virtualenv (http://sourabhbajaj.com/mac-setup/Python/virtualenv.html)
 - virtualenv venv
 - source venv/bin/activate
 - brew install maven
 - 



 - from /chainspace
 	pip install --editable chainspacecontract
- pip install petlib
- from /chainspace/chainspaceapi
	- python setup.py install
