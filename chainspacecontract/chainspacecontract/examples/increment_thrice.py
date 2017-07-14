"""An example smart contract to demonstrate three layer deep cross-contract
calls."""

from chainspacecontract import ChainspaceContract
from chainspacecontract.examples.increment_twice import contract as increment_twice_contract

contract = ChainspaceContract('increment_thrice')
contract.register_dependency(increment_twice_contract)


@contract.method('init')
def init():
    return {
        'outputs': (0,)
    }


@contract.method('increment')
def increment(inputs, reference_inputs, parameters):
    increment_twice_contract.increment((parameters['passed_integer_b'],), None, {'passed_integer': parameters['passed_integer_a']})
    return {
        'outputs': (inputs[0] + 1,)
    }

if __name__ == '__main__':
    contract.run()
