*Please note: this is prototype code which serves as a validation of the ideas expressed in the [Chainspace S-BAC](https://arxiv.org/abs/1708.03778) peer-reviewed academic paper accepted by [NDSS 2018](https://www.youtube.com/watch?v=bYwIxPWyuD4&list=PLgLMkKEt7E3i1cvelsFwTJ2i2RwasarNd). We are currently building out our production codebase in Go, and the Go codebase will be used going forward. We will replace this Java code with the Go code when it's feature-equivalent. Existing contract code will continue to function.*


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

There are two parts to chainspace, the client and the network.

The network is a set of nodes that are communicating with each other based on the BFT SMaRt library.

The client is a http server which connects to the network and allows you to submit transactions.
```
./contrib/core-tools/easystart.mac.sh
```

This will show you all running chainspace processes:

```
ps aux | grep -v grep | grep chainspace | awk '{print $2 $11}'
ps aux | grep -v grep | grep chainspace | awk '{print $2 " " $11 " " $12 " " $13}'
```

If you need to kill everything:

```
ps aux | grep -v grep | grep chainspace | awk '{print $2}' | xargs kill
```

## Developer Setup [IntelliJ Ultimate]

There are intellij modules in this folder for each of the submodules. Create a new empty project in intellij. A dialog will prompt you for the project structure, ignore this and wait for the project to load. You will see the .iml module files in the explorer and you can right click and import them to the project from there.

You will need to set the project sdk to be a JDK 1.8 and also a python virtualenv which you can create and link to each python module.

The modules are:

- chainspaceapi [python]
- chainspacecontract [python]
- chainspacemeasurement [python]
- chainspacecore [java]

You will need to add petlib manually to your python virtualenv from a shell... Intellij will have created your virtual env somewhere of your choosing indicated here as  (`$PATH_TO_VIRTUAL_ENV$`)...

```
source $PATH_TO_VIRTUAL_ENV$/bin/activate
pip install petlib
```




