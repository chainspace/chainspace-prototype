"""A simple smart contract which keeps track of an integer that can be
incremented, with a custom checker."""

from chainspacecontract import ChainspaceContract

contract = ChainspaceContract('addition')


@contract.method('init')
def init():
    return {
        'outputs': (0,)
    }


@contract.method('increment')
def increment(inputs, reference_inputs, parameters):
    return {
        'outputs': (inputs[0] + 1,)
    }


@contract.checker('increment')
def increment_checker(inputs, reference_inputs, parameters, outputs, returns, dependencies):
    result = increment(inputs, reference_inputs, parameters)
    return result['outputs'] == outputs and result['returns'] == returns

if __name__ == '__main__':
    contract.run()
