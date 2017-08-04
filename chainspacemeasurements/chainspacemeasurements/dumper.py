"""Transaction dumper."""
import time

from chainspaceapi import ChainspaceClient
from chainspacemeasurements.contracts import simulator

client = ChainspaceClient()


def dump(transaction):
    client.dump_transaction(transaction)


def dump_many(transactions):
    for transaction in transactions:
        dump(transaction)


def process(transaction):
    client.process_transaction(transaction)


def process_many(transactions):
    for transaction in transactions:
        process(transaction)


def simulation_a1(n):
    init_tx = simulator.init()
    process(init_tx)

    create_tx = simulator.create((init_tx['transaction']['outputs'][0],), None, (str(n),))
    process(create_tx)

    outputs = create_tx['transaction']['outputs']
    transactions = [simulator.consume((output,)) for output in outputs]
    process_many(transactions)


def simulation_a2(n):
    init_tx = simulator.init()
    process(init_tx)

    create_tx = simulator.create((init_tx['transaction']['outputs'][0],), None, (str(n),))
    process(create_tx)

    outputs = create_tx['transaction']['outputs']
    transactions = [simulator.consume((output,)) for output in outputs]
    dump_many(transactions)


def simulation_a3(n):
    init_tx = simulator.init()
    dump(init_tx)

    create_tx = simulator.create((init_tx['transaction']['outputs'][0],), None, (str(n),))
    dump(create_tx)

    outputs = create_tx['transaction']['outputs']
    transactions = [simulator.consume((output,)) for output in outputs]
    dump_many(transactions)


def simulation_b1(n, inputs_per_tx):
    init_tx = simulator.init()
    process(init_tx)

    create_tx = simulator.create((init_tx['transaction']['outputs'][0],), None, (str(n),))
    process(create_tx)

    outputs = create_tx['transaction']['outputs']

    transactions = []
    for i in range(0, len(outputs), inputs_per_tx):
        objects = []
        for j in range(inputs_per_tx):
            objects.append(outputs[i+j])
        transactions.append(simulator.consume(objects))

    process_many(transactions)


def simulation_b2(n, inputs_per_tx):
    init_tx = simulator.init()
    process(init_tx)

    create_tx = simulator.create((init_tx['transaction']['outputs'][0],), None, (str(n),))
    process(create_tx)

    outputs = create_tx['transaction']['outputs']

    transactions = []
    for i in range(0, len(outputs), inputs_per_tx):
        objects = []
        for j in range(inputs_per_tx):
            objects.append(outputs[i+j])
        transactions.append(simulator.consume(objects))

    dump_many(transactions)
