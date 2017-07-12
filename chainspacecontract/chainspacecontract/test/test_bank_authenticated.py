""" test authenticated bank transfer """

####################################################################
# imports
###################################################################
# general
from multiprocessing import Process
import time
import unittest
import requests
# chainsapce
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
        checker_service_process = Process(target=bank_authenticated_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        transaction = bank_authenticated.init()

        ##
        ## submit transaction
        ##
        response = requests.post('http://127.0.0.1:5000/init', json=transaction)
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()


    # --------------------------------------------------------------
    # test create account
    # --------------------------------------------------------------
    def test_create_account(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=bank_authenticated_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # create alice's and bob public key
        (_, alice_pub) = key_gen(setup())

        # create inputs & parameters
        inputs = {'type': 'BankToken'},

        # pack transaction
        transaction = bank_authenticated.create_account(
            inputs,
            None,
            None,
            pack(alice_pub)
        )

        ##
        ## submit transaction
        ##
        response = requests.post('http://127.0.0.1:5000/create_account', json=transaction)
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()



    # --------------------------------------------------------------
    # test an authenticated bank transfer
    # --------------------------------------------------------------
    def test_auth_transfer(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=bank_authenticated_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## create transaction
        ##
        # create alice's and bob public key
        params = setup()
        (alice_priv, alice_pub) = key_gen(params)
        (bob_priv, bob_pub)     = key_gen(params)

        # create inputs & parameters
        inputs = [
            {'type' : 'BankAccount', 'pub': pack(alice_pub), 'balance': 10},
            {'type' : 'BankAccount', 'pub': pack(bob_pub),   'balance': 10}
        ]
        parameters = {'amount': 3}

        # pack transaction
        transaction = bank_authenticated.auth_transfer(
            inputs,
            None,
            parameters,
            pack(alice_priv)
        )

        ##
        ## submit transaction
        ##
        response = requests.post('http://127.0.0.1:5000/auth_transfer', json=transaction)
        self.assertTrue(response.json()['success'])

        ##
        ## stop service
        ##
        checker_service_process.terminate()
        checker_service_process.join()


####################################################################
# main
###################################################################
if __name__ == '__main__':
    unittest.main()
