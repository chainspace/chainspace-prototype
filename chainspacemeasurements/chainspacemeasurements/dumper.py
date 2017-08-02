"""Transaction dumper."""
import time

from chainspaceapi import ChainspaceClient
from chainspacemeasurements.contracts import simulator

client = ChainspaceClient()


def dump(transaction):
    client.process_transaction(transaction)


def dump_many(transactions):
    for transaction in transactions:
        dump(transaction)


def dump_a(n):
    init_tx = simulator.init()
    dump(init_tx)

    create_tx = simulator.create((init_tx['transaction']['outputs'][0],), None, (str(n),))
    dump(create_tx)

    outputs = create_tx['transaction']['outputs']
    transactions = [simulator.consume((output,)) for output in outputs]
    dump_many(transactions)
