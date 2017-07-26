""" test a transaction with a dependency """

####################################################################
# imports
###################################################################
# general
from multiprocessing import Process
from json            import dumps, loads
import unittest
import requests
# chainspace
from chainspacecontract.examples import increment

## chainspace version & port
version = '1.0'
port    = '3001'

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
                'contractID' : 'dump',
                'methodID' : 'do_nothing', 
                'inputIDs' : ['hash_value_1'], 
                'referenceInputIDs' : [], 
                'parameters' : (), 
                'outputs' : ['1'], 
                'returns' : (), 
                'dependencies' : [{
                    'contractID' : 'dump',
                    'methodID' : 'do_nothing', 
                    'inputIDs' : ['hash_value_2'], 
                    'referenceInputIDs' : [], 
                    'parameters' : (), 
                    'outputs' : ['41'], 
                    'returns' : (), 
                    'dependencies' : []
                }]
            }, 
            'store': {
                'hash_value_1': '0',
                'hash_value_2': '40'
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
