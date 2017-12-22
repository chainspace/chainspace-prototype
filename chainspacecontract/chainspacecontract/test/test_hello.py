""" test authenticated bank transfer """

####################################################################
# imports
###################################################################
# general

from json import dumps, loads
import requests

# chainspace
from chainspacecontract import transaction_to_solution
from chainspacecontract.examples.hello import contract as hello_contract
from chainspacecontract.examples import hello

####################################################################
# --------------------------------------------------------------
# test init
# --------------------------------------------------------------
def test_init():
    with hello_contract.test_service():

        ## create transaction
        transaction = hello.init()

        ## submit transaction
        response = requests.post(
            'http://127.0.0.1:5000/' + hello_contract.contract_name 
            + '/init', json=transaction_to_solution(transaction)
        )
        assert response.json()['success']


# --------------------------------------------------------------
# test hello
# --------------------------------------------------------------
def test_hello():
    with hello_contract.test_service():
        
        init_transaction = hello.init()
        token = init_transaction['transaction']['outputs'][0]
        # create instance
        transaction = hello.hello( (token,), None, None)

        ## submit transaction
        response = requests.post(
            'http://127.0.0.1:5000/' + hello_contract.contract_name 
            + '/hello', json=transaction_to_solution(transaction)
        )
        assert response.json()['success']

