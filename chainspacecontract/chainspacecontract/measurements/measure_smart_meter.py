"""Performance measurements for smart meter contract."""

###############################################################
# imports
###############################################################
from tester import tester
from json import dumps, loads
from chainspacecontract import transaction_to_solution
from chainspacecontract.examples import smart_meter
from chainspacecontract.examples.utils import setup, key_gen, pack


###############################################################
# config
###############################################################
RUNS = 10


###############################################################
# main -- run the tests
###############################################################
def main():
    ## get contract and init pint
    smart_meter.contract._populate_empty_checkers()
    print "operation\t\tmean (s)\t\tsd (s)\t\truns"


    # ---------------------------------------------------------
    # get functions
    # ---------------------------------------------------------
    # params
    params = setup()
    G = params[0]
    (provider_priv, provider_pub) = key_gen(params)
    tariffs  = [5, 3, 5, 3, 5]
    readings = [10, 20, 30, 10, 50]
    openings = [G.order().random() for _ in tariffs]

    # init
    init_tx = smart_meter.init()
    token = init_tx['transaction']['outputs'][0]

    # create smart_meter
    create_meter_tx = smart_meter.create_meter(
        (token,),
        None,
        None,
        pack(provider_pub),
        'Some info about the meter.',   # some info about the meter
        dumps([5, 3, 5, 3, 5]),         # the tariffs 
        dumps(764)                      # the billing period
    )
    meter_obj = create_meter_tx['transaction']['outputs'][1]

    # add reading
    add_reading_tx = smart_meter.add_reading(
        (meter_obj,),
        None,
        None,
        pack(provider_priv),
        dumps(10),                 # the new reading 
        pack(G.order().random())   # the opening value
    )

    # compute bill
    last_meter_obj = add_reading_tx['transaction']['outputs'][0]
    compute_bill_tx = smart_meter.compute_bill(
        (last_meter_obj,),
        None,
        None,
        dumps(readings), 
        pack(openings), 
        dumps(tariffs) 
    )


    # ---------------------------------------------------------
    # test create_meter
    # ---------------------------------------------------------
    # [gen]
    tester(RUNS, "create_meter [g]", smart_meter.create_meter, 
        (token,),
        None,
        None,
        pack(provider_pub),
        'Some info about the meter.',   
        dumps([5, 3, 5, 3, 5]),       
        dumps(764)                    
    )
    # [check]
    solution = transaction_to_solution(create_meter_tx)
    tester(RUNS, "create_meter [c]", smart_meter.contract.checkers['create_meter'],
        solution['inputs'],
        solution['referenceInputs'],
        solution['parameters'],
        solution['outputs'],
        solution['returns'],
        solution['dependencies'],
    )


    # ---------------------------------------------------------
    # test add_reading
    # ---------------------------------------------------------
    # [gen]
    tester(RUNS, "add_reading [g]", smart_meter.add_reading, 
        (meter_obj,),
        None,
        None,
        pack(provider_priv),
        dumps(10),                 
        pack(G.order().random())  
    )
    # [check]
    solution = transaction_to_solution(add_reading_tx)
    tester(RUNS, "add_reading [c]", smart_meter.contract.checkers['add_reading'],
        solution['inputs'],
        solution['referenceInputs'],
        solution['parameters'],
        solution['outputs'],
        solution['returns'],
        solution['dependencies'],
    )


    # ---------------------------------------------------------
    # test compute bill
    # ---------------------------------------------------------
    # [gen]
    tester(RUNS, "compute_bill [g]", smart_meter.compute_bill, 
        (last_meter_obj,),
        None,
        None,
        dumps(readings), 
        pack(openings), 
        dumps(tariffs)  
    )
    # [check]
    solution = transaction_to_solution(compute_bill_tx)
    tester(RUNS, "compute_bill [c]", smart_meter.contract.checkers['compute_bill'],
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
