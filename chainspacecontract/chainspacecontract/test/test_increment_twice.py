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
from chainspacecontract.examples.increment_twice import contract as increment_twice_contract
from chainspacecontract.examples import increment_twice


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
        checker_service_process = Process(target=increment_twice_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ##
        ## init
        ##
        transaction = increment_twice.init()


        ##
        ## increment
        ##
        zero = transaction['transaction']['outputs'][0]
        transaction = increment_twice.increment(
            ('1',),
            None,
            [zero]
        )
        print transaction


        ##
        ## submit transaction
        ##
        response = requests.post(
            'http://127.0.0.1:5000/' + increment_twice_contract.contract_name + '/increment', json=transaction_to_solution(transaction)
        )
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
