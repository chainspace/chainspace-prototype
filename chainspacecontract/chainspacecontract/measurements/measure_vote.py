"""Performance measurements for authenticated bank contract."""
import time
import numpy
from json import dumps, loads

from chainspacecontract import transaction_to_solution
from chainspacecontract.examples import vote
from chainspacecontract.examples.utils import setup, key_gen, pack

RUNS = 1000


def main():
    vote.contract._populate_empty_checkers()
    print "operation\t\tmean (s)\t\tsd (s)\t\truns"

    ##
    ## gen init tx
    ##
    times = []
    for i in range(RUNS):
        start_time = time.time()
        vote.init()
        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "gen init tx\t\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)

    ##
    ## check init tx
    ##
    init_tx = vote.init()
    solution = transaction_to_solution(init_tx)
    times = []
    for i in range(RUNS):
        start_time = time.time()
        vote.contract.checkers['init'](
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


    ##
    ## gen create_vote tx
    ##
    times = []
    for i in range(RUNS):
        start_time = time.time()

        # params
        params = setup()
        (tally_priv, tally_pub)  = key_gen(params)
        (_, voter1_pub) = key_gen(params)
        (_, voter2_pub) = key_gen(params)
        (_, voter3_pub) = key_gen(params)
        options      = ['alice', 'bob']
        participants = [pack(voter1_pub), pack(voter2_pub), pack(voter3_pub)]

        # transaction
        vote.create_vote(
            (init_tx['transaction']['outputs'][0],),
            None,
            None,
            dumps(options),
            dumps(participants),
            pack(tally_priv),
            pack(tally_pub)
        )

        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "gen create_vote tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


    ##
    ## check create_vote tx
    ##
    # params
    params = setup()
    (tally_priv, tally_pub)   = key_gen(params)
    (voter1_priv, voter1_pub) = key_gen(params)
    (voter2_priv, voter2_pub) = key_gen(params)
    (voter3_priv, voter3_pub) = key_gen(params)
    options      = ['alice', 'bob']
    participants = [pack(voter1_pub), pack(voter2_pub), pack(voter3_pub)]

    # transaction
    create_vote_tx = vote.create_vote(
        (init_tx['transaction']['outputs'][0],),
        None,
        None,
        dumps(options),
        dumps(participants),
        pack(tally_priv),
        pack(tally_pub)
    )

    solution = transaction_to_solution(create_vote_tx)
    times = []
    for i in range(RUNS):
        start_time = time.time()
        vote.contract.checkers['create_vote'](
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
    print "check create_vote tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


    ##
    ## gen add_vote tx
    ##
    old_vote = create_vote_tx['transaction']['outputs'][1]
    times = []
    for i in range(RUNS):
        start_time = time.time()
        vote.add_vote(
            (old_vote,),
            None,
            None,
            dumps([1,0]),
            pack(voter1_priv),
            pack(voter1_pub)
        )
        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "gen add_vote tx\t\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


    ##
    ## check add_vote tx
    ##
    add_vote_tx = vote.add_vote(
        (old_vote,),
        None,
        None,
        dumps([1,0]),
        pack(voter1_priv),
        pack(voter1_pub)
    )
    solution = transaction_to_solution(add_vote_tx)
    times = []
    for i in range(RUNS):
        start_time = time.time()
        vote.contract.checkers['add_vote'](
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
    print "check add_vote tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


    ##
    ## gen tally tx
    ##
    last_vote = add_vote_tx['transaction']['outputs'][0]
    times = []
    for i in range(RUNS):
        start_time = time.time()
        vote.tally(
            (last_vote,),
            None,
            None,
            pack(tally_priv),
            pack(tally_pub)
        )
        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "gen tally tx\t\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


    ##
    ## check tally tx
    ##
    tally_tx = vote.tally(
        (last_vote,),
        None,
        None,
        pack(tally_priv),
        pack(tally_pub)
    )
    solution = transaction_to_solution(tally_tx)
    times = []
    for i in range(RUNS):
        start_time = time.time()
        vote.contract.checkers['tally'](
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
    print "check tally tx\t\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


if __name__ == '__main__':
    main()
