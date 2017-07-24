"""A simple smart contract which keeps track of an integer that can be
incremented."""

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
        'outputs': (str(integer + 1),)
    }

if __name__ == '__main__':
    contract.run()
