# Checker API Documentation

Checkers in Chainspace are currently invoked via http calls, running as separate processes to allow complete independence of implementation from the Java core.


Here is a summary of the various interactions:

```

+--------+          +------------+           +-----------------+          +-------------------+
| python |   (A)    |            |           |                 |   (B)    | @contract.checker |
| script |   http   | chainspace |   http    | chainspace      |   http   | checker fn        |
|   or   | -------> | client api | ------->  | node            | -------> | in <contract>.py  |
|  repl  |          |            |           | (TreeMapServer) |          |                   |
+--------+          +------------+           +-----------------+          +-------------------+
    |                                                 |
    |                                                 |
   \/                                                \/
 +---------------------+                     +--------------------+
 | @contract.method    |                     | Broadcast to       |
 | fn in <contract>.py |                     | other nodes in     |
 |                     |                     | shard via BFTSmart |
 +---------------------+                     +--------------------+

```

From an external API point of view (for e.g. if we wish to consider implementing the invocation and verification of contracts
in an alternate language to python, we particularly care about the API for call **(A)** and **(B)** which are documented below.

Lets follow the simplest example we have which is the `increment.py` contract which you can find in the `chainspacecontract/chainspacecontract/examples` directory.

Note that this contract has no `@contract.checker` function declared, and so an empty checker which always returns true is provided by default
by the contract python wrapper.

Here is the python code that will invoke this contract. Invoking the contract involves making http calls to the chainspace client api.

```
from chainspaceapi import ChainspaceClient
from chainspacecontract.examples import increment


client = ChainspaceClient()

# Part 1 - initialise the contract
transaction1 = increment.init()
client.process_transaction(transaction1)

# Part 2 - invoke the 'increment' method
our_object = transaction1['transaction']['outputs'][0]
transaction2 = increment.increment(inputs=[our_object])
client.process_transaction(transaction2)

```

Here are the HTTP calls for interaction (B) when the Chainspace node invokes the python checker (via `PythonChecker` Java class).

**Part 1 - initialise the contract**

```
POST http://127.0.0.1:13001/addition/init HTTP/1.1
{
  "contractID":"addition",
  "inputs":[],
  "referenceInputs":[],
  "parameters":[],
  "returns":[],
  "outputs":["0"],
  "dependencies":[],
  "methodID":"init"
}

HTTP/1.0 200 Ok
{
  "success": true
}
```

**Part 2 - invoke the 'increment' method**

```
POST http://127.0.0.1:17001/addition/increment HTTP/1.1
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

HTTP/1.0 200 Ok
{
  "success": true
}
```