## Understanding the Code

There are two parts to Chainspace:

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

You need to have Python (as well as pip) installed to run Chainspace.

Install Python modules:

```
pip install -e ./chainspaceapi
pip install -e ./chainspacecontract
```

(Run with `sudo` if encountering permission issues)

Assemble an application bundle:

```
cd chainspacecore
mvn package assembly:single
```

Make sure chainspace-1.0-SNAPSHOT-jar-with-dependencies.jar file got generated:

```
ls target
```

Run Chainspace local client:

```
./runclientservice.sh
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

Servers are now running.

Let's submit and verify a transaction using the "increment" method in the "addition" contract. It can be found in `contracts/addition.py` in each of the 'chainspacecore-X-Y' directories. In order to do this, you'll need to send transaction data to the chain.

#### Sending transactions:


From here, open the python console:
```
python
```
Continue:
```
from chainspaceapi import ChainspaceClient
from chainspacecontract.examples import increment


client = ChainspaceClient()


transaction1 = increment.init()
client.process_transaction(transaction1)

our_object = transaction['transaction']['outputs'][0]
transaction2 = increment.increment(inputs=[our_object])
client.process_transaction(transaction2)
```

### Troubleshooting

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


# Observations / TODO

[1] Core.java:161 - contract path is hardcoded to by `.py` files - this needs to be pushed to the checker
