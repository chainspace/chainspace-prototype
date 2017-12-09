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
from chainspacecontract.examples.coconut_lib import setup, prepare_mix_sign, verify_mix_sign

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
    return {
        'outputs': (dumps({'type' : 'CoCoToken'}),),
    }

# ------------------------------------------------------------------
# request_issue
# NOTE: 
#   - only 'inputs', 'reference_inputs' and 'parameters' are used to the framework
#   - if there are more than 3 param, the checker has to be implemented by hand
# ------------------------------------------------------------------
@contract.method('request_issue')
def request_issue(inputs, reference_inputs, parameters, pub):
    (q, t, n, epoch) = parameters
    params = setup(q)

    # generate ID
    G = params[0]
    ID = G.order().random()

    # execute PrepareMixSign
    clear_m = [epoch]
    hidden_m = [ID]
    (cm, c, proof) = prepare_mix_sign(params, clear_m, hidden_m, pub)
    enc_ID = c[0]

    # new petition object
    request_issue = {
        'type' : 'CoCoRequest',
        'cm' : pack(cm),
        'c' : (pack(enc_ID[0]), pack(enc_ID[1]))
    }

    # return
    return {
		'outputs': (inputs[0], dumps(request_issue)),
        'extra_parameters' : (bn_pack(proof), pack(pub))
	}


####################################################################
# checker
####################################################################
# ------------------------------------------------------------------
# check petition's creation
# ------------------------------------------------------------------
@contract.checker('request_issue')
def request_issue_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:
        # params
        (q, t, n, epoch) = parameters[0], parameters[1], parameters[2], parameters[3] 
        params = setup(q)

        # get objects
        request_issue = loads(outputs[1])
        cm = unpackG1(params, request_issue['cm'])
        c = [(unpackG1(params, request_issue['c'][0]), unpackG1(params, request_issue['c'][1]))]
        proof = tuple(bn_unpack(parameters[4]))
        pub = unpackG1(params, parameters[5])

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False 
        if len(parameters) != 6:
            return False

        # check tokens
        if loads(inputs[0])['type'] != 'CoCoToken' or loads(outputs[0])['type'] != 'CoCoToken':
            return False
        if request_issue['type'] != 'CoCoRequest':
            return False

        # verify proof
        if not verify_mix_sign(params, pub, c, cm, proof):
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