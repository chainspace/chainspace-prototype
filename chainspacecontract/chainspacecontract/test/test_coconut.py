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
from chainspacecontract.examples.coconut import contract as coconut_contract
from chainspacecontract.examples import coconut
# coconut
from chainspacecontract.examples.coconut_util import pack, unpackG1, unpackG2
from chainspacecontract.examples.coconut_lib import setup, elgamal_keygen


####################################################################
q = 7 # total number of messages
t, n = 2, 3 # threshold and total numbero of authorities
epoch = 1 # coconut's epoch
(priv, pub) = elgamal_keygen(setup(q)) # user's key pair 

class Test(unittest.TestCase):
    # --------------------------------------------------------------
    # test init
    # --------------------------------------------------------------
    def test_init(self):
        checker_service_process = Process(target=coconut_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ## create transaction
        transaction = coconut.init()

        ## submit transaction
        response = requests.post(
            'http://127.0.0.1:5000/' + coconut_contract.contract_name 
            + '/init', json=transaction_to_solution(transaction)
        )
        self.assertTrue(response.json()['success'])

        ## stop service
        checker_service_process.terminate()
        checker_service_process.join()


    # --------------------------------------------------------------
    # test issue
    # --------------------------------------------------------------
    def test_issue(self):
        ## run service
        checker_service_process = Process(target=coconut_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ## create transactions
        # init
        init_transaction = coconut.init()
        token = init_transaction['transaction']['outputs'][0]
        # issue transaction
        parameters = (q, t, n, epoch)
        transaction = coconut.request_issue(
            (token,),
            None,
            parameters,
            pub
        )

        ## submit transaction
        response = requests.post(
            'http://127.0.0.1:5000/' + coconut_contract.contract_name 
            + '/request_issue', json=transaction_to_solution(transaction)
        )
        self.assertTrue(response.json()['success'])

        ## stop service
        checker_service_process.terminate()
        checker_service_process.join()


####################################################################
# main
###################################################################
if __name__ == '__main__':
    unittest.main()
