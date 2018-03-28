""" test CSCoin """

####################################################################
# imports
###################################################################
# general
from multiprocessing import Process
from json            import dumps, loads
import time
import unittest
import requests
# chainspace
from chainspacecontract import transaction_to_solution
from chainspacecontract.examples.cscoin import contract as cscoin_contract
from chainspacecontract.examples import cscoin
# crypto
from chainspacecontract.examples.utils import setup, key_gen, pack


####################################################################
# CSCoin
####################################################################
callback = 'hello.init' # id of the callback contract
params = setup()
(alice_priv, alice_pub) = key_gen(params)
(bob_priv, bob_pub) = key_gen(params)


class TestBankAuthenticated(unittest.TestCase):
    # --------------------------------------------------------------
    # test init
    # --------------------------------------------------------------
    def test_init(self):
        ## run service
        checker_service_process = Process(target=cscoin_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ## create transaction
        transaction_dict = cscoin.init()

        ## submit transaction
        response = requests.post(
            'http://127.0.0.1:5000/' + cscoin_contract.contract_name 
            + '/init', json=transaction_to_solution(transaction_dict)
        )
        self.assertTrue(response.json()['success'])

        ## stop service
        checker_service_process.terminate()
        checker_service_process.join()

    # --------------------------------------------------------------
    # test create account
    # --------------------------------------------------------------
    def test_create_account(self):
        ## run service
        checker_service_process = Process(target=cscoin_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ## create transaction
        # init
        init_transaction = cscoin.init()['transaction']
        token = init_transaction['outputs'][0]
        # create bank account
        transaction = cscoin.create_account(
            (token,),
            None,
            None,
            alice_pub,
            None
        )

        ## submit transaction
        response = requests.post(
            'http://127.0.0.1:5000/' + cscoin_contract.contract_name 
            + '/create_account', json=transaction_to_solution(transaction)
        )
        self.assertTrue(response.json()['success'])

        ## stop service
        checker_service_process.terminate()
        checker_service_process.join()

    # --------------------------------------------------------------
    # test a transfer
    # --------------------------------------------------------------
    def test_transfer(self):
        ## run service
        checker_service_process = Process(target=cscoin_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ## create transaction
        # init
        init_transaction = cscoin.init()
        token = init_transaction['transaction']['outputs'][0]
        # create Alice's bank account
        create_transaction = cscoin.create_account(
            (token,),
            None,
            None,
            alice_pub,
            None
        )
        alice_account = create_transaction['transaction']['outputs'][1]
        # create Bob's bank account
        create_transaction = cscoin.create_account(
            (token,),
            None,
            None,
            bob_pub,
            callback
        )
        bob_account = create_transaction['transaction']['outputs'][1]
        # make transfer
        transaction = cscoin.transfer(
            (alice_account,bob_account),
            None,
            (dumps(1),),
            alice_priv
        )

        ## submit transaction
        response = requests.post(
            'http://127.0.0.1:5000/' + cscoin_contract.contract_name 
            + '/transfer', json=transaction_to_solution(transaction)
        )
        self.assertTrue(response.json()['success'])

        print("\n\n===================== ACCOUNTS =====================\n")
        print('No callback')
        print('Alice\'s balance: ' + str(loads(transaction['transaction']['outputs'][0])['balance']))
        print('Bob\'s balance: ' + str(loads(transaction['transaction']['outputs'][1])['balance']))
        print("\n====================================================\n\n")

        ## stop service
        checker_service_process.terminate()
        checker_service_process.join()

    # --------------------------------------------------------------
    # test a transfer
    # --------------------------------------------------------------
    def test_transfer_callback(self):
        ## run service
        checker_service_process = Process(target=cscoin_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ## create transaction
        # init
        init_transaction = cscoin.init()
        token = init_transaction['transaction']['outputs'][0]
        # create Alice's bank account
        create_transaction = cscoin.create_account(
            (token,),
            None,
            None,
            alice_pub,
            callback
        )
        alice_account = create_transaction['transaction']['outputs'][1]
        # create Bob's bank account
        create_transaction = cscoin.create_account(
            (token,),
            None,
            None,
            bob_pub,
            callback
        )
        bob_account = create_transaction['transaction']['outputs'][1]
        # make transfer
        transaction = cscoin.transfer(
            (alice_account,bob_account),
            None,
            (dumps(1),)
        )

        ## submit transaction
        response = requests.post(
            'http://127.0.0.1:5000/' + cscoin_contract.contract_name 
            + '/transfer', json=transaction_to_solution(transaction)
        )
        self.assertTrue(response.json()['success'])

        print("\n\n===================== ACCOUNTS =====================\n")
        print('Callback: ' + callback)
        print('Alice\'s balance: ' + str(loads(transaction['transaction']['outputs'][0])['balance']))
        print('Bob\'s balance: ' + str(loads(transaction['transaction']['outputs'][1])['balance']))
        print("\n====================================================\n\n")

        ## stop service
        checker_service_process.terminate()
        checker_service_process.join()

####################################################################
# main
###################################################################
if __name__ == '__main__':
    unittest.main()
