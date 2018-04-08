## Understanding the Code


There are three parts to Chainspace:

- Python client libraries
- Client API Service (localhost:5000)
- Chainspace network of nodes (defaults to 2 shards, with 4 nodes in each)

## Chainspace network of nodes

The verification node is built on top of the [BFT-SMaRt](https://github.com/bft-smart/library) library which provides the underlying P2P distributed network and BFT fault tolerance.

We have included the BFT-SMaRt source code in `lib/` so you can include it in your IDE for easy navigation into its code.

The entry point for the node is the `TreeMapServer` class. This has a `main` method and is executable.

It in turn creates

## The Client API Service

The client api service is used to submit transactions to the nodes. The client service maps data to shards, and sends transaction requests to the corresponding shards.

## Verifying Transactions (Checking)

Transactions are verified by calling out to a separate process via http with some context and asking this process to verify the transaction.

This allows for the verification process to be constructed in any language.

Currently, CS supports Python which is implemented through the `PythonChecker` class.

This class automatically instantiates processes (1 for each node) and retains a cache of them so that they can be invoked via http.

Transaction processing is managed through the `Core` class, which in turn calls out to the checkers.

See `Core.processTransaction` which then calls `processTransactionVM` which invokes the checker eventually through the `callChecker` method.

Contracts are loaded from a subdir called `contracts` which is currently hardcoded to look for python files.

The link to the contract is present in the data structure of the `CSTransaction` class which has a field getContractId` which should map to the name of the python contract file.


## Transaction Deployment And Verification Example (Mac)

There is a `Makefile` which has these commands in it:

@todo - tidy up this documentation to use the makefile instead

You need to have Python 2.7 (as well as pip) installed to run Chainspace.

It's preferable to install python virtualenv to manage local python environments and their dependencies.

```
virtualenv .chainspace.env
source .chainspace.env/bin/activate
```

Install Python modules:

```
> pip install -e ./chainspaceapi
> pip install -e ./chainspacecontract
> pip install petlib
> deactivate
```

To run the coconut petition, you will need to install the bilinear pairing library which can be got from
https://github.com/asonnino/coconut/tree/master/bplib-master

```
> pip install -e <place-you-cloned-coconue>/bplib-master
```

and you need numpy

```
> pip install numpy
```

Assemble an application bundle:

```
cd chainspacecore
mvn -Dversion=1.0-SNAPSHOT package assembly:single
```



Make sure chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar file got generated:

```
ls target
```


Run this script to start eight server replicas (nodes):

```
cd chainspace
./contrib/core-tools/easystart.mac.sh
```

`ls` to check eight folders 'chainspacecore-X-Y' were generated, one for each server.

It's possible to track each server's logs. In a separate console tab, run the following to display for the first server in the first shard:

```
screen -r s0n0
```

If you CMD+C in here you'll kill that process
so safer you can just tail the logs:
```
tail -f chainspacecore-0-1/screenlog.0
```

Servers are now running.

This will show you all running chainspace processes:

```
ps aux | grep -v grep | grep chainspace | awk '{print $2 $11}'
ps aux | grep -v grep | grep chainspace | awk '{print $2 " " $11 " " $12 " " $13}'
```

If you need to kill everything:

```
ps aux | grep -v grep | grep chainspace | awk '{print $2}' | xargs kill
cd chainspace
rm -rf chainspacecore-*
```

Run Chainspace local client: This is a local web server that can format transactions and submit them to the network

```
./runclientservice.sh
```

If you find theres an address already in use you can run

```
lsof -n -i:$PORT | grep LISTEN
```

To find out what it is

If that doesnt work try

```
lsof -n -i4TCP:$PORT | grep LISTEN
lsof -n -iTCP:$PORT | grep LISTEN
```

Let's submit and verify a transaction using the "increment" method in the "addition" contract. It can be found in `contracts/addition.py` in each of the 'chainspacecore-X-Y' directories. In order to do this, you'll need to send transaction data to the chain.

#### Sending transactions:

Begin by entering the virtual environment:

```
source .chainspace.env/bin/activate
```

If you ever need to debug the chainspace contract code in your python REPL, you can force
a load of the files there by doing this:

```
execfile("<path-to-chainspace>/chainspacecontract/chainspacecontract/contract.py")
```


From here, open the python console:
```
> python
```
Continue:
```
from chainspaceapi import ChainspaceClient
from chainspacecontract.examples import increment


client = ChainspaceClient()


transaction1 = increment.init()
client.process_transaction(transaction1)

our_object = transaction1['transaction']['outputs'][0]
transaction2 = increment.increment(inputs=[our_object])
client.process_transaction(transaction2)

>>> our_object
'0'
>>> type(our_object)
<class 'chainspacecontract.contract.ChainspaceObject'>

>>> our_object.object_id
'a9bde7fac83d70a4c6811d74d3cb218abc6c0f69e0dc5a77f0097be61faf79c7'
>>>


transaction2['transaction']['outputs']
('1',)

>>> transaction2
{'transaction': {'inputIDs': ['a9bde7fac83d70a4c6811d74d3cb218abc6c0f69e0dc5a77f0097be61faf79c7'], 'methodID': 'increment', 'parameters': (), 'outputs': ('1',), 'returns': (), 'dependencies': [], 'referenceInputIDs': [], 'contractID': 'addition'}, 'store': {'a9bde7fac83d70a4c6811d74d3cb218abc6c0f69e0dc5a77f0097be61faf79c7': '0'}}


```

Note: Object Ids seem to be deterministic ie start the same each time you restart?

### Dockerisation
 
The Dockerfile is a great reference point for understanding what's required to run Chainspace.
 
```cd``` into root chainspace directory first.  
 
To build an image:
```
docker build -t chainspace .
```

Retrieve the docker login command that you can use to authenticate your Docker client to your registry:
```
aws ecr get-login --no-include-email --region eu-west-1
```

Run the docker login command that was returned in the previous step (you can use ```$(!!)`` to do that).

After the build completes, tag your image so you can push the image to this repository:
```
docker tag chainspace:latest 987195267860.dkr.ecr.eu-west-1.amazonaws.com/chainspace/app:latest
```

Run the following command to push this image to your newly created AWS repository:
```
docker push 987195267860.dkr.ecr.eu-west-1.amazonaws.com/chainspace/app:latest
```

If you're replacing an existing task in the AWS cluster, be sure to stop the existing task before running a new one.
Alternatively, create a new task and the run it:
```
aws ecs run-task --task-definition [task-def-name] --cluster [cluster-name]
```

The existing task will run the docker container for you. However, if you need to run it manually:
```
docker run -p 5010:5000 -t [image-name]
```

### Troubleshooting / Debugging

If the response you get contains `"outcome": "accepted_t_abort"`, it might mean that the checker didn't validate the transaction.

You can perform a checker validation manually. Client process fires up a separate process for the checker running on the following endpoint:
`http://127.0.0.1:5001/<contract name>/<method name>`, which in case of the addition contract would be `http://127.0.0.1:5001/addition/increment`.

Submit a POST request to this endpoint with the following body:

```
{  
   "contractID":"addition",
   "inputs":["0"],
   "referenceInputs":[],
   "parameters":[],
   "returns":[],
   "outputs":["1"],
   "dependencies":[],
   "methodID":"increment"
}
```

You should get `{ "success": true }`. If not, check you stored the inputs and outputs in the shards correctly.

If you're getting the ```RESOURCE:PORTS``` error after attempting to run a task on AWS, it's likely that you're running
an existing task using the same port(s).

Once a task has started running, SSH into the relevant ec2 instance:
```
ssh ec2-user@ec2-52-208-247-232.eu-west-1.compute.amazonaws.com
```

List the docker processes using ```docker ps``` and then enter the relevant container:
```
docker exec -it [container id] bash
```

From here, you can ```make tail-node``` to view the screen log of the first node.

It may also be useful to take a look at the checker log:
```
cat chainspacecore-0-1/checker.13001.log.0
```

Be advised that running ```make kill-all``` from within the docker container will kill the entire container. Instead, see
which processes are running using ```make ps``` and then ```kill [process ID]``` for each appropriate process.


# Setting up a working dev cycle

For now there is no log aggregation and so in order to see what is happening with CS its useful to have a routine setup in your terminal.

The scripts do utilise the `screen` program so in theory you can use screen to view all the log files and script some kind of setup.

However, to begin with its probably simpler to just create multiple tabs.

A good setup is to have the following tabs open:

TAB1  - "Control" - terminal in the root CS dir so you can run the make commands
TAB2  - "REPL" - terminal in root CS dir, activate the python virtualenve and running python so you can make client calls
TAB3  - "DB-0-0" - db debug for shard 1 running sqlite - open it in the root CS dir when you restart everything need to repone the db
TAB3  - "DB-1-0" - db debug for shard 2
TAB4  - "Client API" - running the client api (make start-client-api)
TAB5  - "NODE-0-0" - Shard 0 node logs - run make tail-logs to see command basically tailing screen.0.log
TAB6  - "NODE-0-1" - Shard 1 replicas dont really need them all unless you are debugging BFT
TAB7  - "NODE-0-2"
TAB8  - "NODE-0-3"
TAB9  - "NODE-1-0" - Shard 1 node logs - run make tail-logs to see command basically tailing screen.0.log
TAB10 - "NODE-1-1" - Shard 1 replicas dont really need them all unless you are debugging BFT
TAB11 - "NODE-1-2"
TAB12 - "NODE-1-3"

When you kill-all and restart you will need to restart the client api, reopen the dbs and restart the tails. Usually this is pretty quick because it was the last command you typed in the terminal.


# Observations / TODO

[1] Core.java:161 - contract path is hardcoded to by `.py` files - this needs to be pushed to the checker


# Working with the repo

https://help.github.com/articles/syncing-a-fork/
https://help.github.com/articles/configuring-a-remote-for-a-fork/
