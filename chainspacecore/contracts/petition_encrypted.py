"""
    A petition that has encrypted YES|NO signatures but that does not do:

    1) Checking the validity of the petition signatories (are they allowed to sign)
    2) Maintining privacy (the public key of the signatory is used to prevent double signing)

    This petition is an intermediate step towards a full coconut based private petition

"""

####################################################################
# imports
####################################################################
# general
from hashlib import sha256
from json    import dumps, loads
# chainspace
from chainspacecontract import ChainspaceContract
# crypto
from petlib.bn    import Bn
from petlib.ec    import EcGroup
from petlib.ecdsa import do_ecdsa_sign, do_ecdsa_verify
from chainspacecontract.examples.utils import setup, key_gen, pack, unpack, add, add_side
from chainspacecontract.examples.utils import binencrypt, make_table, dec
from chainspacecontract.examples.utils import provezero, verifyzero, provebin, verifybin, proveone, verifyone

## contract name
contract = ChainspaceContract('petition_encrypted')


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
        'outputs': (dumps({'type' : 'PetitionEncToken'}),)
    }

# ------------------------------------------------------------------
# create petition event
# NOTE: 
#   - only 'inputs', 'reference_inputs' and 'parameters' are used to the framework
#   - if there are more than 3 param, the checker has to be implemented by hand
# ------------------------------------------------------------------
@contract.method('create_petition')
def create_petition(inputs, reference_inputs, parameters, options, tally_priv, tally_pub):

    # genrate param
    params = setup()
    pub = unpack(tally_pub)

    # encrypt initial score
    (a, b, k) = binencrypt(params, pub, 0)   # encryption of a zero
    c = (a, b)
    scores = [pack(c) for _ in loads(options)]
    
    # new petition object
    new_petition = {
        'type'          : 'PetitionEncObject',
        'options'       : loads(options),
        'scores'        : scores,
        'tally_pub'     : tally_pub
    }

    # proof that all init values are zero
    proof_init = provezero(params, pub, c, unpack(tally_priv))

    # return
    return {
        'outputs': (inputs[0], dumps(new_petition)),
        'extra_parameters' :
            (pack(proof_init),)
    }

# ------------------------------------------------------------------
# add signature
# NOTE: 
#   - only 'inputs', 'reference_inputs' and 'parameters' are used to the framework
#   - if there are more than 3 param, the checker has to be implemented by hand
# ------------------------------------------------------------------
@contract.method('add_signature')
def add_signature(inputs, reference_inputs, parameters, added_signature):

    old_signature = loads(inputs[0])
    new_signature = loads(inputs[0])
    added_signature = loads(added_signature)
    
    params = setup()
    tally_pub = unpack(old_signature['tally_pub'])

    # encrypt signatures & proofs to build
    enc_added_signatures = []  # encrypted signatures
    proof_bin       = []  # signatures are binary, well-formed, and the prover know the signature's value
    sum_a, sum_b, sum_k = (0, 0, 0)  # sum of signatures equals 1

    # loop over signatures
    for i in range(0, len(added_signature)):
        # encrypt added signature
        (a, b, k) = binencrypt(params, tally_pub, added_signature[i])
        c = (a, b)
        enc_added_signatures.append(pack(c))

        # update new scores
        new_c = add(unpack(old_signature['scores'][i]), c)
        new_signature['scores'][i] = pack(new_c)

        # construct proof of binary
        tmp1 = provebin(params, tally_pub, (a,b), k, added_signature[i])
        proof_bin.append(pack(tmp1))

        # update sum of signature
        if i == 0:
            sum_a, sum_b, sum_k = (a, b, k)
        else:
            sum_c = (sum_a, sum_b)
            sum_a, sum_b, sum_k = add_side(sum_c, c, sum_k, k)
        
    # build proof that sum of signatures equals 1
    sum_c = (sum_a, sum_b)
    proof_sum = proveone(params, tally_pub, sum_c, sum_k)

    # compute signature
    (G, _, _, _) = params
    hasher = sha256()
    hasher.update(dumps(old_signature).encode('utf8'))
    hasher.update(dumps(enc_added_signatures).encode('utf8'))

    # return
    return {
        'outputs': (dumps(new_signature),),
        'extra_parameters' : (
            dumps(enc_added_signatures),
            dumps(proof_bin),
            pack(proof_sum)
        )
    }

# ------------------------------------------------------------------
# tally
# NOTE: 
#   - only 'inputs', 'reference_inputs' and 'parameters' are used to the framework
#   - if there are more than 3 param, the checker has to be implemented by hand
# ------------------------------------------------------------------
@contract.method('tally')
def tally(inputs, reference_inputs, parameters, tally_priv, tally_pub):

    # retrieve last petition
    petition = loads(inputs[0])

    # generate params & retrieve tally's public key
    params = setup()
    table  = make_table(params)
    (G, _, (h0, _, _, _), _) = params

    # decrypt aggregated results
    outcome = []
    for item in petition['scores']:
        outcome.append(dec(params, table, unpack(tally_priv), unpack(item)))

    # proof of decryption
    proof_dec = []
    for i in range(0, len(petition['scores'])):
        a, b = unpack(petition['scores'][i])
        ciphertext = (a, b - outcome[i] * h0) 
        tmp = provezero(params, unpack(tally_pub), ciphertext, unpack(tally_priv))
        proof_dec.append(pack(tmp))

    # signature
    hasher = sha256()
    hasher.update(dumps(petition).encode('utf8'))
    hasher.update(dumps(outcome).encode('utf8'))
    sig = do_ecdsa_sign(G, unpack(tally_priv), hasher.digest())

    # pack result
    result = {
        'type'      : 'PetitionEncResult',
        'outcome'   : outcome
    }

    # return
    return {
        'outputs': (dumps(result),),
        'extra_parameters' : (
            dumps(proof_dec),
            pack(sig)
        )
    }

# ------------------------------------------------------------------
# read
# ------------------------------------------------------------------
@contract.method('read')
def read(inputs, reference_inputs, parameters):

    # return
    return {
        'returns' : (reference_inputs[0],),
    }



####################################################################
# checkers
####################################################################
# ------------------------------------------------------------------
# check petitions's creation
# ------------------------------------------------------------------
@contract.checker('create_petition')
def create_petition_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:

        # retrieve petition
        petition  = loads(outputs[1])
        num_options = len(petition['options'])

        print "CHECKING (create) - check format"
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False 
        if num_options < 1 or num_options != len(petition['scores']):
            return False

        print "CHECKING (create) - check tokens"
        if loads(inputs[0])['type'] != 'PetitionEncToken' or loads(outputs[0])['type'] != 'PetitionEncToken':
            return False
        if petition['type'] != 'PetitionEncObject':
            return False

        print "CHECKING (create) - check proof"
        params = setup()
        proof_init = unpack(parameters[0])
        tally_pub  = unpack(petition['tally_pub'])
        for value in petition['scores']:
            if not verifyzero(params, tally_pub, unpack(value), proof_init):
                return False

        # otherwise
        return True

    except (KeyError, Exception):
        return False

# ------------------------------------------------------------------
# check add signature
# ------------------------------------------------------------------
@contract.checker('add_signature')
def add_signature_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:

        print "CHECKING - parameters " + str(parameters)

        # retrieve petition
        old_signature = loads(inputs[0])
        new_signature = loads(outputs[0])
        num_options = len(old_signature['options'])

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 1 or len(returns) != 0:
            return False 
        if num_options != len(new_signature['scores']) or num_options != len(new_signature['scores']):
            return False
        if old_signature['tally_pub'] != new_signature['tally_pub']:
            return False

        print "CHECKING - tokens"
        if new_signature['type'] != 'PetitionEncObject':
            return False


        print "CHECKING - Generate params"
        # generate params, retrieve tally's public key and the parameters
        params = setup()
        tally_pub  = unpack(old_signature['tally_pub'])
        added_signature = loads(parameters[0])
        proof_bin  = loads(parameters[1])
        proof_sum  = unpack(parameters[2])

        print "CHECKING - verify proofs of binary (Signatures have to be bin values)"
        for i in range(0, num_options):
            if not verifybin(params, tally_pub, unpack(added_signature[i]), unpack(proof_bin[i])):
                return False

        print "CHECKING - verify proof of sum of signatures (sum of signatures has to be 1)"
        sum_a, sum_b = unpack(added_signature[-1])
        sum_c = (sum_a, sum_b)
        for i in range(0, num_options-1):
            sum_c = add(sum_c, unpack(added_signature[i]))
        if not verifyone(params, tally_pub, sum_c, proof_sum):
            return False

        print "CHECKING - verify that output == input + signature"
        for i in range(0, num_options):
            tmp_c = add(unpack(old_signature['scores'][i]), unpack(added_signature[i]))
            if not new_signature['scores'][i] == pack(tmp_c):
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

        # retrieve petition
        petition   = loads(inputs[0])
        result = loads(outputs[0])

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 1 or len(returns) != 0:
            return False 
        if len(petition['options']) != len(result['outcome']):
            return False

        # check tokens
        if result['type'] != 'PetitionEncResult':
            return False

        # generate params, retrieve tally's public key and the parameters
        params = setup()
        (G, _, (h0, _, _, _), _) = params
        tally_pub  = unpack(petition['tally_pub'])
        proof_dec  = loads(parameters[0])
        sig        = unpack(parameters[1])
        outcome    = result['outcome']

        # verify proof of correct decryption
        for i in range(0, len(petition['scores'])):
            a, b = unpack(petition['scores'][i])
            ciphertext = (a, b - outcome[i] * h0) 
            if not verifyzero(params, tally_pub, ciphertext, unpack(proof_dec[i])):
                return False

        # verify signature
        hasher = sha256()
        hasher.update(dumps(petition).encode('utf8'))
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

        # check format
        if len(inputs) != 0 or len(reference_inputs) != 1 or len(outputs) != 0 or len(returns) != 1:
            return False 

        # check values
        if reference_inputs[0] != returns[0]:
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
