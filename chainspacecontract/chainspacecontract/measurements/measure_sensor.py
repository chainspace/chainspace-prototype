"""Performance measurements for sensor contract."""

###############################################################
# imports
###############################################################
from tester import tester
from json import dumps, loads
from chainspacecontract import transaction_to_solution
from chainspacecontract.examples import sensor


###############################################################
# config
###############################################################
RUNS = 10000


###############################################################
# main -- run the tests
###############################################################
def main():
    ## get contract and init pint
    sensor.contract._populate_empty_checkers()
    print "operation\t\tmean (s)\t\tsd (s)\t\truns"


    # ---------------------------------------------------------
    # get functions
    # ---------------------------------------------------------
    # init
    init_tx = sensor.init()
    token = init_tx['transaction']['outputs'][0]

    # create sensor
    create_sensor_tx = sensor.create_sensor(
        (token,),
        None,
        None,
    )
    sensor_obj = create_sensor_tx['transaction']['outputs'][1]

    # add data
    add_data_tx = sensor.add_data(
        (sensor_obj,),
        None,
        [dumps([1, 2, 3])]
    )


    # ---------------------------------------------------------
    # test create_sensor
    # ---------------------------------------------------------
    # [gen]
    tester(RUNS, "create_sensor [g]", sensor.create_sensor, 
        (token,), 
        None, 
        None
    )
    # [check]
    solution = transaction_to_solution(create_sensor_tx)
    tester(RUNS, "create_sensor [c]", sensor.contract.checkers['create_sensor'],
        solution['inputs'],
        solution['referenceInputs'],
        solution['parameters'],
        solution['outputs'],
        solution['returns'],
        solution['dependencies'],
    )


    # ---------------------------------------------------------
    # test add_data
    # ---------------------------------------------------------
    # [gen]
    tester(RUNS, "add_data [g]\t", sensor.add_data, 
        (sensor_obj,), 
        None, 
        [dumps([1, 2, 3])]
    )
    # [gen]
    solution = transaction_to_solution(add_data_tx)
    tester(RUNS, "add_data [c]\t", sensor.contract.checkers['add_data'],
        solution['inputs'],
        solution['referenceInputs'],
        solution['parameters'],
        solution['outputs'],
        solution['returns'],
        solution['dependencies'],
    )
    

###############################################################
# starting point
###############################################################
if __name__ == '__main__':
    main()
