# Dependencies
- virtualenv (recommended):
	- pip install virtualenv
	- More information: http://sourabhbajaj.com/mac-setup/Python/virtualenv.html

- Maven (macOS):
	- brew install maven 

- petlib:
	- pip install petlib
	
# Install
- create and activate the virtual environement
```
virtualenv venv
source venv/bin/activate
```

- compile the core:
from /chainspace/chainspacecore
```
mvn package assembly:single
```

- start the core (macOs):
from /chainspace
```
contrib/core-tools/easystart.mac.sh
```

- starting the console client:
from /chainspace/chainspacecore
```
java -cp lib/BFT-SMaRt.jar:target/chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar uk.ac.ucl.cs.sec.chainspace.bft.ConsoleClient ChainSpaceClientConfig/config.txt
```

- install the smart contract framework:
from /chainspace
```
pip install --editable chainspacecontract
```
from /chainspace/chainspaceapi
```
python setup.py install
```

