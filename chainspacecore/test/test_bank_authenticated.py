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


## chainspace version & port
version = '1.0'
port    = '5000'


####################################################################
# authenticated bank transfer
####################################################################
class TestBankAuthenticated(unittest.TestCase):

    # --------------------------------------------------------------
    # test CSCoin
    # --------------------------------------------------------------
    def test(self):
        ##
        ## params
        ##
        params = setup()
        (alice_priv, alice_pub) = key_gen(params)
        (bob_priv, bob_pub)     = key_gen(params)


        ##
        ## init
        ##
        transaction = bank_authenticated.init()
        response = requests.post(
            'http://127.0.0.1:' +port+ '/api/' +version+ '/transaction/process', json=transaction
        )
        self.assertTrue(response.json()['success'])
        time.sleep(1)


        ##
        ## create alice's account
        ##
        token = transaction['transaction']['outputs'][0]
        create_alice_account_transaction = bank_authenticated.create_account(
            (token,),
            None,
            None,
            pack(alice_pub)
        )
        response = requests.post(
            'http://127.0.0.1:' +port+ '/api/' +version+ '/transaction/process', json=create_alice_account_transaction
        )
        self.assertTrue(response.json()['success'])
        time.sleep(1)


        ##
        ## create bob's account
        ##
        token = create_alice_account_transaction['transaction']['outputs'][0]
        create_bob_account_transaction = bank_authenticated.create_account(
            (token,),
            None,
            None,
            pack(alice_pub)
        )
        response = requests.post(
            'http://127.0.0.1:' +port+ '/api/' +version+ '/transaction/process', json=create_bob_account_transaction
        )
        self.assertTrue(response.json()['success'])
        time.sleep(1)


        ##
        ## make transfer
        ##        
        alice_account = create_alice_account_transaction['transaction']['outputs'][1]
        bob_account = create_bob_account_transaction['transaction']['outputs'][1]
        transfer_transaction = bank_authenticated.auth_transfer(
            [alice_account, bob_account],
            None,
            [dumps(3)],
            pack(alice_priv)
        )
        response = requests.post(
            'http://127.0.0.1:' +port+ '/api/' +version+ '/transaction/process', json=transfer_transaction
        )
        self.assertTrue(response.json()['success'] == 'True')
        time.sleep(1)


        ##
        ## read alice's balance
        ##
        alice_account = transfer_transaction['transaction']['outputs'][0]
        transaction = bank_authenticated.read(
            None,
            (alice_account,),
            None
        )
        response = requests.post(
            'http://127.0.0.1:' +port+ '/api/' +version+ '/transaction/process', json=transaction
        )
        #print response.json()
        self.assertTrue(response.json()['success'])
        time.sleep(1)
        

        ##
        ## read bob's balance
        ##
        bob_account = transfer_transaction['transaction']['outputs'][1]
        transaction = bank_authenticated.read(
            None,
            (bob_account,),
            None
        )
        response = requests.post(
            'http://127.0.0.1:' +port+ '/api/' +version+ '/transaction/process', json=transaction
        )
        #print response.json()
        self.assertTrue(response.json()['success'])
        time.sleep(1)



####################################################################
# main
###################################################################
if __name__ == '__main__':
    unittest.main()
