"""Performance measurements for authenticated bank contract."""

###############################################################
# imports
###############################################################
from tester import tester
from json import dumps, loads
from chainspacecontract import transaction_to_solution
from chainspacecontract.examples import bank_authenticated
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
    bank_authenticated.contract._populate_empty_checkers()
    print "operation\t\tmean (s)\t\tsd (s)\t\truns"


    # ---------------------------------------------------------
    # get functions
    # ---------------------------------------------------------
    # params
    params = setup()
    (alice_priv, alice_pub) = key_gen(params)
    (_, bob_pub) = key_gen(params)

    # init
    init_tx = bank_authenticated.init()
    token = init_tx['transaction']['outputs'][0]

    # create accounts
    create_alice_account_tx = bank_authenticated.create_account(
        (token,),
        None,
        None,
        pack(alice_pub)
    )
    create_bob_account_tx = bank_authenticated.create_account(
        (token,),
        None,
        None,
        pack(bob_pub)
    )
    alice_account = create_alice_account_tx['transaction']['outputs'][1]
    bob_account = create_bob_account_tx['transaction']['outputs'][1]

    # make transfer
    auth_transfer_tx = bank_authenticated.auth_transfer(
        (alice_account, bob_account),
        None,
        ('3',),
        pack(alice_priv)
    )


    # ---------------------------------------------------------
    # test create_account
    # ---------------------------------------------------------
    # [gen]
    tester(RUNS, "create_account [g]", bank_authenticated.create_account, 
        (token,),
        None,
        None,
        pack(alice_pub)
    )
    # [check]
    solution = transaction_to_solution(create_alice_account_tx)
    tester(RUNS, "create_account [c]", bank_authenticated.contract.checkers['create_account'],
        solution['inputs'],
        solution['referenceInputs'],
        solution['parameters'],
        solution['outputs'],
        solution['returns'],
        solution['dependencies'],
    )


    # ---------------------------------------------------------
    # test transfer
    # ---------------------------------------------------------
    # [gen]
    tester(RUNS, "auth_transfer [g]", bank_authenticated.auth_transfer, 
        (alice_account, bob_account),
        None,
        ('3',),
        pack(alice_priv)
    )
    # [gen]
    solution = transaction_to_solution(auth_transfer_tx)
    tester(RUNS, "auth_transfer [c]", bank_authenticated.contract.checkers['auth_transfer'],
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
