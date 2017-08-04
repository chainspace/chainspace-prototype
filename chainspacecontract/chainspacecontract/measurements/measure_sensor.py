"""Performance measurements for authenticated bank contract."""
import time
import numpy
from json import dumps, loads

from chainspacecontract import transaction_to_solution
from chainspacecontract.examples import sensor

RUNS = 10000


def main():
    sensor.contract._populate_empty_checkers()
    print "operation\t\tmean (s)\t\tsd (s)\t\truns"

    ##
    ## gen init tx
    ##
    times = []
    for i in range(RUNS):
        start_time = time.time()
        sensor.init()
        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "gen init tx\t\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


    ##
    ## check init tx
    ##
    init_tx = sensor.init()
    solution = transaction_to_solution(init_tx)
    times = []
    for i in range(RUNS):
        start_time = time.time()
        sensor.contract.checkers['init'](
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
    ## gen create_sensor tx
    ##
    times = []
    for i in range(RUNS):
        start_time = time.time()

        # transaction
        sensor.create_sensor(
            (init_tx['transaction']['outputs'][0],),
            None,
            None,
        )

        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "gen create_sensor tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


    ##
    ## check create_vote tx
    ##
    # transaction
    create_sensor_tx = sensor.create_sensor(
        (init_tx['transaction']['outputs'][0],),
        None,
        None,
    )

    solution = transaction_to_solution(create_sensor_tx)
    times = []
    for i in range(RUNS):
        start_time = time.time()
        sensor.contract.checkers['create_sensor'](
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
    print "check create_sensor tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


    ##
    ## gen add_data tx
    ##
    # transaction
    sensorObj = create_sensor_tx['transaction']['outputs'][1]
    times = []
    for i in range(RUNS):
        start_time = time.time()
        sensor.add_data(
            (sensorObj,),
            None,
            [dumps([1, 2, 3])]
        )
        end_time = time.time()
        times.append(end_time-start_time)
    mean = numpy.mean(times)
    sd = numpy.std(times)
    print "gen add_data tx\t\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)


    ##
    ## check add_data tx
    ##
    add_data_tx = sensor.add_data(
        (sensorObj,),
        None,
        [dumps([1, 2, 3])]
    )
    solution = transaction_to_solution(add_data_tx)
    times = []
    for i in range(RUNS):
        start_time = time.time()
        sensor.contract.checkers['add_data'](
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
    print "check add_data tx\t{:.10f}\t\t{:.10f}\t{}".format(mean, sd, RUNS)

if __name__ == '__main__':
    main()
