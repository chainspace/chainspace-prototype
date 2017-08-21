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
from chainspacecontract.examples.addition import contract as addition_contract
from chainspacecontract.examples import addition


####################################################################
# authenticated bank transfer
####################################################################
class Test(unittest.TestCase):
    # --------------------------------------------------------------
    # test init
    # --------------------------------------------------------------
    def test(self):
        ##
        ## run service
        ##
        checker_service_process = Process(target=addition_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## init
        ##
        transaction = addition.init()


        ##
        ## increment
        ##
        zero = transaction['transaction']['outputs'][0]
        transaction = addition.increment(
            (zero,),
            None,
            None
        )


        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + addition_contract.contract_name + '/increment', json=transaction_to_solution(transaction)
        )
        print response.json()['success']
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
