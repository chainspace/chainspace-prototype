"""An example smart contract to demonstrate cross-contract calls."""

from chainspacecontract import ChainspaceContract
from chainspacecontract.examples.increment import contract as increment_contract

contract = ChainspaceContract('increment_twice')
contract.register_dependency(increment_contract)


@contract.method('init')
def init():
    return {
        'outputs': ('0',)
    }


@contract.method('increment')
def increment(inputs, reference_inputs, parameters):
    integer = int(inputs[0])
    increment_contract.increment((parameters['passed_integer'],), None, None)
    return {
        'outputs': (str(integer + 1),)
    }

if __name__ == '__main__':
    contract.run()
