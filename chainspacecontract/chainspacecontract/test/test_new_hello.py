""" test new hello """

####################################################################
# imports
###################################################################
# general

from json import dumps, loads
import requests

# chainspace
from chainspacecontract import transaction_to_solution
from chainspacecontract.examples.new_hello import contract as new_hello_contract
from chainspacecontract.examples import new_hello

####################################################################
# --------------------------------------------------------------
# test init
# --------------------------------------------------------------
def test_init():
    with new_hello_contract.test_service():

        ## create transaction
        transaction = new_hello.init()

        ## submit transaction
        response = requests.post(
            'http://127.0.0.1:5000/' + new_hello_contract.contract_name
            + '/init', json=transaction_to_solution(transaction)
        )
        assert response.json()['success']


# --------------------------------------------------------------
# test hello
# --------------------------------------------------------------
def test_hello():
    with new_hello_contract.test_service():
        
        init_transaction = new_hello.init()
        token = init_transaction['transaction']['outputs'][0]
        # create instance
        transaction = new_hello.hello( (token,), None, None)

        ## submit transaction
        response = requests.post(
            'http://127.0.0.1:5000/' + new_hello_contract.contract_name
            + '/hello', json=transaction_to_solution(transaction)
        )
        assert response.json()['success']

# --------------------------------------------------------------
# test reply
# --------------------------------------------------------------
def test_reply():
    with new_hello_contract.test_service():
        
        init_transaction = new_hello.init()
        token = init_transaction['transaction']['outputs'][0]
        hello_transaction = new_hello.hello( (token,), None, None)
        message = hello_transaction['transaction']['outputs'][1]
        # create instance
        transaction = new_hello.reply( (message,), None, None)
        
        ## submit transaction
        response = requests.post(
                                 'http://127.0.0.1:5000/' + new_hello_contract.contract_name
                                 + '/reply', json=transaction_to_solution(transaction)
                                 )
        assert response.json()['success']
