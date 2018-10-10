""" 
	New Hello world contract.
"""

####################################################################
# imports
####################################################################

from json import dumps, loads
from chainspacecontract import ChainspaceContract

contract = ChainspaceContract('new_hello')


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

    # Create a label for the conversation
    conversation = dumps({
                         'conversationID': 1
                         })

    # Return both a fresh token and a HelloMessage.
    return { 'outputs': (inputs[0], HelloMessage), 'labels': (conversation,) }


@contract.method('reply')
def reply(inputs, reference_inputs, parameters):
    
    # Checks that the first input is a hello message.
    if not (loads(inputs[0])['type'] == 'HelloMessage'):
        raise Exception("Expected a contract a hello message as a first input.")
    
    # Create a new "HelloMessage" object, just containing a message reply.
    HelloMessage = dumps({
                         'type' : 'HelloMessage',
                         'message' : loads(inputs[0])['message'] + ' Hello, again!'
                         })

    # Create a new label for the conversation
    conversation = dumps({
                         'conversationID': 1
                         })

    # Return both a fresh token and a HelloMessage.
    return { 'outputs': (HelloMessage,), 'labels': (conversation,) }


####################################################################
# checker
####################################################################

@contract.checker('hello')
def hello_checker(inputs, reference_inputs, parameters, outputs, labels, returns, dependencies):
    try:
        ret = True
        
        print('labels: ', labels)

        # parse inputs
        ret &= reference_inputs == () and returns == () and dependencies == []

        # check input format
        ret &= len(inputs) == 1
        ret &= len(outputs) == 2

        token = loads(inputs[0])
        instance = loads(outputs[1])
        tags = loads(labels[0])

        # check types
        ret &= token['type'] == 'HelloToken'
        ret &= inputs[0] == outputs[0]
        ret &= instance['type'] == 'HelloMessage'
        ret &= tags['conversationID'] == 1

        # check content
        ret &= instance['message'] == 'Hello, world!'
        return ret
    except:
        return False


@contract.checker('reply')
def reply_checker(inputs, reference_inputs, parameters, outputs, labels, returns, dependencies):
    try:
        ret = True
        
        # parse inputs
        ret &= reference_inputs == () and returns == () and dependencies == []
        
        # check input format
        ret &= len(inputs) == 1
        ret &= len(outputs) == 1
        
        instance = loads(outputs[0])
        tags = loads(labels[0])
        
        # check types
        ret &= instance['type'] == 'HelloMessage'
        ret &= tags['conversationID'] == 1

        # check content
        ret &= instance['message'] == loads(inputs[0])['message'] + ' Hello, again!'
        return ret
    except:
        return False


####################################################################
# main
####################################################################
if __name__ == '__main__':
    contract.run()

####################################################################
