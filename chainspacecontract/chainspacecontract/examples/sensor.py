"""A smart contract that implements a simple, authenticated bank."""

####################################################################
# imports
####################################################################
# general
from hashlib import sha256
from json    import dumps, loads
# chainspace
from chainspacecontract import ChainspaceContract

## contract name
contract = ChainspaceContract('sensor')


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
        'outputs': (dumps({'type' : 'SensorToken'}),),
    }


# ------------------------------------------------------------------
# create account
# ------------------------------------------------------------------
@contract.method('create_sensor')
def create_sensor(inputs, reference_inputs, parameters):

    # new account
    new_sensor = {'type' : 'SensorData', 'values': []}

    # return
    return {
        'outputs': (inputs[0], dumps(new_sensor))
    }


# ------------------------------------------------------------------
# make transfer
# ------------------------------------------------------------------
@contract.method('add_data')
def add_data(inputs, reference_inputs, parameters):

    # compute outputs
    added_data = loads(parameters[0])
    new_sensor = loads(inputs[0])
    new_sensor['values'] += added_data

    # return
    return {
        'outputs': (dumps(new_sensor),),
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
# checker
####################################################################
# ------------------------------------------------------------------
# check account's creation
# ------------------------------------------------------------------
@contract.checker('create_sensor')
def create_sensor_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:

        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False

        # check tokens
        if loads(inputs[0])['type'] != 'SensorToken' or loads(outputs[0])['type'] != 'SensorToken':
            return False
        if loads(outputs[1])['type'] != 'SensorData':
            return False

        # return
        return True

    except (KeyError, Exception):
        return False


# ------------------------------------------------------------------
# check transfer
# ------------------------------------------------------------------
@contract.checker('add_data')
def add_data_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:
        # load data
        old_data   = loads(inputs[0])['values']
        added_data = loads(parameters[0])
        new_data   = loads(outputs[0])['values']
        
        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 1 or len(returns) != 0:
            return False

        # check tokens
        if loads(inputs[0])['type'] != 'SensorData' or loads(outputs[0])['type'] != 'SensorData':
            return False

        # check list
        if new_data != old_data + added_data:
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
