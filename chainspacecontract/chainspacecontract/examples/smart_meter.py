"""A smart contract that implements a smart meter."""

####################################################################
# imports
####################################################################
# general
from hashlib import sha256
from json    import dumps, loads
# chainspace
from chainspacecontract import ChainspaceContract
# crypto
from petlib.ecdsa import do_ecdsa_sign, do_ecdsa_verify
from chainspacecontract.examples.utils import setup, key_gen, pack, unpack

## contract name
contract = ChainspaceContract('smart_meter')


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
        'outputs': (dumps({'type' : 'SMToken'}),),
    }

# ------------------------------------------------------------------
# create meter
# NOTE:
#   - only 'inputs', 'reference_inputs' and 'parameters' are used to the framework
#   - if there are more than 3 param, the checker has to be implemented by hand
# ------------------------------------------------------------------
@contract.method('create_meter')
def create_meter(inputs, reference_inputs, parameters, pub, info, tariffs, billing_period):

    # new meter
    new_meter = {
        'type'           : 'SMMeter', 
        'pub'            : pub, 
        'info'           : info,
        'readings'       : [],
        'billing_period' : loads(billing_period),
        'tariffs'        : loads(tariffs)
    }

    # return
    return {
        'outputs': (inputs[0], dumps(new_meter))
    }

# ------------------------------------------------------------------
# add_reading
# NOTE:
#   - only 'inputs', 'reference_inputs' and 'parameters' are used to the framework
#   - if there are more than 3 param, the checker has to be implemented by hand
# ------------------------------------------------------------------
@contract.method('add_reading')
def add_reading(inputs, reference_inputs, parameters, meter_priv, reading, opening):

    # compute output
    old_meter = loads(inputs[0])
    new_meter = loads(inputs[0])

    # create commitement to the reading
    (G, g, (h0, _, _, _), _) = setup()
    commitment = loads(reading) * g + unpack(opening) * h0

    # update readings
    new_meter['readings'].append(pack(commitment))

    # hash message to sign
    hasher = sha256()
    hasher.update(dumps(old_meter).encode('utf8'))
    hasher.update(dumps(pack(commitment)).encode('utf8'))

    # sign message
    sig = do_ecdsa_sign(G, unpack(meter_priv), hasher.digest())

    # return
    return {
        'outputs': (dumps(new_meter),),
        'extra_parameters' : (
            pack(commitment),
            pack(sig)
        )
    }

# ------------------------------------------------------------------
# compute_bill
# NOTE:
#   - only 'inputs', 'reference_inputs' and 'parameters' are used to the framework
#   - if there are more than 3 param, the checker has to be implemented by hand
# ------------------------------------------------------------------
@contract.method('compute_bill')
def compute_bill(inputs, reference_inputs, parameters, readings, openings, tariffs):

    # get meter
    meter = loads(inputs[0])

    # compute total bill
    G = setup()[0]
    total_bill   = sum(r*t for r,t in zip(loads(readings), loads(tariffs)))
    sum_openings = sum(o*t for o,t in zip(unpack(openings), loads(tariffs))) % G.order()

    # new bill
    bill = {
        'type'           : 'SMBill', 
        'info'           : meter['info'],
        'total_bill'     : total_bill,
        'billing_period' : meter['billing_period'],
        'tariffs'        : meter['tariffs']
    }

    # return
    return {
        'outputs': (dumps(bill),),
        'extra_parameters' : (
            dumps(total_bill),
            pack(sum_openings),
        )
    }

# ------------------------------------------------------------------
# read_bill
# ------------------------------------------------------------------
@contract.method('read')
def read(inputs, reference_inputs, parameters):

    # return
    return {
        'returns' : (reference_inputs[0],),
    }


####################################################################
# checker
####################################################################
# ------------------------------------------------------------------
# check meter's creation
# ------------------------------------------------------------------
@contract.checker('create_meter')
def create_meter_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:

        # loads data
        meter = loads(outputs[1])

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False
        if meter['pub'] == None or meter['info'] == None or meter['billing_period'] == None:
            return False
        if meter['readings'] == None or meter['tariffs'] == None:
            return False

        # check tokens
        if loads(inputs[0])['type'] != 'SMToken' or loads(outputs[0])['type'] != 'SMToken':
            return False
        if meter['type'] != 'SMMeter':
            return False

        # otherwise
        return True

    except (KeyError, Exception):
        return False

# ------------------------------------------------------------------
# check add reading
# ------------------------------------------------------------------
@contract.checker('add_reading')
def add_reading_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:

        # get objects
        old_meter = loads(inputs[0])
        new_meter = loads(outputs[0])

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 1 or len(returns) != 0:
            return False
        if old_meter['pub'] != new_meter['pub'] or old_meter['info'] != new_meter['info']:
            return False
        if old_meter['tariffs'] != new_meter['tariffs'] or old_meter['billing_period'] != new_meter['billing_period']:
            return False

        # check tokens
        if old_meter['type'] != new_meter['type']:
            return False

        # check readings' consistency
        if new_meter['readings'] != old_meter['readings'] + [parameters[0]]:
            return False

        # hash message to sign
        hasher = sha256()
        hasher.update(dumps(old_meter).encode('utf8'))
        hasher.update(dumps(parameters[0]).encode('utf8'))

        # verify signature
        G = setup()[0]
        pub = unpack(old_meter['pub'])
        sig = unpack(parameters[1])
        if not do_ecdsa_verify(G, pub, sig, hasher.digest()):
            return False

        # otherwise
        return True

    except (KeyError, Exception):
        return False

# ------------------------------------------------------------------
# check compute bill
# ------------------------------------------------------------------
@contract.checker('compute_bill')
def compute_bill_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:

        # get objects
        meter = loads(inputs[0])
        bill  = loads(outputs[0])

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 1 or len(returns) != 0:
            return False
        if meter['billing_period'] != bill['billing_period'] or meter['info'] != bill['info']:
            return False
        if meter['tariffs'] != bill['tariffs']:
            return False
        if bill['total_bill'] != loads(parameters[0]):
            return False

        # check tokens
        if bill['type'] != 'SMBill':
            return False

        # get objects
        tariffs      = bill['tariffs']
        commitements = meter['readings']
        total_bill   = loads(parameters[0])
        sum_openings = unpack(parameters[1])

        # verify bill
        (G, g, (h0, _, _, _), _) = setup()
        bill_commitment = G.infinite()
        for i in range(0, len(commitements)):
            bill_commitment = bill_commitment + tariffs[i] * unpack(commitements[i])

        if bill_commitment - sum_openings * h0 != total_bill * g:
            return False

        # otherwise
        return True

    except (KeyError, Exception):
        return False

# ------------------------------------------------------------------
# check read bill
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
