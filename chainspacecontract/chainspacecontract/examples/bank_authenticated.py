"""A smart contract that implements a simple, authenticated bank."""

####################################################################
# imports
####################################################################
# general
from hashlib import sha256
from json    import dumps
import copy
# chainspace
from chainspacecontract import ChainspaceContract
# crypto
from petlib.ecdsa import do_ecdsa_sign, do_ecdsa_verify
from chainspacecontract.examples.utils import setup, key_gen, pack, unpack

## contract name
contract = ChainspaceContract('bank_authenticated')


####################################################################
# methods
####################################################################
# ------------------------------------------------------------------
# init
# ------------------------------------------------------------------
@contract.method('init')
def init():

    # return
    return {
        'outputs': ({'type' : 'BankToken'},),
    }


# ------------------------------------------------------------------
# create account
# NOTE:
#   - only 'inputs', 'reference_inputs' and 'parameters' are used to the framework
#   - if there are more than 3 param, the checker has to be implemented by hand
# ------------------------------------------------------------------
@contract.method('create_account')
def create_account(inputs, reference_inputs, parameters, pub):

    # new account
    new_account = {'type' : 'BankAccount', 'pub': pack(pub), 'balance': 10}

    # return
    return {
        'outputs': (inputs[0], new_account)
    }


# ------------------------------------------------------------------
# make transfer
# NOTE:
#   - only 'inputs', 'reference_inputs' and 'parameters' are used to the framework
#   - if there are more than 3 param, the checker has to be implemented by hand
# ------------------------------------------------------------------
@contract.method('auth_transfer')
def auth_transfer(inputs, reference_inputs, parameters, priv):

    # compute outputs
    new_from_account = copy.deepcopy(inputs[0])
    new_to_account   = copy.deepcopy(inputs[1])
    new_from_account["balance"] -= parameters['amount']
    new_to_account["balance"]   += parameters['amount']

    # hash message to sign
    hasher = sha256()
    hasher.update(dumps(inputs).encode('utf8'))
    hasher.update(dumps(reference_inputs).encode('utf8'))
    hasher.update(dumps({"amount" : parameters["amount"]}).encode('utf8'))

    # sign message
    G = setup()[0]
    sig = do_ecdsa_sign(G, unpack(priv), hasher.digest())

    # return
    return {
        'outputs': (new_from_account, new_to_account),
        'extra_parameters' : {
            'signature' : pack(sig)
        }
    }


####################################################################
# checker
####################################################################
# ------------------------------------------------------------------
# check account's creation
# ------------------------------------------------------------------
@contract.checker('create_account')
def create_account_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False
        if outputs[1]['pub'] == None or outputs[1]['balance'] != 10:
            return False

        # check tokens
        if inputs[0]['type'] != 'BankToken' or outputs[0]['type'] != 'BankToken':
            return False
        if outputs[1]['type'] != 'BankAccount':
            return False

        # otherwise
        return True

    except (KeyError, Exception):
        return False


# ------------------------------------------------------------------
# check transfer
# ------------------------------------------------------------------
@contract.checker('auth_transfer')
def auth_transfer_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:

        # check format
        if len(inputs) != 2 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False
        if inputs[0]['pub'] != outputs[0]['pub'] or inputs[1]['pub'] != outputs[1]['pub']:
            return False

        # check tokens
        if inputs[0]['type'] != 'BankAccount' or inputs[1]['type'] != 'BankAccount':
            return False
        if outputs[0]['type'] != 'BankAccount' or outputs[1]['type'] != 'BankAccount':
            return False

        # amount transfered should be non-negative
        if parameters['amount'] < 0:
            return False

        # amount transfered should not exceed balance
        if inputs[0]['balance'] < parameters['amount']:
            return False

        # consistency between inputs and outputs
        if inputs[0]['balance'] != outputs[0]['balance'] + parameters['amount']:
            return False
        if inputs[1]['balance'] != outputs[1]['balance'] - parameters['amount']:
            return False


        # hash message to verify signature
        hasher = sha256()
        hasher.update(dumps(inputs).encode('utf8'))
        hasher.update(dumps(reference_inputs).encode('utf8'))
        hasher.update(dumps({"amount" : parameters["amount"]}).encode('utf8'))

        # recompose signed digest
        pub = unpack(inputs[0]['pub'])
        sig = unpack(parameters['signature'])

        # verify signature
        (G, _, _, _) = setup()
        return do_ecdsa_verify(G, pub, sig, hasher.digest())

    except (KeyError, Exception):
        return False




####################################################################
# main
####################################################################
if __name__ == '__main__':
    contract.run()



####################################################################
