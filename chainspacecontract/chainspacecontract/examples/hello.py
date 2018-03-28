""" 
	Hello world contract.
"""

####################################################################
# imports
####################################################################

from json import dumps, loads
from chainspacecontract import ChainspaceContract

contract = ChainspaceContract('hello')


####################################################################
# methods
####################################################################

@contract.method('init')
def init():
	return { 'outputs': (dumps({'type' : 'HelloToken'}),) }


@contract.method('hello')
def hello(inputs, reference_inputs, parameters):

    # Checks that the first input is a token.
    if not (loads(inputs[0]) == {'type' : 'HelloToken'}):
        raise Exception("Expected a contract token as a first input.")

    # Create a new "HelloMessage" object, just containing a message.
    HelloMessage = dumps({
        'type' : 'HelloMessage',
        'message' : 'Hello, world!'
    })

    # Return both a fresh token and a HelloMessage.
    return { 'outputs': (inputs[0], HelloMessage) }


####################################################################
# checker
####################################################################

@contract.checker('hello')
def hello_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    try:
        ret = True

        # parse inputs
        ret &= reference_inputs == () and returns == () and dependencies == []

        # check input format
        ret &= len(inputs) == 1
        ret &= len(outputs) == 2

        token = loads(inputs[0])
        instance = loads(outputs[1])

        # check types
        ret &=  token['type'] == 'HelloToken'
        ret &= inputs[0] == outputs[0]
        ret &=  instance['type'] == 'HelloMessage'

        # check content
        ret &=  instance['message'] == 'Hello, world!'
        return ret
    except:
        return False


####################################################################
# main
####################################################################
if __name__ == '__main__':
    contract.run()

####################################################################