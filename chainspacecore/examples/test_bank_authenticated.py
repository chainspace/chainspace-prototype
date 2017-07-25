""" test authenticated bank transfer """

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
from chainspacecontract.examples.bank_authenticated import contract as bank_authenticated_contract
from chainspacecontract.examples import bank_authenticated
# crypto
from chainspacecontract.examples.utils import setup, key_gen, pack


####################################################################
# authenticated bank transfer
####################################################################
class TestBankAuthenticated(unittest.TestCase):

    # --------------------------------------------------------------
    # test init
    # --------------------------------------------------------------
    def test_init(self):
        ##
        ## run service
        ##
        #checker_service_process = Process(target=bank_authenticated_contract.run_checker_service)
        #checker_service_process.start()
        #time.sleep(0.1)

        ##
        ## create transaction
        ##
        transaction = bank_authenticated.init()

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:3001/api/1.0/transaction/process', json=transaction
        )
        #print response.json()
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        #checker_service_process.terminate()
        #checker_service_process.join()

    # --------------------------------------------------------------
    # test create account
    # --------------------------------------------------------------
    def test_create_account(self):
        ##
        ## run service
        ##
        #checker_service_process = Process(target=bank_authenticated_contract.run_checker_service)
        #checker_service_process.start()
        #time.sleep(0.1)

        ##
        ## create transaction
        ##
        # create alice's public key
        (_, alice_pub) = key_gen(setup())

        # init
        init_transaction = bank_authenticated.init()['transaction']
        token = init_transaction['outputs'][0]

        # create bank account
        transaction = bank_authenticated.create_account(
            (token,),
            None,
            None,
            pack(alice_pub)
        )

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:3001/api/1.0/transaction/process', json=transaction
        )
        #print response.json()
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        #checker_service_process.terminate()
        #checker_service_process.join()
  
    # --------------------------------------------------------------
    # test an authenticated bank transfer
    # --------------------------------------------------------------
    def test_auth_transfer(self):
        ##
        ## run service
        ##
        #checker_service_process = Process(target=bank_authenticated_contract.run_checker_service)
        #checker_service_process.start()
        #time.sleep(0.1)

        ##
        ## create transaction
        ##
        # create alice's and bob public key
        params = setup()
        (alice_priv, alice_pub) = key_gen(params)
        (bob_priv, bob_pub)     = key_gen(params)

        # init
        init_transaction = bank_authenticated.init()['transaction']
        token = init_transaction['outputs'][0]

        # create alice's account
        create_alice_account_transaction = bank_authenticated.create_account(
            (token,),
            None,
            None,
            pack(alice_pub)
        )['transaction']
        token = create_alice_account_transaction['outputs'][0]
        alice_account = create_alice_account_transaction['outputs'][1]

        # create bob's account
        create_bob_account_transaction = bank_authenticated.create_account(
            (token,),
            None,
            None,
            pack(bob_pub)
        )['transaction']
        bob_account = create_bob_account_transaction['outputs'][1]

        # pack transaction
        transaction = bank_authenticated.auth_transfer(
            [alice_account, bob_account],
            None,
            [dumps(3)],
            pack(alice_priv)
        )

        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:3001/api/1.0/transaction/process', json=transaction
        )
        #print response.json()
        self.assertTrue(response.json()['success'] == 'True')

        ##
        ## stop service
        ##
        #checker_service_process.terminate()
        #checker_service_process.join()

    # --------------------------------------------------------------
    # test many authenticated bank transfers
    # --------------------------------------------------------------
    def test_many_auth_transfer(self):
        ##
        ## run service
        ##
        #checker_service_process = Process(target=bank_authenticated_contract.run_checker_service)
        #checker_service_process.start()
        #time.sleep(0.1)

        ##
        ## create transaction
        ##
        # create alice's and bob public key
        num_transfers = 7
        transfer_amount = 1
        params = setup()
        (alice_priv, alice_pub) = key_gen(params)
        (bob_priv, bob_pub)     = key_gen(params)

        # init
        init_transaction = bank_authenticated.init()['transaction']
        token = init_transaction['outputs'][0]

        # create alice's account
        create_alice_account_transaction = bank_authenticated.create_account(
            (token,),
            None,
            None,
            pack(alice_pub)
        )['transaction']
        token = create_alice_account_transaction['outputs'][0]
        alice_account = create_alice_account_transaction['outputs'][1]

        # create bob's account
        create_bob_account_transaction = bank_authenticated.create_account(
            (token,),
            None,
            None,
            pack(bob_pub)
        )['transaction']
        bob_account = create_bob_account_transaction['outputs'][1]

        # pack transaction
        transaction = {}
        input_obj = [alice_account, bob_account]
        for i in range(0, num_transfers):
            transaction = bank_authenticated.auth_transfer(
                input_obj,
                None,
                [dumps(transfer_amount)],
                pack(alice_priv)
            )
            input_obj = transaction['transaction']['outputs']


        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:3001/api/1.0/transaction/process', json=transaction
        )
        #print response.json()
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        #checker_service_process.terminate()
        #checker_service_process.join()

    # --------------------------------------------------------------
    # test read account
    # --------------------------------------------------------------
    def test_read_account(self):
        ##
        ## run service
        ##
        #checker_service_process = Process(target=bank_authenticated_contract.run_checker_service)
        #checker_service_process.start()
        #time.sleep(0.1)

        ##
        ## create transaction
        ##
        # create alice's and bob public key
        num_transfers = 7
        transfer_amount = 1
        params = setup()
        (alice_priv, alice_pub) = key_gen(params)
        (bob_priv, bob_pub)     = key_gen(params)

        # init
        init_transaction = bank_authenticated.init()['transaction']
        token = init_transaction['outputs'][0]

        # create alice's account
        create_alice_account_transaction = bank_authenticated.create_account(
            (token,),
            None,
            None,
            pack(alice_pub)
        )['transaction']
        token = create_alice_account_transaction['outputs'][0]
        alice_account = create_alice_account_transaction['outputs'][1]

        # create bob's account
        create_bob_account_transaction = bank_authenticated.create_account(
            (token,),
            None,
            None,
            pack(bob_pub)
        )['transaction']
        bob_account = create_bob_account_transaction['outputs'][1]

        # pack transaction
        transaction = {}
        input_obj = [alice_account, bob_account]
        for i in range(0, num_transfers):
            transaction = bank_authenticated.auth_transfer(
                input_obj,
                None,
                [dumps(transfer_amount)],
                pack(alice_priv)
            )['transaction']
            input_obj = transaction['outputs']

        # read alice's account
        alice_account = input_obj[0]
        transaction = bank_authenticated.read(
            None,
            (alice_account,),
            None
        )['transaction']
        print transaction['returns']

        # read bob's account
        bob_account = input_obj[1]
        transaction = bank_authenticated.read(
            None,
            (bob_account,),
            None
        )
        print transaction['transaction']['returns']


        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:3001/api/1.0/transaction/process', json=transaction
        )
        #print response.json()
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        #checker_service_process.terminate()
        #checker_service_process.join()


####################################################################
# main
###################################################################
if __name__ == '__main__':
    unittest.main()
