"""Transaction dumper."""
from chainspaceapi import ChainspaceClient
from chainspacemeasurements.contracts import simulator

client = ChainspaceClient()


def push(transaction):
    client.push_transaction(transaction)


def dump(transactions):
    for transaction in transactions:
        push(transaction)


def dump_a(n):
    init_tx = simulator.init()
    push(init_tx)

    create_tx = simulator.create((init_tx['transaction']['outputs'][0],), None, (n,))
    push(create_tx)

    outputs = create_tx['transaction']['outputs']
    transactions = [simulator.consume((output,)) for output in outputs]
    dump(transactions)
