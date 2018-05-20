""" test a simple transaction """

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
from chainspacecontract.examples import increment

## chainspace version & port
version = '1.0'
port    = '5000'

####################################################################
# authenticated bank transfer
####################################################################
class TestIncrement(unittest.TestCase):

    # --------------------------------------------------------------
    # test simple transaction
    # --------------------------------------------------------------
    def test(self):

        ##
        ## init
        ##
        transaction = increment.init()
        response = requests.post('http://127.0.0.1:' +port+ '/api/' +version+ '/transaction/process', json=transaction)
        self.assertTrue(response.json()['success'] == 'True')
        time.sleep(1)


        ##
        ## increment
        ##
        token = transaction['transaction']['outputs'][0]
        transaction = increment.increment(
            (token,),
            None,
            None
        )
        response = requests.post('http://127.0.0.1:' +port+ '/api/' +version+ '/transaction/process', json=transaction)
        self.assertTrue(response.json()['success'] == 'True')



####################################################################
# main
###################################################################
if __name__ == '__main__':
    unittest.main()
