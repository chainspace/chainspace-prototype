"""A smart contract that implements a voting system bank."""

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
from chainspacecontract.examples.utils import setup, key_gen, pack, unpack, binencrypt
from chainspacecontract.examples.utils import provezero 

## contract name
contract = ChainspaceContract('vote')


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
        'outputs': {'type' : 'VoteToken'},
    }


# ------------------------------------------------------------------
# create vote event
# NOTE: 
#   - only 'inputs', 'reference_inputs' and 'parameters' are used to the framework
#   - if there are more than 3 param, the checker has to be implemented by hand
# ------------------------------------------------------------------
@contract.method('create_vote')
def create_vote(inputs, reference_inputs, parameters, options, participants, tally_pub):

    
    # genrate param
    params = setup()
    pub = unpack(tally_pub)

    # encrypt initial score
    a, b, k = binencrypt(params, pub, 0)   #encryption of a zero
    c = (a, b)
    
    # new account
    new_vote = {
        'type'          : 'VoteEvent',
        "options"       : options,
        "scores"        : [pack(c), pack(c)],
        "participants"  : participants,
        "tally_pub"     : tally_pub
    }

    # proof that all init values are zero
    proof_init_1 = (params, pub, input_c1, input_k1)
    proof_init_2 = (params, pub, input_c2, input_k2)

    # return
    return {
        'outputs': (inputs[0], new_vote),
        'extra_parameters' : {
            'proof_init' : [pack(proof_init_1), pack(proof_init_2)]
        }
    }





####################################################################
# checker
####################################################################
# ------------------------------------------------------------------
# check account's creation
# ------------------------------------------------------------------
@contract.checker('create_vote')
def create_vote_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False 
        if outputs[1]['pub'] == None or outputs[1]['balance'] != 10:
            return False

        # check tokens
        if inputs[0]['type'] != 'VoteToken' or outputs[0]['type'] != 'VoteToken':
            return False
        if outputs[1]['type'] != 'VoteToken':
            return False

        # otherwise
        return True

    except (KeyError, Exception):
        return False




####################################################################
# main
####################################################################
if __name__ == '__main__':
    contract.run()



####################################################################
