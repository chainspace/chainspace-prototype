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
from chainspacecontract.examples.utils import provezero, verifyzero

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
        'type'          : 'Vote',
        "options"       : options,
        "scores"        : [pack(c), pack(c)],
        "participants"  : participants,
        "tally_pub"     : tally_pub
    }

    # proof that all init values are zero
    proof_init = provezero(params, pub, c, k)

    # return
    return {
        'outputs': (inputs[0], new_vote),
        'extra_parameters' : {
            'proof_init' : pack(proof_init)
        }
    }

# ------------------------------------------------------------------
# create vote event
# NOTE: 
#   - only 'inputs', 'reference_inputs' and 'parameters' are used to the framework
#   - if there are more than 3 param, the checker has to be implemented by hand
# ------------------------------------------------------------------
@contract.method('add_vote')
def add_vote(inputs, reference_inputs, parameters, voter_pub):

    # return
    return {
        'outputs': (),
        'extra_parameters' : {
            'proof_init' : ()
        }
    }




####################################################################
# checker
####################################################################
# ------------------------------------------------------------------
# check vote's creation
# ------------------------------------------------------------------
@contract.checker('create_vote')
def create_vote_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:

        # retrieve vote
        vote = outputs[1]

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False 
        if  len(vote['options']) < 1 or len(vote['options']) != len(vote['scores']):
            return False
        if vote['participants'] == None:
            return False

        # check tokens
        if inputs[0]['type'] != 'VoteToken' or outputs[0]['type'] != 'VoteToken':
            return False
        if vote['type'] != 'Vote':
            return False

        # check proof
        params = setup()
        proof_init = unpack(parameters['proof_init'])
        tally_pub  = unpack(vote['tally_pub'])
        for value in vote['scores']:
            if not verifyzero(params, tally_pub, unpack(value), proof_init):
                return False

        # otherwise
        return True

    except (KeyError, Exception):
        return False

# ------------------------------------------------------------------
# check add vote
# ------------------------------------------------------------------
@contract.checker('add_vote')
def add_vote_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:

        # retrieve vote
        vote = outputs[1]

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 1 or len(returns) != 0:
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
