""" test a simple transaction """

####################################################################
# imports
###################################################################
# general
from multiprocessing import Process
from json            import dumps, loads
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

        ## create packet
        packet = {
            'transaction': {
                'inputIDs': ['hash_value'], 
                'methodID': 'increment', 
                'parameters': (), 
                'outputs': ['1'], 
                'returns': (), 
                'dependencies': [], 
                'referenceInputIDs': [], 
                'contractID': 'addition'
            }, 
            'store': {
                'hash_value': '0'
            }
        }

        ## send transaction
        response = requests.post('http://127.0.0.1:' +port+ '/api/' +version+ '/transaction/process', json=packet)
        print response.json()
        self.assertTrue(response.json()['success'] == 'True')


####################################################################
# main
###################################################################
if __name__ == '__main__':
    unittest.main()
