"""Performance measurements for authenticated bank contract."""
import time
import numpy

from chainspacecontract import transaction_to_solution
from chainspacecontract.examples import bank_authenticated
from chainspacecontract.examples.utils import setup, key_gen, pack

RUNS = 1000


def main():
    bank_authenticated.contract._populate_empty_checkers()
    print "operation\t\tmean (s)\t\tsd (s)\t\truns"

    # gen init tx
    times = []
    for i in range(RUNS):
        start_time = time.time()
        bank_authenticated.init()
        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "gen init tx\t\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)

    # check init tx
    init_tx = bank_authenticated.init()
    solution = transaction_to_solution(init_tx)
    times = []
    for i in range(RUNS):
        start_time = time.time()
        bank_authenticated.contract.checkers['init'](
            solution['inputs'],
            solution['referenceInputs'],
            solution['parameters'],
            solution['outputs'],
            solution['returns'],
            solution['dependencies'],
        )
        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "check init tx\t\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)

    # gen create_account tx
    times = []
    for i in range(RUNS):
        start_time = time.time()
        (_, alice_pub) = key_gen(setup())
        bank_authenticated.create_account(
            (init_tx['transaction']['outputs'][0],),
            None,
            None,
            pack(alice_pub)
        )
        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "gen create_account tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)

    # check create_account tx
    (a_priv, a_pub) = key_gen(setup())
    create_account_tx = bank_authenticated.create_account(
        (init_tx['transaction']['outputs'][0],),
        None,
        None,
        pack(a_pub)
    )
    solution = transaction_to_solution(create_account_tx)
    times = []
    for i in range(RUNS):
        start_time = time.time()
        bank_authenticated.contract.checkers['create_account'](
            solution['inputs'],
            solution['referenceInputs'],
            solution['parameters'],
            solution['outputs'],
            solution['returns'],
            solution['dependencies'],
        )
        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "check create_account tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)

    # gen auth_transfer tx
    create_account_a_tx = create_account_tx
    (b_priv, b_pub) = key_gen(setup())
    create_account_b_tx = bank_authenticated.create_account(
        (create_account_a_tx['transaction']['outputs'][0],),
        None,
        None,
        pack(b_pub)
    )
    a_account = create_account_a_tx['transaction']['outputs'][1]
    b_account = create_account_b_tx['transaction']['outputs'][1]
    times = []
    for i in range(RUNS):
        start_time = time.time()
        bank_authenticated.auth_transfer(
            [a_account, b_account],
            None,
            ['3'],
            pack(a_priv)
        )
        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "gen auth_transfer tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)

    # check auth_transfer tx
    auth_transfer_tx = bank_authenticated.auth_transfer(
        [a_account, b_account],
        None,
        ['3'],
        pack(a_priv)
    )
    solution = transaction_to_solution(auth_transfer_tx)
    times = []
    for i in range(RUNS):
        start_time = time.time()
        bank_authenticated.contract.checkers['auth_transfer'](
            solution['inputs'],
            solution['referenceInputs'],
            solution['parameters'],
            solution['outputs'],
            solution['returns'],
            solution['dependencies'],
        )
        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "check auth_transfer tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


if __name__ == '__main__':
    main()
