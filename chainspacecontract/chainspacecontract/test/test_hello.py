""" test authenticated bank transfer """

####################################################################
# imports
###################################################################
# general
from multiprocessing import Process
from hashlib import sha256
from binascii import hexlify, unhexlify
from json import dumps, loads
import time
import unittest
import requests
# cypto
from petlib.bn import Bn
# chainspace
from chainspacecontract import transaction_to_solution
from chainspacecontract.examples.hello import contract as hello_contract
from chainspacecontract.examples import hello

####################################################################
class Test(unittest.TestCase):
    # --------------------------------------------------------------
    # test init
    # --------------------------------------------------------------
    def test_init(self):
        checker_service_process = Process(target=hello_contract.run_checker_service)
        checker_service_process.start()
        time.sleep(0.1)

        ## create transaction
        transaction = hello.init()

        ## submit transaction
        response = requests.post(
            'http://127.0.0.1:5000/' + hello_contract.contract_name 
            + '/init', json=transaction_to_solution(transaction)
        )
        self.assertTrue(response.json()['success'])

        ## stop service
        checker_service_process.terminate()
        checker_service_process.join()

    # --------------------------------------------------------------
    # test hello
    # --------------------------------------------------------------
    def test_hello(self):
		checker_service_process = Process(target=hello_contract.run_checker_service)
		checker_service_process.start()
		time.sleep(0.1)

		## create transaction
		# init
		init_transaction = hello.init()
		token = init_transaction['transaction']['outputs'][0]
		# create instance
		transaction = hello.hello(
			(token,),
			None,
			None
		)

		## submit transaction
		response = requests.post(
			'http://127.0.0.1:5000/' + hello_contract.contract_name 
			+ '/hello', json=transaction_to_solution(transaction)
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
