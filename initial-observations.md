## Understanding the Code

There are two parts to chainspace:

- The Client
- The verification node

## The Client

## The Verification Node

The verification node is built on top of the [BFT-SMaRt](https://github.com/bft-smart/library) library which provides the underlying P2P distributed network and BFT fault tolerance.

We have included the BFT-SMaRt source code in `lib/` so you can inlcude it in your IDE for easy navigation into its code.

The entry point for the node is the `TreeMapServer` class. This has a `main` method and is executable.

It in turn creates


## Verifying Transactions (Checking)

Transactions are verified by calling out to a separate process via http with some context and asking this process to verify the transaction.

This allows for the verification process to be constructed in any language.

Currently, CS supports Python which is implemented through the `PythonChecker` class.

This class automatically instantiates processes (1 for each node) and retains a cache of them so that they can be invoked via http.

See `Core.processTransaction` which then calls `processTransactionVM` which invokes the checker eventually through the `callChecker` method.

Contracts are loaded from a subdir called `contracts` which is currently hardcoded to look for python files.

The link to the contract is present in the data structure of the `CSTransaction` class which has a field getContractId` which should map to the name of the python contract file.

Transaction processing is managed through the `Core` class, which in turn calls out to the checkers.


# Observations / TODO

[1] Core.java:161 - contract path is hardcoded to by `.py` files - this needs to be pushed to the checker

