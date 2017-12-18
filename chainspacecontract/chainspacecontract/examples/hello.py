""" 
	Hello world contract.
"""


####################################################################
# imports
####################################################################
# general
from json import dumps, loads
# chainspace
from chainspacecontract import ChainspaceContract

## contract name
contract = ChainspaceContract('hello')


####################################################################
# methods
####################################################################
# ------------------------------------------------------------------
# init
# ------------------------------------------------------------------
@contract.method('init')
def init():
	return {
	    'outputs': (dumps({'type' : 'HelloToken'}),),
	}

# ------------------------------------------------------------------
# hello
# ------------------------------------------------------------------
@contract.method('hello')
def hello(inputs, reference_inputs, parameters):
    instance = {
        'type' : 'HelloMessage',
        'message' : 'Hello, world!'
    }

    # return
    return {
        'outputs': (inputs[0], dumps(instance)),
    }


####################################################################
# checker
####################################################################
# ------------------------------------------------------------------
# check hello
# ------------------------------------------------------------------
@contract.checker('hello')
def create_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:
        # retrieve instances
        token = loads(inputs[0])
        instance = loads(outputs[1])
        
        # check format
        if len(inputs) != 1 or len(reference_inputs) != 0 or len(outputs) != 2 or len(returns) != 0:
            return False 

        # check types
        if token['type'] != 'HelloToken' or inputs[0] != outputs[0]: return False
        if instance['type'] != 'HelloMessage': return False

        # check content
        if instance['message'] != 'Hello, world!': return False
   
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