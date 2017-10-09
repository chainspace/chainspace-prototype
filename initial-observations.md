## Understanding the Code

There are two parts to chainspace:

- The Client
- The verification node

## The Client

## The Verification Node

The verification node is built on top of the [BFT-SMaRt](https://github.com/bft-smart/library) library which provides the underlying P2P distributed network and BFT fault tolerance.

We have included the BFT-SMaRt source code in `lib/` so you can include it in your IDE for easy navigation into its code.

The entry point for the node is the `TreeMapServer` class. This has a `main` method and is executable.

It in turn creates


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

Assemble an application bundle:

```
mvn -Dversion=1.0-SNAPSHOT package assembly:single 

```

Make sure chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar file got generated:

```
ls chainspacecore/target/
```

Run this script to start eight server replicas (nodes):

```
./contrib/core-tools/easystart.mac.sh
```

`ls` to check eight folders 'chainspacecore-X-Y' got generated, one for each server.

It's possible to track each server's logs. In a separate console tab, run the following to display for the first server in the first shard:

```
tail -f chainspacecore-0-0/screenlog.0
```

Servers are now running. 

Let's submit and verify transaction using the "increment" method in the "addition" contract. It can be found in `contracts/addition.py`. In order to do that, you'll need to post transaction data to the chain. 

Contract inputs and outputs are part of transaction data. They get checked and should be stored in a shard in a TreeMap. Let's submit transaction that has "0" as an input and "1" as an output after running incrementation.

Data can be easily added to the shard using interactive console:

```
cd chainspacecore
./runconsoleclient.sh
```

Wait until you're prompted to select a shard. Choose shard 0, then option "1" to add a key and a value to the map. Add key 0 and value 0. Select shard 0 again and add key 1 and value 1. Repeat the same for shard 1.

Now let's run the client to interact with replicas. If you're still in the chainspacecore folder, run:

```
./runclientservice.sh
```

You should get a message that "Node service is running on port 5000".

Make sure the client is running. In your favourite REST client (this example uses [Postman](https://www.getpostman.com/postman)), send any request to http://127.0.0.1:5000/. You should receive html body saying "404 Not found".

In order to submit transaction, POST to the endpoint `http://127.0.0.1:5000/api/1.0/transaction/process` with the following body (you can paste it into "raw" view):

```
{
	"transaction": {
		"contractID": "addition",
		"inputIDs": ["0"],
		"methodID": "increment",
		"outputs": ["1"],
		"parameters": [],
		"referenceInputIDs": [],
		"returns": [],
		"CSTransaction": [],
		"dependencies" : []
	}, 
	"store" : {
		"0" : "0",
		"1" : "1"
	}
}
``` 

Response body should be

```
{
    "success": "True",
    "outcome": "accepted_t_commit"
}
```

### Troubleshooting

If response you get contains `"outcome": "accepted_t_abort"`, it might mean that checker didn't validate the transaction.

You can perform checker validation manually. Client process fires up a separate process for the checker rumming on the following endpoint:
`http://127.0.0.1:5001/<contract name>/<method name>`, which in case of addition contract would be `http://127.0.0.1:5001/addition/increment`.

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

You should get `{ "success": true }`. If not, check you stored inputs and outputs in the shards correctly.


# Observations / TODO

[1] Core.java:161 - contract path is hardcoded to by `.py` files - this needs to be pushed to the checker


