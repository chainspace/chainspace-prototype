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

        # create inputs
        inputs = [
            {'pub': pack(alice_pub), 'balance': 10}, 
            {'pub': pack(bob_pub),   'balance': 10}
        ]

        # create paramters
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
