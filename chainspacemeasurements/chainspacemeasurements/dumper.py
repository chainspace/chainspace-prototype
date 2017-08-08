"""Transaction dumper."""
import time
import random

from chainspaceapi import ChainspaceClient
from chainspacemeasurements.contracts import simulator
from chainspacemeasurements.utils import map_object_to_shard

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


def simulation_batched(n, inputs_per_tx, batch_size=100, batch_sleep=2, nonce=True, shards_per_tx=None, num_shards=None):
    init_tx = simulator.init()
    process(init_tx)

    parameters = [str(n*inputs_per_tx),]
    if nonce:
        parameters.append(str(random.randint(1,1000000)))
    create_tx = simulator.create((init_tx['transaction']['outputs'][0],), None, parameters)
    process(create_tx)
    time.sleep(5)

    outputs = create_tx['transaction']['outputs']

    if shards_per_tx is not None:
        outputs_map = {}
        for shard in range(num_shards):
            outputs_map[shard] = []
        for output in outputs:
            outputs_map[map_object_to_shard(num_shards, output)].append(output)

    transactions =
    for i in range(0, len(outputs), inputs_per_tx):
        if shards_per_tx is not None:
            rand = random.randint(0, num_shards-shards_per_tx)
        objects = []
        for j in range(inputs_per_tx):
            if shards_per_tx is None:
                objects.append(outputs[i+j])
            else:
                shard_id = j%shards_per_tx
                shard_id = shard_id + rand
                objects.append(outputs_map[shard_id].pop())
        transactions.append(simulator.consume(objects))

    for i in range(0, len(transactions), batch_size):
        dump_many(transactions[i:i+batch_size])
        time.sleep(batch_sleep)
