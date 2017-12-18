""" 
	CoCoNut smart contract.
"""


####################################################################
# imports
####################################################################
# general
from json import dumps, loads
from hashlib import sha256
# cypto
from petlib.bn import Bn
# chainspace
from chainspacecontract import ChainspaceContract
# coconut
from chainspacecontract.examples.coconut_util import pet_pack, pet_unpack, pack, unpackG1, unpackG2
from chainspacecontract.examples.coconut_lib import setup, mix_verify, prepare_mix_sign, verify_mix_sign, mix_sign

## contract name
contract = ChainspaceContract('coconut')

## dependencies
from chainspacecontract.examples.hello import contract as hello_contract
contract.register_dependency(hello_contract)


####################################################################
# methods
####################################################################
# ------------------------------------------------------------------
# init
# ------------------------------------------------------------------
@contract.method('init')
def init():
	return {
	    'outputs': (dumps({'type' : 'CoCoToken'}),),
	}

# ------------------------------------------------------------------
# create
# NOTE:
#   - sig is an aggregated sign on the hash of the instance object
# ------------------------------------------------------------------
@contract.method('create')
def create(inputs, reference_inputs, parameters, q, t, n, callback, vvk, sig):
    # pack sig and vvk
    packed_sig = (pack(sig[0]),pack(sig[1]))
    packed_vvk = (pack(vvk[0]),pack(vvk[1]),[pack(vvk[2][i]) for i in range(q)])

    # new petition object
    instance = {
        'type' : 'CoCoInstance',
        'q' : q,
        't' : t,
        'n' : n,
        'callback' : callback,
        'verifier' : packed_vvk
    }

    # return
    return {
        'outputs': (inputs[0], dumps(instance)),
        'extra_parameters' : (packed_sig,)
    }

# ------------------------------------------------------------------
# request
# NOTE: 
#	- args are the arguments for the callback
# ------------------------------------------------------------------
@contract.method('request')
def request(inputs, reference_inputs, parameters, clear_m, hidden_m, pub, *args):
    # execute PrepareMixSign
    q = loads(inputs[0])['q']
    params = setup(q)
    (cm, c, proof) = prepare_mix_sign(params, clear_m, hidden_m, pub)

    # new petition object
    issue_request = {
        'type' : 'CoCoRequest',
        'instance' : loads(inputs[0]),
        'clear_m' : pet_pack(clear_m),
        'cm' : pack(cm),
        'c' : [(pack(ci[0]), pack(ci[1])) for ci in c],
        'sigs' : []
    }

    # create dependency
    # @Mustafa: we need to modify the framework to make possible to pass a callback here;
    # i.e., make possible to execute callback_function(args) for any function passed as argument
    hello_contract.init(args)

    # return
    return {
		'outputs': (inputs[0], dumps(issue_request)),
        'extra_parameters' : (pet_pack(proof), pack(pub))
	}

# ------------------------------------------------------------------
# issue
# ------------------------------------------------------------------
@contract.method('issue')
def issue(inputs, reference_inputs, parameters, sk, index):
    # extract data
    request = loads(inputs[0])
    updated_request = loads(inputs[0])
    instance = request['instance']
    q = instance['q']
    params = setup(q)
    cm = unpackG1(params, request['cm'])
    c = [(unpackG1(params, packed_ci[0]), unpackG1(params, packed_ci[1])) for packed_ci in request['c']]
    clear_m = pet_unpack(request['clear_m'])
    
    # sign
    (h, enc_epsilon) = mix_sign(params, sk, cm, c, clear_m) 
    packed_enc_sig = (pack(h), (pack(enc_epsilon[0]), pack(enc_epsilon[1])))

    # update request
    # NOTE: indexes are used to re-order the signature for threshold aggregation
    updated_request['sigs'].append((index, packed_enc_sig))

    # return
    return {
        'outputs': (dumps(updated_request),),
        'extra_parameters' : ((index, packed_enc_sig),)
    }


####################################################################
# checker
####################################################################
# ------------------------------------------------------------------
# check create
# ------------------------------------------------------------------
@contract.checker('create')
def create_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:
        # retrieve instance
        instance = loads(outputs[1])
        # retrieve parameters
        packed_sig = parameters[0]

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False 

        # check types
        if inputs[0] != outputs[0]: return False
        if instance['type'] != 'CoCoInstance': return False

        # check fields
        q = instance['q'] 
        t = instance['t'] 
        n = instance['n']
        instance['callback']
        packed_vvk = instance['verifier']
        if q < 1 or n < 1 or t > n: return False

        # verify signature
        params = setup(q)
        sig = (unpackG1(params, packed_sig[0]), unpackG1(params, packed_sig[1]))
        vvk = (unpackG2(params,packed_vvk[0]), unpackG2(params,packed_vvk[1]), [unpackG2(params,y) for y in packed_vvk[2]])
        hasher = sha256()
        hasher.update(outputs[1].encode('utf8'))
        m = Bn.from_binary(hasher.digest())
        if not mix_verify(params, vvk, None, sig, None, [m]): return False
   
        # otherwise
        return True

    except (KeyError, Exception):
        return False

# ------------------------------------------------------------------
# check request issue
# ------------------------------------------------------------------
@contract.checker('request')
def request_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:
        # retrieve instance
        instance = loads(outputs[0])
        request = loads(outputs[1])
        # retrieve parameters
        packed_proof = parameters[0]
        packed_pub = parameters[1]

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False 

        # check types
        if request['type'] != 'CoCoRequest': return False

        # check fields
        request['clear_m']
        params = setup(instance['q'])
        cm = unpackG1(params, request['cm'])
        packed_c = request['c']
        c = [(unpackG1(params, ci[0]), unpackG1(params, ci[1])) for ci in packed_c]
        if inputs[0] != outputs[0] or loads(inputs[0]) != request['instance']: return False
        if request['sigs']: return False

        # verify proof
        proof = pet_unpack(packed_proof)
        pub = unpackG1(params, packed_pub)
        if not verify_mix_sign(params, pub, c, cm, proof): return False

        # verify depend transaction -- specified by 'callback'
        # NOTE: the checker of the dependency is automatcally called
        callback = dependencies[0]
        if callback['contractID']+'.'+callback['methodID'] != instance['callback']: return False

        # otherwise
        return True

    except (KeyError, Exception):
        return False

# ------------------------------------------------------------------
# check issue
# ------------------------------------------------------------------
@contract.checker('issue')
def issue_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:
    	# retrieve data
        old_request = loads(inputs[0])
        new_request = loads(outputs[0])
        old_sigs = old_request.pop('sigs', None)
        new_sigs = new_request.pop('sigs', None)
        added_sig = parameters[0]

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 1 or len(returns) != 0:
            return False 

        # check fields
        if old_request != new_request: return False

        # check signature add
      	if new_sigs != old_sigs + [added_sig]: return False

      	# TODO: verify the partial signature using VK (to include in create_instance)
      	
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