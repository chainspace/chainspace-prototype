""" 
	CoCoNut smart contract.
"""


####################################################################
# imports
####################################################################
# general
from json    import dumps, loads
# chainspace
from chainspacecontract import ChainspaceContract
# coconut
from chainspacecontract.examples.coconut_util import bn_pack, bn_unpack, pack, unpackG1, unpackG2
from chainspacecontract.examples.coconut_lib import setup, prepare_mix_sign, verify_mix_sign, mix_sign
from bplib.bp import BpGroup, G2Elem

## contract name
contract = ChainspaceContract('coconut')


####################################################################
# methods
####################################################################
# ------------------------------------------------------------------
# init
# ------------------------------------------------------------------
@contract.method('init')
def init():
	# ID lists
	ID_list = {
		'type' : 'CoCoList',
		'list' : []
	}
	return {
	    'outputs': (dumps({'type' : 'CoCoToken'}), dumps(ID_list)),
	}

# ------------------------------------------------------------------
# request_issue
# NOTE: 
#   - only 'inputs', 'reference_inputs' and 'parameters' are used to the framework
#   - if there are more than 3 param, the checker has to be implemented by hand
# ------------------------------------------------------------------
@contract.method('request_issue')
def request_issue(inputs, reference_inputs, parameters, pub, ID):
    (q, t, n, epoch) = parameters
    params = setup(q)

    # execute PrepareMixSign
    clear_m = [epoch]
    hidden_m = [ID]
    (cm, c, proof) = prepare_mix_sign(params, clear_m, hidden_m, pub)
    enc_ID = c[0]

    # new petition object
    issue_request = {
        'type' : 'CoCoRequest',
        'cm' : pack(cm),
        'c' : (pack(enc_ID[0]), pack(enc_ID[1]))
    }

    # return
    return {
		'outputs': (inputs[0], dumps(issue_request)),
        'extra_parameters' : (bn_pack(proof), pack(pub))
	}

# ------------------------------------------------------------------
# issue
# NOTE: 
#   - To be executed only by the first authority, the others have to
#     call 'add'.
# ------------------------------------------------------------------
@contract.method('issue')
def issue(inputs, reference_inputs, parameters, sk, vvk):
    (q, t, n, epoch) = parameters
    params = setup(q)

    # extract request
    issue_request = loads(inputs[0])
    cm = unpackG1(params, issue_request['cm'])
    c = [(unpackG1(params, issue_request['c'][0]), unpackG1(params, issue_request['c'][1]))]

    (h, enc_epsilon) = mix_sign(params, sk, cm, c, [epoch]) 
    packet = (pack(h), (pack(enc_epsilon[0]), pack(enc_epsilon[1])))

    # new petition object
    credential = {
        'type' : 'CoCoCredential',
        'sigs' : [packet]
    }

    # add vkk in parameters
    (g2, X, Y) = vvk
    packed_vvk = (pack(g2),pack(X),[pack(y) for y in Y])

    # return
    return {
        'outputs': (dumps(credential),),
        'extra_parameters' : (issue_request['cm'], issue_request['c'], packed_vvk),
    }

# ------------------------------------------------------------------
# add
# NOTE: 
#   - Updates the previous object to limit the numer of active obj.
# ------------------------------------------------------------------
@contract.method('add')
def add(inputs, reference_inputs, parameters, sk, packed_cm, packed_c, packed_vvk):
    (q, t, n, epoch) = parameters
    params = setup(q)

    # extract request
    old_credentials = loads(inputs[0])
    new_credentials = loads(inputs[0])
    cm = unpackG1(params, packed_cm)
    c = [(unpackG1(params, packed_c[0]), unpackG1(params, packed_c[1]))]

    # sign
    (h, enc_epsilon) = mix_sign(params, sk, cm, c, [epoch]) 
    
    # new petition object
    packet = (pack(h), (pack(enc_epsilon[0]), pack(enc_epsilon[1])))
    new_credentials['sigs'].append(packet)

    # return
    return {
        'outputs': (dumps(new_credentials),),
        'extra_parameters' : (packet, packed_cm, packed_c, packed_vvk)
    }

# ------------------------------------------------------------------
# spend
# NOTE: 
#   - this transaction should be used as callback for CSCoin.
# ------------------------------------------------------------------
@contract.method('spend')
def spend(inputs, reference_inputs, parameters, sig, ID, packed_vvk):
    (q, t, n, epoch) = parameters
    params = setup(q)

    # add ID to the list of spent ID
    new_ID_list = loads(inputs[0])
    new_ID_list['list'].append(bn_pack(ID))
    print(new_ID_list)

    # pakc sig
    packed_sig = (pack(sig[0]), pack(sig[1]))
    
    # return
    return {
        'outputs': (dumps(new_ID_list),),
        'extra_parameters' : (bn_pack(ID), packed_sig, packed_vvk)
    }


####################################################################
# checker
####################################################################
# ------------------------------------------------------------------
# check request issue
# ------------------------------------------------------------------
@contract.checker('request_issue')
def request_issue_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:
        # params
        (q, t, n, epoch) = parameters[0], parameters[1], parameters[2], parameters[3] 
        params = setup(q)

        # get objects
        issue_request = loads(outputs[1])
        cm = unpackG1(params, issue_request['cm'])
        c = [(unpackG1(params, issue_request['c'][0]), unpackG1(params, issue_request['c'][1]))]
        proof = tuple(bn_unpack(parameters[4]))
        pub = unpackG1(params, parameters[5])

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False 
        if len(parameters) != 6:
            return False

        # check types
        if loads(inputs[0])['type'] != 'CoCoToken' or loads(outputs[0])['type'] != 'CoCoToken':
            return False
        if issue_request['type'] != 'CoCoRequest':
            return False

        # verify proof
        if not verify_mix_sign(params, pub, c, cm, proof):
            return False

        ## TODO
        # verify depend transaction -- payment

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
        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 1 or len(returns) != 0:
            return False 
        if len(parameters) != 7:
            return False

        # check types
        if loads(inputs[0])['type'] != 'CoCoRequest' or loads(outputs[0])['type'] != 'CoCoCredential':
            return False

        # otherwise
        return True

    except (KeyError, Exception):
        return False

# ------------------------------------------------------------------
# check add
# ------------------------------------------------------------------
@contract.checker('add')
def add_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:
        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 1 or len(returns) != 0:
            return False 
        if len(parameters) != 8:
            return False

        # check types
        if loads(inputs[0])['type'] != 'CoCoCredential' or loads(outputs[0])['type'] != 'CoCoCredential':
            return False

        # check list
        new_credentials = loads(outputs[0])
        old_credentials = loads(inputs[0])
        added_sig = parameters[4]
        if new_credentials['sigs'] != old_credentials['sigs'] + [added_sig]:
        	return False

        # otherwise
        return True

    except (KeyError, Exception):
        return False

# ------------------------------------------------------------------
# check spend
# ------------------------------------------------------------------
@contract.checker('spend')
def spend_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:
    	
        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 1 or len(returns) != 0:
            return False 
        if len(parameters) != 7:
            return False

        # check types
        if loads(inputs[0])['type'] != 'CoCoList' or loads(outputs[0])['type'] != 'CoCoList':
            return False
   
    	# get parameters
        (q, t, n, epoch) = parameters[0], parameters[1], parameters[2], parameters[3] 
    	old_ID_list = loads(inputs[0])['list']
    	new_ID_list = loads(outputs[0])['list']
        packed_ID = parameters[4]
        ID = bn_unpack(packed_ID)
        packed_vvk = parameters[6]
    	
        ## verify sign
        params = setup(q)
        (G, o, g1, hs, g2, e) = params
        sig = (unpackG1(params,parameters[5][0]), unpackG1(params,parameters[5][1]))
        vvk = (unpackG2(params,packed_vvk[0]), unpackG2(params,packed_vvk[1]), [unpackG2(params,y) for y in packed_vvk[2]])
        (g2, X, Y) = vvk
        (h, epsilon) = sig
        assert not h.isinf() and e(h, X + ID*Y[0] + epoch*Y[1]) == e(epsilon, g2) 

        # check ID has not been spent
        if ID in old_ID_list:
        	return False

    	if new_ID_list != old_ID_list + [packed_ID]:
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