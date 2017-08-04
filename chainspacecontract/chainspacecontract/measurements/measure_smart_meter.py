"""Performance measurements for authenticated bank contract."""
import time
import numpy
from json import dumps, loads

from chainspacecontract import transaction_to_solution
from chainspacecontract.examples import smart_meter
from chainspacecontract.examples.utils import setup, key_gen, pack

RUNS = 10000


def main():
    smart_meter.contract._populate_empty_checkers()
    print "operation\t\tmean (s)\t\tsd (s)\t\truns"

    ##
    ## gen init tx
    ##
    times = []
    for i in range(RUNS):
        start_time = time.time()
        smart_meter.init()
        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "gen init tx\t\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


    ##
    ## check init tx
    ##
    init_tx = smart_meter.init()
    solution = transaction_to_solution(init_tx)
    times = []
    for i in range(RUNS):
        start_time = time.time()
        smart_meter.contract.checkers['init'](
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
    ## gen create_meter tx
    ##
    times = []
    for i in range(RUNS):
        start_time = time.time()

        # params
        provider_pub = key_gen(setup())[1]

        # transaction
        smart_meter.create_meter(
            (init_tx['transaction']['outputs'][0],),
            None,
            None,
            pack(provider_pub),
            'Some info about the meter.',   # some info about the meter
            dumps([5, 3, 5, 3, 5]),         # the tariffs 
            dumps(764) 
        )

        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "gen create_meter tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


    ##
    ## check create_vote tx
    ##
    # params
    provider_pub = key_gen(setup())[1]

    # transaction
    create_meter_tx = smart_meter.create_meter(
        (init_tx['transaction']['outputs'][0],),
        None,
        None,
        pack(provider_pub),
        'Some info about the meter.',   # some info about the meter
        dumps([5, 3, 5, 3, 5]),         # the tariffs 
        dumps(764) 
    )

    solution = transaction_to_solution(create_meter_tx)
    times = []
    for i in range(RUNS):
        start_time = time.time()
        smart_meter.contract.checkers['create_meter'](
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
    print "check create_meter tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


    ##
    ## gen add_reading tx
    ##
    # params
    G = setup()[0]
    (provider_priv, provider_pub) = key_gen(setup())

    # transaction
    meter = create_meter_tx['transaction']['outputs'][1]
    times = []
    for i in range(RUNS):
        start_time = time.time()
        smart_meter.add_reading(
            (meter,),
            None,
            None,
            pack(provider_priv),
            dumps(10),                 # the new reading 
            pack(G.order().random())   # the opening value
        )
        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "gen add_reading tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


    ##
    ## check add_reading tx
    ##
    add_reading_tx = smart_meter.add_reading(
        (meter,),
        None,
        None,
        pack(provider_priv),
        dumps(10),                 # the new reading 
        pack(G.order().random())   # the opening value
    )
    solution = transaction_to_solution(add_reading_tx)
    times = []
    for i in range(RUNS):
        start_time = time.time()
        smart_meter.contract.checkers['add_reading'](
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
    print "check add_reading tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)

    
    ##
    ## gen tally tx
    ##
    # params
    tariffs  = [5, 3, 5, 3, 5]
    readings = [10, 20, 30, 10, 50]
    openings = [G.order().random() for _ in tariffs]

    # transaction
    last_meter = add_reading_tx['transaction']['outputs'][0]
    times = []
    for i in range(RUNS):
        start_time = time.time()
        smart_meter.compute_bill(
            (last_meter,),
            None,
            None,
            dumps(readings), 
            pack(openings), 
            dumps(tariffs) 
        )
        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "gen compute_bill tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


    ##
    ## check tally tx
    ##
    compute_bill_tx = smart_meter.compute_bill(
        (last_meter,),
        None,
        None,
        dumps(readings), 
        pack(openings), 
        dumps(tariffs) 
    )
    solution = transaction_to_solution(compute_bill_tx)
    times = []
    for i in range(RUNS):
        start_time = time.time()
        smart_meter.contract.checkers['compute_bill'](
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
    print "check compute_bill tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


if __name__ == '__main__':
    main()
