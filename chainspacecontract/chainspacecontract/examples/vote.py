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
from chainspacecontract.examples.utils import setup, key_gen, pack, unpack, add, add_side
from chainspacecontract.examples.utils import binencrypt, make_table, dec
from chainspacecontract.examples.utils import provezero, verifyzero, provebin, verifybin, proveone, verifyone

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
        'outputs': ({'type' : 'VoteToken'},)
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
    (a, b, k) = binencrypt(params, pub, 0)   # encryption of a zero
    c = (a, b)
    scores = [pack(c) for _ in options]
    
    # new vote object
    new_vote = {
        'type'          : 'VoteObject',
        'options'       : options,
        'scores'        : scores,
        'participants'  : participants,
        'tally_pub'     : tally_pub
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
# add vote
# NOTE: 
#   - only 'inputs', 'reference_inputs' and 'parameters' are used to the framework
#   - if there are more than 3 param, the checker has to be implemented by hand
# ------------------------------------------------------------------
@contract.method('add_vote')
def add_vote(inputs, reference_inputs, parameters, added_vote, voter_priv, voter_pub):

    # retrieve old vote & init new vote object
    old_vote = inputs[0]
    new_vote = copy.deepcopy(old_vote)

    # generate params & retrieve tally's public key
    params = setup()
    tally_pub = unpack(old_vote['tally_pub'])

    # encrypt votes & proofs to build
    enc_added_votes = []  # encrypted votes
    proof_bin       = []  # votes are binary, well-formed, and the prover know the vote's value
    sum_a, sum_b, sum_k = (0, 0, 0)  # sum of votes equals 1

    # loop over votes
    for i in range(0,len(added_vote)):
        # encrypt added vote
        (a, b, k) = binencrypt(params, tally_pub, added_vote[i])
        c = (a, b)
        enc_added_votes.append(pack(c))

        # update new scores
        new_c = add(unpack(old_vote['scores'][i]), c)
        new_vote['scores'][i] = pack(new_c)

        # construct proof of binary
        tmp1 = provebin(params, tally_pub, (a,b), k, added_vote[i])
        proof_bin.append(pack(tmp1))

        # update sum of votes
        if i == 0:
            sum_a, sum_b, sum_k = (a, b, k)
        else:
            sum_c = (sum_a, sum_b)
            sum_a, sum_b, sum_k = add_side(sum_c, c, sum_k, k)
        
    # build proof that sum of votes equals 1
    sum_c = (sum_a, sum_b)
    proof_sum = proveone(params, tally_pub, sum_c, sum_k)

    # remove voter from participants
    new_vote['participants'].remove(voter_pub)
    
    # compute signature
    (G, _, _, _) = params
    hasher = sha256()
    hasher.update(dumps(old_vote).encode('utf8'))
    hasher.update(dumps(enc_added_votes).encode('utf8'))
    sig = do_ecdsa_sign(G, unpack(voter_priv), hasher.digest())

    # return
    return {
        'outputs': (new_vote,),
        'extra_parameters' : {
            'votes'     : enc_added_votes,
            'signature' : pack(sig),
            'voter_pub' : voter_pub,  # already packed
            'proof_bin' : proof_bin,
            'proof_sum' : pack(proof_sum)
        }
    }

# ------------------------------------------------------------------
# tally
# NOTE: 
#   - only 'inputs', 'reference_inputs' and 'parameters' are used to the framework
#   - if there are more than 3 param, the checker has to be implemented by hand
# ------------------------------------------------------------------
@contract.method('tally')
def tally(inputs, reference_inputs, parameters, tally_priv):

    # retrieve last vote
    vote = inputs[0]

    # generate params & retrieve tally's public key
    params = setup()
    table  = make_table(params)

    # decrypt aggregated results
    outcome = []
    for item in vote['scores']:
        outcome.append(dec(params, table, unpack(tally_priv), unpack(item)))

    # proof of decryption
    ##
    proof_dec = []
    ##

    # signature
    (G, _, _, _) = params
    hasher = sha256()
    hasher.update(dumps(vote).encode('utf8'))
    hasher.update(dumps(outcome).encode('utf8'))
    sig = do_ecdsa_sign(G, unpack(tally_priv), hasher.digest())

    # pack result
    result = {
        'type'      : 'VoteResult',
        'outcome'   : outcome
    }

    # return
    return {
        'outputs': (result,),
        'extra_parameters' : {
            'proof_dec' : proof_dec,
            'signature' : pack(sig)
        }
    }

# ------------------------------------------------------------------
# read
# ------------------------------------------------------------------
@contract.method('read')
def read(inputs, reference_inputs, parameters):

    # retrieve last vote
    result = reference_inputs[0]

    # set returns
    returns = {
        'outcome' : result['outcome']
    }

    # return
    return {
        'returns': returns
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
        num_votes = len(vote['options'])

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False 
        if num_votes < 1 or num_votes != len(vote['scores']):
            return False
        if vote['participants'] == None:
            return False

        # check tokens
        if inputs[0]['type'] != 'VoteToken' or outputs[0]['type'] != 'VoteToken':
            return False
        if vote['type'] != 'VoteObject':
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
        old_vote = inputs[0]
        new_vote = outputs[0]
        num_votes = len(old_vote['options'])

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 1 or len(returns) != 0:
            return False 
        if num_votes != len(new_vote['scores']) or num_votes != len(new_vote['scores']):
            return False
        if new_vote['participants'] == None:
            return False
        if old_vote['tally_pub'] != new_vote['tally_pub']:
            return False

        # check tokens
        if new_vote['type'] != 'VoteObject':
            return False

        # check that voter has been removed from participants
        if not parameters['voter_pub'] in old_vote['participants']:
            return False
        if parameters['voter_pub'] in new_vote['participants']:
            return False
        if len(old_vote['participants']) != len(new_vote['participants']) + 1:
            return False

        # generate params, retrieve tally's public key and the parameters
        params = setup()
        tally_pub  = unpack(old_vote['tally_pub'])
        added_vote = parameters['votes']
        sig        = unpack(parameters['signature'])
        voter_pub  = unpack(parameters['voter_pub'])
        proof_bin  = parameters['proof_bin']
        proof_sum  = unpack(parameters['proof_sum'])

        # verify signature
        (G, _, _, _) = params
        hasher = sha256()
        hasher.update(dumps(old_vote).encode('utf8'))
        hasher.update(dumps(added_vote).encode('utf8'))
        if not do_ecdsa_verify(G, voter_pub, sig, hasher.digest()):
            return False

        # verify proofs of binary (votes have to be bin values)
        for i in range(0, num_votes):
            if not verifybin(params, tally_pub, unpack(added_vote[i]), unpack(proof_bin[i])):
                return False

        # verify proof of sum of votes (sum of votes has to be 1)
        sum_a, sum_b = unpack(added_vote[-1])
        sum_c = (sum_a, sum_b)
        for i in range(0, num_votes-1):
            sum_c = add(sum_c, unpack(added_vote[i]))
        if not verifyone(params, tally_pub, sum_c, proof_sum):
            return False

        # verify that output == input + vote
        for i in range(0, num_votes):
            tmp_c = add(unpack(old_vote['scores'][i]), unpack(added_vote[i]))
            if not new_vote['scores'][i] == pack(tmp_c):
                return False

        # otherwise
        return True

    except (KeyError, Exception):
        return False

# ------------------------------------------------------------------
# check tally
# ------------------------------------------------------------------
@contract.checker('tally')
def tally_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:

        # retrieve vote
        vote   = inputs[0]
        result = outputs[0]

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 1 or len(returns) != 0:
            return False 
        if len(vote['options']) != len(result['outcome']):
            return False

        # check tokens
        if result['type'] != 'VoteResult':
            return False

        # generate params, retrieve tally's public key and the parameters
        params = setup()
        tally_pub  = unpack(vote['tally_pub'])
        sig        = unpack(parameters['signature'])
        proof_dec  = parameters['proof_dec']

        # verify proof of correct decryption
        ##
        ##

        # verify signature
        (G, _, _, _) = params
        hasher = sha256()
        hasher.update(dumps(vote).encode('utf8'))
        hasher.update(dumps(result['outcome']).encode('utf8'))
        if not do_ecdsa_verify(G, tally_pub, sig, hasher.digest()):
            return False

        # otherwise
        return True

    except (KeyError, Exception):
        return False

# ------------------------------------------------------------------
# check read
# ------------------------------------------------------------------
@contract.checker('read')
def read_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:

        # retrieve results
        result = reference_inputs[0]

        # check format
        if len(inputs) != 0 or len(reference_inputs) != 1 or len(outputs) != 0 or len(returns) != 1:
            return False 

        # check values
        if result['outcome'] != returns['outcome']:
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
