"""Performance measurements for smart meter contract."""

###############################################################
# imports
###############################################################
from tester import tester
from json import dumps, loads
from chainspacecontract import transaction_to_solution
from chainspacecontract.examples import vote
from chainspacecontract.examples.utils import setup, key_gen, pack


###############################################################
# config
###############################################################
RUNS = 10000


###############################################################
# main -- run the tests
###############################################################
def main():
    ## get contract and init pint
    vote.contract._populate_empty_checkers()
    print "operation\t\tmean (s)\t\tsd (s)\t\truns"


    # ---------------------------------------------------------
    # get functions
    # ---------------------------------------------------------
    # params
    params = setup()
    (tally_priv, tally_pub) = key_gen(params)
    (voter1_priv, voter1_pub) = key_gen(params)
    options = ['alice', 'bob']
    participants = [pack(voter1_pub)]

    # init
    init_tx = vote.init()
    token = init_tx['transaction']['outputs'][0]

    # create vote
    create_vote_tx = vote.create_vote(
        (token,),
        None,
        None,
        dumps(options),
        dumps(participants),
        pack(tally_priv),
        pack(tally_pub)
    )
    vote_obj = create_vote_tx['transaction']['outputs'][1]

    # add vote
    add_vote_tx = vote.add_vote(
        (vote_obj,),
        None,
        None,
        dumps([1]),
        pack(voter1_priv),
        pack(voter1_pub)
    )

    # tally
    last_vote_obj = add_vote_tx['transaction']['outputs'][0]
    tally_tx = vote.tally(
        (last_vote_obj,),
        None,
        None,
        pack(tally_priv),
        pack(tally_pub)
    )


    # ---------------------------------------------------------
    # test create_vote
    # ---------------------------------------------------------
    # [gen]
    tester(RUNS, "create_vote [g]", vote.create_vote, 
        (token,),
        None,
        None,
        dumps(options),
        dumps(participants),
        pack(tally_priv),
        pack(tally_pub)                    
    )
    # [check]
    solution = transaction_to_solution(create_vote_tx)
    tester(RUNS, "create_vote [c]", vote.contract.checkers['create_vote'],
        solution['inputs'],
        solution['referenceInputs'],
        solution['parameters'],
        solution['outputs'],
        solution['returns'],
        solution['dependencies'],
    )


    # ---------------------------------------------------------
    # test add_vote
    # ---------------------------------------------------------
    # [gen]
    tester(RUNS, "add_vote [g]\t", vote.add_vote, 
        (vote_obj,),
        None,
        None,
        dumps([1,0]),
        pack(voter1_priv),
        pack(voter1_pub)
    )
    # [check]
    solution = transaction_to_solution(add_vote_tx)
    tester(RUNS, "add_vote [c]\t", vote.contract.checkers['add_vote'],
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
    tester(RUNS, "tally [g]\t", vote.tally, 
        (last_vote_obj,),
        None,
        None,
        pack(tally_priv),
        pack(tally_pub)
    )
    # [check]
    solution = transaction_to_solution(tally_tx)
    tester(RUNS, "tally [c]\t", vote.contract.checkers['tally'],
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
