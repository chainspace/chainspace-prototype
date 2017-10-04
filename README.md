# Chainspace

Chainspace is a distributed ledger platform for high-integrity and transparent processing of transactions within a decentralized system.

## Developer Installation

The bulk of the code is to be found in `chainspacecore`. To run a network of chainspace nodes, you need to first compile and package this module.


### Build
```
cd chainspacecore
mvn -Dversion=1.0-SNAPSHOT package assembly:single
```

This should produce an "uber jar" in the folder `chainspacecore/target`

### Run
```
./contrib/core-tools/easystart.mac.sh
```

This will show you all running chainspace processes:

ps aux | grep -v grep | grep chainspace | awk '{print $2}'

If you need to kill everything:


ps aux | grep -v grep | grep chainspace | awk '{print $2}' | xargs kill
