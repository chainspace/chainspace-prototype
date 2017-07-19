"""A simple smart contract which keeps track of an integer that can be
incremented, with a custom checker."""

from chainspacecontract import ChainspaceContract

contract = ChainspaceContract('addition')


@contract.method('init')
def init():
    return {
        'outputs': ('0',)
    }


@contract.method('increment')
def increment(inputs, reference_inputs, parameters):
    integer = int(inputs[0])
    return {
        'outputs': (integer + 1,)
    }


@contract.checker('increment')
def increment_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    if (len(inputs) == 1
        and len(outputs) == 1
        and int(inputs[0]) + 1 == int(outputs[0])):
        return True
    else:
        return False

if __name__ == '__main__':
    contract.run()
