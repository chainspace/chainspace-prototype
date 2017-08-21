""" test a transaction with a dependency """

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
from chainspacecontract.examples import increment_twice

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
        """
        packet = {
            'transaction': {
                'contractID' : 'increment_twice',
                'methodID' : 'increment', 
                'inputIDs' : ['hash_value_1'], 
                'referenceInputIDs' : [], 
                'parameters' : (), 
                'outputs' : ['1'], 
                'returns' : (), 
                'dependencies' : [{
                    'contractID' : 'addition',
                    'methodID' : 'increment', 
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
        """

         ##
        ## init
        ##
        transaction = increment_twice.init()
        response = requests.post('http://127.0.0.1:' +port+ '/api/' +version+ '/transaction/process', json=transaction)
        self.assertTrue(response.json()['success'] == 'True')
        time.sleep(1)


        ##
        ## increment_twice
        ##
        zero = transaction['transaction']['outputs'][0]
        transaction = increment_twice.increment(
            ('1',),
            None,
            (zero,),
        )
        print transaction
        response = requests.post('http://127.0.0.1:' +port+ '/api/' +version+ '/transaction/process', json=transaction)
        self.assertTrue(response.json()['success'] == 'True')


####################################################################
# main
###################################################################
if __name__ == '__main__':
    unittest.main()
